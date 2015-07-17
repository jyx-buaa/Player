	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.fragments;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nined.player.R;
import com.nined.player.client.PlayerApi;
import com.nined.player.model.Course;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;

import org.jsoup.Jsoup;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

	public class CourseFragment extends Fragment {
	/*********************************/
    /*      Logging Assistant(s)    **/
	/*********************************/
	private static final String TAG = CourseFragment.class.getSimpleName();
	private static final boolean SHOW_LOG = false;
	/*********************************/
    /*          Constant(s)         **/
	/*********************************/
	private static final String CURRENT_COURSE = "currentCourse";
	@LayoutRes
	private static final int LAYOUT = R.layout.fragment_course;
	@IdRes
	private static final int TITLE = R.id.course_title;
	@IdRes
	private static final int BODY = R.id.course_body;
	@IdRes
	private static final int PURCHASE = R.id.purchase_button;

	/*********************************/
    /*      View Injection(s)    	**/
	/*********************************/
	@Bind(TITLE) protected TextView title;
	@Bind(BODY) protected TextView body;
	@Bind(PURCHASE) protected Button purchase;

	/*********************************/
    /*       Member Variable(s)     **/
	/*********************************/
	private Context context;
	private Course course;
	/*********************************/
    /*      	Constructor(s)	    **/
	/*********************************/
	public CourseFragment() { super();	}
		/*********************************/
    /*      Lifecycle Override(s)   **/
	/*********************************/
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (course!=null);
		else if (savedInstanceState.getParcelable(CURRENT_COURSE)!=null) {
			this.course = (Course) savedInstanceState.getParcelable("currentCourse");
			if(SHOW_LOG) Log.d(TAG, "create Course from Parcel");
		} else return;
		setUpPayPalPayment();
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
		if (course!=null) outState.putParcelable("currentCourse", course);
	}
	public void setCourse(Course course) {
		this.course = course;
	}
	public Course getCourse() {
		return course;
	}
	/*********************************/
	/**		View Manipulation(s)	**/
	/*********************************/
	protected void refreshViews() {
		title.setText(course.getTitle());
		body.setText(Jsoup.parse(course.getContent()).text());
	}

	@OnClick(PURCHASE)
	protected void onPurchase() {
		PayPalPayment thingToBuy = getThisCoursePayment(PayPalPayment.PAYMENT_INTENT_SALE);
		Intent intent = new Intent(context, PaymentActivity.class);
		intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy);
		startActivityForResult(intent, PlayerApi.REQUEST_CODE_PAYMENT);
	}

	/*********************************/
	/**		SET UP PayPal PAYMENT	**/
	/*********************************/
	protected void setUpPayPalPayment() {
		PayPalConfiguration config = new PayPalConfiguration()
				.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK)
				.clientId(PlayerApi.CONFIG_MERCHANT_ID)
				.merchantName(PlayerApi.CONFIG_MERCHANT_NAME)
				.merchantPrivacyPolicyUri(Uri.parse(PlayerApi.CONFIG_PRIVACY_POLICY_PAGE))
				.merchantUserAgreementUri(Uri.parse(PlayerApi.CONFIG_USER_AGREEMENT_PAGE));

		Intent intent = new Intent(context, PayPalService.class);
		intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
		context.startService(intent);
	}

	private PayPalPayment getThisCoursePayment(String paymentIntent) {
		String productTitle = "DEMO PRICE";
		String currency = "HKD";
		String price = "175.00";
        return new PayPalPayment(new BigDecimal(price), currency, productTitle,
                paymentIntent);
    }
}
