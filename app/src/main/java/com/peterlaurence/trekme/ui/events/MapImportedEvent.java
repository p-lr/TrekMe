package com.peterlaurence.trekme.ui.events;

import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter;

/**
 * Event emitted by the view-model of the {@link com.peterlaurence.trekme.ui.mapimport.MapImportFragment},
 * more precisely by the {@link com.peterlaurence.trekme.viewmodel.mapimport.MapImportViewModel},
 * when a map has been successfully imported.
 *
 * @author P.Laurence on 22/12/17.
 */
public class MapImportedEvent {
    public Map map;
    public int archiveId;
    public MapImporter.MapParserStatus status;

    public MapImportedEvent(Map map, int archiveId, MapImporter.MapParserStatus status) {
        this.map = map;
        this.archiveId = archiveId;
        this.status = status;
    }
}
