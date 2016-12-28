package com.peterlaurence.trekadvisor.core.map;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.map.gson.RuntimeTypeAdapterFactory;
import com.peterlaurence.trekadvisor.core.projection.MercatorProjection;
import com.peterlaurence.trekadvisor.core.projection.Projection;
import com.peterlaurence.trekadvisor.core.projection.UniversalTransverseMercator;
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderDummy;
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderLibVips;
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderOsm;
import com.peterlaurence.trekadvisor.util.FileUtils;
import com.qozix.tileview.graphics.BitmapProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Singleton object that provides utility methods to read json files that describe each map.
 * <p/>
 * A {@link MapUpdateTask} will :
 * <ul>
 * <li>
 * Search for maps on the SD card (json files).
 * </li>
 * <li>
 * Parse the json files to, e.g, process calibration information.
 * </li>
 * <li>
 * populate the internal list of {@link Map}.
 * </li>
 * </ul>
 *
 * @author peterLaurence
 */
public class MapLoader implements MapImporter.MapParseListener {

    private static final String APP_FOLDER_NAME = "trekadvisor";
    static final String MAP_FILE_NAME = "map.json";

    private Gson mGson;
    private static final File DEFAULT_APP_DIR = new File(Environment.getExternalStorageDirectory(),
            APP_FOLDER_NAME);
    /* For instance maps are searched anywhere under the app folder */
    private static final File DEFAULT_MAPS_DIR = DEFAULT_APP_DIR;

    private List<Map> mMapList;
    private List<MapListUpdateListener> mMapListUpdateListeners;
    private List<MapArchive> mMapArchiveList;
    private List<MapArchiveListUpdateListener> mMapArchiveListUpdateListeners;

    /* Singleton implementation */
    private static class SingletonHolder {
        private static final MapLoader instance = new MapLoader();
    }

    /**
     * All {@link Projection}s are registered here.
     */
    public static final HashMap<String, Class<? extends Projection>> PROJECTION_HASH_MAP = new HashMap<String, Class<? extends Projection>>() {{
        put(MercatorProjection.NAME, MercatorProjection.class);
        put(UniversalTransverseMercator.NAME, UniversalTransverseMercator.class);
    }};

    public enum CALIBRATION_METHOD {
        SIMPLE_2_POINTS,
        UNKNOWN;

