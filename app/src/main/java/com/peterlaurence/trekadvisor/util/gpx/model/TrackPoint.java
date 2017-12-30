package com.peterlaurence.trekadvisor.util.gpx.model;

import java.util.Date;

/**
 * Represents a waypoint, point of interest, or named feature on a map.
 *
 * @author peterLaurence on 12/02/17.
 */
public class TrackPoint {
    private final Double mLatitude;
    private final Double mLongitude;
    private final Double mElevation;
    private final Date mTime;

    private TrackPoint(Builder builder) {
        mLatitude = builder.mLatitude;
        mLongitude = builder.mLongitude;
        mElevation = builder.mElevation;
        mTime = builder.mTime;
    }

    public Double getElevation() {
        return mElevation;
    }

    public Double getLatitude() {
        return mLatitude;
    }

    public Double getLongitude() {
        return mLongitude;
    }

    public Date getTime() {
        return mTime;
    }

    public static class Builder {
        private Double mLatitude;
        private Double mLongitude;
        private Double mElevation;
        private Date mTime;

        public Builder setLatitude(Double latitude) {
            mLatitude = latitude;
            return this;
        }

        public Builder setLongitude(Double longitude) {
            mLongitude = longitude;
            return this;
        }

        public Builder setElevation(Double elevation) {
            mElevation = elevation;
            return this;
        }

        public Builder setTime(Date time) {
            mTime = time;
            return this;
        }

        public TrackPoint build() {
            return new TrackPoint(this);
        }
    }

}
