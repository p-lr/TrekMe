package com.peterlaurence.trekme.ui.maplist.dialogs.events;

/**
 * @author P.Laurence on 16/10/17.
 */
public class UrlDownloadFinishedEvent {
    public boolean success;
    public int urlHash;

    public UrlDownloadFinishedEvent(boolean success, int urlHash) {
        this.success = success;
        this.urlHash = urlHash;
    }
}
