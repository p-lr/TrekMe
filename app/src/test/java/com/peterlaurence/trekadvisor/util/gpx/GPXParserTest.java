package com.peterlaurence.trekadvisor.util.gpx;

import com.peterlaurence.trekadvisor.BuildConfig;
import com.peterlaurence.trekadvisor.util.gpx.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpx.model.Track;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * @author peterLaurence on 12/02/17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class GPXParserTest {
    private static File mGpxFilesDirectory;

    static {
        try {
            URL gpxDirURL = GPXParserTest.class.getClassLoader().getResource("gpxfiles");
            mGpxFilesDirectory = new File(gpxDirURL.toURI());
        } catch (Exception e) {
            System.out.println("No resource file for gpx files directory.");
        }
    }

    @Test
    public void simpleFileTest() {
        if (mGpxFilesDirectory != null) {
            File aGpxFile = new File(mGpxFilesDirectory, "sample_gpx_1.gpx");
            if (aGpxFile.exists()) {
                try {
                    Gpx gpx = GPXParser.parse(new FileInputStream(aGpxFile));

                    List<Track> trackList = gpx.getTracks();
                    assertEquals(1, trackList.size());

                    Track track = trackList.get(0);
                    List<TrackSegment> trackSegmentList = track.getTrackSegments();
                    assertEquals("Example track", track.getName());
                    assertEquals(1, trackSegmentList.size());
                    TrackSegment trackSegment = trackSegmentList.get(0);
                    List<TrackPoint> trackPointList = trackSegment.getTrackPoints();
                    assertEquals(7, trackPointList.size());
                    TrackPoint firstTrackPoint = trackPointList.get(0);

                    Double lat = firstTrackPoint.getLatitude();
                    Double lon = firstTrackPoint.getLongitude();
                    Double elevation = firstTrackPoint.getElevation();
                    assertEquals(46.57608333, lat);
                    assertEquals(8.89241667, lon);
                    assertEquals(2376.0, elevation);

                    assertEquals(firstTrackPoint.getTime(),
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).
                                    parse("2007-10-14T10:09:57Z"));
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

}
