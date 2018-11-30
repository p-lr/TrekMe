package com.peterlaurence.trekme.ui.record.components.events;

import java.io.File;

/**
 * @author peterLaurence on 26/08/2018
 */
public class RequestEditRecording {
    public File recording;

    public RequestEditRecording(File recording) {
        this.recording = recording;
    }
}
