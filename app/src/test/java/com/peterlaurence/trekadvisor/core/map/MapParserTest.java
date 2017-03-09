package com.peterlaurence.trekadvisor.core.map;

import com.peterlaurence.trekadvisor.BuildConfig;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

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

            MapLoader.MapListUpdateListener mapListUpdateListener = new MapLoader.MapListUpdateListener() {
                @Override
                public void onMapListUpdate(boolean mapsFound) {
                    List<Map> mapList = MapLoader.getInstance().getMaps();
                    assertEquals(1, mapList.size());

                    Map map = mapList.get(0);
                    MapGson.Route route = map.getMapGson().routes.get(0);
                    assertEquals("A sample route", route.name);
                    List<MapGson.Marker> markers = route.route_markers;
                    assertEquals(2, markers.size());

                    MapGson.Marker marker1 = markers.get(0);
                    assertEquals("First marker", marker1.name);
                    assertEquals(12.6585, marker1.proj_x, 0);

                    MapGson.Marker marker2 = markers.get(1);
                    assertEquals("Second marker", marker2.name);
                    assertEquals(13.6585, marker2.proj_x, 0);
                }
            };

            MapLoader mapLoader = MapLoader.getInstance();
            mapLoader.addMapListUpdateListener(mapListUpdateListener);
            mapLoader.generateMaps(dirs);
        }
    }
}
