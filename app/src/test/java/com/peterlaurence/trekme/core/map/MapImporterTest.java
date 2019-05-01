package com.peterlaurence.trekme.core.map;


import com.peterlaurence.trekme.core.map.mapimporter.MapImporter;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for importing maps.
 *
 * @author peterLaurence on 19/08/16.
 */
@RunWith(RobolectricTestRunner.class)
public class MapImporterTest {
    private static File mMapsDirectory;

    static {
        try {
            URL mapDirURL = MapImporterTest.class.getClassLoader().getResource("maps");
            mMapsDirectory = new File(mapDirURL.toURI());
        } catch (Exception e) {
            System.out.println("No resource file for map test directory.");
        }
    }

    @Before
    public void clear() {
        MapLoader.INSTANCE.clearMaps();
        MapLoader.INSTANCE.clearMapListUpdateListener();
    }

    @Test
    public void libvipsMapImporter() {
        if (mMapsDirectory != null) {
            final File libVipsMapDir = new File(mMapsDirectory, "libvips-no-json");
            final File expectedParentFolder = new File(libVipsMapDir, "mapname");
            if (libVipsMapDir.exists()) {
                MapImporter.MapImportListener dummyMapImportListener = new MapImporter.MapImportListener() {
                    @Override
                    public void onMapImported(Map map, MapImporter.MapParserStatus status) {
                        assertNotNull(map);

                        /* A subfolder under "libvips" subdirectory has been voluntarily created, to test
                         * the case when the import is done from a parent directory. Indeed, when a map is
                         * extracted from an archive, we don't know whether the map was zipped within a
                         * subdirectory or not. A way to know that is to analyse the extracted file structure.
                         */
                        assertEquals(expectedParentFolder, map.getDirectory());
                        assertEquals("mapname", map.getName());

                        assertEquals(4, map.getMapGson().levels.size());
                        assertEquals(100, map.getMapGson().levels.get(0).tile_size.x);
                        assertEquals(".jpg", map.getImageExtension());
                        assertNull(map.getImage());
                    }

                    @Override
                    public void onMapImportError(MapImporter.MapParseException e) {
                        fail();
                    }
                };

                /* Previous execution of this test created a map.json file. So delete it. */
                File existingJsonFile = new File(expectedParentFolder, MapLoader.MAP_FILE_NAME);
                if (existingJsonFile.exists()) {
                    existingJsonFile.delete();
                }
                MapImporter.importFromFile(libVipsMapDir, MapImporter.MapProvider.LIBVIPS, dummyMapImportListener);
            }
        }
    }

    @Test
    public void existingMapImport() {
        if (mMapsDirectory != null) {
            final File libVipsMapDir = new File(mMapsDirectory, "libvips-with-json");
            if (libVipsMapDir.exists()) {
                MapLoader.MapListUpdateListener mapListUpdateListener = mapsFound -> {
                    assertTrue(mapsFound);
                    List<Map> mapList = MapLoader.INSTANCE.getMaps();
                    assertEquals(1, mapList.size());
                    int firstMapId = mapList.get(0).getId();
                    Map map = MapLoader.INSTANCE.getMap(firstMapId);
                    assertNotNull(map);
                    assertEquals("La Réunion - Est", map.getName());
                    assertEquals(3, map.getMapGson().levels.size());
                };
                MapLoader mapLoader = MapLoader.INSTANCE;
                mapLoader.setMapListUpdateListener(mapListUpdateListener);

                MapImporter.importFromFile(libVipsMapDir, MapImporter.MapProvider.LIBVIPS, null);
            }
        }
    }
}
