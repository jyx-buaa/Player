package com.nined.player.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.nined.player.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

public class ContentParser {
	/*********************************/
	/**     Logging Assistant(s)    **/
	/*********************************/
	private static final String TAG = ContentParser.class.getSimpleName();
	private static final boolean SHOW_LOG = false;
	/*********************************/
	/**       Member Variable(s)    **/
	/*********************************/
	private static Context context;
	/*********************************/
	/**       	  Initiate		    **/
	/*********************************/
	public static void init (Context context) {
		ContentParser.context = context;
	}
	public static void parse(LinearLayout frame, String contentString) throws Exception {
		if (ContentParser.context == null) {
			if (SHOW_LOG) Log.e(TAG, "You must instantiate ContentParser first and foremost");
			throw new Exception("You must instantiate ContentParser first and foremost");
		}
		final LinearLayout content = frame;
		Document contentDoc = Jsoup.parse(contentString);
		for (Node n: contentDoc.body().childNodes()) {
			switch (n.nodeName()) {
			case "a": {
				content.addView(makeLink(n));
				break;
			}
			case "img": {
				content.addView(makeImg(n));
				break;
			}
			case "video": {
				if (SHOW_LOG) Log.d(TAG, "Found Video: "+ n.attr("mp4"));
				VideoView video = new VideoView(context);
				video.setVideoURI(Uri.parse(n.attr("mp4")));
				//SimpleVideoFragment video = new SimpleVideoFragment(context, "Content Video", n.attr("mp4"));
				content.addView(video);
				break;
			}
			default:
				Document doc = Jsoup.parse(n.toString());
				String docParsed = doc.normalise().text();
				if (docParsed.contains("[video")) {
					String url = docParsed.substring(docParsed.indexOf("mp4=\"")+5, docParsed.indexOf(".mp4")+4);
					String width = docParsed.substring(docParsed.indexOf("width=\"")+7, docParsed.indexOf("width=\"")+11);
					String height = docParsed.substring(docParsed.indexOf("height=\"")+8, docParsed.indexOf("height=\"")+11);
					if (SHOW_LOG) Log.d(TAG, "Found Video: "+ url);
					VideoView video = new VideoView(context);
					MediaController controller  = new MediaController(context);
					controller.setAnchorView(video);
				    controller.setMediaPlayer(video);
				    video.setMediaController(controller);
					video.setVideoURI(Uri.parse(url));
					video.setLayoutParams(new FrameLayout.LayoutParams(
												Integer.parseInt(width), 
												Integer.parseInt(height)));
					content.addView(video);
					video.start();
					break;
				} else if (docParsed.contains("[one_half first=]")) {
					docParsed = docParsed.substring(17);
					continue;
				} else if (docParsed.contains("[header]")) {
					String temp = docParsed.substring(docParsed.indexOf("[header]")+8, docParsed.indexOf("[/header]"));
					TextView text = new TextView(context);
					text.setText(temp);
					text.setTextSize(context.getResources().getDimension(R.dimen.header_text_size));
					content.addView(text);
					docParsed.substring(temp.length()+17);
					continue;
				} else if (docParsed.contains("[heading]")) {
					String temp = docParsed.substring(docParsed.indexOf("[heading]")+9, docParsed.indexOf("[/heading]"));
					TextView text = new TextView(context);
					text.setText(temp);
					text.setTextSize(context.getResources().getDimension(R.dimen.header_text_size));
					content.addView(text);
					docParsed.substring(temp.length()+19);
					continue;
				}
				if (SHOW_LOG) Log.d(TAG, "Found Text: "+ docParsed);
				TextView text = new TextView(context);
				text.setText(docParsed);
				text.setPadding(
						(int) context.getResources().getDimension(R.dimen.activity_horizontal_margin),
						0, 
						(int) context.getResources().getDimension(R.dimen.activity_horizontal_margin), 
						0);
				content.addView(text);
			}
		}
	}
	private static View makeLink(final Node n) throws Exception {
		if (SHOW_LOG) Log.d(TAG, "Found link: " + n.attr("href"));
		final String link = n.attr("href");
		LinearLayout recursiveContent = new LinearLayout(context);
		recursiveContent.setOrientation(LinearLayout.HORIZONTAL);
		recursiveContent.setGravity(Gravity.CENTER_VERTICAL);
		recursiveContent.setPadding(
				(int) context.getResources().getDimension(R.dimen.activity_horizontal_margin),
				0, 
				(int) context.getResources().getDimension(R.dimen.activity_horizontal_margin), 
				0);
		ContentParser.parse(recursiveContent, n.childNodes().toString());
		ImageButton go = new ImageButton(context);
		go.setImageResource(R.drawable.arrow_btn_selector);
		//go.setLayoutParams(new FrameLayout.LayoutParams(50, 50));
		go.setScaleType(ScaleType.FIT_CENTER);
		go.setBackgroundColor(Color.TRANSPARENT);
		go.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(link));
				context.startActivity(i);
			}
		});
		recursiveContent.addView(go);
		return recursiveContent;
	}
	private static View makeImg(final Node n) throws Exception {
		if (SHOW_LOG) Log.d(TAG, "Found Image: "+ n.attr("src"));
		ImageView img = new ImageView(context);
		Glide.with(context).load(n.attr("src")).into(img);
		int width = Integer.parseInt(n.attr("width"));
		int height = Integer.parseInt(n.attr("height"));
		img.setLayoutParams(new FrameLayout.LayoutParams(width, height));
		return img;
	}
}
