package com.peterlaurence.trekadvisor.util.gpxparser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a track - an ordered list of Track Segment describing a path.
 *
 * @author peterLaurence on 12/02/17.
 */
public class Track {
    private final List<TrackSegment> mTrackSegments;

    private Track(Builder builder) {
        mTrackSegments = Collections.unmodifiableList(new ArrayList<>(builder.mTrackSegments));
    }

    public List<TrackSegment> getTrackSegments() {
        return mTrackSegments;
    }

    public static class Builder {
        private List<TrackSegment> mTrackSegments;

        public Builder setTrackSegments(List<TrackSegment> trackSegments) {
            mTrackSegments = trackSegments;
            return this;
        }

        public Track build() {
            return new Track(this);
        }
    }
}
