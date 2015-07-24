package com.nined.player.upnp;

import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderService;
import android.util.Log;

/**
 * @author Aekasitt Guruvanich
 * on 7/23/2015.
 */
public class ProviderService extends MediaRouteProviderService {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = ProviderService.class.getSimpleName();
    private static final boolean SHOW_LOG = false;

    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private Provider provider;

    /*********************************/
    /** MediaRouteProviderService(s)**/
    /*********************************/
    @Override
    public MediaRouteProvider onCreateMediaRouteProvider() {
        if (SHOW_LOG) Log.d(TAG, "onCreateMediaRouteProvider");
        if (this.provider == null) {
            this.provider = new Provider(this);
        }
        return provider;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SHOW_LOG) Log.d(TAG, "onDestroy");
        this.provider.close();
        this.provider=null;
    }
}