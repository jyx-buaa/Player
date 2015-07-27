package com.nined.player.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nined.player.R;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.PlaylistItem;
import org.fourthline.cling.support.model.item.VideoItem;

import java.net.URI;
import java.util.List;

/**
 * ArrayAdapter specialization for UPnP server directory contents.
 *
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/24/2015.
 */
public class FileArrayAdapter extends ArrayAdapter<DIDLObject> {

    public FileArrayAdapter(Context context) {
        super(context, R.layout.listitem_route);
    }

    /**
     * Returns a view with folder/media title, and artist name (for audio only).
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listitem_route, parent, false);
        }
        DIDLObject item = getItem(position);
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView artist = (TextView) convertView.findViewById(R.id.subtitle);
        artist.setText("");
        if (item instanceof MusicTrack) {
            MusicTrack track = (MusicTrack) item;
            String trackNumber = (track.getOriginalTrackNumber() != null)
                    ? Integer.toString(track.getOriginalTrackNumber()) + ". "
                    : "";
            title.setText(trackNumber + item.getTitle());
            if (track.getArtists().length > 0) {
                artist.setText(track.getArtists()[0].getName());
            }
        }
        else {
            title.setText(item.getTitle());
        }

        RemoteImageView image = (RemoteImageView) convertView.findViewById(R.id.image);
        URI icon = item.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class);
        if (icon != null) {
            image.setImageUri(icon);
        }
        else {
            int resId;
            if (item instanceof AudioItem) {
                resId = R.drawable.ic_doc_audio_am;
            }
            else if (item instanceof VideoItem) {
                resId = R.drawable.ic_doc_video_am;
            }
            else if (item instanceof ImageItem) {
                resId = R.drawable.ic_doc_image;
            }
            else if (item instanceof PlaylistItem) {
                resId = R.drawable.ic_doc_album;
            }
            else {
                resId = R.drawable.ic_root_folder_am;
            }
            image.setImageResource(resId);
        }

        return convertView;
    }

    /**
     * Replacement for addAll, which is not implemented on lower API levels.
     */
    public void add(List<Item> playlist) {
        for (DIDLObject d : playlist) {
            add(d);
        }
    }

}
