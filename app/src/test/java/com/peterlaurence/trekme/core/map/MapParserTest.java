package com.peterlaurence.trekme.core.map;

import com.peterlaurence.trekme.BuildConfig;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.gson.RouteGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for maps's json file parsing.
 *
 * @author peterLaurence on 26/02/17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MapParserTest {
    private static File mJsonFilesDirectory;

    static {
        try {
            URL mapDirURL = MapImporterTest.class.getClassLoader().getResource("mapjson-example");
            mJsonFilesDirectory = new File(mapDirURL.toURI());
        } catch (Exception e) {
            System.out.println("No json directory found.");
        }
    }

    @Test
    public void mapTracksParse() {
        if (mJsonFilesDirectory != null) {
            File[] dirs = new File[1];
            dirs[0] = mJsonFilesDirectory;
            final Map[] map = new Map[1];

            MapLoader.MapListUpdateListener mapListUpdateListener = new MapLoader.MapListUpdateListener() {
                @Override
                public void onMapListUpdate(boolean mapsFound) {
                    List<Map> mapList = MapLoader.getInstance().getMaps();

                    /* One map should be found */
                    assertEquals(1, mapList.size());
                    map[0] = mapList.get(0);
                }
            };

            MapLoader.MapRouteUpdateListener mapRouteUpdateListener = new MapLoader.MapRouteUpdateListener() {
                @Override
                public void onMapRouteUpdate() {
                    /* 2 routes should be found */
                    assertEquals(2, map[0].getRoutes().size());

                    RouteGson.Route route = map[0].getRoutes().get(0);
                    assertEquals("A test route 1", route.name);
                    assertEquals(true, route.visible);
                    List<MarkerGson.Marker> markers = route.route_markers;
                    assertEquals(2, markers.size());

                    MarkerGson.Marker marker1 = markers.get(0);
                    assertEquals("marker1", marker1.name);
                    assertEquals(6198798.5047565, marker1.proj_x, 0);

                    MarkerGson.Marker marker2 = markers.get(1);
                    assertEquals("marker2", marker2.name);
                    assertEquals(-2418744.7142449305, marker2.proj_y, 0);
                }
            };

            MapLoader mapLoader = MapLoader.getInstance();
            mapLoader.setMapRouteUpdateListener(mapRouteUpdateListener);
            mapLoader.setMapListUpdateListener(mapListUpdateListener);
            mapLoader.clearAndGenerateMaps(dirs);
            mapLoader.getRoutesForMap(map[0]);
        }
    }
}
