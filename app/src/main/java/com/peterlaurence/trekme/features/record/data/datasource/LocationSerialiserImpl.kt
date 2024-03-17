package com.peterlaurence.trekme.features.record.data.datasource

import com.peterlaurence.trekme.core.appName
import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.gpx.model.Bounds
import com.peterlaurence.trekme.core.lib.gpx.model.GPX_VERSION
import com.peterlaurence.trekme.core.lib.gpx.model.Gpx
import com.peterlaurence.trekme.core.lib.gpx.model.GpxElevationSource
import com.peterlaurence.trekme.core.lib.gpx.model.GpxElevationSourceInfo
import com.peterlaurence.trekme.core.lib.gpx.model.Metadata
import com.peterlaurence.trekme.core.lib.gpx.model.Track
import com.peterlaurence.trekme.core.lib.gpx.model.TrackPoint
import com.peterlaurence.trekme.core.lib.gpx.model.TrackSegment
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.map.data.TEMP_FOLDER_NAME
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.features.record.domain.model.LocationsSerializer
import com.peterlaurence.trekme.features.record.domain.model.RecordRestorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.time.TimeSource

class LocationSerializerImpl @Inject constructor(
    outputStream: OutputStream
) : LocationsSerializer {
    private val sink = PrintWriter(outputStream, false)
    private val timeSource = TimeSource.Monotonic

    private var rcvCount = 0L

    @Volatile
    private var lastFlush: TimeSource.Monotonic.ValueTimeMark? = null

    private val mutex = Mutex()

    override suspend fun onLocation(location: Location) = mutex.withLock {
        rcvCount++

        withContext(Dispatchers.IO) {
            sink.print(json.encodeToString(location.toLocationJson()) + '\n')
            val now = timeSource.markNow()
            val lastMark = lastFlush
            if (rcvCount % 5 == 0L || (lastMark != null && (now - lastMark).inWholeSeconds > 5)) {
                sink.flush()
                lastFlush = now
            }
        }
    }

    override suspend fun pause() = mutex.withLock {
        withContext(Dispatchers.IO) {
            sink.print(PAUSE)
            sink.flush()
            lastFlush = timeSource.markNow()
        }
    }
}

class AppRecordRestorer(private val settings: Settings) : RecordRestorer {
    private var sink: File? = null
    private var hasInit = false

    override suspend fun hasRecordToRestore(): Boolean {
        sink = getSinkFile(settings)
        hasInit = true
        return sink != null
    }

    override suspend fun restore(): Pair<GeoRecord, BoundingBox?>? {
        if (!hasInit) {
            sink = getSinkFile(settings)
        }

        val file = sink ?: return null
        val stream = FileInputStream(file)
        val gpx = LocationDeSerializerImpl(stream).readGpx() ?: run {
            /* If we failed to parse as Gpx, delete the file */
            deleteRecord()
            return null
        }
        val name = gpx.metadata?.name ?: file.name
        val boundingBox = gpx.metadata?.bounds?.let {
            BoundingBox(
                minLat = it.minLat,
                maxLat = it.maxLat,
                minLon = it.minLon,
                maxLon = it.maxLon
            )
        }

        return Pair(gpxToDomain(gpx, name = name), boundingBox)
    }

    override suspend fun deleteRecord(): Unit = withContext(Dispatchers.IO) {
        runCatching {
            sink?.delete()
        }
    }
}

class LocationDeSerializerImpl(inputStream: InputStream) {
    private val reader = inputStream.bufferedReader()

    suspend fun readGpx(): Gpx? = withContext(Dispatchers.IO) {
        var isPaused = false
        val trkSegList = mutableListOf<TrackSegment>()
        var trackPoints = mutableListOf<TrackPoint>()

        var minLat = Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        runCatching {
            reader.forEachLine { line ->
                if (line == PAUSE.trim()) {
                    if (!isPaused) {
                        isPaused = true
                        if (trackPoints.isNotEmpty()) {

                            trkSegList.add(TrackSegment(trackPoints, UUID.randomUUID().toString()))
                        }
                        trackPoints = mutableListOf()
                    }
                } else {
                    isPaused = false
                    runCatching {
                        json.decodeFromString<LocationJson>(line)
                    }.onSuccess {
                        val pt = it.toTrackPoint()
                        minLat = min(minLat, pt.latitude)
                        minLon = min(minLon, pt.longitude)
                        maxLat = max(maxLat, pt.latitude)
                        maxLon = max(maxLon, pt.longitude)

                        trackPoints.add(pt)
                    }
                }
            }
        }

        if (trackPoints.isNotEmpty()) {
            trkSegList.add(TrackSegment(trackPoints, UUID.randomUUID().toString()))
        }

        if (trkSegList.isEmpty()) return@withContext null

        /* Name the track using the current date */
        val date = Date()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH'h'mm-ss's'", Locale.ENGLISH)
        val trackName = "recovery-" + dateFormat.format(date)

        val track = Track(trkSegList, trackName, id = UUID.randomUUID().toString())

        val bounds =
            if (minLat != Double.MAX_VALUE && minLon != Double.MAX_VALUE && maxLat != -Double.MAX_VALUE && maxLon != -Double.MAX_VALUE) {
                Bounds(minLat, minLon, maxLat, maxLon)
            } else null

        /* Make the metadata. We indicate the source of elevation is the GPS, regardless of the
         * actual source (which might be wifi, etc), with a sampling of 1 since each point has
         * its own elevation value. Note that GPS elevation isn't considered trustworthy. */
        val metadata = Metadata(
            trackName,
            date.time,
            bounds, // This isn't mandatory to put this into the metadata
            elevationSourceInfo = GpxElevationSourceInfo(GpxElevationSource.GPS, 1)
        )

        val trkList = ArrayList<Track>()
        trkList.add(track)

        val wayPoints = ArrayList<TrackPoint>()

        Gpx(metadata, trkList, wayPoints, appName, GPX_VERSION)
    }
}

/**
 * Creates an application-specific file, intended to be used to create an [OutputStream] and give
 * it to [LocationSerializerImpl] constructor.
 */
suspend fun createSinkFile(settings: Settings) = runCatching {
    val appDir = settings.getAppDir().firstOrNull() ?: return@runCatching null
    val dir = File(appDir, TEMP_FOLDER_NAME)
    if (!dir.exists()) {
        dir.mkdir()
    }

    val sinkFileName = PREFIX + dateFormat.format(LocalDateTime.now())
    val sinkFile = File(dir, sinkFileName).also {
        it.createNewFile()
    }
    sinkFile
}.getOrNull()

suspend fun getSinkFile(settings: Settings): File? = runCatching {
    val appDir = settings.getAppDir().firstOrNull() ?: return@runCatching null
    val dir = File(appDir, TEMP_FOLDER_NAME)

    dir.listFiles()?.firstOrNull { it.isFile && it.name.startsWith(PREFIX) }
}.getOrNull()

private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH'h'mm-ss's'", Locale.ENGLISH)
private val json by lazy {
    Json { isLenient = true; ignoreUnknownKeys = true }
}
private const val PREFIX = "recording-"

@Serializable
private data class LocationJson(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float? = null,
    val altitude: Double? = null,
    val time: Long = 0L
)

private const val PAUSE = "PAUSE\n"

private fun Location.toLocationJson(): LocationJson {
    return LocationJson(
        latitude = latitude, longitude = longitude, speed = speed, altitude = altitude, time = time
    )
}

private fun LocationJson.toTrackPoint(): TrackPoint {
    return TrackPoint(
        latitude = latitude,
        longitude = longitude,
        elevation = altitude,
        time = time,
        name = null
    )
}