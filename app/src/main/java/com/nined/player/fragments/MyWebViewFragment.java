package com.nined.player.fragments;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewFragment;

public class MyWebViewFragment extends WebViewFragment {
	/*********************************/
    /**      Logging Assistant(s)    **/
	/*********************************/
	private static final String TAG = MyWebViewFragment.class.getSimpleName();
	private static final boolean SHOW_LOG = false;
	/*********************************/
    /**      	Constant(s)		    **/
	/*********************************/
	private static final String ARGUMENT_URL = "url";
	/*********************************/
    /**      Member Variable(s)	    **/
	/*********************************/
	private String mUrl;

	/*********************************/
    /**		   	 Instance		    **/
	/*********************************/
	public static MyWebViewFragment getInstance(String url) {
		MyWebViewFragment f = new MyWebViewFragment();
		Bundle args = new Bundle();
		args.putString(ARGUMENT_URL, url);
		f.setArguments(args);
		
		return f;
	}

	/*********************************/
	/**		Lifecycle Override(s)   **/
	/*********************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		mUrl = getArguments().getString(ARGUMENT_URL);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		WebView webView = getWebView();
		if (webView!=null) {
			if (webView.getOriginalUrl() == null) {
				webView.getSettings().setJavaScriptEnabled(true);
				webView.loadUrl(mUrl);
			}
		}
	}
}
