package com.pict.ever;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


public class ResetPassword extends PicteverActivity {

	static final String TAG = "ResetPassword";
	Typeface font;
	Controller controller;
	private Context context;

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent intent = new Intent(ResetPassword.this,Login.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
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
			setContentView(R.layout.reset_password);
		}
		else {
			setContentView(R.layout.reset_password);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}

		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");

		Button reset_button = (Button) findViewById(R.id.button_reset);
		reset_button.setAlpha(1);
		reset_button.setTextSize(28);
		reset_button.setTypeface(font);
		reset_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String user_password="";
				String verification_code = ((EditText) findViewById(R.id.edit_verification_code))
						.getText().toString();
				String new_password = ((EditText) findViewById(R.id.edit_new_password)).getText().toString();
				String confirm_new_password = ((EditText) findViewById(R.id.edit_confirm_new_password))
						.getText().toString();
				if (new_password.length() > 6) {
				if (new_password.equals(confirm_new_password)) {
					try {
						user_password = controller.computeHash(new_password);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					controller.editor = controller.prefs.edit();
					controller.editor.putString("user_password",user_password);
					controller.editor.commit();
					controller.defineNewPassword(controller.prefs.getString("user_email",""),
							verification_code,user_password);
					controller.loader = (ProgressBar) findViewById(R.id.progress_bar_loading);
					controller.loader.setVisibility(View.VISIBLE);
					controller.loader.animate();
				}
				else
					Toast.makeText(context, "The two passwords are different", Toast.LENGTH_LONG).show();
				}
				else
					Toast.makeText(context, "The password should be 6 characters min", Toast.LENGTH_LONG).show();
			}
		});
	}
}

