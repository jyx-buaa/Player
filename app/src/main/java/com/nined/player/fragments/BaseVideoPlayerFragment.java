    /**
     * @author Aekasitt Guruvanich, 9D Technologies
     * on 7/21/2015.
     */
package com.nined.player.fragments;

    import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.AnimatorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nined.player.R;
import com.nined.player.utils.PrefUtils;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.vlc.util.VLCInstance;

import java.io.File;
import java.lang.ref.WeakReference;

    public abstract class BaseVideoPlayerFragment
        extends Fragment
        implements  IVideoPlayer,
                    SurfaceHolder.Callback
{
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = BaseVideoPlayerFragment.class.getSimpleName();
    private static final boolean SHOW_LOG = true;
    private static final boolean SHOW_TOASTS = true;

    /*********************************/
    /**          Constant(s)        **/
    /*********************************/
    private static final int VIDEO_SIZE_CHANGED = -1;
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    @DrawableRes
    private static final int DEFAULT_PLAY_DRAWABLE = R.drawable.play_btn;
    @DrawableRes
    private static final int DEFAULT_PAUSE_DRAWABLE = R.drawable.pause_btn;
    @AnimatorRes
    private static final int DEFAULT_TOGGLE_ANIMATOR = R.animator.fade_out;
    private static final String DEFAULT_PROMPT_BUFFERING = "Buffering. . .";
    /*
     * Saved Variables on SavedInstanceState and/or Shared Preferences
     */
    private static final String PREFS_RESUME_POSITION = "BaseVideoPlayerFragment.Prefs.ResumePosition";
    private static final String INSTANCE_RESUME_POSITION = "BaseVideoPlayerFragment.Instance.ResumePosition";
    /*
     * Error Messages
     */
    private static final String ERROR_NO_MEDIA = "No media entered.";
    private static final String ERROR_INSTANTIATION = "LibVLC instantiation error: %s";
    /*
     * Fragment Arguments
     */
    protected static final String ARGS_TITLE = "BaseVideoPlayerFragment.Arguments.Title";
    protected static final String ARGS_LOCATION = "BaseVideoPlayerFragment.Arguments.Location";

    /*********************************/
    /**          Argument(s)          **/
    /*********************************/
    protected String PROMPT_BUFFERRING = DEFAULT_PROMPT_BUFFERING;
    @DrawableRes
    protected int PLAY = DEFAULT_PLAY_DRAWABLE;
    @DrawableRes
    protected int PAUSE = DEFAULT_PAUSE_DRAWABLE;
    @AnimatorRes
    protected int ANIM_TOGGLE = DEFAULT_TOGGLE_ANIMATOR;

    /*********************************/
    /**           View(s)           **/
    /*********************************/
    private ProgressDialog pDialog;
    private RelativeLayout root;
    private SurfaceView display;
    private SurfaceHolder holder;
    private ImageView play, pause;

    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private GestureDetector gesDetect;
    private Handler handler;
    private LibVLC libVLC;
    private Media media;
    private String title, location;
    private long resumePosition;
    private static final int currentSize = SURFACE_BEST_FIT;

    private boolean ready = false;
    private boolean ended = false;
    private boolean seeking = false;
    private boolean reloaded = false;

    private int videoWidth, videoHeight;
    private int videoVisibleWidth, videoVisibleHeight;
    private int sarNum, sarDen;

    private boolean disabledHardwareAcceleration = false;
    private int previousHardWareAcceleration;

    /*********************************/
    /**          Constructor        **/
    /*********************************/
    protected BaseVideoPlayerFragment() { super(); }

    /*********************************/
    /**     Lifecycle Override(s)   **/
    /*********************************/
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        this.handler = new MyHandler();
        this.gesDetect = new GestureDetector(getActivity(), new MyGestureListener());
        this.resumePosition = (savedInstanceState !=null)?
                savedInstanceState.getInt(BaseVideoPlayerFragment.INSTANCE_RESUME_POSITION) : 0;
                // If saved resume position somewhere, also 0 if none found.
                // if no saved instance state found
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (getActivity()==null)?
                super.onCreateView(inflater, container, savedInstanceState):
                setUpViews(getActivity());
        // TODO ButterKnife.bind(this, view);
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        libVLC = VLCInstance.get();
        libVLC.setVerboseMode(false);
        holder = getDisplay().getHolder();
        holder.addCallback(this);
        libVLC.attachSurface(holder.getSurface(), this);
        // Add VLC instance handler
        EventHandler eventHandler = EventHandler.getInstance();
        eventHandler.addHandler(handler);
        //libVLC = VLCInstance.get(); //TODO Update LibVLC version.
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getActivity();
        Bundle args = getArguments();
        if (args==null) {
            if (SHOW_LOG) Log.e(TAG, ERROR_NO_MEDIA);
            if (SHOW_TOASTS && context!=null) Toast.makeText(context, ERROR_NO_MEDIA, Toast.LENGTH_SHORT).show();
            return;
        }
        this.title = args.getString(BaseVideoPlayerFragment.ARGS_TITLE);
        this.location = args.getString(BaseVideoPlayerFragment.ARGS_LOCATION);
        setUpProgressDialog(context);
        //
        resetSize();
        loadMedia();
        // TODO


    }

    @Override
    public void onPause() {
        super.onPause();
        if (libVLC != null) {
            long currentTime = libVLC.getTime();
            PrefUtils.save(getActivity(), BaseVideoPlayerFragment.PREFS_RESUME_POSITION, currentTime);
            libVLC.stop();
        }
        getDisplay().setKeepScreenOn(false);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove VLC instance handler
        EventHandler eventHandler = EventHandler.getInstance();
        eventHandler.removeHandler(handler);
        // Destroy VLC instance
        libVLC.destroy();
        //PrefUtils.save(getActivity(), PREFS_RESUME_POSITION, this.resumePosition);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //TODO ButterKnife.unbind(this);
    }
    /*********************************/
    /**         Options Menu        **/
    /*********************************/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.player, menu); // TODO create a cast button
        menu.findItem(R.id.action_cast).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play: {
                togglePlayPause();
                break;
            }
            case R.id.action_pause: {
                togglePlayPause();
                break;
            }
            case R.id.action_refresh: {
                loadMedia();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /*********************************/
    /**      Surface Callback(s)    **/
    /*********************************/
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        if (libVLC != null) libVLC.detachSurface();
    }

    /*********************************/
    /**   IVideoPlayer Override(s)  **/
    /*********************************/
    @Override
    public void setSurfaceLayout(int width, int height, int visible_width, int visible_height, int sar_num, int sar_den) {

    }

    @Override
    public int configureSurface(Surface surface, int width, int height, int hal) {
        return 0;
    }
    /*********************************/
    /**       LibVLC Set-up(s)      **/
    /*********************************/
    protected void loadMedia()
    {
        if (SHOW_LOG) Log.d(TAG, "loadMedia");
        String LOCAL_PREFIX = "file://";
        String ANDROID_RESOURCE = "android.resource://";
        String HTTP_PREFIX = "http://";
        String HTTPS_PREFIX = "https://";
        String PACKAGE_NAME = getActivity().getPackageName();
        setProgressVisible(true);
        String path;
        if (TextUtils.isEmpty(location)) return;
        if (!location.startsWith(LOCAL_PREFIX) && !location.startsWith(HTTP_PREFIX) && !location.startsWith(HTTPS_PREFIX))
            path = ANDROID_RESOURCE + PACKAGE_NAME + /**File.separator + "raw" +**/ File.separator + location;
        else path = location;
        if (SHOW_LOG) Log.w(TAG, String.format("Location: %s", location));
        if (SHOW_LOG) Log.w(TAG, String.format("Path: %s", path));
        setReady(true);
        getDisplay().setKeepScreenOn(true);
        if (path.startsWith("http")) {
            path = LibVLC.PathToURI(path);
        }

        libVLC.playMRL(path);
        setEnded(false);
        /*handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadMedia();
                //setProgressVisible(true); //Redundant?
            }
        }, 2000);*/
        //TODO
        //long resumeTime = PrefUtils.get(getActivity(), MainActivity.RESUME_TIME, this.resumePosition);
        //if (resumeTime> 0) libVLC.setTime(resumeTime);
    }

    /*********************************/
    /**        View Set-up(s)       **/
    /*********************************/
    /**
     *  Putting Views together and returning the Bundled View in root View.
     *  @return root
	 */
    protected View setUpViews(@NonNull Context context)
    {
        root = new RelativeLayout(context);
        display = new SurfaceView(context);
        display.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gesDetect.onTouchEvent(event);
                return true;
            }
        });
        play  = setUpImageView(PLAY);
        pause = setUpImageView(PAUSE);
        RelativeLayout.LayoutParams wrap = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        wrap.addRule(RelativeLayout.CENTER_HORIZONTAL);
        root.setLayoutParams(wrap);
        root.addView(display);
        root.addView(play);
        root.addView(pause);
        return root;
    }

    /**
     * Set up ImageViews for the Screen to show using imageDrawable
     * @param imageId drawable resource id to inflate
     * @return imageView with Center layout params
     */
    protected ImageView setUpImageView(@DrawableRes int imageId) {
        if (SHOW_LOG) Log.d(TAG, "setUpImageView");
        RelativeLayout.LayoutParams center = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        center.addRule(RelativeLayout.CENTER_IN_PARENT);
        ImageView imageView = new ImageView(getActivity());
        imageView.setImageResource(imageId);
        imageView.setLayoutParams(center);
        imageView.setVisibility(View.INVISIBLE);
        return imageView;
    }
    /**
     *  Set Progress Dialog to show before video is ready
     *  @param context applicationContext used to launch Progress Dialog
     */
    protected void setUpProgressDialog(Context context) {
        pDialog = new ProgressDialog(context);
        pDialog.setTitle(title);
        pDialog.setMessage(PROMPT_BUFFERRING);
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);
        pDialog.show();
    }

    /**
     * Resets all the View size.
     */
    protected void resetSize() {
        if (SHOW_LOG) Log.d(TAG, "resetSize");
        this.videoWidth = 200;//libVLC.getPlayerState();
        this.videoHeight = 200;//media.getHeight();
        Point screenSize = getRealSize();
        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y;
        LayoutParams layoutParams = display.getLayoutParams();
        layoutParams.width = screenWidth;
        layoutParams.height = videoHeight * (screenHeight / screenWidth);
        display.setLayoutParams(layoutParams);
        display.invalidate();
        this.videoVisibleWidth = display.getWidth();
        this.videoVisibleHeight = display.getHeight();
        if (SHOW_TOASTS) toastSize(screenWidth, screenHeight, videoWidth, videoHeight, videoVisibleWidth, videoVisibleHeight);
    }

    /**
     * @return realSize of the screen in pixels
     */
    protected Point getRealSize() {
        if (SHOW_LOG) Log.d(TAG, "getRealSize");
        Point realSize = new Point();
        Activity act = getActivity();
        if (act==null) return realSize;
        Display d = act.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        int widthPixels  = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
            // includes window decorations (statusbar bar/menu bar)
        else if (Build.VERSION.SDK_INT >= 17) {
            try {
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                widthPixels = realSize.x;
                heightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        }
        realSize.set(widthPixels, heightPixels);
        return realSize;
    }

    /**
     * Make a toast of all the size
     * @param screenWidth width of screen
     * @param screenHeight height of screen
     * @param videoWidth width of video
     * @param videoHeight height of video
     * @param videoVisibleWidth visible width of video
     * @param videoVisibleHeight visible height of video
     */
    protected void toastSize(int screenWidth, int screenHeight,
                             int videoWidth, int videoHeight,
                             int videoVisibleWidth, int videoVisibleHeight) {
        Context context = getActivity();
        if (!SHOW_TOASTS || context == null) return;
        String sizes = String.format(
                "Device Screen Size: (%d, %d)\nMedia Size: (%d, %d)\nDisplay Screen Size: (%d, %d)",
                screenWidth, screenHeight, videoWidth, videoHeight, videoVisibleWidth, videoVisibleHeight) ;
        Toast.makeText(context, sizes, Toast.LENGTH_SHORT).show();
        if (SHOW_LOG) Log.i(TAG, sizes);
    }

    /*********************************/
    /**         Animation(s)        **/
    /*********************************/
    protected void applyAnimation(
            @NonNull final View view,
            @AnimatorRes int animator)
    {
        if (SHOW_LOG) Log.d(TAG, "applyAnimation");
        Context context = getActivity();
        if (context==null) return;
        view.setVisibility(View.VISIBLE);
        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(context, animator);
        animatorSet.setTarget(view);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                view.setVisibility(View.INVISIBLE);
            }
        });
        animatorSet.start();
    }

    /*********************************/
    /**     Media Playback Handler  **/
    /*********************************/
    protected void resumeMedia() {
        // TODO
    }

    protected void togglePlayPause() {
        if (SHOW_LOG) Log.d(TAG, "playPause");
        if (libVLC==null) return;
        if (isEnded()) {
            this.reloaded = true;
            this.loadMedia();
        } else {
            if (libVLC.isPlaying()) {
                libVLC.pause();
                display.setKeepScreenOn(false);
                applyAnimation(pause, ANIM_TOGGLE);
            } else {
                libVLC.play();
                display.setKeepScreenOn(true);
                applyAnimation(play, ANIM_TOGGLE);
            }
        }
    }

    protected void seek(int delta) {
        if (libVLC==null || (libVLC.getTime() <= 0 && !seeking)) return;
        long position = libVLC.getTime() + delta;
        if (position<0) position = 0;
        setCurrentTime(position);
        showOverlay();
        onProgressChanged(libVLC.getTime(), libVLC.getLength());
    }

    /*********************************/
    /**  Private [Gesture Listener] **/
    /*********************************/
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int HORIZONTAL_THRESHOLD = 30;
        private static final int SKIP_INTERVAL = 10000;
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            boolean handled = super.onSingleTapUp(e);
            if (SHOW_LOG) Log.d(TAG, "onSingleTapUp");
            togglePlayPause();
            Context context = getActivity();
            if (SHOW_TOASTS && context!=null)
                Toast.makeText(context,
                        String.format("lalalala"),
                        Toast.LENGTH_SHORT)
                        .show();
            return handled;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            if (SHOW_LOG) Log.d(TAG, "onLongPress");
            Context context = getActivity();
            if (SHOW_TOASTS && context!=null)
                Toast.makeText(context,
                        String.format("Currently viewing %s", title),
                        Toast.LENGTH_SHORT)
                        .show();
        }

        @Override
        public boolean onScroll(MotionEvent start, MotionEvent end, float distanceX, float distanceY) {
            if (Math.abs(distanceX) > HORIZONTAL_THRESHOLD) {
                if (distanceX < 0) seek(SKIP_INTERVAL);
                else if (distanceX>0) seek(-SKIP_INTERVAL);
            }
            return super.onScroll(start, end, distanceX, distanceY);
        }
    }

    /*********************************/
    /**      Private [Handler]      **/
    /*********************************/
    private class MyHandler extends Handler {
        private WeakReference<BaseVideoPlayerFragment> owner;
        public MyHandler () {
            owner = new WeakReference<>(BaseVideoPlayerFragment.this);
        }
        @Override
        public void handleMessage(Message message) {
            BaseVideoPlayerFragment frag = owner.get();
            Bundle bundle = message.getData();
            if (message.what == VIDEO_SIZE_CHANGED) {
                frag.resetSize();
                holder.setFixedSize(message.arg1, message.arg2);
                display.invalidate();
                return;
            }
            switch (bundle.getInt("event")) {
                case EventHandler.MediaPlayerPlaying:
                { frag.onReady();                    break; }
                case EventHandler.MediaPlayerEndReached:
                { frag.onCompletion();                  break; }
                case EventHandler.MediaPlayerEncounteredError:
                { frag.onError();                       break; }
                case EventHandler.HardwareAccelerationError :
                { frag.onHardwareAccelerationError();   break; }
                default:
            }
        }
    }

    /*********************************/
    /**VLC Event Handler Override(s)**/
    /*********************************/
    public void eventHardwareAccelerationError() {
        if (SHOW_LOG) Log.d(TAG, "eventHardwareAccelerationError");
        EventHandler eventHandler = EventHandler.getInstance();
        eventHandler.callback(EventHandler.HardwareAccelerationError, new Bundle());
    }
    protected void onReady() {
        if (pDialog!=null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }
    protected void onCompletion() {
        if (SHOW_LOG) Log.d(TAG, "onCompletion");
        setEnded(true);
    }
    protected void onError() {
        if (SHOW_LOG) Log.d(TAG, "onError");
    }
    protected void onHardwareAccelerationError() {
        if (SHOW_LOG) Log.d(TAG, "onHardwareAccelerationError");
    }

    private void handleHardwareAccelerationError() {
        if (SHOW_LOG) Log.d(TAG, "handleHardwareAccelerationError");
        if (libVLC!=null) libVLC.stop();
        onHardwareAccelerationError();
    }
    /*********************************/
    /**     Getters - Setters       **/
    /*********************************/
    /**
     * @return curent playing time of currently playing media, -1 if null
     */
    public long getCurrentTime() {
        return (libVLC!=null)? libVLC.getTime() : -1;
    }
    /**
     * @param currentTime to be set
     */
    public void setCurrentTime(long currentTime) {
        if (libVLC!=null)
            libVLC.setTime(currentTime);
    }
    /**
     * @return duration of currently playing media, 0 if null
     */
    public long getDuration() {
        return (libVLC!=null)? libVLC.getLength() : 0;
    }

    /**
     * @return true if media is ready to be played, false otherwise
     */
    public boolean isReady() {
        return this.ready;
    }

    /**
     * @param ready to be set
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }
    /**
     * @return true if media playback has ended, false otherwise
     */
    public boolean isEnded() {
        return this.ended;
    }

    /**
     * @param ended to be set
     */
    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    /**
     * @return true if seeking is in progress, false otherwise
     */
    public boolean isSeeking() {
        return this.seeking;
    }

    /**
     * @param seeking to be set
     */
    public void setSeeking(boolean seeking) {
        this.seeking = seeking;
    }

    /**
     * @return display, surfaceView used for showing video stream.
     */
    public SurfaceView getDisplay() {
        return this.display;
    }
    /*********************************/
    /**    Abstract Function(s)     **/
    /*********************************/
    protected abstract void showOverlay();
    protected abstract void setProgressVisible(boolean visible);
    protected abstract void onProgressChanged(long currentTime, long duration);
}
