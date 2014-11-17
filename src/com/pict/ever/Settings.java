package com.pict.ever;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends PicteverActivity {

	String TAG = "Settings";
	Controller controller;
	Typeface font;
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.overridePendingTransition(R.animator.do_not_move,R.animator.animation_leave_on_bottom2);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		controller.status = controller.prefs.getString("status","Newbie");
		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");

		if (controller.api < 17) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.settings);
		}
		else {
			setContentView(R.layout.settings);
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		TextView tvShyft= (TextView) findViewById(R.id.tvShyft);
		tvShyft.setTextSize(30);
		tvShyft.setTypeface(font,Typeface.BOLD);
		TextView settings_title = (TextView) findViewById(R.id.settings_title);
		settings_title.setTextSize(30);
		settings_title.setTypeface(font);
		TextView info_title = (TextView) findViewById(R.id.info_title);
		info_title.setTypeface(font);
		info_title.setTextSize(30);
		TextView contacts_title = (TextView) findViewById(R.id.contacts_title);
		contacts_title.setTypeface(font);
		contacts_title.setTextSize(30);
		TextView status_title = (TextView) findViewById(R.id.status_title);
		status_title.setTypeface(font);
		status_title.setTextSize(30);
		TextView bug_title = (TextView) findViewById(R.id.bug_title);
		bug_title.setTypeface(font);
		bug_title.setTextSize(30);

		controller.rl_title = (RelativeLayout) findViewById(R.id.rl_title);
		controller.ll_progress = (LinearLayout) findViewById(R.id.ll_progress);

		if (controller.upload_is_active.equals("yes")) {
			if (controller.upload!=null) {
				controller.ll_progress.setVisibility(View.VISIBLE);
				controller.ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) controller.uploadProgression/100*
								controller.prefs.getInt("SCREEN_HEIGHT",0)),
						Math.round((float) controller.prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (controller.rl_title!=null)
					controller.ll_progress_params.addRule(RelativeLayout.BELOW,controller.rl_title.getId());
				controller.ll_progress.setLayoutParams(controller.ll_progress_params);
			}
		}
		else {
			if (controller.upload_is_active.equals("failed")) {
				controller.ll_progress.setVisibility(View.VISIBLE);
				controller.ll_progress.setBackgroundResource(R.color.LightGrey);
				controller.ll_progress_params = new RelativeLayout.LayoutParams(
						Math.round((float) controller.uploadProgression/100*
								controller.prefs.getInt("SCREEN_HEIGHT",0)),
						Math.round((float) controller.prefs.getInt("SCREEN_WIDTH", 500)/100));
				if (controller.rl_title!=null)
					controller.ll_progress_params.addRule(RelativeLayout.BELOW,controller.rl_title.getId());
				controller.ll_progress.setLayoutParams(controller.ll_progress_params);
			}
			else 
				controller.ll_progress.setVisibility(View.INVISIBLE);
		}

		((TextView) findViewById(R.id.email_adress)).setText(controller.prefs.getString("user_email", ""));
		((TextView) findViewById(R.id.phone_number)).setText(controller.prefs.getString("user_phone", ""));
		TextView status = (TextView) findViewById(R.id.status);
		status.setText(controller.status);

		((TextView) findViewById(R.id.block_another_shyfter)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				((TextView) findViewById(R.id.block_another_shyfter)).setBackgroundResource(R.color.LightBlue);
				((TextView) findViewById(R.id.block_another_shyfter)).postDelayed(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(Settings.this,"Noo... why do u wanna do this ?!",Toast.LENGTH_SHORT).show();
						((TextView) findViewById(R.id.block_another_shyfter)).
						setBackgroundResource(R.color.Transparent);
					}
				}, 100);
			}
		});
		((TextView) findViewById(R.id.like_or_share)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				((TextView) findViewById(R.id.like_or_share)).setBackgroundResource(R.color.LightBlue);
				((TextView) findViewById(R.id.like_or_share)).postDelayed(new Runnable() {
					@Override
					public void run() {
						((TextView) findViewById(R.id.like_or_share)).setBackgroundResource(R.color.Transparent);
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://pictever.com"));
						startActivity(browserIntent);
					}
				}, 100);
			}
		});
		((TextView) findViewById(R.id.give_us_a_call)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				((TextView) findViewById(R.id.give_us_a_call)).setBackgroundResource(R.color.LightBlue);
				((TextView) findViewById(R.id.give_us_a_call)).postDelayed(new Runnable() {
					@Override
					public void run() {
						((TextView) findViewById(R.id.give_us_a_call)).setBackgroundResource(R.color.Transparent);
						String posted_by = "+33668648212";

						String uri = "tel:" + posted_by.trim() ;
						Intent intent = new Intent(Intent.ACTION_DIAL);
						intent.setData(Uri.parse(uri));
						startActivity(intent);
					}
				}, 100);
			}
		});

		((ImageButton) findViewById(R.id.button_back)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				Settings.this.overridePendingTransition(R.animator.do_not_move,
						R.animator.animation_leave_on_bottom2);
			}
		});
	}
}






