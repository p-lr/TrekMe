package com.peterlaurence.trekadvisor.menu.trackview

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.track.TrackStatistics
import com.peterlaurence.trekadvisor.util.UnitFormatter
import kotlinx.android.synthetic.main.fragment_track_view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class TrackViewFragment : Fragment() {

    companion object {
        private const val ARG_STATS = "stats"

        @JvmStatic
        fun newInstance(trackStats: TrackStatistics): Fragment {
            val fragment = Fragment()
            val args = Bundle()
            args.putParcelable(ARG_STATS, trackStats)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_track_view, container, false)
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

    @Subscribe
    fun onTrackStatistics(event: TrackStatistics) {
        setStatistics(event)
    }

    private fun setStatistics(statistics: TrackStatistics) {
        val formattedDistance = UnitFormatter.formatDistance(statistics.distance)
        trackDistanceView.setDistanceText(formattedDistance)
    }
}