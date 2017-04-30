package com.peterlaurence.trekadvisor.core.map;


import com.peterlaurence.trekadvisor.BuildConfig;
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for importing maps.
 *
 * @author peterLaurence on 19/08/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
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

    @Test
    public void libvipsMapImporter() {
        if (mMapsDirectory != null) {
            final File libVipsMapDir = new File(mMapsDirectory, "libvips");
            if (libVipsMapDir.exists()) {
                MapImporter.MapParseListener dummyMapParseListener = new MapImporter.MapParseListener() {
                    @Override
                    public void onMapParsed(Map map) {
                        assertNotNull(map);

                        /* A subfolder under "libvips" subdirectory has been voluntarily created, to test
                         * the case when the import is done from a parent directory. Indeed, when a map is
                         * extracted from an archive, we don't know whether the map was zipped within a
                         * subdirectory or not. A way to know that is to analyse the extracted file structure.
                         */
                        File expectedParentFolder = new File(libVipsMapDir, "mapname");
                        assertEquals(expectedParentFolder, map.getDirectory());
                        assertEquals("mapname", map.getName());

                        assertEquals(4, map.getMapGson().levels.size());
                        assertEquals(100, map.getMapGson().levels.get(0).tile_size.x);
                        assertEquals(".jpg", map.getImageExtension());
                        assertNull(map.getImage());
                    }

                    @Override
                    public void onError(MapImporter.MapParseException e) {
                        fail();
                    }
                };

                MapImporter.importFromFile(libVipsMapDir, MapImporter.MapProvider.LIBVIPS, dummyMapParseListener);
            }
        }
    }
}
