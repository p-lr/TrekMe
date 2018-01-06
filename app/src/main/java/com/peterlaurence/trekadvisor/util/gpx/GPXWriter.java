package com.peterlaurence.trekadvisor.util.gpx;

import com.peterlaurence.trekadvisor.util.gpx.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpx.model.Track;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static com.peterlaurence.trekadvisor.util.gpx.model.GpxSchema.*;

/**
 * A writer compliant with the GPX 1.1 schema. <br>
 * But its features are limited to the needs of TrekAdvisor app, which for instance only consist in
 * writing tracks (with track segments and way-points).
 *
 * @author peterLaurence on 30/12/17.
 */
public abstract class GPXWriter {
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);

    public static void write(Gpx gpx, OutputStream out) throws ParserConfigurationException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Node gpxNode = doc.createElement(TAG_GPX);

        NamedNodeMap attrs = gpxNode.getAttributes();
        if (gpx.getVersion() != null) {
            Node verNode = doc.createAttribute(ATTR_VERSION);
            verNode.setNodeValue(gpx.getVersion());
            attrs.setNamedItem(verNode);
        }
        if (gpx.getCreator() != null) {
            Node creatorNode = doc.createAttribute(ATTR_CREATOR);
            creatorNode.setNodeValue(gpx.getCreator());
            attrs.setNamedItem(creatorNode);
        }

        if (gpx.getTracks() != null) {
            for (Track track : gpx.getTracks()) {
                addTrackToNode(track, gpxNode, doc);
            }
        }

        doc.appendChild(gpxNode);

        // Use a Transformer for output
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(out);
        transformer.transform(source, result);
    }

    private static void addTrackToNode(Track trk, Node n, Document doc) {
        Node trkNode = doc.createElement(TAG_TRACK);

        if (trk.getName() != null) {
            Node node = doc.createElement(TAG_NAME);
            node.appendChild(doc.createTextNode(trk.getName()));
            trkNode.appendChild(node);
        }

        if (trk.getTrackSegments() != null) {
            for (TrackSegment ts : trk.getTrackSegments()) {
                addTrackSegmentToNode(ts, trkNode, doc);
            }
        }
        n.appendChild(trkNode);
    }

    private static void addTrackSegmentToNode(TrackSegment ts, Node n, Document doc) {
        Node tsNode = doc.createElement(TAG_SEGMENT);

        for (TrackPoint trkpt : ts.getTrackPoints()) {
            addWaypointToNode(TAG_POINT, trkpt, tsNode, doc);
        }

        n.appendChild(tsNode);

    }

    private static void addWaypointToNode(String tag, TrackPoint trkPt, Node n, Document doc) {
        Node wptNode = doc.createElement(tag);
        NamedNodeMap attrs = wptNode.getAttributes();
        if (trkPt.getLatitude() != 0) {
            Node latNode = doc.createAttribute(ATTR_LAT);
            latNode.setNodeValue(String.valueOf(trkPt.getLatitude()));
            attrs.setNamedItem(latNode);
        }
        if (trkPt.getLongitude() != 0) {
            Node longNode = doc.createAttribute(ATTR_LON);
            longNode.setNodeValue(String.valueOf(trkPt.getLongitude()));
            attrs.setNamedItem(longNode);
        }
        if (trkPt.getElevation() != 0) {
            Node node = doc.createElement(TAG_ELEVATION);
            node.appendChild(doc.createTextNode(String.valueOf(trkPt.getElevation())));
            wptNode.appendChild(node);
        }
        if (trkPt.getTime() != null) {
            Node node = doc.createElement(TAG_TIME);
            node.appendChild(doc.createTextNode(DATE_FORMATTER.format(trkPt.getTime())));
            wptNode.appendChild(node);
        }
        n.appendChild(wptNode);
    }
}
