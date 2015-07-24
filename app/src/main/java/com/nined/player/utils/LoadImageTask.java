package com.nined.player.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/24/2015.
 */
public class LoadImageTask extends AsyncTask<URI, Void, Bitmap>
{
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = LoadImageTask.class.getSimpleName();
    private static final boolean SHOW_LOG = false;

    /*********************************/
    /**     AsyncTask Override(s)   **/
    /*********************************/
    @Override
    protected Bitmap doInBackground(URI... params) {
        Bitmap bitmap = null;
        if (params[0] == null) return bitmap;
        try {
            URLConnection connection = new URL(params[0].toString()).openConnection();
            connection.connect();
            InputStream is = connection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bitmap = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            if (SHOW_LOG) Log.e(TAG, "Failed to load artwork image", e);
        }
        return bitmap;
    }
}
