package com.peterlaurence.trekadvisor.menu.maplist.dialogs.events;

/**
 * @author peterLaurence on 14/10/17.
 */
public class UrlDownloadEvent {
    public int percentProgress;

    public UrlDownloadEvent(int percentProgress) {
        this.percentProgress = percentProgress;
    }
}
