package com.peterlaurence.trekme.core.providers;

import android.content.Context;
import android.graphics.Bitmap;

import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.tiles.Tile;

/**
 * A dummy {@link BitmapProvider} that does nothing.
 *
 * @author peterLaurence
 */
public class BitmapProviderDummy implements BitmapProvider {
    @Override
    public Bitmap getBitmap(Tile tile, Context context) {
        return null;
    }
}
