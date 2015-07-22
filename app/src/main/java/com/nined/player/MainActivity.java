/**
 * @author Aekasitt Guruvanich, 9D Tech
 */
package com.nined.player;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.nined.player.fragments.NineDPlayerFragment;
import com.nined.player.fragments.PlaceHolderFragment;
import com.nined.player.utils.MainPagerHelper;
import com.nined.player.utils.NavUnit;
import com.nined.player.views.NavigationAdapter;

import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity{
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    @LayoutRes
    private static final int LAYOUT = R.layout.activity_main;
    @IdRes
    private static final int DRAWER_LAYOUT = R.id.main_drawer_layout;
    @IdRes
    private static final int DRAWER = R.id.main_nav_drawer;
    @IdRes
    private static final int FRAME = R.id.main_content_frame;
    @IdRes
    private static final int PAGER = R.id.main_view_pager;

    /*********************************/
    /**      View Injection(s)      **/
    /*********************************/
    @Bind(DRAWER_LAYOUT)
    protected DrawerLayout drawerLayout;
    @Bind(DRAWER)
    protected ExpandableListView drawer;
    @Bind(FRAME)
    protected FrameLayout frame;
    @Bind(PAGER)
    protected ViewPager pager;

    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private ActionBar actionBar;
    private ActionBarDrawerToggle drawerToggle;
    private Stack<NavUnit> navigations;
    private String title, drawerTitle;
    private NavigationAdapter navigationAdapter;
    private MainPagerHelper helper;
    private Menu optionsMenu;

    /*********************************/
    /**     Lifecycle Override(s)   **/
    /*********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /*
         *  Initiates Activity and Get values and views required
         */
        if (SHOW_LOG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(LAYOUT);
        ButterKnife.bind(this);
        /*
         * Set appropriate adapters and properties for each elements of the Activity
         */
        setUpNavPane();
        /*
         *  Initialize ActionBar and Toggle for mDrawerLayout
         */
        actionBar = setUpActionBar();
        drawerToggle = setUpDrawerToggle();
        drawerToggle.syncState();
        drawerLayout.setDrawerListener(drawerToggle);
        /**
         * Initiate ViewPager
         */
        helper = new MainPagerHelper(MainActivity.this, pager,
                new PlaceHolderFragment(getResources().getColor(R.color.Opaque_Black)),
                new PlaceHolderFragment(getResources().getColor(R.color.Murky_Black))); //TODO Add fragments here
    	/*
    	 * If there is no previous savedState, starts at 0 'Main Menu'
    	 */
        if (savedInstanceState == null) {
            navigations = new Stack<>();
            navigations.push(new NavUnit(0, 0));
            selectItem(navigations.peek());
        }
        /**
         * Set up Cast Media Router;
         */
        //initMediaRouter();
    }

    /**
     * Create Menu Options with res/menu/main.xml
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (SHOW_LOG) Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        this.optionsMenu = menu;
        return super.onCreateOptionsMenu( menu );
    }

    /**
     * Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button,
     * so long as you specify a parent activity in AndroidManifext.xml
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        switch(id) {
            case R.id.action_refresh: {
                getFragmentManager().popBackStack();
                if (navigations.size()<1)
                    navigations.push(new NavUnit(0,0));
                selectItem(navigations.peek());
                break;
            }
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    /*********************************/
    /**      Other Function(s)      **/
    /*********************************/
    /**
     * @param navigationUnit the current selected page as portrayed by our NavUnit class
     */
    protected void selectItem (NavUnit navigationUnit) {
        if (SHOW_LOG) Log.d(TAG, "selectItem");
        final int group = navigationUnit.getGroup();
        final int child = navigationUnit.getChild();
        if (SHOW_LOG) Log.w(TAG, String.format("Group: %d, Child: %d", group, child));
        // Highlights the selected item, set the title and close the drawer.
        drawer.setItemChecked(navigationUnit.getGroup(), true);
        drawerLayout.closeDrawer(drawer);
        switch (group) {
            case 0: // MOOC HK
            {
                helper.clear();
                helper.add(NineDPlayerFragment.newInstance("Not Taichi", "https://s3-ap-southeast-1.amazonaws.com/ninedcloud/not+Tai+chi.wav"));
                    //NineDPlayerFragment.newInstance("Pewdiepie", "sample_video.mp4"),
                    //NineDPlayerFragment.newInstance("Pewdiepie", "http://download.wavetlan.com/SVV/Media/HTTP/H264/Talkinghead_Media/H264_test4_Talkingheadclipped_mp4_480x320.mp4"),
                if (SHOW_LOG) Log.i(TAG, "added Not Taichi audio");
                break;
            }
            case 1: // All Courses
            {
                helper.clear();
                helper.add(NineDPlayerFragment.newInstance("Indian dude", "http://download.wavetlan.com/SVV/Media/HTTP/H264/Talkinghead_Media/H264_test4_Talkingheadclipped_mp4_480x320.mp4"));
                if (SHOW_LOG) Log.i(TAG, "added Indian Dude video");
                break;
            }
            case 2: // Smart Education
            {
                switch (child) {
                    default:
                }
                break;
            }
            case 3: // Job Seekers
            {
                switch(child) {
                    default:
                }
                break;
            }
            case 4: //Contact Us
            {
                break;
            }
            case 5: // 5.1 Audio Demo
            {
                break;
            }
            case 6: // Live Broadcast
            {
                //Broadcast bc = LiveBroadcast.loadBroadcast("http://wechat.moochk.com/live.json");
                break;
            }
            case 7:
            {
                exitApplication();
            }
            default:
        }
    }
    /**
     * Set the Title in Action Bar to correspond with selected item from the navigation drawer
     */
    @Override
    public void setTitle(CharSequence title) {
        if (SHOW_LOG) Log.d(TAG, "setTitle");
        super.setTitle(title);
        if (!TextUtils.isEmpty(title))
            drawerTitle=title.toString();
    }

    /**
     * Set Up the Navigation Drawer Panel after this Activity is created.
     */
    public void setUpNavPane() {
        DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int myLeft = drawer.getWidth()-40;
        int myRight = drawer.getWidth()-20;

        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            drawer.setIndicatorBounds(width - getPixelsFromDips(myLeft), width - getPixelsFromDips(myRight));
        } else {
            drawer.setIndicatorBoundsRelative(width - getPixelsFromDips(myLeft), width - getPixelsFromDips(myRight));
        }
        //
        navigationAdapter = new NavigationAdapter(this);
        drawer.setAdapter(navigationAdapter);
        //
        drawer.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                if (navigationAdapter.getGroup(groupPosition).getChildrenCount() < 1) {
                    navigations.push(new NavUnit(groupPosition, 0));
                    selectItem(navigations.peek());
                    return true;
                }
                return false;
            }

        });
        drawer.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                navigations.push(new NavUnit(groupPosition, childPosition));
                selectItem(navigations.peek());
                return true;
            }

        });
    }

    /**
     * Set up and customize the ActionBar shown in this application
     * @return Custom ActionBar used for this application
     */
    public ActionBar setUpActionBar() {
        if (SHOW_LOG) Log.d(TAG, "setUpActionBar");
        ActionBar actionbar = getSupportActionBar();
        //actionbar.setBackgroundDrawable(new ColorDrawable(R.color.MOOC_Blue));
        if (actionbar!=null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeButtonEnabled(true);
            if (actionbar.getTitle()!=null)
                title = drawerTitle = actionbar.getTitle().toString();
        }
        return actionbar;
    }
    /**
     * Set up and customize the ActionBarDrawerToggle shown in this application
     * @return Custom ActionBarDrawerToggle for this application
     */
    public ActionBarDrawerToggle setUpDrawerToggle() {
        if (SHOW_LOG) Log.d(TAG, "setUpDrawerToggle");
        return new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
        //return new ActionBarDrawerToggle(this, drawerLayout, null, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                setTitle(title);
                invalidateOptionsMenu();
            }
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };
    }
    /**
     * Checks if there is any Fragment in the backstack and replace Content Frame with the last inserted Fragment on the backstack
     * Go to android home screen otherwise
     */
    @Override
    public void onBackPressed() {
        if (SHOW_LOG) Log.d(TAG, "onBackPressed");
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }
        if (getLastFragment()) return;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    /**
     * Get the Last Fragment added onto the BackStack on the FragmentManager
     * @return true if the Fragment can be obtained, false if stack is empty.
     */
    private boolean getLastFragment() {
        if (SHOW_LOG) Log.d(TAG, "getLastFragment");
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 1) {
            manager.popBackStack();
            while (navigations.size() > manager.getBackStackEntryCount()) navigations.pop();
            return true;
        }
        return false;
    }
    /**
     * Clear Fragment Back Stack and NavigationObjs in navigations
     */
    private void clearBackStack() {
        if (SHOW_LOG) Log.d(TAG, "clearBackStack");
        FragmentManager manager = getSupportFragmentManager();
        while (manager.getBackStackEntryCount() > 0) {
            manager.popBackStackImmediate();
        }
        navigations.clear();
    }
    /**
     * Close connections to database and Exit application.
     */
    private void exitApplication() {
        if (SHOW_LOG) Log.d(TAG, "exitApplication");
        clearBackStack();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
    /**
     * Set the refresh icon to spinning ProgressBar when refreshing or loading views
     * @param refresh if true, change the view of the refresh button to spinning, static otherwise
     */
    public void setRefreshActionButtonState(final boolean refresh)
    {
        if (SHOW_LOG) Log.d(TAG, "setRefreshActionButtonState");
        if (this.optionsMenu != null)
        {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.action_refresh);
            if (refreshItem !=null)
            {
                if (refresh)
                    refreshItem.setActionView(R.layout.actionbar_loading_progress);
                else
                    refreshItem.setActionView(null);
            }
        }
    }
    public int getPixelsFromDips(int dips) {
        // Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (dips * scale + 0.5f);
    }
    /*********************************/
    /**      Private Class(s)       **/
    /*********************************/

}
