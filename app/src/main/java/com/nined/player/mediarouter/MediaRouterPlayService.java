package com.nined.player.mediarouter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nined.player.MainActivity;
import com.nined.player.R;
import com.nined.player.fragments.BaseVideoPlayerFragment;
import com.nined.player.fragments.RouteFragment;
import com.nined.player.utils.LoadImageTask;
import com.nined.player.utils.PrefUtils;

import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Aekasitt Guruvanich, 9D Technolgies
 * on 7/23/2015.
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
    private static final int POLL_STATUS_INTERVAL = 1;
    private static final boolean DEFAULT_PREFS_PAUSE_FOR_PHONECALL = true;
    private static final String PREFS_KEY_PAUSE_FOR_PHONECALL = MediaRouterPlayService.class.getPackage() + ".Pause.For.Phonecall";

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
    private MediaRouter.Callback mediaRouterCallback = createMediaRouterCallback();
    /*
     *
     */
    private List<Item> playlist = new ArrayList<>();
    /*
     *
     */
    private int currentTrack = -1;
    private boolean currentlyPausedForPhoneCall = false;
    private boolean shuffleEnabled = false;
    private boolean repeatEnabled = false;
    private boolean pollingStatus = false;
    private boolean bound;
    private WeakReference<BaseVideoPlayerFragment> videoPlayerFragment = new WeakReference<>(null);
    private WeakReference<RouteFragment> routeFragment = new WeakReference<>(null);
    private String itemId;
    private String sessionId;

    /*********************************/
    /**      Service Override(s)    **/
    /*********************************/
    @Override
    public void onCreate() {
        super.onCreate();
        if (SHOW_LOG) Log.i(TAG, "onCreate");
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
        if (SHOW_LOG) Log.i(TAG, "onBind");
        bound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (SHOW_LOG) Log.i(TAG, "onUnbind");
        if (this.pollingStatus) {
            stopSelf();
        }
        this.bound = false;
        return super.onUnbind(intent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SHOW_LOG) Log.i(TAG, "onDestroy");
        if (mediaRouter!=null)
            mediaRouter.removeCallback(mediaRouterCallback);
    }

    /*********************************/
    /** Member Service Initiation(s)**/
    /*********************************/
    private PhoneStateListener createPhoneCallListener() {
        return new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                boolean prefsPauseForPhoneCall = PrefUtils.getBoolean(MediaRouterPlayService.this,
                        MediaRouterPlayService.PREFS_KEY_PAUSE_FOR_PHONECALL, MediaRouterPlayService.DEFAULT_PREFS_PAUSE_FOR_PHONECALL);

                if (!currentlyPausedForPhoneCall) {
                    if (TelephonyManager.CALL_STATE_RINGING == state || TelephonyManager.CALL_STATE_OFFHOOK == state){
                        pause();
                        currentlyPausedForPhoneCall=true;
                    }

                    if (prefsPauseForPhoneCall && TelephonyManager.CALL_STATE_IDLE==state) {
                        resume();
                        currentlyPausedForPhoneCall = false;
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
                if (MediaRouterPlayService.this.currentRoute!=null && route.getId().equals(MediaRouterPlayService.this.currentRoute.getId())) {
                    MediaRouterPlayService.this.setRoute(route);
                    if (MediaRouterPlayService.this.currentTrack >= 0 &&  MediaRouterPlayService.this.currentTrack <= (MediaRouterPlayService.this.playlist.size())) {
                        new CreateNotificationTask().execute(MediaRouterPlayService.this.playlist.get(MediaRouterPlayService.this.currentTrack)
                                .getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class));
                    }
                }
            }

            @Override
            public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                super.onRouteRemoved(router, route);
                if(route.equals(MediaRouterPlayService.this.currentRoute)) {
                    stopForeground(true);
                }
                if(!MediaRouterPlayService.this.bound && !MediaRouterPlayService.this.pollingStatus) {
                    stopSelf();
                }
            }
        };
    }
    /*********************************/
    /** Foreground Notification Call**/
    /*********************************/
    private class CreateNotificationTask extends LoadImageTask {
        @Override
        protected void onPostExecute(Bitmap result) {
            String title = "", artist = "";
            if (MediaRouterPlayService.this.currentTrack < MediaRouterPlayService.this.playlist.size()) {

                title = MediaRouterPlayService.this.playlist.get(MediaRouterPlayService.this.currentTrack).getTitle();
                if (MediaRouterPlayService.this.playlist.get(MediaRouterPlayService.this.currentTrack) instanceof MusicTrack) {
                    MusicTrack track = (MusicTrack) MediaRouterPlayService.this.playlist.get(MediaRouterPlayService.this.currentTrack);
                    if (track.getArtists().length > 0) {
                        artist = track.getArtists()[0].getName();
                    }
                }

                Intent nextIntent = new Intent (MediaRouterPlayService.this, MainActivity.class);
                nextIntent.setAction("showRouteFragment");
                Notification notif = new NotificationCompat.Builder(MediaRouterPlayService.this)
                        .setContentIntent(PendingIntent.getActivity(MediaRouterPlayService.this, 0, nextIntent, 0))
                        .setContentTitle(title)
                        .setContentText(artist)
                        .setLargeIcon(result)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .build();
                notif.flags = NotificationCompat.FLAG_ONGOING_EVENT;
                startForeground(MediaRouterPlayService.NOTIFICATION_ID, notif);
            }
        }
    }

    /*********************************/
    /**      Playback Control       **/
    /*********************************/
    /**
     * Sends a 'seek' signal with absolute value for seconds in the track the current renderer
     * @param seconds
     */
    public void seek(int seconds) {
        if (SHOW_LOG) Log.i(TAG, "seek");
        if (this.playlist==null || this.playlist.isEmpty()) return;

        Intent seekIntent = new Intent(MediaControlIntent.ACTION_SEEK);
        seekIntent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        seekIntent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, this.sessionId);
        seekIntent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, this.itemId);
        seekIntent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, (long) seconds * 1000);

        this.mediaRouter.getSelectedRoute().sendControlRequest(seekIntent, null);
    }
    /**
     * Sends play signal to current renderer
     * @param trackNumber number of track from playlist to be played
     */
    public void play(int trackNumber) {
        if (SHOW_LOG) Log.i(TAG, "play");
        if (trackNumber < 0 || trackNumber >= this.playlist.size()) return;

        this.currentTrack = trackNumber;
        Item track = playlist.get(currentTrack);
        DIDLParser parser = new DIDLParser();
        DIDLContent didl = new DIDLContent();
        didl.addItem(track);
        String metadata = "";
        try {
            metadata = parser.generate(didl, true);
        } catch (Exception e) {
            if (SHOW_LOG) Log.w(TAG, "Metadata generation failed", e);
        }

        Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.setData(Uri.parse(track.getFirstResource().getValue()));
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_METADATA, metadata);

        mediaRouter.getSelectedRoute().sendControlRequest(intent, new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                super.onResult(data);
                MediaRouterPlayService.this.sessionId = data.getString(MediaControlIntent.EXTRA_SESSION_ID);
                MediaRouterPlayService.this.itemId = data.getString(MediaControlIntent.EXTRA_ITEM_ID);
                MediaRouterPlayService.this.pollingStatus = true;
                new CreateNotificationTask().execute(MediaRouterPlayService.this.playlist.get(MediaRouterPlayService.this.currentTrack)
                        .getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class));
                if (MediaRouterPlayService.this.routeFragment.get() != null)
                    MediaRouterPlayService.this.routeFragment.get().scrollToCurrent();
            }
        });
    }
    /**
     * Sends a 'pause' signal to current renderer
     */
    public void pause() {
        if (SHOW_LOG) Log.i(TAG, "pause");
        if (this.playlist.isEmpty()) return;

        Intent intent = new Intent(MediaControlIntent.ACTION_PAUSE);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, this.sessionId);

        this.mediaRouter.getSelectedRoute().sendControlRequest(intent, null);
        this.pollingStatus = false;
        stopForeground(true);
    }
    /**
     * Sends a 'stop' signal to current renderer
     */
    public void stop() {
        if (SHOW_LOG) Log.i(TAG, "stop");
        if (this.playlist.isEmpty()) return;

        Intent intent = new Intent(MediaControlIntent.ACTION_STOP);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, this.sessionId);

        this.mediaRouter.getSelectedRoute().sendControlRequest(intent, null);
        this.pollingStatus = false;
        stopForeground(true);
    }
    /**
     * Sends a 'resume' signal to current renderrer
     */
    public void resume() {
        if (SHOW_LOG) Log.i(TAG, "resume");
        if (this.playlist.isEmpty()) return;

        Intent intent = new Intent(MediaControlIntent.ACTION_RESUME);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, this.sessionId);

        this.mediaRouter.getSelectedRoute().sendControlRequest(intent, null);
        this.pollingStatus = true;
        new CreateNotificationTask().execute(this.playlist.get(this.currentTrack)
                .getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class));
    }
    /**
     * Decides to continue playing or not according to foreset conditions
     * @return true when able to find a next track to play, false otherwise.
     */
    public boolean playNext() {
        if (SHOW_LOG) Log.i(TAG, "playNext");
        if (this.currentTrack==-1) return false;
        if (this.shuffleEnabled) {
            play(new Random().nextInt(this.playlist.size()));
            return false;
        }
        else if (this.currentTrack+1 < this.playlist.size()) {
            play(this.currentTrack+1);
            return true;
        } else if (this.repeatEnabled) {
            play(0);
            return true;
        }
        stop();
        if (!this.bound) {
            stopSelf();
        }
        pollingStatus = false;
        return false;
    }
    /**
     * @return true when able to find a previous track to play, false otherwise.
     */
    public boolean playPrevious() {
        if (SHOW_LOG) Log.i(TAG, "playPrevious");
        if (this.currentTrack==-1) return false;
        if (this.shuffleEnabled) {
            play(new Random().nextInt(this.playlist.size()));
            return true;
        }
        play(this.currentTrack - 1);
        return true;
    }

    /**
     * Increare the volume at the current renderer by one arbitrary unit
     */
    public void increaseVolume() {
        if (SHOW_LOG) Log.i(TAG, "increaseVolume");
        if (this.mediaRouter!=null)
            this.mediaRouter.getSelectedRoute().requestUpdateVolume(1);
    }

    /**
     * Decrease the volume at the current renderer by one arbitrary unit
     */
    public void decreaseVolume() {
        if (SHOW_LOG) Log.i(TAG, "decreaseVolume");
        if (this.mediaRouter!=null)
            this.mediaRouter.getSelectedRoute().requestUpdateVolume(-1);
    }



    /**
     * Requests playback information every second, as long as RendererFragment
     * is attached or media is playing
     */
    private void pollStatus() {
        if (SHOW_LOG) Log.i(TAG, "pollStatus");
        if (this.pollingStatus && this.sessionId != null && this.itemId !=null) {
            Intent pollIntent = new Intent(MediaControlIntent.ACTION_GET_STATUS);
            pollIntent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, this.sessionId);
            pollIntent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, this.itemId);

            this.mediaRouter.getSelectedRoute().sendControlRequest(pollIntent,
                    new MediaRouter.ControlRequestCallback() {
                        @Override
                        public void onError(String error, Bundle data) {
                            super.onError(error, data);
                            if (SHOW_LOG && error != null)
                                Log.w(TAG, "Failed to get status: " + error);
                        }

                        @Override
                        public void onResult(Bundle data) {
                            super.onResult(data);
                            MediaItemStatus status = MediaItemStatus.fromBundle(data);
                            if (status == null) return;
                            if (routeFragment.get() != null)
                                routeFragment.get().receivePlaybackStatus(status);
                            switch (status.getPlaybackState()) {
                                case MediaItemStatus.PLAYBACK_STATE_FINISHED:
                                case MediaItemStatus.PLAYBACK_STATE_CANCELED:
                                    playNext();
                                    break;
                                case MediaItemStatus.PLAYBACK_STATE_PENDING:
                                case MediaItemStatus.PLAYBACK_STATE_BUFFERING:
                                case MediaItemStatus.PLAYBACK_STATE_PLAYING:
                                    break;
                                default:
                                    stopForeground(true);
                            }
                        }
                    });
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                pollStatus();
            }
        }, POLL_STATUS_INTERVAL * 1000);
    }

    /*********************************/
    /**      Getters - Setters      **/
    /*********************************/
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
    /**
     * Set a list of items to be played by the current renderer
     * @param playlist to be set
     */
    public void setPlaylist(List<Item> playlist) {
        this.playlist = playlist;
    }
    /**
     * Get the list of items, music and other media on the play list of current render
     * @return the playlist
     */
    public List<Item> getPlaylist() {
        return playlist;
    }
    /**
     * @param track number of track on the playlist to be played
     */
    public void setCurrentTrack(int track) {
        if (SHOW_LOG) Log.i(TAG, "setCurrentTrack");
        if (this.playlist==null || this.playlist.isEmpty())
            return;
        if (track>=0 &&track< this.playlist.size())
            this.currentTrack = track;
    }
    /**
     * @return the currently playing track number
     */
    public int getCurrentTrack() {
        return this.currentTrack;
    }
    /**
     * Create a weak reference to the BaseVideoPlayerFragment controlling this service
     * @param videoPlayerFragment that controls this service
     */
    public void setVideoPlayerFragment(BaseVideoPlayerFragment videoPlayerFragment) {
        this.videoPlayerFragment =
                new WeakReference<>(videoPlayerFragment);
    }
    /**
     * Create a weak reference to the routeFragment controlling this service
     * @param routeFragment that controls this service
     */
    public void setRouteFragment(RouteFragment routeFragment) {
        this.routeFragment =
                new WeakReference<>(routeFragment);
    }
    /**
     * Change the route of the MediaRouter
     * @param route to be selected by MediaRouter
     */
    public void setRoute(RouteInfo route) {
        if (SHOW_LOG) Log.i(TAG, String.format("Set Route: %s", route.getDescription()));
        this.mediaRouter.selectRoute(route);
        this.currentRoute = route;
    }
    /**
     * @return the current Renderer's route
     */
    public RouteInfo getCurrentRoute() {
        return this.currentRoute;
    }
}
