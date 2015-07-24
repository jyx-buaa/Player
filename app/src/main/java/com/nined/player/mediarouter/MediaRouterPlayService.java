package com.nined.player.mediarouter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.nined.player.fragments.BaseVideoPlayerFragment;
import com.nined.player.fragments.RouteFragment;

import org.teleal.cling.support.model.item.Item;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aekasitt on 7/23/2015.
 */
public class MediaRouterPlayService extends Service {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = MediaRouterPlayService.class.getSimpleName();
    private static final boolean SHOW_LOG = true;
    /*********************************/
    /**          Constant(s)        **/
    /*********************************/
    private static final int NOTIFICATION_ID = 1;
    //TODO
    private static final boolean PAUSED_WHEN_RECEIVING_PHONECALL = true;
    /*********************************/
    /**       Member Variable(s)    **/
    /*********************************/
    /*
     * Declare Binder
     */
    private final MediaRouterPlayServiceBinder binder = new MediaRouterPlayServiceBinder(this);
    private PhoneStateListener phoneCallListener;
    /*
     *
     */
    private MediaRouter mediaRouter;
    private RouteInfo currentRoute;
    private MediaRouter.Callback mediaRouterCallback;
    /*
     *
     */
    private List<Item> playlist = new ArrayList<>();
    /*
     *
     */
    private int currentTrack = -1;
    private boolean pausedForCall = false;
    private boolean shuffleEnabled = false;
    private boolean repeatEnabled = false;
    private boolean pollingStatus = false;
    private boolean bound;
    private String itemId;
    private String sessionId;
    private WeakReference<BaseVideoPlayerFragment> videoPlayerFragment = new WeakReference<>(null);
    private WeakReference<RouteFragment> routeFragment = new WeakReference<>(null);

    /*********************************/
    /**      Service Override(s)    **/
    /*********************************/
    @Override
    public void onCreate() {
        super.onCreate();
        mediaRouter = MediaRouter.getInstance(this);
        pollStatus();
        phoneCallListener = createPhoneCallListener();
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);

        MediaRouteSelector selector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();

        mediaRouter.addCallback(selector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }



    @Override
    public IBinder onBind(Intent intent) {
        bound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (pollingStatus) {
            stopSelf();
        }
        bound = false;
        return super.onUnbind(intent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaRouter.removeCallback(mediaRouterCallback);
    }

    /*********************************/
    /**      Playback Control       **/
    /*********************************/
    private void pollStatus() {
        //TODO
    }
    public void seek(int delta) {

    }
    public void play(int playMode) {

    }
    public boolean playPrevious() {
        return true;
    }
    public boolean playNext() {
        return true;
    }
    public void pause() {

    }
    public void resume() {

    }
    public void stop() {

    }
    public void toggleRepeatEnabled() {
        this.repeatEnabled = !this.repeatEnabled;
    }
    public boolean isRepeatEnabled() {
        return this.repeatEnabled;
    }

    public void toggleShuffleEnabled() {
        this.shuffleEnabled = !this.shuffleEnabled;
    }
    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public void setPlaylist(List<Item> playlist) {
        this.playlist = playlist;
    }

    public List<Item> getPlaylist() {
        return playlist;
    }

    public void decreaseVolume() {
        //TODO
    }

    public void increaseVolume() {
        //TODO
    }

    public int getCurrentTrack() {
        return currentTrack;
    }
    /*********************************/
    /**      Getters - Setters      **/
    /*********************************/
    public void setVideoPlayerFragment(BaseVideoPlayerFragment videoPlayerFragment)
    {
        this.videoPlayerFragment =
                new WeakReference<>(videoPlayerFragment);
    }
    public void setRouteFragment(RouteFragment routeFragment) {
        this.routeFragment =
                new WeakReference<>(routeFragment);
    }
    public void setRoute(RouteInfo route) {
        this.mediaRouter.selectRoute(route);
        this.currentRoute = route;
    }
    public RouteInfo getCurrentRoute() {
        return this.currentRoute;
    }
    /*********************************/
    /** Member Service Initiation(s)**/
    /*********************************/
    private PhoneStateListener createPhoneCallListener() {
        return new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                //if (PreferenceManager.getDefaultSharedPreferences(MediaRouterPlayService.this).getBoolean(PrefenrencesActivity.KEY_INCOMING_PHONE_CALL_O)
                if (!PAUSED_WHEN_RECEIVING_PHONECALL) {
                    if (TelephonyManager.CALL_STATE_RINGING == state || TelephonyManager.CALL_STATE_OFFHOOK == state){
                        pause();
                        pausedForCall=true;
                    }

                    if (pausedForCall && TelephonyManager.CALL_STATE_IDLE==state) {
                        resume();
                        pausedForCall = false;
                    }
                }
            }
        };
    }

    private MediaRouter.Callback createMediaRouterCallback () {
        return new MediaRouter.Callback() {
            @Override
            public void onRouteAdded(MediaRouter router, RouteInfo route) {
                super.onRouteAdded(router, route);
            }

            @Override
            public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                super.onRouteRemoved(router, route);
            }
        };
    }
}
