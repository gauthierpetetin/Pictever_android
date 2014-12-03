package com.pict.ever;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


public class SignUp extends PicteverActivity {

	Controller controller;
	Toast iolos;
	Typeface font;

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		SignUp.this.overridePendingTransition(R.animator.animation_enter_on_left,
				R.animator.animation_leave_on_right);
	}

	@SuppressLint("ShowToast")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		iolos = Toast.makeText(SignUp.this,"Init",Toast.LENGTH_SHORT);
		iolos.setGravity(Gravity.CENTER_VERTICAL, 0, 0);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();

		if (controller.api < 17) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        setContentView(R.layout.sign_up);
		}
		else {
			setContentView(R.layout.sign_up);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
		
		font = Typeface.createFromAsset(getAssets(),"gabriola.ttf");
		
		((EditText) findViewById(R.id.edit_email_adress)).postDelayed(new Runnable() {
			@Override
			public void run() {
				((EditText) findViewById(R.id.edit_email_adress)).requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(((EditText) findViewById(R.id.edit_email_adress)), 0);
			}
		}, 100);

		controller.btn_signup = (Button) findViewById(R.id.button_sign_up);
		controller.btn_signup.setTextSize(26);
		controller.btn_signup.setTypeface(font);
		controller.btn_signup.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.btn_signup.setAlpha((float)0.3);
				controller.btn_signup.setEnabled(false);
				String email = ((EditText) findViewById(R.id.edit_email_adress)).getText().toString();
				String password = ((EditText) findViewById(R.id.edit_password)).getText().toString();
				if (password.length() > 5) {
					String hashpass="";
					try {
						hashpass = controller.computeHash(password);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					controller.editor = controller.prefs.edit();
					controller.editor.putString("user_email", email);
					controller.editor.putString("user_password",hashpass);
					controller.editor.commit();
					controller.loader = (ProgressBar) findViewById(R.id.progress_bar_loading);
					controller.loader.setVisibility(View.VISIBLE);
					controller.loader.animate();
					controller.signUp(email,hashpass);
				}
				else {
					iolos.setText("The password should be at least 6 characters long");
					iolos.show();
					controller.btn_signup.setAlpha(1);
					controller.btn_signup.setEnabled(true);
				}
			}
		});
	}
}
