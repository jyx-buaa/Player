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

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * ArrayAdapter specialization for UPnP server directory contents.
 *
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/24/2015.
 */
public class FileArrayAdapter extends ArrayAdapter<DIDLObject> {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = FileArrayAdapter.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    private static final int LISTITEM = R.layout.listitem_route;
    private static final int ITEM_TITLE = R.id.title;
    private static final int ITEM_SUBTITLE = R.id.subtitle;
    private static final int ITEM_IMAGE = R.id.image;

    /*********************************/
    /**       Member Variable(s)    **/
    /*********************************/
    private Context context;

    /*********************************/
    /**         Constructor         **/
    /*********************************/
    public FileArrayAdapter(Context context) {
        super(context, R.layout.listitem_route);
        this.context =context;
    }

    /*********************************/
    /**   ArrayAdapter Override(s)  **/
    /*********************************/
    /**
     * Returns a view with folder/media title, and artist name (for audio only).
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) holder = (ViewHolder) convertView.getTag();
        else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(LISTITEM, parent, false);
            holder = new ViewHolder(convertView);
        }
        convertView.setTag(holder);

        DIDLObject item = getItem(position);
        holder.artist.setText("");
        if (item instanceof MusicTrack) {
            MusicTrack track = (MusicTrack) item;
            String trackNumber = (track.getOriginalTrackNumber() != null)
                    ? Integer.toString(track.getOriginalTrackNumber()) + ". "
                    : "";
            holder.title.setText(trackNumber + item.getTitle());
            if (track.getArtists().length > 0) {
                holder.artist.setText(track.getArtists()[0].getName());
            }
        }
        else {
            holder.title.setText(item.getTitle());
        }

        URI icon = item.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class);
        if (icon != null) {
            holder.image.setImageUri(icon);
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
            holder.image.setImageResource(resId);
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
    /*********************************/
    /**         View Holder         **/
    /*********************************/
    static class ViewHolder {
        @Bind(ITEM_TITLE) TextView title;
        @Bind(ITEM_SUBTITLE) TextView artist;
        @Bind(ITEM_IMAGE) RemoteImageView image;
        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

 /*   public class LocalServer extends NanoHTTPD {
        public LocalServer() {
            super(8089);
        }
        public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parameters, Map<String, String> files) {
            String answer = "";
            FileInputStream input = null;
            try {
                input = new FileInputStream(Environment.getExternalStorageDirectory() + "/music/musicfile.mp3");
            }catch (FileNotFoundException e) {
                if (SHOW_LOG) Log.w(TAG, "File not found", e);
            }
            return new NanoHTTPD.Response(Response.Status.OK, "audio/mpeg", input);
        }
    }*/
}
