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
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.MediaRouteDiscoveryFragment;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.ProviderInfo;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.util.Insertable;
import com.nined.player.MainActivity;
import com.nined.player.R;
import com.nined.player.mediarouter.MediaRouterPlayService;
import com.nined.player.mediarouter.MediaRouterPlayServiceBinder;
import com.nined.player.views.FileArrayAdapter;

import org.fourthline.cling.support.model.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

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
        MainActivity.OnBackPressedListener, OnSeekBarChangeListener, OnScrollListener {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = RouteFragment.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    private static final String LIST_STATE = "list_state";
    @LayoutRes
    private static final int LAYOUT = R.layout.fragment_route;
    @IdRes
    private static final int LIST = android.R.id.list;
    @IdRes
    private static final int EMPTY = android.R.id.empty;
    @IdRes
    private static final int CONTROLS = R.id.controls;
    @IdRes
    private static final int SHUFFLE = R.id.shuffle;
    @IdRes
    private static final int PLAYPAUSE = R.id.playpause;
    @IdRes
    private static final int REPEAT = R.id.repeat;
    @IdRes
    private static final int PREVIOUS = R.id.previous;
    @IdRes
    private static final int NEXT = R.id.next;
    @IdRes
    private static final int PROGRESS = R.id.progressBar;
    @IdRes
    private static final int CURRENT = R.id.current_time;
    @IdRes
    private static final int TOTAL = R.id.total_time;

    @LayoutRes
    private static final int LISTITEM = R.layout.listitem_route;
    @IdRes
    private static final int ITEM_TITLE = R.id.title;
    @IdRes
    private static final int ITEM_SUBTITLE = R.id.subtitle;
    
    /*********************************/
    /**      View Injection(s)      **/
    /*********************************/
    @Bind(LIST)
    protected  ListView list;
    @Bind(EMPTY)
    protected TextView empty;
    @Bind(CONTROLS)
    protected View controls;
    @Bind({SHUFFLE, PLAYPAUSE, REPEAT, PREVIOUS, NEXT})
    protected List<ImageButton> controlButtons;
    @Bind(PROGRESS)
    protected SeekBar progress;
    @Bind({CURRENT, TOTAL})
    protected List<TextView> timeViews;

    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private RouteAdapter routeAdapter;

    private FileArrayAdapter playlistAdapter;

    private RouteInfo selectedRoute;

    private boolean playing;
    /**
     * Keeps Currently highlighted track from the list
     */
    private View currentTrack;

    /**
     * Count of the number of taps on the previous button within the
     * doubletap interval.
     */
    private int previousTapCount = 0;

    /**
     * If true, the item at this position will be played as soon as a route is selected.
     */
    private int startPlayingOnSelect = -1;
    /**
     * Media Router Service
     */
    private MediaRouterPlayService mediaRouterPlayService;
    private ServiceConnection playServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MediaRouterPlayServiceBinder binder = (MediaRouterPlayServiceBinder) service;
            mediaRouterPlayService = binder.getService();
            mediaRouterPlayService.setRouteFragment(RouteFragment.this);
            playlistAdapter.add(mediaRouterPlayService.getPlaylist());
            applyColors();
            RouteInfo currentRoute = mediaRouterPlayService.getCurrentRoute();
            if (currentRoute != null) {
                playlistMode(currentRoute);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mediaRouterPlayService = null;
        }
    };

    /**
     * Sorts routes by name. Call {@code sort(COMPARATOR)} whenever an item is inserted.
     */
    @SuppressWarnings("unused")
    public static final Comparator<RouteInfo> comparator = new Comparator<RouteInfo>() {
        @Override
        public int compare(RouteInfo lhs, RouteInfo rhs) {
            return lhs.getName().compareTo(rhs.getName());
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

    /*********************************/
    /**     Lifecycle Override(s)   **/
    /*********************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(LAYOUT, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    /**
     * Initializes views, connects to service, adds default route.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.routeAdapter = new RouteAdapter(getActivity());
        this.routeAdapter.addAll(MediaRouter.getInstance(getActivity()).getRoutes());
        this.routeAdapter.remove(MediaRouter.getInstance(getActivity()).getDefaultRoute());
        //TODO this.routeAdapter.sort(RouteFragment.COMPARATOR);
        this.playlistAdapter = new FileArrayAdapter(getActivity());

        this.list.setAdapter(this.routeAdapter);
        this.list.setOnScrollListener(this);
        this.list.setEmptyView(this.empty);

        // Start MediaRouterPlayService
        getActivity().getApplicationContext().startService(
                new Intent(getActivity(), MediaRouterPlayService.class));
        // Start PlayServiceConnection
        getActivity().getApplicationContext().bindService(
                new Intent(getActivity(), MediaRouterPlayService.class),
                this.playServiceConnection, Context.BIND_AUTO_CREATE);

        if (savedInstanceState != null) {
            list.onRestoreInstanceState(savedInstanceState.getParcelable(RouteFragment.LIST_STATE));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RouteFragment.LIST_STATE, list.onSaveInstanceState());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        getActivity().getApplicationContext().unbindService(this.playServiceConnection);
    }

    /*********************************/
    /** MediaRouteDiscoveryInteface **/
    /*********************************/
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
                if (SHOW_LOG) Log.d(TAG, "onRouteAdded");
                for (int i = 0; i < RouteFragment.this.routeAdapter.getCount(); i++) {
                    if (RouteFragment.this.routeAdapter.getItem(i).getId().equals(route.getId())) {
                        RouteFragment.this.routeAdapter.remove(RouteFragment.this.routeAdapter.getItem(i));
                        break;
                    }
                }
                RouteFragment.this.routeAdapter.add(route);
                //TODO RouteFragment.this.routeAdapter.sort(RouteFragment.COMPARATOR);
                RouteInfo current = RouteFragment.this.mediaRouterPlayService.getCurrentRoute();
                if (current != null && route.getId().equals(current.getId())) {
                    playlistMode(current);
                }
            }
            @Override
            public void onRouteChanged(MediaRouter router, RouteInfo route) {
                if (SHOW_LOG) Log.d(TAG, "onRouteChanged");
                RouteFragment.this.routeAdapter.notifyDataSetChanged();
            }
            @Override
            public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                if (SHOW_LOG) Log.d(TAG, "onRouteRemoved");
                RouteFragment.this.routeAdapter.remove(route);
                if (route.equals(RouteFragment.this.selectedRoute)) {
                    RouteFragment.this.playing = false;
                    onBackPressed();
                }
            }
            @Override
            public void onRouteSelected(MediaRouter router, RouteInfo route) { }
            @Override
            public void onRouteUnselected(MediaRouter router, RouteInfo route) { }
            @Override
            public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) { }
            @Override
            public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo route) { }
            @Override
            public void onProviderAdded(MediaRouter router, ProviderInfo provider) { }
            @Override
            public void onProviderRemoved(MediaRouter router, ProviderInfo provider) { }
            @Override
            public void onProviderChanged(MediaRouter router, ProviderInfo provider) { }
        };
    }

    /*********************************/
    /**    Interaction Control(s)   **/
    /*********************************/
    /**
     * Selects a route or starts playback (depending on current ListAdapter).
     */
    @OnItemClick(LIST)
    protected void routeSelected(int position) {
        if (list.getAdapter() == this.routeAdapter) {
            playlistMode(this.routeAdapter.getItem(position));
        }
        else {
            this.mediaRouterPlayService.play(position);
            changePlayPauseState(true);
        }
//        playTaiChi();
    }


    /*********************************/
    /**      Playback Control(s)    **/
    /*********************************/
    /**
     * Displays UPNP devices in the ListView.
     */
    private void deviceListMode() {
        this.controls.setVisibility(View.GONE);
        this.list.setAdapter(this.routeAdapter);
        disableTrackHighlight();
        this.selectedRoute = null;
        this.empty.setText(R.string.route_list_empty);
    }

    /**
     * Displays playlist for route in the ListView.
     */
    private void playlistMode(RouteInfo route) {
        this.selectedRoute = route;
        this.mediaRouterPlayService.setRoute(this.selectedRoute);
        list.setAdapter(this.playlistAdapter);
        controls.setVisibility(View.VISIBLE);
        if (this.startPlayingOnSelect != -1) {
            this.mediaRouterPlayService.play(this.startPlayingOnSelect);
            changePlayPauseState(true);
            this.startPlayingOnSelect = -1;
        }
        this.empty.setText(R.string.playlist_empty);
        this.list.post(new Runnable() {
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
        if (list.getAdapter() == this.routeAdapter ||
                this.mediaRouterPlayService == null || !isVisible())
            return;

        disableTrackHighlight();
        this.currentTrack = list.getChildAt(this.mediaRouterPlayService.getCurrentTrack()
                - list.getFirstVisiblePosition() + list.getHeaderViewsCount());
        if (this.currentTrack != null) {
            this.currentTrack.setBackgroundColor(
                    getResources().getColor(R.color.currently_playing_background));
        }
    }

    /**
     * Removes highlight from the item that was last highlighted.
     */
    private void disableTrackHighlight() {
        if (this.currentTrack != null) {
            this.currentTrack.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Unselects current media renderer if one is selected (with dialog).
     */
    @Override
    public boolean onBackPressed() {
        if (list.getAdapter() == this.playlistAdapter) {
            if (this.playing) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.exit_renderer)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        RouteFragment.this.mediaRouterPlayService.stop();
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
    @OnClick({SHUFFLE, PLAYPAUSE, REPEAT, PREVIOUS, NEXT})
    protected void controlButtonClicked(ImageButton button) {
        switch(button.getId()) {
            case PLAYPAUSE:
                if (this.playing) {
                    this.mediaRouterPlayService.pause();
                    changePlayPauseState(false);
                } else {
                    this.mediaRouterPlayService.resume();
                    scrollToCurrent();
                    changePlayPauseState(true);
                }
                break;
            case SHUFFLE:
                this.mediaRouterPlayService.toggleShuffleEnabled();
                applyColors();
                break;
            case PREVIOUS:
                this.previousTapCount++;
                Handler handler = new Handler();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        // Single tap.
                        RouteFragment.this.previousTapCount = 0;
                        RouteFragment.this.mediaRouterPlayService.play(RouteFragment.this.mediaRouterPlayService.getCurrentTrack());
                        changePlayPauseState(true);
                    }
                };
                if (this.previousTapCount == 1)
                    handler.postDelayed(r, ViewConfiguration.getDoubleTapTimeout());
                else if(this.previousTapCount == 2) {
                    // Double tap.
                    this.previousTapCount = 0;
                    this.mediaRouterPlayService.playPrevious();
                }
                break;
            case NEXT:
                boolean stillPlaying = this.mediaRouterPlayService.playNext();
                changePlayPauseState(stillPlaying);
                break;
            case REPEAT:
                this.mediaRouterPlayService.toggleRepeatEnabled();
                applyColors();
                break;
            default:
        }
    }
    /**
     * Enables or disables highlighting on shuffle/repeat buttons (depending
     * if they are enabled or disabled).
     */
    private void applyColors() {
        if (this.controlButtons==null || this.controlButtons.isEmpty()) return;
        int highlight = getResources().getColor(R.color.button_highlight);
        int transparent = getResources().getColor(android.R.color.transparent);
        // SHUFFLE
        this.controlButtons.get(0).setColorFilter((this.mediaRouterPlayService.isShuffleEnabled())
                ? highlight
                : transparent);
        // REPEAT
        this.controlButtons.get(2).setColorFilter((this.mediaRouterPlayService.isRepeatEnabled())
                ? highlight
                : transparent);
    }

    /**
     * Sends manual seek on progress bar to renderer.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            this.mediaRouterPlayService.seek(progress);
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

    /**
     * Applies the playlist and starts playing at position.
     */
    public void play(List<Item> playlist, int start) {
        this.playlistAdapter.clear();
        this.playlistAdapter.add(playlist);
        this.mediaRouterPlayService.setPlaylist(playlist);

        if (this.selectedRoute != null) {
            this.mediaRouterPlayService.play(start);
            changePlayPauseState(true);
        }
        else {
            Toast.makeText(getActivity(), R.string.select_route, Toast.LENGTH_SHORT)
                    .show();
            this.startPlayingOnSelect = start;
        }
    }
    public void increaseVolume() {
        this.mediaRouterPlayService.increaseVolume();
    }

    public void decreaseVolume() {
        this.mediaRouterPlayService.decreaseVolume();
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

        this.timeViews.get(0).setText(generateTimeString(currentTime));
        this.timeViews.get(1).setText(generateTimeString(totalTime));

        this.progress.setProgress(currentTime);
        this.progress.setMax(totalTime);

        if (status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_PLAYING ||
                status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_BUFFERING ||
                status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_PENDING) {
            changePlayPauseState(true);
        }
        else {
            changePlayPauseState(false);
        }

        if (list.getAdapter() == this.playlistAdapter) {
            enableTrackHighlight();
        }
    }

    /**
     * Changes the state of mPlayPause button to pause/resume according to
     * current playback state, also sets this.playing.
     *
     * @param playing True if an item is currently being played, false otherwise.
     */
    private void changePlayPauseState(boolean playing) {
        this.playing = playing;
        if (this.playing) {
            // PLAYPAUSE state changed
            this.controlButtons.get(1).setImageResource(R.drawable.ic_action_pause);
            this.controlButtons.get(1).setContentDescription(getResources().getString(R.string.route_pause));
            getMainActivity().setRefreshActionButtonState(false);
        }
        else {
            // PLAYPAUSE state changed
            this.controlButtons.get(1).setImageResource(R.drawable.ic_action_play);
            this.controlButtons.get(1).setContentDescription(getResources().getString(R.string.route_play));
            getMainActivity().setRefreshActionButtonState(true);
        }
    }

    /**
     * When in playlist mode, scrolls to the item that is currently playing.
     */
    public void scrollToCurrent() {
        if (this.mediaRouterPlayService != null) {
            list.smoothScrollToPosition(
                    this.mediaRouterPlayService.getCurrentTrack());
        }
    }

    /*********************************/
    /**   Route Adapter Definition  **/
    /*********************************/
    private class RouteAdapter extends BaseAdapter implements Insertable<RouteInfo> {
        private Context context;
        private List<RouteInfo> routes;
        public RouteAdapter(Context context) {
            this.context = context;
            this.routes = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return (this.routes==null)? 0 : this.routes.size();
        }

        @Override
        public RouteInfo getItem(int position) {
            return (this.routes==null)? null : this.routes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) holder = (ViewHolder) convertView.getTag();
            else{
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(LISTITEM, parent, false);
                holder = new ViewHolder(convertView);
            }
            convertView.setTag(holder);
            RouteInfo routeInfo = getItem(position);
            holder.title .setText(routeInfo.getName());
            holder.subtitle .setText(routeInfo.getDescription());
            return convertView;
        }
        @Override
        public void add(int position, @NonNull RouteInfo route) {
            if (this.routes!=null) {
                this.routes.add(position, route);
                notifyDataSetChanged();
            }
        }
        public void add(RouteInfo route) {
            if (this.routes!=null) {
                add(this.routes.size(), route);
            }
        }
        /**
         * Replacement for addAll, which is not implemented on lower API levels.
         */
        public void addAll(List<RouteInfo> routes) {
            for (RouteInfo r : routes) {
                this.routes.add(r);
            }
            notifyDataSetChanged();
        }
        public void remove(RouteInfo route) {
            this.routes.remove(route);
            notifyDataSetChanged();
        }
    }

    /*********************************/
    /**     List's View Holder      **/
    /*********************************/
    static class ViewHolder {
        @Bind(ITEM_TITLE) protected  TextView title;
        @Bind(ITEM_SUBTITLE) protected TextView subtitle;
        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

/*    protected void playTaiChi() {
        //DIDLContent didl = new DIDLContent();
        String album = ("Aeka in the dark");
        String creator = "9D Technolgoies"; // Required
        PersonWithRole artist = new PersonWithRole(creator, "Performer");
        MimeType mimeType = new MimeType("audio", "mpeg");

        List<Item> items = new ArrayList<>();
        items.add(new MusicTrack(
                "101", "3", //101 is the Item ID, 3 is the parent Container ID
                "Not Tai chi",
                creator, album, artist,
                new Res(mimeType, (long) 22222221, "00:04:11", (long) 81921, "https://s3-ap-southeast-1.amazonaws.com/ninedcloud/not+Tai+chi.wav")
        ));
        play(items,0);
    }*/

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}