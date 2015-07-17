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
	import android.graphics.ImageFormat;
	import android.graphics.PixelFormat;
	import android.graphics.Point;
	import android.media.AudioManager;
	import android.net.Uri;
	import android.os.Build;
	import android.os.Bundle;
	import android.os.Handler;
	import android.os.Message;
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

	import org.videolan.libvlc.EventHandler;
	import org.videolan.libvlc.IVideoPlayer;
	import org.videolan.libvlc.LibVLC;
	import org.videolan.libvlc.Media;
	import org.videolan.libvlc.MediaList;

	import java.lang.ref.WeakReference;

public class VideoFragment extends Fragment implements SurfaceHolder.Callback {
	/**
	 * TAG used for Logging.
	 */
	private static final String TAG = VideoFragment.class.getSimpleName();
	private static final String PROMPT_BUFFERRING = "buffering";
	/** Views **/
	private ProgressDialog pDialog;
	private RelativeLayout main;
	private SurfaceView display;
	private SurfaceHolder holder;
	private ImageView play, pause;
	/** Media Control **/
	private LibVLC mediaplayer;
	private Handler mHandler;
	private GestureDetector gesDetect;
	/** Audio Control **/
	private AudioManager audi;
	/** Initial Variables **/
	private Context context;
	private String vidAddress;
	private String mTitle;
	private long duration, curr;
	/** Constants **/
	private final static int VIDEO_SIZE_CHANGED = -1;
	private final static boolean SHOW_TOASTS = false;
	/**
	 * Creates the View for this fragment after attaching to the Activity
	 * @return main RelativeLayout
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstaceState) {
		Log.d(TAG, "onCreateView");
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
        return main;
	}
	/**
	 * Triggers when Fragment becomes functional.
	 */
	@Override
	public void onResume() {
		super.onResume();
        Log.d(TAG, "onResume");
	}
	/**
	 * Triggers when VideoFragment is brought to background
	 */
	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		curr = mediaplayer.getTime();
		Log.v(TAG, String.format("current time:%s", curr));
		Log.v(TAG, String.format("duration:%s", duration));
		releasePlayer();
	}
	/**
	 * Triggers when VideoFragment is destroyed
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		releasePlayer();
	}
	/**
	 * Return the view showing the video
	 * @return main RelativeLayout
	 */
	@Override
	public View getView() {
		Log.d(TAG, "getView");
		return main;
	}
	/**
	 * Set display's url-path to video file
	 * @param url
	 * @return
	 */
	public void setVideoURI (Uri url) {
		Log.d(TAG, "setVideoURI");
	}
	/**
	 * requestFocus to display
	 * @return
	 */
	public void requestFocus() {
		Log.d(TAG, "requestFocus");
		getView().requestFocus();
	}
	/**
	 * Constructor takes in Application Context, Title and URL location of the video 
	 * @param c Application Context used in creating views.
	 * @param title String title to be shown once tapped
	 * @param url String url to be parsed and loaded by display
	 */
	public VideoFragment(Context c, String title, String url) {
		// super();
		Log.d(TAG, "Constructor");
		// Initialize variables
		context = c;
		mTitle = title;
		vidAddress = url;
		curr = 0;
		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		/*
		 *  Set Progress Dialog to show before video is ready
		 */
		pDialog = new ProgressDialog(context);
		pDialog.setTitle(title);
		pDialog.setMessage(PROMPT_BUFFERRING);
		pDialog.setIndeterminate(false);
	    pDialog.setCancelable(true);
	    pDialog.show();
	    /*
	     *  Instantiate MyHandler & GestureDetector
	     */
	    mHandler = new MyHandler();
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
		Log.d(TAG, "surfaceCreated");
		Log.d(TAG, vidAddress);
		/*
		 * Create the mediaPlayer using LibVLC library
		 */
		mediaplayer = createPlayer(null, vidAddress);
		if (mediaplayer==null) {
			Log.d(TAG, "media is null");
			if (SHOW_TOASTS) Toast.makeText(context, "media is null", Toast.LENGTH_SHORT).show();
			return;
		}
		mediaplayer.setTime(curr);
		duration = mediaplayer.getMediaList().getMedia(0).getLength();
		Log.d(TAG, String.format("duration time:%s", curr));
        holder.setKeepScreenOn(true);
        onPrepared();
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "surfaceChanged");
		if (mediaplayer != null) {
            mediaplayer.attachSurface(holder.getSurface(), 
            		new IVideoPlayer() {
            		@Override
            		public void setSurfaceSize(int width, int height, int visible_width,
            			int visible_height, int sar_num, int sar_den) {
            			Log.d(TAG, "setSurfaceSize");
            			Message msg = Message.obtain(mHandler, VIDEO_SIZE_CHANGED, width, height);
            			msg.sendToTarget();
            			mediaplayer.setTime(curr);
            		}
            	}
            );
            mediaplayer.setTime(curr);
		}
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
//		progress.release();
		if (mediaplayer==null) return;
		mediaplayer.stop();
		mediaplayer.detachSurface();
		mediaplayer.destroy();
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
			Log.d(TAG, "onDoubleTap");
			return false;
		}
		@Override
		public void onLongPress(MotionEvent event) {
			Log.d(TAG, "onLongPress");
			if (SHOW_TOASTS) Toast.makeText(context, String.format("Currently Viewing: %s", mTitle), Toast.LENGTH_SHORT).show();
			return;
		}
		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			Log.d(TAG, "onSingleTapUp");
			playPause();
			return false;
		}
		@Override
		public boolean onDown(MotionEvent event) {
			Log.d(TAG, "Can't touch this");
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
			curr = mediaplayer.getTime();
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
			mediaplayer.setTime(curr);
			mediaplayer.play();
/*			try {
				progress.setMedia(media);
			} catch (IOException e) {
				Log.d(TAG, "VideoProgressBar cannot find media");
				if (SHOW_TOASTS) Toast.makeText(context, "VideoProgressBar cannot find media.", Toast.LENGTH_SHORT).show();
			}*/
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
	/**		LibVLC Creation/Release		**/
	/*************************************/
	/**
	 * Creates and Prepares Media File to be displayed on display by using MediaPlayer.create(..);
	 * @param folder_name Folder inside this application package where the file is located, 
	 * @param file_name File name, without extension, supported by MediaPlayer class {For example: mp4 with AAC audio}
	 * @return
	 */
	private LibVLC createPlayer(String folder_name, String file_name) {
		Log.d(TAG, "createPlayer");
		if (file_name.length()<1) return null;
		releasePlayer();
		try {
			LibVLC player = LibVLC.getInstance();
			if (player==null) {
				Log.e(TAG, "LibVLC cannot be instantiated.");
				if (SHOW_TOASTS) Toast.makeText(context, "LibVLC cannot be instantiated.", Toast.LENGTH_SHORT).show();
				return null;
			}
			Log.d(TAG, player.toString());
			player.setNetworkCaching(20000);
			player.init(context);
			player.setSubtitlesEncoding("");
			player.setTimeStretching(true);
			player.setVerboseMode(true);
			if (Build.VERSION.SDK_INT < 14) { // For Devices with android Jellybean and above
				holder.setFormat(ImageFormat.YV12);
				player.setVout(LibVLC.VOUT_OPEGLES2);
				player.setChroma("YV12");
				player.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DISABLED);
			} else { 
				holder.setFormat(PixelFormat.RGBX_8888);
				player.setVout(LibVLC.VOUT_ANDROID_SURFACE);
				player.setChroma("RV32");
				player.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);
			}
			/*
			 * Audio Output Initiation
			 */
			audi = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			audi.setStreamSolo(AudioManager.STREAM_MUSIC, true);
			player.setAout(LibVLC.AOUT_AUDIOTRACK_JAVA);
			/*
			 *  Add Handler to EventHandler
			 */
			EventHandler.getInstance().addHandler(mHandler);
			/*
			 * Clear Current Playlist
			 */
			MediaList list = player.getMediaList();
			list.clear();
			/*
			 * Add File to Playlist
			 */
			String[] locations = {"http://192.168.2.102:8080/surround/%s", 
									"file:///mnt/sda1/%s", 
									"file:///sdcard/%s", 
									"android.resource://"+context.getPackageName()+"/raw/%s"};
			Media video = null;
			int i = 0;
			do {
				String path = String.format(locations[i++], file_name);
				video = new Media(player, LibVLC.PathToURI(path));
			} while (video.getLength()<1);
			switch (i-1) {
			case 0: {
				Log.d(TAG, "Media created via server");
				if (SHOW_TOASTS) Toast.makeText(context, "Media created via server", Toast.LENGTH_SHORT).show();
				break;
			}
			case 1: {
				Log.d(TAG, "Media created via USB storage access");
				if (SHOW_TOASTS) Toast.makeText(context, "Media created via USB storage access", Toast.LENGTH_SHORT).show();
				break;
			}
			case 2: {
				Log.d(TAG, "Media created via SDCard storage access");
				if (SHOW_TOASTS) Toast.makeText(context, "Media created via Internal Storage access", Toast.LENGTH_SHORT).show();
				break;
			}
			case 3: {
				Log.d(TAG, "Media created via Internal App storage access");
				if (SHOW_TOASTS) Toast.makeText(context, "Media created via Internal Storage access", Toast.LENGTH_SHORT).show();
				break;
			}
			default:
				Log.d(TAG, "Error finding video/audio media");
				if (SHOW_TOASTS) Toast.makeText(context, "Error finding video/audio media", Toast.LENGTH_SHORT).show();
				break;
			}
			list.add(video, false);
			/*
			 * Preferred return statement
			 */
			return player;
		} catch (Exception e) {
			Log.e(TAG, "Error creating player!");
			if (SHOW_TOASTS) Toast.makeText(context, "Error creating player!", Toast.LENGTH_LONG).show();
		}
		/*
		 */
		return null;
	}
	private void releasePlayer() {
		Log.d(TAG, "releasePlayer");
		if (mediaplayer==null) {
			Log.d(TAG, "No player to be released.");
			return;
		}
		EventHandler.getInstance().removeHandler(mHandler);
		mediaplayer.stop();
		mediaplayer.detachSurface();
		holder = null;
		mediaplayer.closeAout();
		mediaplayer.destroy();
		mediaplayer = null;
		Log.d(TAG, "playerReleased");
	}
	/*************************************/
	/**		Event-Handling Functions	**/
	/*************************************/
	/**
	 * Once the display triggers onPrepared, Progress Dialog dismisses and Video starts automatically
	 * @return
	 */
	public void onPrepared() {
		Log.d(TAG, "onPrepared");
		pDialog.dismiss();
	    mediaplayer.playIndex(0);
/*		try {
			progress.setMedia(media);
			RelativeLayout.LayoutParams below = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			below.addRule(RelativeLayout.END_OF, display.getId());
			progress.setLayoutParams(below);
		} catch (IOException e) {
			Log.e(TAG, "VideoProgressBar cannot find media");
			if (SHOW_TOASTS) Toast.makeText(context, "VideoProgressBar cannot find media!", Toast.LENGTH_SHORT).show();
		}*/
	}
	/**
	 * Error Handling.
	 * @param what
	 * @param extra
	 * @return boolean (default: true)
	 */
	public boolean onError(int what, int extra) {
		Log.e(TAG, String.format("Error(%s%s)", what, extra));
		return true;  
	}
	/**
	 * Activated when LibVLC media completes playback.
	 */
	public void onCompletion() {
		Log.d(TAG, "Media playback finished");
		if (SHOW_TOASTS) Toast.makeText(context, "Media playback finished!", Toast.LENGTH_SHORT).show();
	}
	
	/*************************************/
	/** 		Handler-Class			**/
	/*************************************/
	private class MyHandler extends Handler {
		private WeakReference<VideoFragment> owner;
		public MyHandler() {
			owner = new WeakReference<VideoFragment>(VideoFragment.this);
		}
		/**
		 * Maps event from EventHandler-class to Event Handling functions in this Fragment. 
		 */
		@Override
		public void handleMessage(Message msg) {
			VideoFragment frag = owner.get();
			Bundle bundle = msg.getData();
			if (msg.what==VIDEO_SIZE_CHANGED) {
				frag.resetSize();
				holder.setFixedSize(msg.arg1, msg.arg2);
				display.invalidate();
				return;
			}
			switch (bundle.getInt("event")) {
			case EventHandler.MediaPlayerEncounteredError: {
				frag.onError(bundle.getInt("what"), bundle.getInt("extra"));
				break;
			}
			case EventHandler.MediaPlayerEndReached: frag.onCompletion();
			default:
			}
		}
	}
	private void resetSize() {
		Log.d(TAG, "setSize");
		int videoWidth = mediaplayer.getMediaList().getMedia(0).getWidth();
		int videoHeight = mediaplayer.getMediaList().getMedia(0).getHeight();
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
	    Log.v(TAG, "Device Screen Size: ("+screenSize.x+", "+screenSize.y+ ")");
	    Log.v(TAG, "Display Screen Size: ("+display.getWidth()+", " +display.getHeight()+")");
	    if (!SHOW_TOASTS) 
	    	Toast.makeText(context, String.format("Device Screen Size: (%s, %s)\n Display Screen Size: (%s, %s)\n Media Size: (%s, %s)", 
	    		screenSize.x, screenSize.y,
	    		display.getWidth(), display.getHeight(),
	    		videoWidth, videoHeight)
	    		, Toast.LENGTH_LONG).show();
	}
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
