package com.peterlaurence.trekadvisor.util.gpx;

import com.peterlaurence.trekadvisor.util.gpx.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpx.model.Track;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

/**
 * GPX tests.
 *
 * @author peterLaurence on 12/02/17.
 */
@RunWith(RobolectricTestRunner.class)
public class GPXTest {
    private static File mGpxFilesDirectory;

    static {
        try {
            URL gpxDirURL = GPXTest.class.getClassLoader().getResource("gpxfiles");
            mGpxFilesDirectory = new File(gpxDirURL.toURI());
        } catch (Exception e) {
            System.out.println("No resource file for gpx files directory.");
        }
    }

    @Rule
    public TemporaryFolder mTestFolder = new TemporaryFolder();

    private final File referenceGpxFile = new File(mGpxFilesDirectory, "sample_gpx_1.gpx");

    @Test
    public void simpleFileTest() {
        if (mGpxFilesDirectory != null) {
            if (referenceGpxFile.exists()) {
                try {
                    Gpx gpx = GPXParser.INSTANCE.parse(new FileInputStream(referenceGpxFile));

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

                    assertEquals(GPXParser.INSTANCE.getDateParser().
                                    parse("2007-10-14T10:09:57Z").getTime(),
                            firstTrackPoint.getTime(), 0.0);

                    /* Check that the track has statistics */
                    assertNotNull(track.getStatistics());
                    assertEquals(track.getStatistics().getDistance(), 102.0);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

    /**
     * Tests the gpx writer against the gpx parser : parse an existing gpx file, the use the gpx
     * writer to write a gpx file somewhere in a temp folder, then use the gpx parser again to parse
     * the resulting file. <br>
     * The resulting file should have identical values (at least for tags that the writer supports).
     */
    @Test
    public void writeTest() {
        try {
            /* First read an existing gpx file */
            Gpx gpxInput = null;
            try {
                gpxInput = GPXParser.INSTANCE.parse(new FileInputStream(referenceGpxFile));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }

            /* Write it in a temporary folder */
            File testFile = mTestFolder.newFile();
            FileOutputStream fos = new FileOutputStream(testFile);
            GPXWriter.INSTANCE.write(gpxInput, fos);

            /* Now read it back */
            Gpx gpx = GPXParser.INSTANCE.parse(new FileInputStream(testFile));
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

            assertEquals(GPXParser.INSTANCE.getDateParser().
                            parse("2007-10-14T10:09:57Z").getTime(), firstTrackPoint.getTime(), 0.0);

            assertNotNull(track.getStatistics());
            assertEquals(track.getStatistics().getDistance(), 102.0);
        } catch (IOException | ParserConfigurationException | TransformerException | ParseException | XmlPullParserException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void dateParse() {
        String aDate = "2017-09-26T08:38:12+02:00";
        try {
            Date date = GPXParser.INSTANCE.getDateParser().parse(aDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int secs = cal.get(Calendar.SECOND);
            assertEquals(2017, year);
            assertEquals(8, month);
            assertEquals(26, day);
            assertEquals(12, secs);
        } catch (ParseException e) {
            fail();
        }
    }
}
