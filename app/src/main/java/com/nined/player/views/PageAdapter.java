	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.views;

	import android.content.Context;
	import android.support.annotation.IdRes;
	import android.support.annotation.LayoutRes;
	import android.support.annotation.NonNull;
	import android.util.Log;
	import android.view.LayoutInflater;
	import android.view.View;
	import android.view.ViewGroup;
	import android.widget.BaseAdapter;
	import android.widget.ImageView;
	import android.widget.TextView;

	import com.bumptech.glide.Glide;
	import com.nhaarman.listviewanimations.util.Insertable;
	import com.nined.player.R;
	import com.nined.player.client.PlayerApi;
	import com.nined.player.client.PlayerSvc;
	import com.nined.player.model.Image;
	import com.nined.player.model.Page;
	import com.nined.player.utils.CallableTask;
	import com.nined.player.utils.TaskCallback;

	import org.jsoup.Jsoup;

	import java.util.ArrayList;
	import java.util.Collection;
	import java.util.List;
	import java.util.concurrent.Callable;

	import butterknife.Bind;
	import butterknife.ButterKnife;

public class PageAdapter extends BaseAdapter implements Insertable<Page>{
	/*********************************/
	/**     Logging Assistant(s)    **/
	/*********************************/
	private final static String TAG = PageAdapter.class.getSimpleName();
	private final static boolean SHOW_LOG = false;

	/*********************************/
	/**     	Constant(s)		    **/
	/*********************************/
	@LayoutRes
	private static final int LAYOUT = R.layout.listitem_page;
	@IdRes
	private static final int ITEM_TITLE = R.id.page_item_title;
	@IdRes
	private static final int ITEM_ATTACHMENT = R.id.page_item_attachment;
	@IdRes
	private static final int ITEM_BODY = R.id.page_item_body;

	/*********************************/
	/**     Member Variable(s)      **/
	/*********************************/
	private Context context;
	private List<Page> pages;

	/*********************************/
	/**     	Constructor 	    **/
	/*********************************/
	public PageAdapter (Context context, Collection<Page> pages) {
		this.context = context;
		this.pages = (ArrayList<Page>) pages;
	}

	/*********************************/
	/**   Base Adapter Override(s)  **/
	/*********************************/
	@Override
	public int getCount() {
		return pages.size();
	}

	@Override
	public Page getItem(int position) {
		return pages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return pages.get(position).getId();
	}

	public void add(@NonNull Page page) {
		add(getCount(), page);
	}

	@Override
	public void add(int position, @NonNull Page page) {
		if (pages.contains(page)) return; // Do nothing if object already exists
		pages.add(position, page);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewHolder holder;
		if (convertView!=null) {
			holder = (ViewHolder) convertView.getTag();
		} else {
			convertView = inflater.inflate(LAYOUT, parent, false);
			holder = new ViewHolder(convertView);
		}
		convertView.setTag(holder);

	    holder.title.setText(pages.get(position).getTitle());
	    String body = Jsoup.parse(pages.get(position).getContent()).text();
	    holder.body.setText(body);
	    makeThumbnail(holder, getItemId(position));
		return convertView;
	}
	/*********************************/
	/**     	View Holder	 	    **/
	/*********************************/
	protected static class ViewHolder {
	    @Bind(ITEM_TITLE) 		TextView title;
	    @Bind(ITEM_ATTACHMENT) 	ImageView attachment;
	    @Bind(ITEM_BODY) 		TextView body;
	    public ViewHolder(View view) {
			ButterKnife.bind(this, view);
	    }
	}

	/*********************************/
	/**      Other Function(s)	    **/
	/*********************************/
	protected void makeThumbnail(final ViewHolder holder, final long id) {
		final PlayerApi svc = PlayerSvc.getOrBuildInstance(context);
		if (svc!=null) {
			CallableTask.invoke(new Callable<Collection<Image>>() {
				@Override
				public Collection<Image> call() throws Exception {
					return svc.findImageByParent(id);
				}
			}, new TaskCallback<Collection<Image>>() {
				@Override
				public void success(Collection<Image> result) {
					List<Image> attachments = (ArrayList<Image>) result;
					if (attachments==null || attachments.size() < 1) return;
					Image attachment = attachments.get(0);
					String image_url = attachment.getGuid();//.substring(0, attachment.getGuid().lastIndexOf("."));

					Glide.with(context)
							.load(image_url)
							.into(holder.attachment);
				}
				@Override
				public void error(Exception e) {
					if (SHOW_LOG) Log.d(TAG, "Failed loading image");
				}			
			});
		}
	}

}
