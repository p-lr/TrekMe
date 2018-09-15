package com.peterlaurence.trekadvisor.menu.trackview

import android.os.Bundle
import android.support.v4.app.Fragment
import com.peterlaurence.trekadvisor.core.track.TrackStatistics

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

}