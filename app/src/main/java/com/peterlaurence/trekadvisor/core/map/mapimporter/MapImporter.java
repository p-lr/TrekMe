package com.peterlaurence.trekadvisor.core.map.mapimporter;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapArchive;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderLibVips;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The {@link MapImporter} is created by a {@link MapProvider}. Then, the corresponding map parser
 * is used to parse the map (given as a {@link File}). <br>
 * This is typically used after a {@link MapArchive} has been extracted.
 *
 * @author peterLaurence on 23/06/16.
 */
public class MapImporter {
    private static final java.util.Map<MapProvider, MapParser> mProviderToParserMap;
    private static final int THUMBNAIL_ACCEPT_SIZE = 256;
    private static final String[] IMAGE_EXTENSIONS = new String[]{
            "jpg", "gif", "png", "bmp", "webp"
    };

    private static final String TAG = "MapImporter";

    private static final FilenameFilter THUMBNAIL_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            for (final String ext : IMAGE_EXTENSIONS) {
                if (filename.endsWith("." + ext)) {
                    return true;
                }
            }
            return false;
        }
    };

    private static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            /* We only look at files */
            if (new File(dir, filename).isDirectory()) {
                return false;
            }

            boolean accept = true;
            for (final String ext : IMAGE_EXTENSIONS) {
                if (!filename.endsWith("." + ext)) {
                    accept = false;
                }
                try {
                    Integer.parseInt(filename.substring(0, filename.lastIndexOf(".")));
                } catch (Exception e) {
                    accept = false;
                }
                if (accept) return true;
            }
            return false;
        }
    };

    private static final FilenameFilter DIR_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            return new File(dir, filename).isDirectory();
        }
    };

    static {
        java.util.Map<MapProvider, MapParser> map = new HashMap<>();
        map.put(MapProvider.LIBVIPS, new LibvipsMapParser());
        mProviderToParserMap = Collections.unmodifiableMap(map);
    }

    /* Don't allow instantiation */
    private MapImporter() {
    }

    public static void importFromFile(File dir, MapProvider provider, MapImportListener listener) {
        MapParser parser = mProviderToParserMap.get(provider);
        if (parser != null) {
            MapParseTask mapParseTask = new MapParseTask(parser, dir, listener);
            mapParseTask.execute();
        }
    }

    /**
     * Possible {@link Map} providers.
     */
    public enum MapProvider {
        LIBVIPS
    }

    /**
     * Produces a {@link Map} from a given {@link File}.
     */
    private interface MapParser {
        @Nullable
        Map parse(File file) throws MapParseException;

        MapParserStatus getStatus();
    }

    public enum MapParserStatus {
        NEW_MAP,        // a new map was successfully created
        EXISTING_MAP,   // a map.json file was found
    }

    /**
     * When a {@link Map} is parsed for {@link MapGson} object creation, a {@link MapImportListener}
     * is called after the task is done.
     */
    public interface MapImportListener {
        void onMapImported(Map map, MapParserStatus status);

        void onMapImportError(MapParseException e);
    }

    /**
     * An Exception thrown when an error occurred in a {@link MapParser}.
     */
    public static class MapParseException extends Exception {
        private Issue mIssue;

        MapParseException(Issue issue) {
            mIssue = issue;
        }

        enum Issue {
            NO_PARENT_FOLDER_FOUND,
            NO_LEVEL_FOUND,
            UNKNOWN_IMAGE_EXT,
            MAP_SIZE_INCORRECT
        }
    }

    private static class MapParseTask extends AsyncTask<Void, Void, Map> {
        private MapParser mMapParser;
        private WeakReference<File> mDirWeakReference;
        private WeakReference<MapImportListener> mMapParseListenerWeakReference;
        private MapParseException mException;

        MapParseTask(MapParser parser, File dir, MapImportListener listener) {
            mMapParser = parser;
            mDirWeakReference = new WeakReference<>(dir);
            mMapParseListenerWeakReference = new WeakReference<>(listener);
        }

        @Override
        protected Map doInBackground(Void... params) {
            File dir = null;
            if (mDirWeakReference != null) {
                dir = mDirWeakReference.get();
            }

            if (dir == null) return null;

            try {
                return mMapParser.parse(dir);
            } catch (MapParseException e) {
                mException = e;
            }
            return null;
        }

        /**
         * Call back the provided {@link MapImportListener}.
         */
        @Override
        protected void onPostExecute(Map map) {
            if (mException != null) {
                MapLoader.getInstance().onMapImportError(mException);
            } else {
                MapLoader.getInstance().onMapImported(map, mMapParser.getStatus());
            }

            if (mMapParseListenerWeakReference != null) {
                MapImportListener mapImportListener = mMapParseListenerWeakReference.get();
                if (mapImportListener != null) {
                    if (mException != null) {
                        mapImportListener.onMapImportError(mException);
                    } else {
                        mapImportListener.onMapImported(map, mMapParser.getStatus());
                    }
                }
            }
        }
    }

    /**
     * This {@link MapParser} expects a directory {@link File} being the first parent of all files
     * produced by libvips.
     */
    private static class LibvipsMapParser implements MapParser {
        private static final String THUMBNAIL_EXCLUDE_NAME = "blank";
        private BitmapFactory.Options options = new BitmapFactory.Options();
        private MapParserStatus mStatus;

        LibvipsMapParser() {
            options.inJustDecodeBounds = true;
        }

        @Override
        public Map parse(File mapDir) throws MapParseException {
            if (!mapDir.isDirectory()) {
                return null;
            }

            /* Find the first image */
            File imageFile = findFirstImage(mapDir, 0, 5);

            /* .. use it to deduce the parent folder */
            File parentFolder = findParentFolder(imageFile);

            if (parentFolder == null) {
                throw new MapParseException(MapParseException.Issue.NO_PARENT_FOLDER_FOUND);
            }

            /* Check whether there is already a map.json file or not */
            File existingJsonFile = new File(parentFolder, MapLoader.MAP_FILE_NAME);
            if (existingJsonFile.exists()) {
                MapLoader.getInstance().generateMaps(parentFolder);
                mStatus = MapParserStatus.EXISTING_MAP;
                return null;
            }

            /* Create levels */
            List<MapGson.Level> levelList = new ArrayList<>();
            int maxLevel = getMaxLevel(parentFolder);
            File levelDir = null;
            MapGson.Level.TileSize lastLevelTileSize = null;  // used later, for the map size
            for (int i = 0; i <= maxLevel; i++) {
                levelDir = new File(parentFolder, String.valueOf(i));
                if (levelDir.exists()) {
                    MapGson.Level.TileSize tileSize = getTileSize(levelDir);
                    lastLevelTileSize = tileSize;
                    MapGson.Level level = new MapGson.Level();
                    level.level = i;
                    level.tile_size = tileSize;
                    levelList.add(level);
                    Log.d(TAG, "creating level " + i + " tileSize " + tileSize.x);
                }
            }

            if (levelDir == null) {
                throw new MapParseException(MapParseException.Issue.NO_LEVEL_FOUND);
            }

            MapGson mapGson = new MapGson();
            mapGson.levels = levelList;

            /* Create provider */
            MapGson.Provider provider = new MapGson.Provider();
            provider.generated_by = BitmapProviderLibVips.GENERATOR_NAME;
            provider.image_extension = getImageExtension(imageFile);
            mapGson.provider = provider;

            /* Map size */
            if (lastLevelTileSize == null) {
                throw new MapParseException(MapParseException.Issue.NO_LEVEL_FOUND);
            }
            mapGson.size = computeMapSize(levelDir, lastLevelTileSize);
            if (mapGson.size == null) {
                throw new MapParseException(MapParseException.Issue.MAP_SIZE_INCORRECT);
            }

            /* Find a thumnail */
            File thumbnail = getThumbnail(parentFolder);
            mapGson.thumbnail = thumbnail != null ? thumbnail.getName() : null;

            /* Set the map name to the parent folder name */
            mapGson.name = parentFolder.getName();

            /* Set default calibration */
            mapGson.calibration = new MapGson.Calibration();
            mapGson.calibration.calibration_method = MapLoader.CALIBRATION_METHOD.SIMPLE_2_POINTS.name();

            /* The json file */
            File jsonFile = new File(parentFolder, MapLoader.MAP_FILE_NAME);

            mStatus = MapParserStatus.NEW_MAP;
            return new Map(mapGson, jsonFile, thumbnail);
        }

        @Override
        public MapParserStatus getStatus() {
            return mStatus;
        }

        /**
         * The map can be contained in a subfolder inside the given directory.
         *
         * @param imageFile an image in the file structure.
         */
        private
        @Nullable
        File findParentFolder(File imageFile) {
            if (imageFile != null) {
                try {
                    File parentFolder = imageFile.getParentFile().getParentFile().getParentFile();
                    if (parentFolder.isDirectory()) {
                        return parentFolder;
                    }
                } catch (NullPointerException e) {
                    // don't care, will return null
                }
            }
            return null;
        }

        /**
         * Find the first image which is named with an integer (so this excludes the thumbnail).
         */
        private File findFirstImage(File dir, int depth, int maxDepth) {
            if (depth > maxDepth) return null;

            File listFile[] = dir.listFiles();
            if (listFile != null) {
                for (File aListFile : listFile) {
                    if (aListFile.isDirectory()) {
                        File found = findFirstImage(aListFile, depth++, maxDepth);
                        if (found != null) {
                            return found;
                        }
                    } else {
                        File listImage[] = dir.listFiles(IMAGE_FILTER);
                        if (listImage.length > 0) {
                            return listImage[0];
                        }
                    }
                }
            }
            return null;
        }

        /* Get the maximum zoom level */
        private int getMaxLevel(File mapDir) {
            int maxLevel = 0;
            int level;
            for (File f : mapDir.listFiles()) {
                if (f.isDirectory()) {
                    try {
                        level = Integer.parseInt(f.getName());
                        maxLevel = level > maxLevel ? level : maxLevel;
                    } catch (NumberFormatException e) {
                        // an unknown folder, ignore it.
                    }
                }
            }
            return maxLevel;
        }

        /* We assume that the tile size is constant at a given zoom level */
        private
        @Nullable
        MapGson.Level.TileSize getTileSize(File levelDir) {
            File[] lineDirList = levelDir.listFiles(DIR_FILTER);
            if (lineDirList.length == 0) {
                return null;
            }

            /* take the first line */
            File lineDir = lineDirList[0];
            File[] imageFiles = lineDir.listFiles(IMAGE_FILTER);
            if (imageFiles != null && imageFiles.length > 0) {
                File anImage = imageFiles[0];
                BitmapFactory.decodeFile(anImage.getPath(), options);
                MapGson.Level.TileSize tileSize = new MapGson.Level.TileSize();
                tileSize.x = options.outWidth;
                tileSize.y = options.outHeight;
                return tileSize;
            }

            return null;
        }

        /**
         * Get the image extension, width the dot. For example : ".jpg"
         */
        private
        @Nullable
        String getImageExtension(File imageFile) {
            String imagePath = imageFile.getPath();
            String ext = imagePath.substring(imagePath.lastIndexOf("."));
            if (ext.length() > 0) {
                return ext;
            }

            return null;
        }

        private
        @Nullable
        File getThumbnail(File mapDir) {
            for (File imageFile : mapDir.listFiles(THUMBNAIL_FILTER)) {
                BitmapFactory.decodeFile(imageFile.getPath(), options);
                if (options.outWidth == THUMBNAIL_ACCEPT_SIZE &&
                        options.outHeight == THUMBNAIL_ACCEPT_SIZE) {
                    if (!imageFile.getName().toLowerCase().contains(THUMBNAIL_EXCLUDE_NAME.toLowerCase())) {
                        return imageFile;
                    }
                }
            }
            return null;
        }

        private
        @Nullable
        MapGson.MapSize computeMapSize(File lastLevel, MapGson.Level.TileSize lastLevelTileSize) {
            File[] lineDirList = lastLevel.listFiles(DIR_FILTER);
            if (lineDirList == null || lineDirList.length == 0) {
                return null;
            }
            /* Only look into the first line */
            int rowCount = lineDirList.length;
            File[] imageFiles = lineDirList[0].listFiles(IMAGE_FILTER);
            int columnCount = imageFiles.length;
            if (columnCount == 0) {
                return null;
            }

            MapGson.MapSize mapSize = new MapGson.MapSize();
            mapSize.x = columnCount * lastLevelTileSize.x;
            mapSize.y = rowCount * lastLevelTileSize.y;
            return mapSize;
        }
    }
}
