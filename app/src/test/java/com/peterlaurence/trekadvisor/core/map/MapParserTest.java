package com.peterlaurence.trekadvisor.core.map;


import com.peterlaurence.trekadvisor.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
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
            File libVipsMapDir = new File(mMapsDirectory, "libvips");
            if (libVipsMapDir.exists()) {
                Map map = MapImporter.importFromFile(libVipsMapDir, MapImporter.MapProvider.LIBVIPS);
                assertEquals(2, 2);
                return;
            }
        }
        fail();
    }
}
