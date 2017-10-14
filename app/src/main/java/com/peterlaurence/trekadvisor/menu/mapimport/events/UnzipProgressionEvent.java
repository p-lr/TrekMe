package com.peterlaurence.trekadvisor.menu.mapimport.events;

/**
 * @author peterLaurence on 12/10/17.
 */
public class UnzipProgressionEvent {
    public final int archiveId;
    public final int progression;

    public UnzipProgressionEvent(int archiveId, int progression) {
        this.archiveId = archiveId;
        this.progression = progression;
    }
}
