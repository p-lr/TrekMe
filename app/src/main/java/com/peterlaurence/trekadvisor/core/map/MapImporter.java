package com.peterlaurence.trekadvisor.core.map;

import android.support.annotation.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

/**
 * The {@link MapImporter} is created from a {@link MapProvider}. Then, the corresponding map parser
 * is used to parse the map (given as a {@link File}). <br>
 * This is typically used after a {@link MapArchive} has been extracted.
 *
 * @author peterLaurence on 23/06/16.
 */
public class MapImporter {
    private final MapParser mParser;
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


    public MapImporter(MapProvider provider) {
        mParser = mProviderToParserMap.get(provider);
    }

    public Map importFromFile(File file) {
        Map map = mParser.parse(file);
        return map;
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

            /* Get the maximum zoom level */
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

            return null;
        }

        /* Get the tile size */
        private int getTileSizeForLevel(File directory) {
            File file = new File(directory, "0");
//            if (file.exists()) {
//
//            }
            return 0;
        }
    }
}
