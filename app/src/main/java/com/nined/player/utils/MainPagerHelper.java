/**
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/17/2015.
 */
package com.nined.player.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainPagerHelper implements ViewPager.OnPageChangeListener {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = MainPagerHelper.class.getSimpleName();
    private static final boolean SHOW_LOG = true;
    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private final AppCompatActivity activity;
    private final ViewPager pager;
    private final MainPagerAdapter adapter;
    private int currentPage = 0;
    /*********************************/
    /**         Constructor         **/
    /*********************************/
    public MainPagerHelper(AppCompatActivity activity,
                           ViewPager pager,
                           Fragment... fragments)
    {
        this.activity = activity;
        this.pager = pager;
        if (fragments==null || fragments.length < 1) {
            this.adapter = new MainPagerAdapter(this.activity.getSupportFragmentManager(), null);
        } else {
            this.adapter = new MainPagerAdapter(this.activity.getSupportFragmentManager(), Arrays.asList(fragments));
        }
        this.pager.setAdapter(adapter);
        this.pager.setOnPageChangeListener(this);
    }
    /*********************************/
    /**       Item Add-Remove       **/
    /*********************************/
    public void clear() {
        if(this.adapter==null) return;
        this.adapter.clear();
    }
    public void add(@NonNull Fragment fragment) {
        if (this.adapter==null) return;
        this.adapter.add(fragment);
    }
    public void add(int position, Fragment fragment) {
        if (this.adapter==null) return;
        this.adapter.add(position, fragment);
    }

    /*********************************/
    /**         Private Class       **/
    /*********************************/
    private class MainPagerAdapter extends FragmentPagerAdapter
    {
        private List<Fragment> fragments;
        public MainPagerAdapter (@NonNull FragmentManager fragmentManager,
                                 @Nullable List<Fragment> fragments)
        {
            super(fragmentManager);
            this.fragments = new ArrayList<>();
            if (fragments==null) return;
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

        protected void add(@NonNull Fragment fragment) {
            if (fragments==null) return;
            add(fragments.size(), fragment);
        }
        protected void add(int position, @NonNull Fragment fragment) {
            if (fragments==null) return;
            fragments.add(position, fragment);
            notifyDataSetChanged();
        }
        protected void clear() {
            if (fragments==null || fragments.isEmpty()) return;
            fragments.clear();
            notifyDataSetChanged();
        }
    }
    /*********************************/
    /**     Page Change Listener    **/
    /*********************************/
    @Override
    public void onPageSelected(int position) {
        setCurrentPage(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }
    @Override
    public void onPageScrollStateChanged(int state) {

    }
    /*********************************/
    /**     Getters - Setters       **/
    /*********************************/
    /**
     * @return the currentPage
     */
    public int getCurrentPage() {
        return this.currentPage;
    }

    /**
     * @param pos position to set
     */
    public void setCurrentPage(int pos) {
        if (this.pager==null) return;
        this.currentPage = pos;
        this.pager.setCurrentItem(this.currentPage);
    }

    public Fragment getCurrentFragment() {
        return this.adapter.fragments.get(pager.getCurrentItem());
    }
}
