	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.fragments;

	import android.animation.Animator;
	import android.animation.AnimatorInflater;
	import android.animation.AnimatorListenerAdapter;
	import android.animation.AnimatorSet;
	import android.app.Fragment;
	import android.app.ProgressDialog;
	import android.content.Context;
	import android.graphics.Point;
	import android.media.AudioManager;
	import android.media.MediaPlayer;
	import android.media.MediaPlayer.OnCompletionListener;
	import android.media.MediaPlayer.OnErrorListener;
	import android.media.MediaPlayer.OnPreparedListener;
	import android.net.Uri;
	import android.os.Build;
	import android.os.Bundle;
	import android.util.DisplayMetrics;
	import android.util.Log;
	import android.view.Display;
	import android.view.GestureDetector;
	import android.view.LayoutInflater;
	import android.view.MotionEvent;
	import android.view.SurfaceHolder;
	import android.view.SurfaceView;
	import android.view.View;
	import android.view.View.OnTouchListener;
	import android.view.ViewGroup;
	import android.view.ViewGroup.LayoutParams;
	import android.widget.ImageView;
	import android.widget.RelativeLayout;
	import android.widget.Toast;

	import com.nined.player.R;
	import com.nined.player.views.VideoProgressBar;

	import java.io.IOException;

	public class SimpleVideoFragment extends Fragment implements
								SurfaceHolder.Callback, 
								OnPreparedListener,
								OnErrorListener, 
								OnCompletionListener {
	/**
	 * Logging Assistants
	 */
	private static final String TAG = SimpleVideoFragment.class.getSimpleName();
	private static final boolean SHOW_LOG = false;
	private final static boolean SHOW_TOASTS = false;
	/** Views **/
	private ProgressDialog pDialog;
	private RelativeLayout main;
	private SurfaceView display;
	private SurfaceHolder holder;
	private VideoProgressBar progress;
	private ImageView play, pause;
	/** Media Control **/
	private MediaPlayer mediaplayer;
	private GestureDetector gesDetect;
	/** Initial Variables **/
	private Context context;
	private String vidAddress;
	private String mTitle;
	private long duration, curr;
	/** Constants **/
	/**
	 * Creates the View for this fragment after attaching to the Activity
	 * @return main RelativeLayout
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstaceState) {
		if (SHOW_LOG) Log.d(TAG, "onCreateView");
		main = new RelativeLayout(context);
		display = new SurfaceView(context);
		holder = display.getHolder();
		display.getHolder().addCallback(this);
		display.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 *  Override Touch Event Listener with our own implemented MyGestureListener
				 */
				gesDetect.onTouchEvent(event);
				return true;
			}
		});
		/*
		 * Creating the Play and Pause ImageViews
		 */
		RelativeLayout.LayoutParams center = new RelativeLayout.LayoutParams(
													RelativeLayout.LayoutParams.WRAP_CONTENT,
													RelativeLayout.LayoutParams.WRAP_CONTENT);
		center.addRule(RelativeLayout.CENTER_IN_PARENT);
		play = new ImageView(context);
		play.setImageResource(R.drawable.play_btn);
		play.setLayoutParams(center);
		play.setVisibility(View.INVISIBLE);
		pause = new ImageView(context);
		pause.setImageResource(R.drawable.pause_btn);
		pause.setLayoutParams(center);
		pause.setVisibility(View.INVISIBLE);

		/*
		 * Creating the Progress Bar to show how far you are in the video
		 */
		progress = new VideoProgressBar(context);
		/*
		 *  Putting Views together and returning the Bundled View in main.
		 */
		RelativeLayout.LayoutParams wrap = new RelativeLayout.LayoutParams(
													RelativeLayout.LayoutParams.WRAP_CONTENT,
													RelativeLayout.LayoutParams.WRAP_CONTENT);
		wrap.addRule(RelativeLayout.CENTER_HORIZONTAL);
		main.setLayoutParams(wrap);
		main.addView(display);
		main.addView(play);
		main.addView(pause);
		main.addView(progress);
        return main;
	}
	/**
	 * Triggers when Fragment becomes functional.
	 */
	@Override
	public void onResume() {
		super.onResume();
        if (SHOW_LOG) Log.d(TAG, "onResume");
	}
	/**
	 * Triggers when VideoFragment is brought to background
	 */
	@Override
	public void onPause() {
		super.onPause();
		if(SHOW_LOG) Log.d(TAG, "onPause");
		curr = mediaplayer.getCurrentPosition();
		if (SHOW_LOG) Log.v(TAG, String.format("current time:%s", curr));
		if (SHOW_LOG) Log.v(TAG, String.format("duration:%s", duration));
	}
	/**
	 * Triggers when VideoFragment is destroyed
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(SHOW_LOG) Log.d(TAG, "onDestroy");
	}
	/**
	 * Return the view showing the video
	 * @return main RelativeLayout
	 */
	@Override
	public View getView() {
		if(SHOW_LOG) Log.d(TAG, "getView");
		return main;
	}
	/**
	 * Set display's url-path to video file
	 * @param url
	 * @return
	 */
	public void setVideoURI (Uri url) {
		if(SHOW_LOG) Log.d(TAG, "setVideoURI");
	}
	/**
	 * requestFocus to display
	 * @return
	 */
	public void requestFocus() {
		if(SHOW_LOG) Log.d(TAG, "requestFocus");
		getView().requestFocus();
	}
	/**
	 * Constructor takes in Application Context, Title and URL location of the video 
	 * @param c Application Context used in creating views.
	 * @param title String title to be shown once tapped
	 * @param url String url to be parsed and loaded by display
	 */
	public SimpleVideoFragment(Context c, String title, String url) {
		// super();
		if (SHOW_LOG) Log.d(TAG, "Constructor");
		// Initialize variables
		context = c;
		mTitle = title;
		vidAddress = url;
		//TODO: Don't do this
		vidAddress = "http://192.168.2.102/mooc/mooc_final.mp4";
		curr = 0;
		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		/*
		 *  Set Progress Dialog to show before video is ready
		 */
		pDialog = new ProgressDialog(context);
		pDialog.setTitle(title);
		pDialog.setMessage(context.getResources().getString(R.string.prompt_buffering));
		pDialog.setIndeterminate(false);
	    pDialog.setCancelable(true);
	    pDialog.show();
	    /*
	     *  Instantiate MyHandler & GestureDetector
	     */
	    gesDetect = new GestureDetector(context, new MyGestureListener());
	}
	/**
	 * Once the Surface is created, create and set up mediaPlayer then attach to holder for display
	 */
	/*************************************/
	/**			Surface Callbacks		**/
	/*************************************/
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (SHOW_LOG) Log.d(TAG, "surfaceCreated");
		/*
		 * Create the mediaPlayer using LibVLC library
		 */
		mediaplayer = MediaPlayer.create(context, Uri.parse(vidAddress));
		mediaplayer.seekTo((int) curr);
		duration = mediaplayer.getDuration();
		Log.d(TAG, String.format("current time:%s",  curr));
		Log.d(TAG, String.format("duration time:%s", duration));
		this.mediaplayer.setOnPreparedListener(this);
		this.mediaplayer.setOnErrorListener(this);
		this.mediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		this.mediaplayer.setScreenOnWhilePlaying(true);
		this.mediaplayer.setDisplay(holder);
        this.holder.setKeepScreenOn(true);
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (SHOW_LOG) Log.d(TAG, "surfaceChanged");
		resetSize();
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (SHOW_LOG) Log.d(TAG, "surfaceDestroyed");
		if (mediaplayer==null) return;
		progress.setReleased(true);
		mediaplayer.stop();
		mediaplayer.release();
		mediaplayer = null;
	}
	/*************************************/
	/**	Gesture Control: Play/Pause	Btn	**/
	/*************************************/
	/**
	 * GestureDetector-extended Class created to handle all of the gestures for Video Screen.
	 * @author Aekasitt Guruvanich, 9D Technologies Limited.
	 */
	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent event) {
			if (SHOW_LOG) Log.d(TAG, "onDoubleTap");
			return false;
		}
		@Override
		public void onLongPress(MotionEvent event) {
			if (SHOW_LOG) Log.d(TAG, "onLongPress");
			if (SHOW_TOASTS) Toast.makeText(context, String.format("Currently Viewing: %s", mTitle), Toast.LENGTH_SHORT).show();
			return;
		}
		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			if (SHOW_LOG) Log.d(TAG, "onSingleTapUp");
			playPause();
			return false;
		}
		@Override
		public boolean onDown(MotionEvent event) {
			if (SHOW_LOG) Log.d(TAG, "Can't touch this");
			return false;
		}
	}
	/**
	 * Toggles the state of mediaplayer and show briefly on the display
	 */
	public void playPause() {
		if (mediaplayer.isPlaying()) {
			resetSize();
			mediaplayer.pause();
			curr = mediaplayer.getCurrentPosition();
			pause.setVisibility(View.VISIBLE);
			AnimatorSet fadeAnim = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.fade_out);
			fadeAnim.setTarget(pause);
			fadeAnim.addListener(new AnimatorListenerAdapter() {
	        	@Override
	        	public void onAnimationEnd(Animator anim) {
	        		pause.setVisibility(View.INVISIBLE);
	        	}
	        });
			fadeAnim.start();
		} else {
			mediaplayer.seekTo((int) curr);
			mediaplayer.start();
			try {
				progress.setMedia(mediaplayer);
			} catch (IOException e) {
				if (SHOW_LOG) Log.d(TAG, "VideoProgressBar cannot find media");
				if (SHOW_TOASTS) Toast.makeText(context, "VideoProgressBar cannot find media.", Toast.LENGTH_SHORT).show();
			}
			play.setVisibility(View.VISIBLE);
			AnimatorSet fadeAnim = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.fade_out);
	        fadeAnim.setTarget(play);
	        fadeAnim.addListener(new AnimatorListenerAdapter() {
	        	@Override
	        	public void onAnimationEnd(Animator anim) {
	        		play.setVisibility(View.INVISIBLE);
	        	}
	        });
	        fadeAnim.start();
		}
	}
	/*************************************/
	/**		Event-Handling Functions	**/
	/*************************************/
	/**
	 * Once the display triggers onPrepared, Progress Dialog dismisses and Video starts automatically
	 * @return
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		if (SHOW_LOG) Log.d(TAG, "onPrepared");
		pDialog.dismiss();
	    mediaplayer.start();
		try {
			progress.setMedia(mediaplayer);
			RelativeLayout.LayoutParams below = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			below.addRule(RelativeLayout.END_OF, display.getId());
			progress.setLayoutParams(below);
		} catch (IOException e) {
			if (SHOW_LOG) Log.e(TAG, "VideoProgressBar cannot find media");
			if (SHOW_TOASTS) Toast.makeText(context, "VideoProgressBar cannot find media!", Toast.LENGTH_SHORT).show();
		}
	}
	/**
	 * Error Handling.
	 * @param what
	 * @param extra
	 * @return boolean (default: true)
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		if (SHOW_LOG) Log.e(TAG, String.format("Error(%s%s)", what, extra));
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			mp.reset();
		} else if(what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
	    	mp.reset();
		}
		return true;  
	}
	/**
	 * Activated when LibVLC media completes playback.
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (SHOW_LOG) Log.d(TAG, "Media playback finished");
		if (SHOW_TOASTS) Toast.makeText(context, "Media playback finished!", Toast.LENGTH_SHORT).show();
	}
	/*************************************/
	/**			Size Does Matter		**/
	/*************************************/
	/**
	 * Sets the size of the screen holder according to the screen size at hand.
	 */
	private void resetSize() {
		if (SHOW_LOG) Log.d(TAG, "setSize");
		int videoWidth = mediaplayer.getVideoWidth();
		int videoHeight = mediaplayer.getVideoHeight();
		Point screenSize = getRealSize();
		int width = screenSize.x;
		int height = (int) ((float) videoHeight / (float) videoWidth * (float) width);
		// Set the size of the media player according to mediaplayer
		LayoutParams layoutParams = display.getLayoutParams();
	    layoutParams.width = width;
	    layoutParams.height = height;
	    display.setLayoutParams(layoutParams);
	    display.invalidate();
	    // Useful Logs
	    if (SHOW_LOG) Log.v(TAG, "Device Screen Size: ("+screenSize.x+", "+screenSize.y+ ")");
	    if (SHOW_LOG) Log.v(TAG, "Display Screen Size: ("+display.getWidth()+", " +display.getHeight()+")");
	    if (SHOW_TOASTS) 
	    	Toast.makeText(context, String.format("Device Screen Size: (%s, %s)\n Display Screen Size: (%s, %s)\n Media Size: (%s, %s)", 
	    		screenSize.x, screenSize.y,
	    		display.getWidth(), display.getHeight(),
	    		videoWidth, videoHeight)
	    		, Toast.LENGTH_LONG).show();
	}
	/**
	 * Get the screen size
	 * @return Point with x and y equal to Display Screen width and height
	 */
	private Point getRealSize() {
		Point realSize = new Point();
		Display d = getActivity().getWindowManager().getDefaultDisplay();
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
}
