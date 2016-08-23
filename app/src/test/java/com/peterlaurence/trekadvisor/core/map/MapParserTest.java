package com.peterlaurence.trekadvisor.core.map;


import com.peterlaurence.trekadvisor.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for importing maps.
 *
 * @author peterLaurence on 19/08/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MapParserTest {
    private static File mMapsDirectory;

    static {
        try {
            URL mapDirURL = MapParserTest.class.getClassLoader().getResource("maps");
            mMapsDirectory = new File(mapDirURL.toURI());
        } catch (Exception e) {
            System.out.println("No resource file for map test directory.");
        }
    }

    @Test
    public void libvipsMapParser() {
        if (mMapsDirectory != null) {
            final File libVipsMapDir = new File(mMapsDirectory, "libvips");
            if (libVipsMapDir.exists()) {
                MapImporter.MapParseListener dummyMapParseListener = new MapImporter.MapParseListener() {
                    @Override
                    public void onMapParsed(Map map) {
                        assertNotNull(map);
                        assertEquals(MapImporter.DEFAULT_MAP_NAME, map.getName());

                        /* A subfolder under "libvips" subdirectory has been voluntarily created, to test
                         * the case when the import is done from a parent directory. Indeed, when a map is
                         * extracted from an archive, we don't know whether the map was zipped within a
                         * subdirectory or not. Only an analyse of the extracted file structure can tell us.
                         */
                        File expectedParentFolder = new File(libVipsMapDir, "mapname");
                        assertEquals(expectedParentFolder, map.getDirectory());

                        assertEquals("jpg", map.getImageExtension());
                        return;
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
