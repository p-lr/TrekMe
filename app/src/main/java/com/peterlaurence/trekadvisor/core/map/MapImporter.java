package com.peterlaurence.trekadvisor.core.map;

import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderLibVips;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The {@link MapImporter} is created from a {@link MapProvider}. Then, the corresponding map parser
 * is used to parse the map (given as a {@link File}). <br>
 * This is typically used after a {@link MapArchive} has been extracted.
 *
 * @author peterLaurence on 23/06/16.
 */
public class MapImporter {
    private static final java.util.Map<MapProvider, MapParser> mProviderToParserMap;

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
    }

    static {
        java.util.Map<MapProvider, MapParser> map = new HashMap<>();
        map.put(MapProvider.LIBVIPS, new LibvipsMapParser());
        mProviderToParserMap = Collections.unmodifiableMap(map);
    }

    /* Don't allow instantiation */
    private MapImporter() {
    }

    public static
    @Nullable
    Map importFromFile(File file, MapProvider provider) {
        MapParser parser = mProviderToParserMap.get(provider);
        if (parser != null) {
            try {
                Map map = parser.parse(file);
                return map;
            } catch (MapParseException e) {
                // TODO : properly handle this and inform the user.
            }
        }
        return null;
    }

    /**
     * An Exception thrown when an error occurred in a {@link MapParser}.
     */
    static class MapParseException extends Exception {
        private Issue mIssue;

        enum Issue {
            NO_LEVEL_FOUND,
            UNKNOWN_IMAGE_EXT
        }

        MapParseException(Issue issue) {
            mIssue = issue;
        }
    }

    /**
     * This {@link MapParser} expects a directory {@link File} being the first parent of all files
     * produced by libvips.
     */
    private static class LibvipsMapParser implements MapParser {
        private java.util.Map<Integer, Integer> mTileSizeForLevelMap;
        private BitmapFactory.Options options = new BitmapFactory.Options();

        LibvipsMapParser() {
            options.inJustDecodeBounds = true;
        }

        @Override
        public Map parse(File file) throws MapParseException {
            if (!file.isDirectory()) {
                return null;
            }

            MapGson mapGson = new MapGson();

            /* Create levels */
            List<MapGson.Level> levelList = new ArrayList<>();
            int maxLevel = getMaxLevel(file);
            File levelDir = null;
            for (int i = 0; i <= maxLevel; i++) {
                levelDir = new File(file, String.valueOf(i));
                if (levelDir.exists()) {
                    MapGson.Level.TileSize tileSize = getTileSize(levelDir);
                    MapGson.Level level = new MapGson.Level();
                    level.level = i;
                    level.tile_size = tileSize;
                    levelList.add(level);
                    System.out.println("creating level " + i + " tileSize " + tileSize.x);
                }
            }

            if (levelDir == null) {
                throw new MapParseException(MapParseException.Issue.NO_LEVEL_FOUND);
            }

            /* Create provider */
            MapGson.Provider provider = new MapGson.Provider();
            provider.generated_by = BitmapProviderLibVips.GENERATOR_NAME;
            provider.image_extension = getImageExtension(levelDir);

            return null;
        }

        /* Get the maximum zoom level */
        private int getMaxLevel(File file) {
            int maxLevel = 0;
            int level;
            for (File f : file.listFiles()) {
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

        private
        @Nullable
        MapGson.Level.TileSize getTileSize(File levelDir) {
            File[] imageFiles = levelDir.listFiles();
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

        private
        @Nullable
        String getImageExtension(File levelDir) {
            File[] imageFiles = levelDir.listFiles();
            if (imageFiles != null && imageFiles.length > 0) {
                String anImagePath = imageFiles[0].getPath();
                String ext = anImagePath.substring(anImagePath.lastIndexOf(".") + 1);
                if (ext.length() > 0) {
                    return ext;
                }
            }
            return null;
        }
    }
}
