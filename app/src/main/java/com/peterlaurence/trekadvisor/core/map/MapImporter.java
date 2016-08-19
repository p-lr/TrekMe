package com.peterlaurence.trekadvisor.core.map;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The {@link MapImporter} is created from a {@link MapProvider}. Then, the corresponding map parser
 * is used to parse the map (given as a {@link File}). <br>
 * This is typically used after a {@link MapArchive} has been extracted.
 *
 * @author peterLaurence on 23/06/16.
 */
public class MapImporter {
    private static final java.util.Map<MapProvider, MapParser> mProviderToParserMap;

    /**
     * Possible {@link Map} providers.
     */
    public enum MapProvider {
        LIBVIPS
    }

    /**
     * Produces a {@link Map} from a given {@link File}.
     */
    private interface MapParser {
        @Nullable Map parse(File file);
    }

    static {
        java.util.Map<MapProvider, MapParser> map = new HashMap<>();
        map.put(MapProvider.LIBVIPS, new LibvipsMapParser());
        mProviderToParserMap = Collections.unmodifiableMap(map);
    }

    /* Don't allow instantiation */
    private MapImporter() {
    }

    public static @Nullable Map importFromFile(File file, MapProvider provider) {
        MapParser parser = mProviderToParserMap.get(provider);
        if (parser != null) {
            Map map = parser.parse(file);
            return map;
        }
        return null;
    }

    /**
     * This {@link MapParser} expects a directory {@link File} being the first parent of all files
     * produced by libvips.
     */
    private static class LibvipsMapParser implements MapParser {
        private java.util.Map<Integer, Integer> mTileSizeForLevelMap;

        @Override
        public Map parse(File file) {
            if (!file.isDirectory()) {
                return null;
            }

            MapGson mapGson = new MapGson();
            List<MapGson.Level> levels = new ArrayList<>();
            int maxLevel = getMaxLevel(file);
            for (int i=0; i<=maxLevel; i++) {
                System.out.println("creating level " + i);
            }

            return null;
        }

        /* Get the maximum zoom level */
        private int getMaxLevel(File file) {
            int maxLevel = 0;
            int level;
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    try {
                        level = Integer.parseInt(f.getName());
                        maxLevel = level > maxLevel ? level : maxLevel;
                    } catch (NumberFormatException e) {
                        // an unknown folder, ignore it.
                    }
                }
            }
            return maxLevel;
        }

        /* Get the tile size */
        private int getTileSizeForLevel(File directory) {
            File file = new File(directory, "0");
            return 0;
        }
    }
}
