/**
 *
 * @author Aekasitt Guruvanich, 9D Tech
 *
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.nined.player.fragments.PlaceHolderFragment;
import com.nined.player.utils.MainPagerHelper;
import com.nined.player.utils.NavUnit;
import com.nined.player.views.NavigationAdapter;

import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Aekasitt on 7/17/2015.
 */
public class MainActivity extends AppCompatActivity{
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean SHOW_LOG = false;

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
    protected void onCreate(Bundle savedInstanceState) {
        /*
         *  Initiates Activity and Get values and views required
         */
        if (SHOW_LOG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        title = drawerTitle = getSupportActionBar().getTitle().toString();
        /*
         * Set appropriate adapters and properties for each elements of the Activity
         */
        navigationAdapter = new NavigationAdapter(this);
        setUpNavPane();
        /*
         *  Initialize ActionBar and Toggle for mDrawerLayout
         */
        actionBar = setUpActionBar();
        drawerToggle = setUpDrawerToggle();
        drawerToggle.syncState();
        drawerLayout.setDrawerListener(drawerToggle);
    	/*
    	 * If there is no previous savedState, starts at 0 'Main Menu'
    	 */
        if (savedInstanceState == null) {
            navigations = new Stack<NavUnit>();
            navigations.push(new NavUnit(0, 0));
            selectItem(navigations.peek());
        }
        /**
         * Initiate ViewPager
         */
        helper = new MainPagerHelper(MainActivity.this, pager, new PlaceHolderFragment(getResources().getColor(R.color.primaryDark))); //TODO Add fragments here
        /**
         * Set up Cast Media Router;
         */
        //initMediaRouter();
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
        // Highlights the selected item, set the title and close the drawer.
        drawer.setItemChecked(navigationUnit.getGroup(), true);
        drawerLayout.closeDrawer(drawer);
        switch (group) {
            case 0: // MOOC HK
            {
/*    		switch (navigationUnit.child) {
    		case 0: { viewCreator.launchPage((long) 4859); break;} // About Us
    		case 1: { viewCreator.launchPage((long) 61); break; }	 // How to attend class
    		default:
    		}*/
                break;
            }
            case 1: // All Courses
            {
                //viewCreator.makeCourseList();
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
        drawerTitle=title.toString();
    }

    /**
     * Set Up the Navigation Drawer Panel after this Activity is created.
     */
    public void setUpNavPane() {
        int myLeft = drawer.getWidth()-40;
        int myRight = drawer.getWidth()-20;
        drawer.setAdapter(navigationAdapter);
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            drawer.setIndicatorBounds(myLeft, myRight);
        } else {
            drawer.setIndicatorBoundsRelative(myLeft, myRight);
        }
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
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeButtonEnabled(true);
        return actionbar;
    }
    /**
     * Set up and customize the ActionBarDrawerToggle shown in this application
     * @return Custom ActionBarDrawerToggle for this application
     */
    public ActionBarDrawerToggle setUpDrawerToggle() {
        if (SHOW_LOG) Log.d(TAG, "setUpDrawerToggle");
        return new ActionBarDrawerToggle(this, drawerLayout, null, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                setTitle(title);
                invalidateOptionsMenu();
            }
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                actionBar.setTitle(drawerTitle);
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
     * @return
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
     * @return
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
     * @param refresh
     */
    public void setRefreshActionButtonState(final boolean refresh)
    {
        if (SHOW_LOG) Log.d(TAG, "setRefreshActionButtonState");
        if (this.optionsMenu != null)
        {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.menuRefresh);
            if (refreshItem !=null)
            {
                if (refresh)
                    refreshItem.setActionView(R.layout.actionbar_loading_progress);
                else
                    refreshItem.setActionView(null);
            }
        }
    }
    /*********************************/
    /**      Private Class(s)       **/
    /*********************************/

}
