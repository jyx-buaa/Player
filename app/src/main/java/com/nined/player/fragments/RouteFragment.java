package com.nined.player.fragments;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.MediaRouteDiscoveryFragment;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.ProviderInfo;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.nined.player.MainActivity;
import com.nined.player.R;
import com.nined.player.mediarouter.MediaRouterPlayService;
import com.nined.player.mediarouter.MediaRouterPlayServiceBinder;
import com.nined.player.views.FileArrayAdapter;
import com.nined.player.views.RouteAdapter;

import org.teleal.cling.support.model.item.Item;

import java.util.List;

/**
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/24/2015.
 */

/**
 * Controls media playback by showing a list of routes, and after selecting one,
 * the current playlist and playback controls.
 *
 * @author Felix Ableitner
 *
 */
public class RouteFragment extends MediaRouteDiscoveryFragment implements
        MainActivity.OnBackPressedListener, OnItemClickListener, OnClickListener,
        OnSeekBarChangeListener, OnScrollListener {

    private ListView mListView;

    private View mControls;
    private SeekBar mProgressBar;
    private ImageButton mPlayPause;
    private ImageButton mShuffle;
    private ImageButton mRepeat;
    private TextView mCurrentTimeView;
    private TextView mTotalTimeView;
    private TextView mEmptyView;

    private View mCurrentTrackView;

    private boolean mPlaying;

    private RouteAdapter mRouteAdapter;

    private FileArrayAdapter mPlaylistAdapter;

    private RouteInfo mSelectedRoute;

    /**
     * Count of the number of taps on the previous button within the
     * doubletap interval.
     */
    private int mPreviousTapCount = 0;

    /**
     * If true, the item at this position will be played as soon as a route is selected.
     */
    private int mStartPlayingOnSelect = -1;

    private MediaRouterPlayService mMediaRouterPlayService;

    private ServiceConnection mPlayServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            MediaRouterPlayServiceBinder binder = (MediaRouterPlayServiceBinder) service;
            mMediaRouterPlayService = binder.getService();
            mMediaRouterPlayService.setRouteFragment(RouteFragment.this);
            mPlaylistAdapter.add(mMediaRouterPlayService.getPlaylist());
            applyColors();
            RouteInfo currentRoute = mMediaRouterPlayService.getCurrentRoute();
            if (currentRoute != null) {
                playlistMode(currentRoute);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mMediaRouterPlayService = null;
        }
    };

    /**
     * Selects remote playback route category.
     */
    public RouteFragment() {
        MediaRouteSelector mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();
        setRouteSelector(mSelector);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_route, null);
    };

    /**
     * Initializes views, connects to service, adds default route.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRouteAdapter = new RouteAdapter(getActivity());
        mRouteAdapter.add(MediaRouter.getInstance(getActivity()).getRoutes());
        mRouteAdapter.remove(MediaRouter.getInstance(getActivity()).getDefaultRoute());
        mRouteAdapter.sort(RouteAdapter.COMPARATOR);
        mPlaylistAdapter = new FileArrayAdapter(getActivity());

        mListView = (ListView) getView().findViewById(R.id.listview);
        mListView.setAdapter(mRouteAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        mListView.setEmptyView(mEmptyView);

        mControls = getView().findViewById(R.id.controls);
        mProgressBar = (SeekBar) getView().findViewById(R.id.progressBar);
        mProgressBar.setOnSeekBarChangeListener(this);

        mShuffle = (ImageButton) getView().findViewById(R.id.shuffle);
        mShuffle.setImageResource(R.drawable.ic_action_shuffle);
        mShuffle.setOnClickListener(this);

        ImageButton previous = (ImageButton) getView().findViewById(R.id.previous);
        previous.setImageResource(R.drawable.ic_action_previous);
        previous.setOnClickListener(this);

        ImageButton next = (ImageButton) getView().findViewById(R.id.next);
        next.setImageResource(R.drawable.ic_action_next);
        next.setOnClickListener(this);

        mRepeat = (ImageButton) getView().findViewById(R.id.repeat);
        mRepeat.setImageResource(R.drawable.ic_action_repeat);
        mRepeat.setOnClickListener(this);

        mPlayPause = (ImageButton) getView().findViewById(R.id.playpause);
        mPlayPause.setOnClickListener(this);
        mPlayPause.setImageResource(R.drawable.ic_action_play);

        mCurrentTimeView = (TextView) getView().findViewById(R.id.current_time);
        mTotalTimeView = (TextView) getView().findViewById(R.id.total_time);

        getActivity().getApplicationContext().startService(
                new Intent(getActivity(), MediaRouterPlayService.class));
        getActivity().getApplicationContext().bindService(
                new Intent(getActivity(), MediaRouterPlayService.class),
                mPlayServiceConnection, Context.BIND_AUTO_CREATE);

        if (savedInstanceState != null) {
            mListView.onRestoreInstanceState(savedInstanceState.getParcelable("list_state"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("list_state", mListView.onSaveInstanceState());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(mPlayServiceConnection);
    }

    /**
     * Starts active route discovery (which is automatically stopped on
     * fragment stop by parent class).
     */
    @Override
    public int onPrepareCallbackFlags() {
        return  MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY |
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN;
    }

    @Override
    public Callback onCreateCallback() {
        return new MediaRouter.Callback() {
            @Override
            public void onRouteAdded(MediaRouter router, RouteInfo route) {
                for (int i = 0; i < mRouteAdapter.getCount(); i++) {
                    if (mRouteAdapter.getItem(i).getId().equals(route.getId())) {
                        mRouteAdapter.remove(mRouteAdapter.getItem(i));
                        break;
                    }
                }
                mRouteAdapter.add(route);
                mRouteAdapter.sort(RouteAdapter.COMPARATOR);

                RouteInfo current = mMediaRouterPlayService.getCurrentRoute();
                if (current != null && route.getId().equals(current.getId())) {
                    playlistMode(current);
                }
            }

            @Override
            public void onRouteChanged(MediaRouter router, RouteInfo route) {
                mRouteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                mRouteAdapter.remove(route);
                if (route.equals(mSelectedRoute)) {
                    mPlaying = false;
                    onBackPressed();
                }
            }

            @Override
            public void onRouteSelected(MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onRoutePresentationDisplayChanged(
                    MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onProviderAdded(MediaRouter router, ProviderInfo provider) {
            }

            @Override
            public void onProviderRemoved(MediaRouter router, ProviderInfo provider) {
            }

            @Override
            public void onProviderChanged(MediaRouter router, ProviderInfo provider) {
            }
        };

    }

    /**
     * Selects a route or starts playback (depending on current ListAdapter).
     */
    @Override
    public void onItemClick(AdapterView<?> a, View v, final int position, long id) {
        if (mListView.getAdapter() == mRouteAdapter) {
            playlistMode(mRouteAdapter.getItem(position));
        }
        else {
            mMediaRouterPlayService.play(position);
            changePlayPauseState(true);
        }
    }

    /**
     * Displays UPNP devices in the ListView.
     */
    private void deviceListMode() {
        mControls.setVisibility(View.GONE);
        mListView.setAdapter(mRouteAdapter);
        disableTrackHighlight();
        mSelectedRoute = null;
        mEmptyView.setText(R.string.route_list_empty);
    }

    /**
     * Displays playlist for route in the ListView.
     */
    private void playlistMode(RouteInfo route) {
        mSelectedRoute = route;
        mMediaRouterPlayService.setRoute(mSelectedRoute);
        mListView.setAdapter(mPlaylistAdapter);
        mControls.setVisibility(View.VISIBLE);
        if (mStartPlayingOnSelect != -1) {
            mMediaRouterPlayService.play(mStartPlayingOnSelect);
            changePlayPauseState(true);
            mStartPlayingOnSelect = -1;
        }
        mEmptyView.setText(R.string.playlist_empty);
        mListView.post(new Runnable() {
            @Override
            public void run() {
                scrollToCurrent();
            }
        });
    }

    /**
     * Sets colored background on the item that is currently playing.
     */
    private void enableTrackHighlight() {
        if (mListView.getAdapter() == mRouteAdapter ||
                mMediaRouterPlayService == null || !isVisible())
            return;

        disableTrackHighlight();
        mCurrentTrackView = mListView.getChildAt(mMediaRouterPlayService.getCurrentTrack()
                - mListView.getFirstVisiblePosition() + mListView.getHeaderViewsCount());
        if (mCurrentTrackView != null) {
            mCurrentTrackView.setBackgroundColor(
                    getResources().getColor(R.color.currently_playing_background));
        }
    }

    /**
     * Removes highlight from the item that was last highlighted.
     */
    private void disableTrackHighlight() {
        if (mCurrentTrackView != null) {
            mCurrentTrackView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Unselects current media renderer if one is selected (with dialog).
     */
    @Override
    public boolean onBackPressed() {
        if (mListView.getAdapter() == mPlaylistAdapter) {
            if (mPlaying) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.exit_renderer)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        mMediaRouterPlayService.stop();
                                        changePlayPauseState(false);
                                        deviceListMode();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
            else {
                deviceListMode();
            }
            return true;
        }
        return false;
    }

    /**
     * Plays/pauses playback on button click.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playpause:
                if (mPlaying) {
                    mMediaRouterPlayService.pause();
                    changePlayPauseState(false);
                } else {
                    mMediaRouterPlayService.resume();
                    scrollToCurrent();
                    changePlayPauseState(true);
                }
                break;
            case R.id.shuffle:
                mMediaRouterPlayService.toggleShuffleEnabled();
                applyColors();
                break;
            case R.id.previous:
                mPreviousTapCount++;
                Handler handler = new Handler();
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        // Single tap.
                        mPreviousTapCount = 0;
                        mMediaRouterPlayService.play(mMediaRouterPlayService.getCurrentTrack());
                        changePlayPauseState(true);
                    }
                };
                if (mPreviousTapCount == 1)
                    handler.postDelayed(r, ViewConfiguration.getDoubleTapTimeout());
                else if(mPreviousTapCount == 2) {
                    // Double tap.
                    mPreviousTapCount = 0;
                    mMediaRouterPlayService.playPrevious();
                }
                break;
            case R.id.next:
                boolean stillPlaying = mMediaRouterPlayService.playNext();
                changePlayPauseState(stillPlaying);
                break;
            case R.id.repeat:
                mMediaRouterPlayService.toggleRepeatEnabled();
                applyColors();
                break;
        }
    }

    /**
     * Enables or disables highlighting on shuffle/repeat buttons (depending
     * if they are enabled or disabled).
     */
    private void applyColors() {
        int highlight = getResources().getColor(R.color.button_highlight);
        int transparent = getResources().getColor(android.R.color.transparent);

        mShuffle.setColorFilter((mMediaRouterPlayService.isShuffleEnabled())
                ? highlight
                : transparent);
        mRepeat.setColorFilter((mMediaRouterPlayService.isRepeatEnabled())
                ? highlight
                : transparent);
    }

    /**
     * Sends manual seek on progress bar to renderer.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser) {
            mMediaRouterPlayService.seek(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    /**
     * Keeps track highlighting on the correct item while views are rebuilt.
     */
    @Override
    public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
        enableTrackHighlight();
    }

    @Override
    public void onScrollStateChanged(AbsListView arg0, int arg1) {
        enableTrackHighlight();
    }

    public void increaseVolume() {
        mMediaRouterPlayService.increaseVolume();
    }

    public void decreaseVolume() {
        mMediaRouterPlayService.decreaseVolume();
    }

    /**
     * Applies the playlist and starts playing at position.
     */
    public void play(List<Item> playlist, int start) {
        mPlaylistAdapter.clear();
        mPlaylistAdapter.add(playlist);
        mMediaRouterPlayService.setPlaylist(playlist);

        if (mSelectedRoute != null) {
            mMediaRouterPlayService.play(start);
            changePlayPauseState(true);
        }
        else {
            Toast.makeText(getActivity(), R.string.select_route, Toast.LENGTH_SHORT)
                    .show();
            mStartPlayingOnSelect = start;
        }
    }

    /**
     * Generates a time string in the format mm:ss from a time value in seconds.
     *
     * @param time Time value in seconds (non-negative).
     * @return Formatted time string.
     */
    private String generateTimeString(int time) {
        int seconds = time % 60;
        int minutes = time / 60;
        if (minutes > 99) {
            return "99:99";
        }
        else {
            return Integer.toString(minutes) + ":" +
                    ((seconds > 9)
                            ? seconds
                            : "0" + Integer.toString(seconds));
        }
    }

    /**
     * Receives information from MediaRouterPlayService about playback status.
     */
    public void receivePlaybackStatus(MediaItemStatus status) {
        // Views may not exist if fragment was just created/destroyed.
        if (getView() == null)
            return;

        int currentTime = (int) status.getContentPosition() / 1000;
        int totalTime = (int) status.getContentDuration() / 1000;

        mCurrentTimeView.setText(generateTimeString(currentTime));
        mTotalTimeView.setText(generateTimeString(totalTime));

        mProgressBar.setProgress(currentTime);
        mProgressBar.setMax(totalTime);

        if (status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_PLAYING ||
                status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_BUFFERING ||
                status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_PENDING) {
            changePlayPauseState(true);
        }
        else {
            changePlayPauseState(false);
        }

        if (mListView.getAdapter() == mPlaylistAdapter) {
            enableTrackHighlight();
        }
    }

    /**
     * Changes the state of mPlayPause button to pause/resume according to
     * current playback state, also sets mPlaying.
     *
     * @param playing True if an item is currently being played, false otherwise.
     */
    private void changePlayPauseState(boolean playing) {
        mPlaying = playing;
        if (mPlaying) {
            mPlayPause.setImageResource(R.drawable.ic_action_pause);
            mPlayPause.setContentDescription(getResources().getString(R.string.route_pause));
        }
        else {
            mPlayPause.setImageResource(R.drawable.ic_action_play);
            mPlayPause.setContentDescription(getResources().getString(R.string.route_play));
        }
    }


    /**
     * When in playlist mode, scrolls to the item that is currently playing.
     */
    public void scrollToCurrent() {
        if (mMediaRouterPlayService != null) {
            mListView.smoothScrollToPosition(
                    mMediaRouterPlayService.getCurrentTrack());
        }
    }

}