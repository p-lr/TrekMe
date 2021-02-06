package com.peterlaurence.trekme.util.gpx

import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.util.gpx.model.*
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
 * A GPX writer compliant with the [GPX 1.1 schema](https://www.topografix.com/gpx/1/1/).
 * But its features are limited to the needs of TrekMe app, which for instance only consist in
 * writing tracks (with track segments and way-points).
 *
 * @author P.Laurence on 30/12/17.
 */
@Throws(ParserConfigurationException::class, TransformerException::class)
fun writeGpx(gpx: Gpx, out: OutputStream) {
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

    /* Metadata */
    if (gpx.metadata != null) {
        gpxNode.addMetadata(gpx.metadata, doc)
    }

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

private fun Node.addMetadata(metadata: Metadata, doc: Document) {
    val metadataNode = doc.createElement(TAG_METADATA)

    if (metadata.name != null) {
        val node = doc.createElement(TAG_NAME)
        node.appendChild(doc.createTextNode(metadata.name))
        metadataNode.appendChild(node)
    }

    if (metadata.time != null) {
        val node = doc.createElement(TAG_TIME)
        node.appendChild(doc.createTextNode(DATE_FORMATTER.format(metadata.time)))
        metadataNode.appendChild(node)
    }

    if (metadata.elevationSourceInfo != null) {
        val node = doc.createElement(TAG_ELE_SOURCE_INFO)
        val attrs = node.attributes

        /* Source */
        val eleSrcNode = doc.createAttribute(ATTR_ELE_SOURCE)
        eleSrcNode.nodeValue = metadata.elevationSourceInfo.elevationSource.toString()
        attrs.setNamedItem(eleSrcNode)

        /* Sampling */
        val samplingNode = doc.createAttribute(ATTR_SAMPLING)
        samplingNode.nodeValue = metadata.elevationSourceInfo.sampling.toString()
        attrs.setNamedItem(samplingNode)

        metadataNode.appendChild(node)
    }
    appendChild(metadataNode)
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
        addTrackStatisticsToNode(trk.statistics!!, nodeExtensions, doc)
        trkNode.appendChild(nodeExtensions)
    }
    n.appendChild(trkNode)
}

private fun addTrackStatisticsToNode(statistics: TrackStatistics, n: Node, doc: Document) {
    val statisticsNode = doc.createElement(TAG_TRACK_STATISTICS)
    val statisticsNodeAttr = statisticsNode.attributes

    val distanceAttribute = doc.createAttribute(ATTR_TRK_STAT_DIST)
    distanceAttribute.nodeValue = statistics.distance.toString()
    statisticsNodeAttr.setNamedItem(distanceAttribute)

    val elevationDiffMax = doc.createAttribute(ATTR_TRK_STAT_ELE_DIFF_MAX)
    elevationDiffMax.nodeValue = statistics.elevationDifferenceMax.toString()
    statisticsNodeAttr.setNamedItem(elevationDiffMax)

    val elevationUpStatck = doc.createAttribute(ATTR_TRK_STAT_ELE_UP_STACK)
    elevationUpStatck.nodeValue = statistics.elevationUpStack.toString()
    statisticsNodeAttr.setNamedItem(elevationUpStatck)

    val elevationDownStack = doc.createAttribute(ATTR_TRK_STAT_ELE_DOWN_STACK)
    elevationDownStack.nodeValue = statistics.elevationDownStack.toString()
    statisticsNodeAttr.setNamedItem(elevationDownStack)

    val durationInSec = doc.createAttribute(ATTR_TRK_STAT_DURATION)
    durationInSec.nodeValue = statistics.durationInSecond.toString()
    statisticsNodeAttr.setNamedItem(durationInSec)

    val avgSpeed = doc.createAttribute(ATTR_TRK_STAT_AVG_SPEED)
    avgSpeed.nodeValue = statistics.avgSpeed.toString()
    statisticsNodeAttr.setNamedItem(avgSpeed)

    n.appendChild(statisticsNode)
}

private fun addTrackSegmentToNode(ts: TrackSegment, n: Node, doc: Document) {
    val tsNode = doc.createElement(TAG_SEGMENT)

    for (trkpt in ts.trackPoints) {
        addWaypointToNode(TAG_TRK_POINT, trkpt, tsNode, doc)
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
    if (trkPt.elevation != null) {
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

private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)