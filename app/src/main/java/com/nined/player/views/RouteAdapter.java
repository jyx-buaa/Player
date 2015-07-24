package com.nined.player.views;

import android.content.Context;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nined.player.R;

import java.util.Comparator;
import java.util.List;

/**
 * @author Aekasitt Guruvanich
 * on 7/24/2015.
 */
public class RouteAdapter extends ArrayAdapter<RouteInfo> {
    /**
     * Sorts routes by name. Call {@code sort(COMPARATOR)} whenever an item is inserted.
     */
    public static final Comparator COMPARATOR = new Comparator<RouteInfo>() {
        @Override
        public int compare(RouteInfo lhs, RouteInfo rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    };

    public RouteAdapter(Context context) {
        super(context, R.layout.listitem_route);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listitem_route, parent, false);
        }

        TextView title = (TextView) convertView.findViewById(R.id.title);
        title.setText(getItem(position).getName());

        TextView subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        subtitle.setText(getItem(position).getDescription());

        return convertView;
    }

    /**
     * Replacement for addAll, which is not implemented on lower API levels.
     */
    public void add(List<RouteInfo> routes) {
        for (RouteInfo r : routes) {
            add(r);
        }
    }
}