package com.peterlaurence.trekme.ui.trackview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.util.UnitFormatter
import kotlinx.android.synthetic.main.fragment_track_view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * This fragment provides a view of a track's statistics.
 *
 * @author peterLaurence on 28/10/18
 */
class TrackViewFragment : Fragment() {

    companion object {
        private const val ARG_STATS = "stats"
        private const val ARG_TITLE = "title"

        @JvmStatic
        fun newInstance(trackStats: TrackStatistics, title: String): Fragment {
            val fragment = Fragment()
            val args = Bundle()
            args.putParcelable(ARG_STATS, trackStats)
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_track_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        trackStatsTitle.text = savedInstanceState?.getString(ARG_TITLE) ?: getText(R.string.current_recording)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

        /* When the user goes back and forth or rotates the device, we should display the last values */
        val event = EventBus.getDefault().getStickyEvent(TrackStatistics::class.java)
        if (event != null) {
            setStatistics(event)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTrackStatistics(event: TrackStatistics) {
        setStatistics(event)
    }

    private fun setStatistics(statistics: TrackStatistics) {
        trackDistanceView.setDistanceText(
                UnitFormatter.formatDistance(statistics.distance))

        trackElevationStackView.setElevationStack(
                UnitFormatter.formatDistance(statistics.elevationUpStack),
                UnitFormatter.formatDistance(statistics.elevationDownStack))

        trackDurationView.setDurationText(
                UnitFormatter.formationDuration(statistics.durationInSecond)
        )
    }
}