package com.pict.ever;

import java.io.IOException;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class SetPhoneNumber extends Activity {

	Controller controller;
	String user_phone = "";;
	String TAG = "PhoneNumber";
	ListView listview_country_codes;
	Typeface font;
	EditText edit_phone;
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	GoogleCloudMessaging gcm;
	String reg_id = "",local_code,display_code="";
	private Context context;
	String[] source;

	@SuppressLint("ShowToast")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();

		context = SetPhoneNumber.this;

		if (controller.api < 17) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.phone_number);
		}
		else {
			setContentView(R.layout.phone_number);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}

		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");
//		TextView txt2 = (TextView) findViewById(R.id.phone_number_title);
//		txt2.setTextSize(30);
//		txt2.setTypeface(font);

		edit_phone = (EditText) findViewById(R.id.edit_phone_number);

		String where_am_i ="";
		try {
			final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			final String simCountry = tm.getSimCountryIso();
			if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
				where_am_i = simCountry.toLowerCase(Locale.US);
			}
			else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
				String networkCountry = tm.getNetworkCountryIso();
				if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
					where_am_i = networkCountry.toLowerCase(Locale.US);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		source = this.getResources().getStringArray(R.array.CountryCodes);
		for (String s : source){
			if (where_am_i.equals(s.split(",")[1].toLowerCase(Locale.US))) {
				((ImageButton) findViewById(R.id.button_countries)).setImageResource(
						context.getResources().getIdentifier("drawable/" +  where_am_i, 
								null, context.getPackageName()));
				local_code = s.split(",")[0];
				controller.editor = controller.prefs.edit();
				controller.editor.putString("local_code_selected", "00"+local_code);
				controller.editor.commit();
				if (where_am_i.equals("us"))
					display_code="";
				else
					display_code = "+"+local_code+ " ";
				edit_phone.setText(display_code);
				edit_phone.setSelection(edit_phone.length());
			}
		}
		listview_country_codes = (ListView) findViewById(R.id.listview_country_codes);
		listview_country_codes.setVisibility(View.INVISIBLE);
		listview_country_codes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)  {
				local_code = source[position].split(",")[0];
				Log.v(TAG,local_code);
				listview_country_codes.setVisibility(View.INVISIBLE);
				String ssid = source[position].split(",")[1];
				Locale locale = new Locale("", ssid);
				String pngName = ssid.trim().toLowerCase(locale);
				((ImageButton) findViewById(R.id.button_countries)).setImageResource(
						context.getResources().getIdentifier("drawable/" + pngName, 
								null, context.getPackageName()));
				controller.editor = controller.prefs.edit();
				controller.editor.putString("local_code_selected", "00"+local_code);
				controller.editor.commit();
				if (ssid.toLowerCase(Locale.US).equals("us"))
					display_code="";
				else
					display_code = "+"+local_code+ " ";
				edit_phone.setText(display_code);
				edit_phone.setSelection(edit_phone.length());
				edit_phone.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(edit_phone, 0);
			}
		});
		listview_country_codes.setAdapter(new AdapterCountriesList(this, source));

		ImageButton ibFlag = (ImageButton) findViewById(R.id.button_countries);
		ibFlag.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (listview_country_codes.getVisibility()==View.VISIBLE) {
					Animation anim = AnimationUtils.loadAnimation(context,R.animator.animation_leave_on_bottom);
					listview_country_codes.startAnimation(anim);
					listview_country_codes.setVisibility(View.INVISIBLE);
					((EditText) findViewById(R.id.edit_phone_number)).requestFocus();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(((EditText) findViewById(R.id.edit_phone_number)), 0);
				}
				else {
					listview_country_codes.setVisibility(View.VISIBLE);
					Animation anim = AnimationUtils.loadAnimation(context,R.animator.animation_enter_on_bottom);
					listview_country_codes.startAnimation(anim);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(edit_phone.getWindowToken(), 
							InputMethodManager.RESULT_UNCHANGED_SHOWN);
				}
			}
		});
		edit_phone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listview_country_codes.getVisibility()==View.VISIBLE) {
					Animation anim = AnimationUtils.loadAnimation(context,R.animator.animation_leave_on_bottom);
					listview_country_codes.startAnimation(anim);
					listview_country_codes.setVisibility(View.INVISIBLE);
				}
			}
		});
		edit_phone.postDelayed(new Runnable() {
			@Override
			public void run() {
				edit_phone.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(edit_phone, 0);
			}
		}, 100);
		edit_phone.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.toString().length() < 5) {
					controller.btn_verify.setAlpha((float) 0.4);
					controller.btn_verify.setEnabled(false);
				}
				else {
					controller.btn_verify.setVisibility(View.VISIBLE);
					controller.btn_verify.setAlpha((float) 1);
					controller.btn_verify.setEnabled(true);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			reg_id = controller.prefs.getString("reg_id", "");
			if (reg_id.isEmpty()) {
				registerInBackground();
			}
			else {
				if (controller.prefs.getString("facebook_id", "").isEmpty()) 
					controller.login("");
				else
					controller.login("from_facebook");
			}
		} else {
			Log.i(TAG,"No valid Google Play Services APK found.");
		}
		
		controller.btn_verify = (Button) findViewById(R.id.button_verify);
		controller.btn_verify.setTextSize(25);
		controller.btn_verify.setTypeface(font);
		controller.btn_verify.setAlpha((float)0.4);
		controller.btn_verify.setEnabled(false);
		controller.btn_verify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.btn_verify.setAlpha((float) 0.2);
				controller.btn_verify.setEnabled(false);
				String code = controller.prefs.getString("local_code_selected","001");
				String num="";
				if (edit_phone.getText().toString().startsWith(display_code))
					num = edit_phone.getText().toString().substring(display_code.length());
				else
					num = edit_phone.getText().toString();
				if (num.length() < 2) {
					controller.btn_verify.setAlpha((float) 1);
					controller.btn_verify.setEnabled(true);
				}
				else {
					num = num.replace(" ", "");
					if (num.startsWith("00")) {
						user_phone = num;
					}
					else {
						if (num.startsWith("+")) {
							user_phone = "00" + num.substring(1);
						}
						else {
							if (num.startsWith("0")) {
								num = num.substring(1);
								user_phone = code + num;
							}
							else {
								user_phone = code + num;
							}
						}
					}
					Log.v(TAG,"user phone = " + user_phone);
					if (user_phone.startsWith("00")) {
						controller.editor = controller.prefs.edit();
						controller.editor.putString("user_phone", user_phone);
						controller.editor.commit();
						Log.v(TAG,"user phone = " + user_phone);
						controller.loader = (ProgressBar) findViewById(R.id.progress_bar_loading);
						controller.loader.setVisibility(View.VISIBLE);
						controller.loader.animate();
						controller.defineFirstPhoneNumber(user_phone);
					}
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
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
						Toast.makeText(context, gcm.toString(), Toast.LENGTH_SHORT).show();
					}
					reg_id = gcm.register(controller.SENDER_ID);
					msg = "Device registered, registration ID=" + reg_id;
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
				}
				return msg;
			}
			@Override
			protected void onPostExecute(String msg) {
				Log.i(TAG,"Registration ID =  " + reg_id);
				controller.editor = controller.prefs.edit();
				controller.editor.putString("reg_id", reg_id);
				controller.editor.commit();
				if (controller.prefs.getString("facebook_id", "").isEmpty()) 
					controller.login("");
				else
					controller.login("from_facebook");
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i("PhoneNumber", " Play Service : This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}
}
