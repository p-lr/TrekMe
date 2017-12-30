package com.peterlaurence.trekadvisor.util.gpx.model;

/**
 * A Track Segment holds a list of Track Points which are logically connected in order.
 *
 * @author peterLaurence on 12/02/17.
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackSegment {
    private final List<TrackPoint> mTrackPoints;

    private TrackSegment(Builder builder) {
        mTrackPoints = Collections.unmodifiableList(new ArrayList<>(builder.mTrackPoints));
    }

    public List<TrackPoint> getTrackPoints() {
        return mTrackPoints;
    }

    public static class Builder {
        private List<TrackPoint> mTrackPoints;

        public Builder setTrackPoints(List<TrackPoint> trackPoints) {
            mTrackPoints = trackPoints;
            return this;
        }

        public TrackSegment build() {
            return new TrackSegment(this);
        }
    }
}
