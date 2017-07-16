package com.peterlaurence.trekadvisor.core.map.maploader;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapArchive;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson;
import com.peterlaurence.trekadvisor.core.map.gson.RuntimeTypeAdapterFactory;
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter;
import com.peterlaurence.trekadvisor.core.map.maploader.tasks.MapArchiveSearchTask;
import com.peterlaurence.trekadvisor.core.map.maploader.tasks.MapDeleteTask;
import com.peterlaurence.trekadvisor.core.map.maploader.tasks.MapMarkerImportTask;
import com.peterlaurence.trekadvisor.core.map.maploader.tasks.MapRouteImportTask;
import com.peterlaurence.trekadvisor.core.map.maploader.tasks.MapUpdateTask;
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
public class MapLoader implements MapImporter.MapImportListener {

    public static final String MAP_FILE_NAME = "map.json";
    public static final String MAP_MARKER_FILE_NAME = "markers.json";
    public static final String MAP_ROUTE_FILE_NAME = "routes.json";
    /**
     * All {@link Projection}s are registered here.
     */
    public static final HashMap<String, Class<? extends Projection>> PROJECTION_HASH_MAP = new HashMap<String, Class<? extends Projection>>() {{
        put(MercatorProjection.NAME, MercatorProjection.class);
        put(UniversalTransverseMercator.NAME, UniversalTransverseMercator.class);
    }};
    private static final String APP_FOLDER_NAME = "trekadvisor";
    private static final File DEFAULT_APP_DIR = new File(Environment.getExternalStorageDirectory(),
            APP_FOLDER_NAME);
    /* For instance maps are searched anywhere under the app folder */
    private static final File DEFAULT_MAPS_DIR = DEFAULT_APP_DIR;
    private static final String TAG = "MapLoader";
    private Gson mGson;
    private List<Map> mMapList;
    private List<MapListUpdateListener> mMapListUpdateListeners;
    private MapMarkerUpdateListener mMapMarkerUpdateListener;
    private MapRouteUpdateListener mMapRouteUpdateListener;
    private List<MapArchive> mMapArchiveList;
    private List<MapArchiveListUpdateListener> mMapArchiveListUpdateListeners;

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
        mMapList = new ArrayList<>();
    }

    public static MapLoader getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Factory of {@link BitmapProvider} depending on the origin of the map.
     *
     * @param map The {@link Map} object
     * @return The {@link BitmapProvider} or a {@link BitmapProviderDummy} if the origin is unknown.
     */
    public static BitmapProvider makeBitmapProvider(Map map) {
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
     * Clear and sets the internal list of {@link Map} : {@code mMapList}. <br>
     * Once done, all of the registered {@link MapListUpdateListener} are called.
     *
     * @param dirs The directories in which to search for maps. If not specified, a default value is
     *             taken.
     */
    public void clearAndGenerateMaps(File... dirs) {
        mMapList = new ArrayList<>();
        if (dirs.length == 0) { // No directories specified? We take the default value.
            dirs = new File[1];
            dirs[0] = DEFAULT_MAPS_DIR;
        }
        generateMaps(dirs);
    }

    /**
     * Appends found {@link Map}s to the internal list of {@link Map} : {@code mMapList}.
     * Once done, all of the registered {@link MapListUpdateListener} are called.
     *
     * @param dirs The directories in which to search for new maps.
     */
    public void generateMaps(File... dirs) {
        MapUpdateTask updateTask = new MapUpdateTask(mMapListUpdateListeners, mGson, mMapList);
        updateTask.execute(dirs);
    }

    public List<Map> getMaps() {
        return mMapList;
    }

    /**
     * Launch a {@link MapMarkerImportTask} which reads the markers.json file.
     */
    public void getMarkersForMap(Map map) {
        MapMarkerImportTask mapMarkerImportTask = new MapMarkerImportTask(mMapMarkerUpdateListener,
                map, mGson);
        mapMarkerImportTask.execute();
    }

    /**
     * Launch a {@link MapRouteImportTask} which reads the routes.json file.
     */
    public void getRoutesForMap(Map map) {
        MapRouteImportTask mapRouteImportTask = new MapRouteImportTask(mMapRouteUpdateListener,
                map, mGson);
        mapRouteImportTask.execute();
    }

    /**
     * Update the internal list of {@link MapArchive} : {@code mMapArchiveList}. Once done, all of
     * the registered {@link MapArchiveListUpdateListener} are called.
     *
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

    public void setMapMarkerUpdateListener(MapMarkerUpdateListener listener) {
        mMapMarkerUpdateListener = listener;
    }

    public void setMapRouteUpdateListener(MapRouteUpdateListener listener) {
        mMapRouteUpdateListener = listener;
    }

    public void addMapArchiveListUpdateListener(MapArchiveListUpdateListener listener) {
        mMapArchiveListUpdateListeners.add(listener);
    }

    public void clearMapListUpdateListener() {
        mMapListUpdateListeners.clear();
    }

    public void clearMapMarkerUpdateListener() {
        mMapMarkerUpdateListener = null;
    }

    public void clearMapRouteUpdateListener() {
        mMapRouteUpdateListener = null;
    }

    /**
     * Add a {@link Map} to the internal list and generated the json file. <br>
     * This is typically called after an import, after a {@link Map} has been generated from a file
     * structure.
     */
    @Override
    public void onMapImported(Map map, MapImporter.MapParserStatus status) {
        if (map == null) return;

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
    public void onMapImportError(MapImporter.MapParseException e) {
        Log.e(TAG, "Error while parsing a map");
        Log.e(TAG, e.getMessage(), e);
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
            Log.e(TAG, "Error while saving the map");
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Save the {@link MarkerGson} of a {@link Map}, so the changes persist upon application restart.
     * <p>
     * Here, it writes to the corresponding json file.
     * </p>
     *
     * @param map The {@link Map} to save.
     */
    public void saveMarkers(Map map) {
        String jsonString = mGson.toJson(map.getMarkerGson());

        File markerFile = new File(map.getDirectory(), MapLoader.MAP_MARKER_FILE_NAME);
        try {
            PrintWriter writer = new PrintWriter(markerFile);
            writer.print(jsonString);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the markers");
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Save the {@link RouteGson} of a {@link Map}, so the changes persist upon application restart.
     * <p>
     * Here, it writes to the corresponding json file.
     * </p>
     *
     * @param map The {@link Map} to save.
     */
    public void saveRoutes(Map map) {
        String jsonString = mGson.toJson(map.getRouteGson());

        File routeFile = new File(map.getDirectory(), MapLoader.MAP_ROUTE_FILE_NAME);
        try {
            PrintWriter writer = new PrintWriter(routeFile);
            writer.print(jsonString);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the routes");
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Delete a {@link Map}. Recursively deletes its directory.
     *
     * @param map The {@link Map} to delete.
     */
    public void deleteMap(Map map, DeleteMapListener listener) {
        File mapDirectory = map.getDirectory();
        mMapList.remove(map);

        /* Notify for view update */
        if (mMapListUpdateListeners != null) {
            for (MapListUpdateListener listUpdateListener : mMapListUpdateListeners) {
                listUpdateListener.onMapListUpdate(mMapList.size() > 0);
            }
        }

        /* Delete the map directory in a separate thread */
        MapDeleteTask mapDeleteTask = new MapDeleteTask(mapDirectory);
        mapDeleteTask.execute();

        if (listener != null) {
            listener.onMapDeleted();
        }
    }

    /**
     * Delete a {@link MarkerGson.Marker} from a {@link Map}.
     */
    public void deleteMarker(Map map, MarkerGson.Marker marker) {
        List<MarkerGson.Marker> markerList = map.getMarkers();
        if (markerList != null) {
            markerList.remove(marker);
        }

        saveMarkers(map);
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

    public interface MapListUpdateListener {
        void onMapListUpdate(boolean mapsFound);
    }

    /**
     * When a map's markers are retrieved from their json content, this listener is called.
     */
    public interface MapMarkerUpdateListener {
        void onMapMarkerUpdate();
    }

    /**
     * When a map's routes are retrieved from their json content, this listener is called.
     */
    public interface MapRouteUpdateListener {
        void onMapRouteUpdate();
    }

    public interface MapArchiveListUpdateListener {
        void onMapArchiveListUpdate();
    }

    public interface DeleteMapListener {
        void onMapDeleted();
    }

    /* Singleton implementation */
    private static class SingletonHolder {
        private static final MapLoader instance = new MapLoader();
    }
}
