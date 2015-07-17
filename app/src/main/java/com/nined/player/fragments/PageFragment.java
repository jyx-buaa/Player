/**
 * @author Aekasitt Guruvanich, 9D Technologies
 */
package com.nined.player.fragments;


import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.nined.player.R;
import com.nined.player.model.Page;
import com.nined.player.utils.ContentParser;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PageFragment extends Fragment {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = PageFragment.class.getSimpleName();
    private static final boolean SHOW_LOG = false;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    private static final String CURRENT_PAGE = "currentPage";
    @LayoutRes
    private static final int LAYOUT = R.layout.page_view;
    @IdRes
    private static final int TITLE = R.id.page_title;
    @IdRes
    private static final int CONTENT = R.id.page_content;

    /*********************************/
    /**      View Injection(s)      **/
    /*********************************/
    @Bind(TITLE) protected TextView title;
    @Bind(CONTENT) protected LinearLayout content;

    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private Page page;

    /*********************************/
    /**        Constructor(s)       **/
    /*********************************/
    public PageFragment() { super(); }

    /*********************************/
    /**      Lifecycle Override(s)  **/
    /*********************************/
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (page!=null) return;
        else if (savedInstanceState!=null){
            this.page = savedInstanceState.getParcelable("currentPage");
            if (this.page==null) {
                this.page = new Page();
            }
        }
        if (SHOW_LOG) Log.e(TAG, "cannot instantiate Page");
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstaceState) {
        if (SHOW_LOG) Log.d(TAG, "onCreateView");
        View view = inflater.inflate(LAYOUT, container, false);
        ButterKnife.bind(this, view);
        refreshViews();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (page!=null)
            outState.putParcelable(CURRENT_PAGE, page);
    }


    public void setPage(Page page) {
        this.page = page;
    }
    public Page getPage() {
        return page;
    }
    public void refreshViews() {
        /*********************************/
        /**		UPDATE VIEWS HERE		**/
        /*********************************/
        title.setText(page.getTitle());
        try {
            ContentParser.init(getActivity());
            ContentParser.parse(content, page.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        if (getView()!=null)
            getView().invalidate();
    }
}
