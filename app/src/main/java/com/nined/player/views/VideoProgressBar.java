	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.views;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.SeekBar;

import java.io.IOException;

public class VideoProgressBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {
	/**
	 * Logging Assistants
	 */
	private static final String TAG = VideoProgressBar.class.getSimpleName();
	private static final boolean SHOW_LOG = false;
	private static final long THREAD_REFRESH_RATE = 1000;		// Set to change SeekBar progress every 1 second.
	private MediaPlayer player;
	private ProgressHandler handler;
	private AnimationDrawable customthumb;
	private boolean released;
	public VideoProgressBar(Context context) {
		super(context);
		//setBackgroundResource(R.drawable.nyanthumb);
		customthumb = (AnimationDrawable) getBackground();
		setThumb(customthumb);
		released = false;
	}
	public void setMedia(MediaPlayer media) throws IOException {
		if (media==null) {
			if (SHOW_LOG) Log.d(TAG, "Media Not Found");
			throw new IOException("Media Exception");
		}
		this.player = media;
		released = false;
		setMax((int) player.getDuration());
		handler = new ProgressHandler();
		handler.execute();
		setOnSeekBarChangeListener(this);
	}
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (released || player==null)return;
		player.seekTo(progress);
		if (SHOW_LOG) Log.d(TAG, "Progress Changed: " + progress);
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {			
	}
	
	private class ProgressHandler extends AsyncTask <Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			customthumb.start();
		}
		@Override
		protected Void doInBackground(Void... params) {
			if (released || player==null) return null;
			while (player.isPlaying()) {
				try {
					Thread.sleep(THREAD_REFRESH_RATE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (released) return null;
				int mCurrentPosition = player.getCurrentPosition();
				VideoProgressBar.this.setProgress(mCurrentPosition);
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			customthumb.stop();
			if (SHOW_LOG) Log.d(TAG, "onPostExecute");
		}
	}
	public void release() {
		released = true;
	}
}