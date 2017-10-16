package com.peterlaurence.trekadvisor.menu.maplist.dialogs.events;

/**
 * @author peterLaurence on 16/10/17.
 */
public class UrlDownloadFinishedEvent {
    public boolean success;

    public UrlDownloadFinishedEvent(boolean success) {
        this.success = success;
    }
}
