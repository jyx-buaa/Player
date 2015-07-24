package com.nined.player.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.nined.player.utils.LoadImageTask;

import java.net.URI;

/**
 * ImageView that can directly load from a UPNP URI.
 *
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/24/2015.
 */
public class RemoteImageView extends ImageView {

    private class AssignImageTask extends LoadImageTask {

        @Override
        protected void onPostExecute(Bitmap bm) {
            if (bm != null) {
                setImageBitmap(bm);
            }
        }

    };

    public RemoteImageView(Context context) {
        super(context);
    }

    public RemoteImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    /**
     * Sets the URI where the image should be loaded from, loads and assigns it.
     */
    public void setImageUri(URI uri) {
        setImageDrawable(null);
        new AssignImageTask().execute(uri);
    }

}
