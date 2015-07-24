package com.nined.player.mediarouter;

import android.os.Binder;

/**
 * Provides connection to MediaRouterPlayService
 * @author Aekasitt Guruvanich
 * on 7/23/2015.
 */
public class MediaRouterPlayServiceBinder extends Binder
{
    MediaRouterPlayService service;
    public MediaRouterPlayServiceBinder(MediaRouterPlayService service){
        this.service = service;
    }

    public MediaRouterPlayService getService() {
        return this.service;
    }
}
