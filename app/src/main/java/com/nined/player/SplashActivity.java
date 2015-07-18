/**
 *
 * @author Aekasitt Guruvanich, 9D Tech
 *
 */
package com.nined.player;

/**
 * Created by Aekasitt on 7/17/2015.
 */

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AnimatorRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

//import com.nined.player.client.PlayerSvc;

public class SplashActivity extends Activity {
    /*********************************/
    /*      Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = SplashActivity.class.getSimpleName();
    private static final boolean SHOW_LOG = false;
    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    @LayoutRes
    private static final int LAYOUT_ID = R.layout.activity_splash;
    @IdRes
    private static final int SPLASH = R.id.splash;
    @IdRes
    private static final int BLOCK = R.id.splash_entry_block;
    //@IdRes
    //private static final int BACKGROUND = R.id.splash_background;
    @IdRes
    private static final int USERNAME = R.id.splash_username_entry;
    @IdRes
    private static final int PASSWORD = R.id.splash_password_entry;
    @IdRes
    private static final int LOGIN = R.id.splash_login_btn;
    @AnimatorRes
    private static final int ROTATE = R.animator.rotate;
    @AnimatorRes
    private static final int POPUP = R.animator.pop_up;
    @AnimatorRes
    private static final int CRTOFF = R.animator.crt_off;
    @StringRes
    private static final int PROMPT = R.string.prompt_login;

    /*********************************/
    /**       View Injection(s)     **/
    /*********************************/
    @Bind(SPLASH) protected ImageView splash;
    @Bind(BLOCK) protected LinearLayout block;
    //@Bind(BACKGROUND) protected LinearLayout background;
    @Bind(USERNAME) protected EditText username;
    @Bind(PASSWORD) protected EditText password;
    @Bind(LOGIN) protected Button login;

    /*********************************/
    /**     Lifecycle Override(s)   **/
    /*********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(LAYOUT_ID);
        ButterKnife.bind(this);

        // Attach Animation to the Splash Logo
        AnimatorSet rotation = (AnimatorSet) AnimatorInflater.loadAnimator(this, ROTATE);
        rotation.setTarget(splash);
        // Attach Animation to the Entry Block
        final AnimatorSet popUp = (AnimatorSet) AnimatorInflater.loadAnimator(this, POPUP);
        popUp.setTarget(block);

        // Run Animations in Succession
        rotation.start();
        rotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                popUp.start();
            }
        });
    }
    /**
     * Just let me have this one ok?
     * @author Aekasitt
     */
    @OnTextChanged (USERNAME) protected void changeButtonText() {
        if (SHOW_LOG) Log.d(TAG, "changeButtonText");
        login.setText(PROMPT);
    }
    /**
     * Create an instance of MoocSvc using specified username and password if given,
     * initialize an anonymous session otherwise.
     * then Launch MainActivity via Intent
     */
    @OnClick (LOGIN)
    protected void launchMain() {
        if (SHOW_LOG) Log.d(TAG, "launchMain");

        if (username.getText().length()<1) {
            //MoocSvc.initAnonymous();
        } else {
            /*MoocSvc.init(
                    user.getText().toString(),
                    pwd.getText().toString());*/
        }
        AnimatorSet launch = (AnimatorSet) AnimatorInflater.loadAnimator(this,  CRTOFF);
        launch.setTarget(login);
        launch.start();
        launch.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                SplashActivity.this.finish();
            }
        });
    }

}

