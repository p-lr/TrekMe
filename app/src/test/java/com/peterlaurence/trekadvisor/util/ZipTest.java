package com.peterlaurence.trekadvisor.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.fail;

/**
 * This tests the {@link ZipTask} against the {@link UnzipTask}. A sample map (a simple folder
 * structure) located in the resources of the app is zipped in a temporary folder. Right after
 * that, it's unzipped in the same location. <br>
 * The test is considered successful if this operation is done completely without any error.
 *
 * @author peterLaurence on 10/08/17.
 */
@RunWith(RobolectricTestRunner.class)
public class ZipTest {
    private static final String MAP_NAME = "libvips-with-json";
    private static File mMapsDirectory;

    static {
        try {
            URL mapDirURL = ZipTest.class.getClassLoader().getResource("maps");
            mMapsDirectory = new File(mapDirURL.toURI());
        } catch (Exception e) {
            System.out.println("No resource file for map test directory.");
        }
    }

    private File mTestFolder = new File(System.getProperty("java.io.tmpdir"), "junit_ziptest");

    @Test
    public void zipTest() {
        if (mMapsDirectory != null) {
            final File libVipsMapDir = new File(mMapsDirectory, MAP_NAME);

            final UnzipTask.UnzipProgressionListener unzipProgressionListener = new UnzipTask.UnzipProgressionListener() {
                @Override
                public void onProgress(int p) {

                }

                @Override
                public void onUnzipFinished(File outputDirectory) {
                    System.out.println("Unzip finished");
                    FileUtils.deleteRecursive(mTestFolder);
                }

                @Override
                public void onUnzipError() {
                    fail();
                    FileUtils.deleteRecursive(mTestFolder);
                }
            };

            try {
                final File tempMapArchive = new File(mTestFolder, "testmap.zip");
                tempMapArchive.getParentFile().mkdirs();
                tempMapArchive.createNewFile();

                ZipTask.ZipProgressionListener progressionListener = new ZipTask.ZipProgressionListener() {
                    @Override
                    public void fileListAcquired() {
                        System.out.println("File list acquired");
                    }

                    @Override
                    public void onProgress(int p) {

                    }

                    @Override
                    public void onZipFinished(File outputDirectory) {
                        UnzipTask unzipTask = new UnzipTask(tempMapArchive, mTestFolder, unzipProgressionListener);
                        unzipTask.start();
                    }

                    @Override
                    public void onZipError() {
                        fail();
                    }
                };

                ZipTask zipTask = new ZipTask(libVipsMapDir, tempMapArchive, progressionListener);
                zipTask.execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            fail();
        }
    }
}