        public static CALIBRATION_METHOD fromCalibrationName(String name) {
            if (name != null) {
                for (CALIBRATION_METHOD method : CALIBRATION_METHOD.values()) {
                    if (name.equalsIgnoreCase(method.toString())) {
                        return method;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Create once for all the {@link Gson} object, that is used to serialize/deserialize json content.
     * Register all {@link Projection} types, depending on their name.
     */
    private MapLoader() {
        RuntimeTypeAdapterFactory<Projection> factory = RuntimeTypeAdapterFactory.of(
                Projection.class, "projection_name");
        for (java.util.Map.Entry<String, Class<? extends Projection>> entry : PROJECTION_HASH_MAP.entrySet()) {
            factory.registerSubtype(entry.getValue(), entry.getKey());
        }
        mGson = new GsonBuilder().serializeNulls().setPrettyPrinting().
                registerTypeAdapterFactory(factory).create();

        mMapListUpdateListeners = new ArrayList<>();
        mMapArchiveListUpdateListeners = new ArrayList<>();
    }

    public static MapLoader getInstance() {
        return SingletonHolder.instance;
    }

    public interface MapListUpdateListener {
        void onMapListUpdate(boolean mapsFound);
    }

    public interface MapArchiveListUpdateListener {
        void onMapArchiveListUpdate();
    }

    private static class MapUpdateTask extends AsyncTask<File, Void, Void> {
        List<MapListUpdateListener> mListenerList;
        Gson mGson;
        List<Map> mMapList;

        private List<File> mapFilesFoundList;
        private static final int MAX_RECURSION_DEPTH = 6;

        public MapUpdateTask(@Nullable List<MapListUpdateListener> listener,
                             Gson gson,
                             List<Map> mapList) {
            super();
            mListenerList = listener;
            mGson = gson;
            mMapList = mapList;
            mapFilesFoundList = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(File... dirs) {
            /* Search for json files on SD card */
            for (File dir : dirs) {
                findMaps(dir, 1);
            }

            /* Now parse the json files found */
            for (File f : mapFilesFoundList) {
                /* Get json file content as String */
                String jsonString;
                try {
                    jsonString = FileUtils.getStringFromFile(f);
                } catch (Exception e) {
                    // Error while decoding the json file
                    e.printStackTrace();
                    continue;
                }

                try {
                    /* json deserialization */
                    MapGson mapGson = mGson.fromJson(jsonString, MapGson.class);

                    Map map = new Map(mapGson, f, new File(f.getParent(), mapGson.thumbnail));

                    /* Calibration */
                    map.calibrate();

                    /* Set BitMapProvider */
                    map.setBitmapProvider(makeBitmapProvider(map));

                    mMapList.add(map);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private void findMaps(File root, int depth) {
            if (depth > MAX_RECURSION_DEPTH) return;

            File[] list = root.listFiles();
            if (list == null) {
                return;
            }

            for (File f : list) {
                if (f.isDirectory()) {
                    File jsonFile = new File(f, MAP_FILE_NAME);

                    /* Don't allow nested maps */
                    if (jsonFile.exists() && jsonFile.isFile()) {
                        mapFilesFoundList.add(jsonFile);
                    } else {
                        findMaps(f, depth + 1);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mListenerList != null) {
                for (MapListUpdateListener listUpdateListener : mListenerList) {
                    listUpdateListener.onMapListUpdate(mMapList.size() > 0);
                }
            }
        }
    }

    private static class MapArchiveSearchTask extends AsyncTask<File, Void, Void> {
        private static final int MAX_RECURSION_DEPTH = 6;
        private List<MapArchiveListUpdateListener> mMapArchiveListUpdateListeners;
        private List<MapArchive> mMapArchiveList;
        private List<File> mMapArchiveFilesFoundList;
        private static final List<String> mArchiveFormatList;

        static {
            mArchiveFormatList = new ArrayList<>();
            mArchiveFormatList.add("zip");
        }

        public MapArchiveSearchTask(@Nullable List<MapArchiveListUpdateListener> listeners, List<MapArchive> mapArchiveList) {
            super();
            mMapArchiveList = mapArchiveList;
            mMapArchiveFilesFoundList = new ArrayList<>();
            mMapArchiveListUpdateListeners = listeners;
        }

        @Override
        protected Void doInBackground(File... dirs) {
            /* Search for archive files on SD card */
            for (File dir : dirs) {
                findArchives(dir, 1);
            }

            for (File archiveFile : mMapArchiveFilesFoundList) {
                mMapArchiveList.add(new MapArchive(archiveFile));
            }

            return null;
        }

        private void findArchives(File root, int depth) {
            if (depth > MAX_RECURSION_DEPTH) return;

            File[] list = root.listFiles();
            if (list == null) {
                return;
            }

            for (File f : list) {
                if (f.isDirectory()) {
                    File jsonFile = new File(f, MAP_FILE_NAME);

                    /* Don't allow archives inside maps */
                    if (!jsonFile.exists()) {
                        findArchives(f, depth + 1);
                    }
                } else {
                    int index = f.getName().lastIndexOf('.');
                    if (index > 0) {
                        try {
                            String ext = f.getName().substring(index + 1);
                            if (mArchiveFormatList.contains(ext)) {
                                mMapArchiveFilesFoundList.add(f);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            // don't care
                        }
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mMapArchiveListUpdateListeners != null) {
                for (MapArchiveListUpdateListener listener : mMapArchiveListUpdateListeners) {
                    listener.onMapArchiveListUpdate();
                }
            }
        }
    }

    /**
     * Update the internal list of {@link Map} : {@code mMapList}. Once done, all of the registered
     * {@link MapListUpdateListener} are called.
     * @param dirs The directories in which to search for maps. If not specified, a default value is
     *             taken.
     */
    public void generateMaps(File... dirs) {
        mMapList = new ArrayList<>();

        MapUpdateTask updateTask = new MapUpdateTask(mMapListUpdateListeners, mGson, mMapList);
        if (dirs.length == 0) { // No directories specified? We take the default value.
            dirs = new File[1];
            dirs[0] = DEFAULT_MAPS_DIR;
        }
        updateTask.execute(dirs);
    }

    public List<Map> getMaps() {
        return mMapList;
    }

    /**
     * Update the internal list of {@link MapArchive} : {@code mMapArchiveList}. Once done, all of
     * the registered {@link MapArchiveListUpdateListener} are called.
     * @param dirs The directories in which to search for map archives. If not specified, a default
     *             value is taken.
     */
    public void generateMapArchives(File... dirs) {
        mMapArchiveList = new ArrayList<>();

        MapArchiveSearchTask searchTask = new MapArchiveSearchTask(mMapArchiveListUpdateListeners, mMapArchiveList);
        if (dirs.length == 0) { // No directories specified? We take the default value.
            dirs = new File[1];
            dirs[0] = DEFAULT_APP_DIR;
        }
        searchTask.execute(dirs);
    }

    public List<MapArchive> getMapArchives() {
        return mMapArchiveList;
    }

    /**
     * Get a {@link Map} given its name.
     *
     * @param name the name of the {@link Map} to retrieve.
     */
    @Nullable
    public Map getMap(String name) {
        for (Map map : mMapList) {
            if (map.getName().equalsIgnoreCase(name)) {
                return map;
            }
        }
        return null;
    }

    public void addMapListUpdateListener(MapListUpdateListener listener) {
        mMapListUpdateListeners.add(listener);
    }

    public void addMapArchiveListUpdateListener(MapArchiveListUpdateListener listener) {
        mMapArchiveListUpdateListeners.add(listener);
    }

    /**
     * Add a {@link Map} to the internal list and generated the json file. <br>
     * This is typically called after an import, after a {@link Map} has been generated from a file
     * structure.
     */
    @Override
    public void onMapParsed(Map map) {
        /* Set BitMapProvider */
        map.setBitmapProvider(makeBitmapProvider(map));

        /* Add the map */
        if (mMapList != null) {
            mMapList.add(map);
        }

        /* Generate the json file */
        saveMap(map);

        /* Notify for view update */
        if (mMapListUpdateListeners != null) {
            for (MapListUpdateListener listUpdateListener : mMapListUpdateListeners) {
                listUpdateListener.onMapListUpdate(mMapList.size() > 0);
            }
        }
    }

    @Override
    public void onError(MapImporter.MapParseException e) {
        e.printStackTrace();
    }

    /**
     * Constructs the {@link BitmapProvider} depending on the origin of the map.
     *
     * @param map The {@link Map} object
     * @return The {@link BitmapProvider} or a {@link BitmapProviderDummy} if the origin is unknown.
     */
    private static BitmapProvider makeBitmapProvider(Map map) {
        switch (map.getOrigin()) {
            case BitmapProviderLibVips.GENERATOR_NAME:
                return new BitmapProviderLibVips(map);
            case BitmapProviderOsm.GENERATOR_NAME:
                return new BitmapProviderOsm(map);
            default:
                return new BitmapProviderDummy();
        }
    }

    /**
     * Save the content of a {@link Map}, so the changes persist upon application restart.
     * <p>
     * Here, it writes to the corresponding json file.
     * </p>
     *
     * @param map The {@link Map} to save.
     */
    public void saveMap(Map map) {
        String jsonString = mGson.toJson(map.getMapGson());

        File configFile = map.getConfigFile();
        try {
            PrintWriter writer = new PrintWriter(configFile);
            writer.print(jsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mutate the {@link Projection} of a given {@link Map}.
     *
     * @return true on success, false if something went wrong.
     */
    public boolean mutateMapProjection(Map map, String projectionName) {
        Class<? extends Projection> projectionType = PROJECTION_HASH_MAP.get(projectionName);
        try {
            Projection projection = projectionType.newInstance();
            map.setProjection(projection);
        } catch (InstantiationException | IllegalAccessException e) {
            // wrong projection name
            return false;
        }
        return true;
    }
}
