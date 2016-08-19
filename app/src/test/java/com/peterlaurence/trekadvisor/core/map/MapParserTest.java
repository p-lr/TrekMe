package com.peterlaurence.trekadvisor.core.map;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author peterLaurence on 19/08/16.
 */
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
