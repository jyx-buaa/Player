    /**
     * @author Aekasitt Guruvanich, 9D Technologies
     * on 7/21/2015.
     */

package com.nined.player.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class NineDPlayerFragment extends BaseVideoPlayerFragment {
    /*********************************/
    /**         Constructor         **/
    /*********************************/
    public NineDPlayerFragment () { super(); }

    /*********************************/
    /**      Instance Creation      **/
    /*********************************/
    public static NineDPlayerFragment newInstance(@NonNull String title,
                                                      @NonNull String location) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(location)) return null;
        Bundle args = new Bundle();
        args.putString(BaseVideoPlayerFragment.ARGS_TITLE, title);
        args.putString(BaseVideoPlayerFragment.ARGS_LOCATION, location);
        NineDPlayerFragment videoPlayerFragment = new NineDPlayerFragment();
        videoPlayerFragment.setArguments(args);
        return videoPlayerFragment;
    }
    @Override
    protected void showOverlay() {

    }

    @Override
    protected void setProgressVisible(boolean visible) {

    }

    @Override
    protected void onProgressChanged(long currentTime, long duration) {

    }
}
