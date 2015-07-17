package com.nined.player.client;

import android.content.Context;

import retrofit.RestAdapter;

/**
 * Created by Aekasitt on 7/17/2015.
 */
public class PlayerSvc {
    private static PlayerApi instance_;
    public static PlayerApi getOrBuildInstance(Context context) {
        if (instance_==null) {
            instance_ = new RestAdapter.Builder().build().create(PlayerApi.class);
        }
        return instance_;
    }
}
