	/**
	 * @author Aekasitt Guruvanich, 9D Tech
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
	import android.widget.TextView;

	import com.bumptech.glide.Glide;
	import com.nhaarman.listviewanimations.util.Insertable;
	import com.nined.player.R;
	import com.nined.player.client.PlayerApi;
	import com.nined.player.client.PlayerSvc;
	import com.nined.player.model.Course;
	import com.nined.player.model.Image;
	import com.nined.player.utils.CallableTask;
	import com.nined.player.utils.TaskCallback;

	import java.util.ArrayList;
	import java.util.Collection;
	import java.util.List;
	import java.util.concurrent.Callable;

	import butterknife.Bind;
	import butterknife.ButterKnife;

public class CourseGridAdapter extends BaseAdapter implements Insertable<Course> {
	/*********************************/
    /*      Logging Assistant(s)    **/
	/*********************************/
	private final static String TAG = CourseGridAdapter.class.getSimpleName();
	private final static boolean SHOW_LOG = false;

	/*********************************/
    /*       	Constant(s)		    **/
	/*********************************/
	@LayoutRes
	private static final int LAYOUT_ID = R.layout.listitem_course;
	@IdRes
	private static final int ITEM_TITLE = R.id.course_item_title;
	@IdRes
	private static final int ITEM_THUMBNAIL = R.id.course_item_thumbnail;

	/*********************************/
    /*       Member Variable(s)	    **/
	/*********************************/
	private Context context;
	private List<Course> courses;

	/*********************************/
    /*       Constructor (s)	    **/
	/*********************************/
	public CourseGridAdapter (Context context, Collection<Course> courses) {
		this.context = context;
		this.courses = (ArrayList<Course>) courses;
	}

	/*********************************/
    /*       Adapter Override(s)    **/
	/*********************************/
	@Override
	public int getCount() {
		return courses.size();
	}

	@Override
	public Course getItem(int position) {
		return courses.get(position);
	}

	@Override
	public long getItemId(int position) {
		return courses.get(position).getId();
	}

	public void add(@NonNull Course course) {
		add(getCount(), course);
	}

	@Override
	public void add(int position, @NonNull Course course) {
		if (courses.contains(course)) return; // Do nothing if object already exists
		courses.add(position, course);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder holder;
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		if (view!=null) {
			holder = (ViewHolder) view.getTag();
		} else {
			view = inflater.inflate(LAYOUT_ID, parent, false);
			holder = new ViewHolder(view);
		}
	    view.setTag(holder);
	    holder.title.setText(courses.get(position).getTitle());
	    makeThumbnail(holder, getItemId(position));
		return view;
	}

    /*********************************/
    /*          View Holder         **/
    /*********************************/
	protected static class ViewHolder {
	    @Bind(ITEM_TITLE) TextView title;
	    @Bind(ITEM_THUMBNAIL) SquareImageView thumbnail;
	    public ViewHolder(View view) {
	    	ButterKnife.bind(this, view);
	    }
	}

    /*********************************/
    /*        Other Function(s)     **/
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
					String image_url = (!attachments.isEmpty())? attachments.get(0).getGuid() : "";
					//String image_url = attachment.getGuid().substring(0, attachment.getGuid().lastIndexOf("."));
					//image_url += PICTURE_FORMAT;
					if (SHOW_LOG) Log.d(TAG, image_url);

					Glide.with(context)
							.load(image_url)
							.into(holder.thumbnail);
				}
				@Override
				public void error(Exception e) {
					if (SHOW_LOG) Log.d(TAG, "Failed loading image");
				}			
			});
		}
	}

}
