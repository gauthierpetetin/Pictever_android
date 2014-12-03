package com.pict.ever;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class Login extends PicteverActivity {

	public static final String EXTRA_MESSAGE = "message";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	static final String TAG = "Login";
	Typeface font;
	GoogleCloudMessaging gcm;
	String reg_id,user_email,user_password;
	Controller controller;
	private Context context;

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent intent = new Intent(Login.this,WelcomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		this.overridePendingTransition(R.animator.animation_enter_on_left,R.animator.animation_leave_on_right);
		finish();
	}	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		context = getApplicationContext();

		if (controller.api < 17) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.login);
		}
		else {
			setContentView(R.layout.login);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}

		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(context);
			reg_id = controller.prefs.getString("reg_id", "");
			if (reg_id.isEmpty()) {
				registerInBackground();
			}
		} else {
			Log.v(TAG, "No valid Google Play Services APK found.");
		}

		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");

		if (!controller.prefs.getString("user_email", "").isEmpty())
			((EditText) findViewById(R.id.edit_email_adress)).
			setText(controller.prefs.getString("user_email", ""));

		((EditText) findViewById(R.id.edit_email_adress)).postDelayed(new Runnable() {
			@Override
			public void run() {
				((EditText) findViewById(R.id.edit_email_adress)).requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(((EditText) findViewById(R.id.edit_email_adress)), 0);
			}
		}, 100);

		((TextView) findViewById(R.id.reset_password)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = ((EditText) findViewById(R.id.edit_email_adress)).getText().toString();
				if (!email.isEmpty() && email.contains("@")) {
					email = email.replace(" ","");
					controller.editor = controller.prefs.edit();
					controller.editor.putString("user_email", email);
					controller.editor.commit();
					Log.v(TAG,"reset");
					((TextView) findViewById(R.id.reset_password)).setAlpha((float)0.3);
				}
				if (!controller.prefs.getString("user_email", "").isEmpty())
					controller.send_reset_mail(controller.prefs.getString("user_email", ""));
				else {
					Toast.makeText(context,"Please enter a valid email adress",Toast.LENGTH_SHORT).show();
					((TextView) findViewById(R.id.reset_password)).setAlpha(1);
				}
			}
		});

		controller.btn_login = (Button) findViewById(R.id.button_login);
		controller.btn_login.setAlpha(1);
		controller.btn_login.setTextSize(26);
		controller.btn_login.setTypeface(font);
		controller.btn_login.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.btn_login.setAlpha((float) 0.3);
				controller.btn_login.setEnabled(false);
				user_email = ((EditText) findViewById(R.id.edit_email_adress)).getText().toString();
				String password = ((EditText) findViewById(R.id.edit_password)).getText().toString();
				try {
					user_password = controller.computeHash(password);
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (checkPlayServices()) {
					gcm = GoogleCloudMessaging.getInstance(context);
					reg_id = controller.prefs.getString("reg_id", "");
					if (reg_id.isEmpty())
						registerInBackground();
					else 
						login();
				} else {
					controller.btn_login.setAlpha(1);
					Log.v(TAG, "No valid Google Play Services APK found.");
				}
			}
		});
	}

	private void registerInBackground() {
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				String msg = "";
				try {
					if (gcm == null)
						gcm = GoogleCloudMessaging.getInstance(context);
					reg_id = gcm.register(controller.SENDER_ID);
					msg = "Device registered, registration ID=" + reg_id;

				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
				}
				return msg;
			}
			@Override
			protected void onPostExecute(String msg) {
				if (!controller.btn_login.isEnabled())
					login();
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			else {
				Log.i(TAG, " Play Service : This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	private void login() {
		controller.loader = (ProgressBar) findViewById(R.id.progress_bar_loading);
		controller.loader.setVisibility(View.VISIBLE);
		controller.loader.animate();
		controller.editor = controller.prefs.edit();
		controller.editor.putString("user_email",user_email);
		controller.editor.putString("user_password",user_password);
		controller.editor.putString("reg_id",reg_id);
		controller.editor.commit();
		controller.login(controller.FROM_LOGIN);
	}
}

