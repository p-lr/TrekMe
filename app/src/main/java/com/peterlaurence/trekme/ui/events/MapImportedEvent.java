package com.peterlaurence.trekme.ui.events;

import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter;

/**
 * Event emitted inside the {@link com.peterlaurence.trekme.ui.mapimport.MapImportFragment},
 * more precisely by the {@link com.peterlaurence.trekme.ui.mapimport.MapArchiveViewHolder},
 * when a map has been successfully imported.
 *
 * @author peterLaurence on 22/12/17.
 */
public class MapImportedEvent {
    public Map map;
    public MapImporter.MapParserStatus status;

    public MapImportedEvent(Map map, MapImporter.MapParserStatus status) {
        this.map = map;
        this.status = status;
    }
}
