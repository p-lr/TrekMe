package com.peterlaurence.trekadvisor.menu.mapimport.events;

import java.io.File;

/**
 * @author peterLaurence on 12/10/17.
 */
public class UnzipFinishedEvent {
    public final int archiveId;
    public final File outputFolder;

    public UnzipFinishedEvent(int archiveId, File outputFolder) {
        this.outputFolder = outputFolder;
        this.archiveId = archiveId;
    }
}
