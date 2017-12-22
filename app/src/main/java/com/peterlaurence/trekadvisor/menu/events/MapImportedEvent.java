package com.peterlaurence.trekadvisor.menu.events;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter;

/**
 * Event emitted inside the {@link com.peterlaurence.trekadvisor.menu.mapimport.MapImportFragment},
 * more precisely by the {@link com.peterlaurence.trekadvisor.menu.mapimport.MapArchiveViewHolder},
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
