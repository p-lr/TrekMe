package com.peterlaurence.trekadvisor.util.gpx

import com.peterlaurence.trekadvisor.core.track.TrackStatistics
import com.peterlaurence.trekadvisor.util.gpx.model.*
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * A writer compliant with the [GPX 1.1 schema](https://www.topografix.com/gpx/1/1/). <br></br>
 * But its features are limited to the needs of TrekAdvisor app, which for instance only consist in
 * writing tracks (with track segments and way-points).
 *
 * @author peterLaurence on 30/12/17.
 */
object GPXWriter {
    private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

    @Throws(ParserConfigurationException::class, TransformerException::class)
    fun write(gpx: Gpx, out: OutputStream) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.newDocument()
        val gpxNode = doc.createElement(TAG_GPX)

        /* Version */
        val attrs = gpxNode.attributes
        val verNode = doc.createAttribute(ATTR_VERSION)
        verNode.nodeValue = gpx.version
        attrs.setNamedItem(verNode)

        /* Creator */
        val creatorNode = doc.createAttribute(ATTR_CREATOR)
        creatorNode.nodeValue = gpx.creator
        attrs.setNamedItem(creatorNode)

        /* Tracks */
        for (track in gpx.tracks) {
            addTrackToNode(track, gpxNode, doc)
        }

        doc.appendChild(gpxNode)

        // Use a Transformer for output
        val tFactory = TransformerFactory.newInstance()
        val transformer = tFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")

        val source = DOMSource(doc)
        val result = StreamResult(out)
        transformer.transform(source, result)
    }

    private fun addTrackToNode(trk: Track, n: Node, doc: Document) {
        val trkNode = doc.createElement(TAG_TRACK)

        /* Track name */
        val node = doc.createElement(TAG_NAME)
        node.appendChild(doc.createTextNode(trk.name))
        trkNode.appendChild(node)

        /* Track segments */
        for (ts in trk.trackSegments) {
            addTrackSegmentToNode(ts, trkNode, doc)
        }

        /* Track statistics */
        if (trk.statistics != null) {
            val nodeExtensions = doc.createElement(TAG_EXTENSIONS)
            addTrackstatisticsToNode(trk.statistics!!, nodeExtensions, doc)
            trkNode.appendChild(nodeExtensions)
        }
        n.appendChild(trkNode)
    }

    private fun addTrackstatisticsToNode(statistics: TrackStatistics, n: Node, doc: Document) {
        val statisticsNode = doc.createElement(TAG_TRACK_STATISTICS)
        val statisticsNodeAttr = statisticsNode.attributes

        val distanceAttribute = doc.createAttribute(ATTR_TRK_STAT_DIST)
        distanceAttribute.nodeValue = statistics.distance.toString()
        statisticsNodeAttr.setNamedItem(distanceAttribute)

        n.appendChild(statisticsNode)
    }

    private fun addTrackSegmentToNode(ts: TrackSegment, n: Node, doc: Document) {
        val tsNode = doc.createElement(TAG_SEGMENT)

        for (trkpt in ts.trackPoints) {
            addWaypointToNode(TAG_POINT, trkpt, tsNode, doc)
        }

        n.appendChild(tsNode)

    }

    private fun addWaypointToNode(tag: String, trkPt: TrackPoint, n: Node, doc: Document) {
        val wptNode = doc.createElement(tag)
        val attrs = wptNode.attributes
        if (trkPt.latitude != 0.0) {
            val latNode = doc.createAttribute(ATTR_LAT)
            latNode.nodeValue = trkPt.latitude.toString()
            attrs.setNamedItem(latNode)
        }
        if (trkPt.longitude != 0.0) {
            val longNode = doc.createAttribute(ATTR_LON)
            longNode.nodeValue = trkPt.longitude.toString()
            attrs.setNamedItem(longNode)
        }
        if (trkPt.elevation != 0.0) {
            val node = doc.createElement(TAG_ELEVATION)
            node.appendChild(doc.createTextNode(trkPt.elevation.toString()))
            wptNode.appendChild(node)
        }
        if (trkPt.time != null) {
            val node = doc.createElement(TAG_TIME)
            node.appendChild(doc.createTextNode(DATE_FORMATTER.format(trkPt.time)))
            wptNode.appendChild(node)
        }
        n.appendChild(wptNode)
    }
}
