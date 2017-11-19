package com.peterlaurence.trekadvisor.util;

import com.peterlaurence.trekadvisor.BuildConfig;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
@Config(constants = BuildConfig.class)
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

    @Rule
    public TemporaryFolder mTestFolder = new TemporaryFolder();

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
                }

                @Override
                public void onUnzipError() {
                    fail();
                }
            };

            try {
                final File tempMapArchive = mTestFolder.newFile();

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
                        UnzipTask unzipTask = new UnzipTask(tempMapArchive, mTestFolder.getRoot(), unzipProgressionListener);
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
