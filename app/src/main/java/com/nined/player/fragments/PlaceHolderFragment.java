package com.nined.player.fragments;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nined.player.R;

public class PlaceHolderFragment extends Fragment {
	private int color;
	public PlaceHolderFragment() {
	}
	public PlaceHolderFragment(int color) {
		this.color = color;
	}
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.place_holder, null);
		view.setBackgroundColor(this.color);
		return view;
	}
}