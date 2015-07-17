package com.nined.player.utils;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Aekasitt on 7/17/2015.
 */
public class MainPagerHelper implements ViewPager.OnPageChangeListener {
    private final AppCompatActivity activity;
    private final ViewPager pager;
    private final MainPagerAdapter adapter;
    private int currentPage = 0, pageCount;
    public MainPagerHelper(AppCompatActivity activity, ViewPager pager, Fragment... fragments) {
        this.activity = activity;
        this.pager = pager;
        if (fragments==null || fragments.length < 1) {
            this.adapter = new MainPagerAdapter(activity.getSupportFragmentManager(), null);
        } else {
            this.adapter = new MainPagerAdapter(activity.getSupportFragmentManager(), Arrays.asList(fragments));
        }
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(this);
    }
    private class MainPagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments;
        private FragmentManager fragmentManager;

        public MainPagerAdapter (@NonNull FragmentManager fragmentManager,
                                 @NonNull List<Fragment> fragments)
        {
            super(fragmentManager);
            this.fragmentManager = fragmentManager;
            this.fragments = new ArrayList<>();
            for (Fragment f: fragments) {
                this.fragments.add(f);
            }
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragments.get(position).getTag();
        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
