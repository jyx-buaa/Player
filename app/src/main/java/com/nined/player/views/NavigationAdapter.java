package com.nined.player.views;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.nined.player.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


public class NavigationAdapter extends BaseExpandableListAdapter {
	/*********************************/
	/**     Logging Assistant(s)    **/
	/*********************************/
	private static final String TAG = NavigationAdapter.class.getSimpleName();
	private static final boolean SHOW_LOG = false;
	/*********************************/
	/**      	Constant(s)		    **/
	/*********************************/
	@LayoutRes
	private static final int LAYOUT_PARENT = R.layout.listitem_drawer_parent;
	@IdRes
	private static final int ITEM_PARENT_TEXT = R.id.parent_text;
	@LayoutRes
	private static final int LAYOUT_CHILD = R.layout.listitem_drawer_child;
	@IdRes
	private static final int ITEM_CHILD_TEXT = R.id.child_text;
	/*********************************/
	/**      Member Variable(s)	    **/
	/*********************************/
	private Context context;
	private final SparseArray<Node> nodes;

	/*********************************/
	/**      	Constructor(s)	    **/
	/*********************************/
	public NavigationAdapter(Context context) {
		if (SHOW_LOG) Log.d(TAG, "constructor");
		this.context = context;
		this.nodes = new SparseArray<>();
		String[] parents = context.getResources().getStringArray(R.array.nav_items);
		for (int i = 0; i<parents.length; i++) {
			Node n = new Node(parents[i]);
			if (i==0) n.children.addAll(Arrays.asList(context.getResources().getStringArray(R.array.expand_0)));
			if (i==2) n.children.addAll(Arrays.asList(context.getResources().getStringArray(R.array.expand_2)));
			if (i==3) n.children.addAll(Arrays.asList(context.getResources().getStringArray(R.array.expand_3)));
			nodes.append(i, n);
		}
	}
	/*********************************************/
	/**  BaseExpandableListAdapter Override(s)  **/
	/*********************************************/
	@Override
	public int getGroupCount() {
		return nodes.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return nodes.get(groupPosition).children.size();
	}

	@Override
	public Node getGroup(int groupPosition) {
		return nodes.get(groupPosition);
	}

	@Override
	public String getChild(int groupPosition, int childPosition) {
		return nodes.get(groupPosition).children.get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return (groupPosition*10)+childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		GroupViewHolder holder;
		if (convertView != null) {
			holder = (GroupViewHolder) convertView.getTag();
		} else {
			convertView = layoutInflater.inflate(LAYOUT_PARENT, null);
			holder = new GroupViewHolder(convertView);
		}
		convertView.setTag(holder);

		holder.text.setText(getGroup(groupPosition).parent);
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ChildViewHolder holder;
		if (convertView!=null) {
			holder = (ChildViewHolder) convertView.getTag();
		} else {
			convertView = layoutInflater.inflate(LAYOUT_CHILD, null);
			holder = new ChildViewHolder(convertView);
		}
		convertView.setTag(holder);

		holder.text.setText(getChild(groupPosition, childPosition));
		return convertView;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	/*********************************/
	/**      	View Holder(s)	    **/
	/*********************************/
	protected static class GroupViewHolder {
		@Bind(ITEM_PARENT_TEXT) TextView text;
		public GroupViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}
	protected static class ChildViewHolder {
		@Bind(ITEM_CHILD_TEXT) TextView text;
		public ChildViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}
	/*********************************/
	/**      	Private Class	    **/
	/*********************************/
	public class Node {
		public String parent;
		public final List<String> children = new ArrayList<>();
		public Node(String parent) {
			this.parent = parent;
		}
		public int getChildrenCount() {
			return children.size();
		}
	}
}
