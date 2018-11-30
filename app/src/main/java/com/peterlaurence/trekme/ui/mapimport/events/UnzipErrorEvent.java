package com.peterlaurence.trekme.ui.mapimport.events;

/**
 * @author peterLaurence on 12/10/17.
 */
public class UnzipErrorEvent {
    public final int archiveId;

    public UnzipErrorEvent(int archiveId) {
        this.archiveId = archiveId;
    }
}
