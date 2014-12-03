package com.pict.ever;

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

public class WelcomeActivity extends PicteverActivity {

	Button button_sign_up, button_login;
	LoginButton button_fb;
	Controller controller;
	public static final String TAG = "WelcomeActivity";
	Typeface font1;
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};
	private class meCallback implements GraphUserCallback {
		@Override
		public void onCompleted(GraphUser user, Response response) {
			if (response.getError()==null) {
				String name = user.getName();
				controller.editor=controller.prefs.edit();
				controller.editor.putString("facebook_id",user.getId());
				controller.editor.putString("facebook_birthday",user.getBirthday());
				if (!name.isEmpty())
					controller.editor.putString("facebook_name",name);
				if (user.getProperty("email")!=null)
					controller.editor.putString("user_email",user.getProperty("email").toString());
				controller.editor.commit();
				Intent intent = new Intent(WelcomeActivity.this, SetPhoneNumber.class);
				startActivity(intent);
				finish();
			}
			else {
				Toast.makeText(WelcomeActivity.this,"Facebook server has encountered an error",Toast.LENGTH_SHORT).show();
				((LoginButton) findViewById(R.id.authButton)).setVisibility(View.VISIBLE);
				((Button) findViewById(R.id.button_welcome_sign_up)).setVisibility(View.VISIBLE);
				((Button) findViewById(R.id.button_welcome_login)).setVisibility(View.VISIBLE);
			}
		}       
	}

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (state.isOpened()) {
			Log.i(TAG, "Logged in...");
			Request.newMeRequest(session,new meCallback()).executeAsync();
		} else if (state.isClosed()) {
			Log.i(TAG, "Logged out...");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
		if (!controller.prefs.getString("facebook_id", "").isEmpty()) {
			((LoginButton) findViewById(R.id.authButton)).setVisibility(View.INVISIBLE);
			((Button) findViewById(R.id.button_welcome_sign_up)).setVisibility(View.INVISIBLE);
			((Button) findViewById(R.id.button_welcome_login)).setVisibility(View.INVISIBLE);
		}

		button_fb = (LoginButton) findViewById(R.id.authButton);
		button_fb.setReadPermissions(Arrays.asList("public_profile", "email"));
		button_fb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				(new Handler()).postDelayed(new Runnable() {
					@Override
					public void run() {
						((LoginButton) findViewById(R.id.authButton)).setVisibility(View.INVISIBLE);
						((Button) findViewById(R.id.button_welcome_sign_up)).setVisibility(View.INVISIBLE);
						((Button) findViewById(R.id.button_welcome_login)).setVisibility(View.INVISIBLE);
					}
				}, 300);
				Session session = Session.getActiveSession();
				if (session != null &&
						(session.isOpened() || session.isClosed()) ) {
					onSessionStateChange(session, session.getState(), null);
				}
				uiHelper.onResume();
			}
		});

		button_sign_up = (Button) findViewById(R.id.button_welcome_sign_up);
		button_sign_up.setTypeface(font1,Typeface.NORMAL);
		button_sign_up.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation anim = AnimationUtils.loadAnimation(WelcomeActivity.this,R.animator.anim_on_click);
				button_sign_up.startAnimation(anim);
				Intent intent = new Intent(WelcomeActivity.this, SignUp.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				WelcomeActivity.this.overridePendingTransition(R.animator.animation_enter_on_right,
						R.animator.animation_leave_on_left);
			}
		});
		button_login = (Button) findViewById(R.id.button_welcome_login);
		button_login.setTypeface(font1,Typeface.NORMAL);
		button_login.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation anim = AnimationUtils.loadAnimation(WelcomeActivity.this,R.animator.anim_on_click);
				button_login.startAnimation(anim);
				Intent intent = new Intent(WelcomeActivity.this, Login.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				WelcomeActivity.this.overridePendingTransition(R.animator.animation_enter_on_right,
						R.animator.animation_leave_on_left);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_full_screen);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		controller.api = android.os.Build.VERSION.SDK_INT;
		uiHelper = new UiLifecycleHelper(WelcomeActivity.this, callback);
		uiHelper.onCreate(savedInstanceState);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		Log.v(TAG, "Screen size = " + Integer.toString(size.x)+ " x " + Integer.toString(size.y));
		controller.editor = controller.prefs.edit();
		controller.editor.putInt("api_level",controller.api);
		controller.editor.putInt("SCREEN_WIDTH", size.y);
		controller.editor.putInt("SCREEN_HEIGHT",size.x);
		controller.SCREEN_WIDTH = size.y;
		controller.SCREEN_HEIGHT = size.x;
		controller.editor.commit();
		font1 = Typeface.createFromAsset(getAssets(), "robotomedium.ttf");
		camera_sizes();
		
		Session session = Session.getActiveSession();
		if (session!=null && session.getState().isOpened()){
			Log.v(TAG,"onCreate session opened");
			Request.newMeRequest(session,new meCallback()).executeAsync();
		}
	}

	public void camera_sizes() {
		try {
			new AsyncTask<String,Void,String>() {
				@Override
				protected String doInBackground(String... params) {
					Camera camera=null;
					if (Camera.getNumberOfCameras() > 1) {
						camera = openFrontFacingCamera();	
						getPreviewandPictureSizes(camera,"front");
						camera.release();
						camera=null;
					}
					camera = openBackFacingCamera();
					getPreviewandPictureSizes(camera,"back");
					camera.release();
					camera=null;
					return "done";
				}

				private void getPreviewandPictureSizes(Camera camera,String facing) {
					if (camera!=null) {				
						Camera.Size preview_size = null;
						Camera.Size picture_size = null;
						Camera.Parameters parameters = camera.getParameters();
						List<Size> picture_sizes = parameters.getSupportedPictureSizes();

						List<Size> preview_sizes = parameters.getSupportedPreviewSizes();
						double PREVIEW_ASPECT_TOLERANCE  = Controller.ASPECT_TOLERANCE;
						while (preview_size==null && PREVIEW_ASPECT_TOLERANCE < 0.5) {
							preview_size = getOptimalPreviewSize(preview_sizes,
									controller.SCREEN_WIDTH,controller.SCREEN_HEIGHT,PREVIEW_ASPECT_TOLERANCE);
							PREVIEW_ASPECT_TOLERANCE = PREVIEW_ASPECT_TOLERANCE + 0.01;
						}
						if (preview_size!=null) {
							Log.v(TAG,facing +"_preview_optisize" + " stored = " 
									+ Integer.toString(preview_size.width) + " x "+ 
									Integer.toString(preview_size.height) +  " : " + 
									Double.toString((double) preview_size.width / preview_size.height));

							//CALCULATE FRAME LAYOUT DIMENSIONS
							double frame_ratio = Math.max((double) controller.SCREEN_WIDTH/preview_size.width, 
									(double) controller.SCREEN_HEIGHT/preview_size.height);
							int frame_width = (int) Math.round((double) preview_size.width*frame_ratio);
							int frame_height = (int) Math.round((double) preview_size.height*frame_ratio);

							Log.v(TAG,facing +"_frame" + " stored = " + Integer.toString(frame_width) + " x "+ 
									Integer.toString(frame_height) +  " : " + 
									Double.toString((double) frame_width / frame_height));

							controller.editor = controller.prefs.edit();
							controller.editor.putInt(facing + "_frame_width",frame_width);
							controller.editor.putInt(facing + "_frame_height",frame_height);
							controller.editor.putInt(facing + "_preview_optisize_width",preview_size.width);
							controller.editor.putInt(facing + "_preview_optisize_height", preview_size.height);
							controller.editor.commit();


							if (picture_sizes.contains(preview_size)) {
								picture_size=preview_size;
							}
							else {
								PREVIEW_ASPECT_TOLERANCE  = Controller.ASPECT_TOLERANCE;
								while (picture_size==null && PREVIEW_ASPECT_TOLERANCE < 1) {
									picture_size = getOptimalPreviewSize(picture_sizes,
											controller.SCREEN_WIDTH,controller.SCREEN_HEIGHT,
											PREVIEW_ASPECT_TOLERANCE);
									PREVIEW_ASPECT_TOLERANCE = PREVIEW_ASPECT_TOLERANCE + 0.01;
								}
							}
							if (picture_size!=null) {
								Log.v(TAG, facing +"_picture_optisize"+ " stored = " 
										+ Integer.toString(picture_size.width) + " x "+ 
										Integer.toString(picture_size.height) +  " : " + 
										Double.toString((double) picture_size.width/picture_size.height));

								//CALCULATE DISPLAY DIMENSIONS
								double display_ratio = Math.max((double) controller.SCREEN_WIDTH/picture_size.width, 
										(double) controller.SCREEN_HEIGHT/picture_size.height);
								int display_width = (int) Math.round((double) picture_size.width*display_ratio);
								int display_height = (int) Math.round((double) picture_size.height*display_ratio);

								Log.v(TAG,facing +"_display" + " stored = " 
										+ Integer.toString(display_width) + " x "+ 
										Integer.toString(display_height) +  " : " + 
										Double.toString((double) display_width / display_height));
								controller.editor = controller.prefs.edit();
								controller.editor.putInt(facing + "_display_width",display_width);
								controller.editor.putInt(facing + "_display_height",display_height);
								controller.editor.putInt(facing +"_picture_optisize_width",picture_size.width);
								controller.editor.putInt(facing +"_picture_optisize_height", picture_size.height);
								controller.editor.commit();
							}
						}
					}
				}

				private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, 
						int w, int h, double aspect_tolerance) {

					try {
						double targetRatio=(double) w / h; 
						Log.v(TAG, "target ratio = " + Double.toString(targetRatio));

						if (sizes == null)
							return null;

						Camera.Size optimalSize = null;
						double old_pixels = 0.0;

						for (Camera.Size size : sizes) {
							double ratio = (double) size.width / size.height;
							Log.v(TAG, Integer.toString(size.width) + " x "
									+ Integer.toString(size.height) + " : " + Double.toString(ratio));
							double pixels = (double) size.width *size.height;

							if (Math.abs(ratio - targetRatio) < aspect_tolerance) {
								if (pixels > old_pixels  && pixels < Controller.PICTURE_MAX_SIZE) {
									old_pixels = pixels;
									optimalSize = size;
								}	
							}
						}
						return optimalSize;
					}
					catch(Exception e) {
						e.printStackTrace();
						return null;
					}
				}

				private Camera openBackFacingCamera(){
					Camera object = null;
					try {
						object = Camera.open(); 
					}
					catch (Exception e){
					}
					return object; 
				}

				private Camera openFrontFacingCamera() {
					int cameraCount = 0;
					Camera cam = null;
					Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
					cameraCount = Camera.getNumberOfCameras();
					Log.v(TAG,"number of cameras : " + Integer.toString(cameraCount));
					for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
						try {
							Camera.getCameraInfo(camIdx, cameraInfo);
						}
						catch (Exception e) {
							Log.e(TAG,"getCameraInfo exception: " + e.getMessage());
						}
						if (cameraInfo!=null) {
							if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
								try {
									cam = Camera.open(camIdx);
								} catch (RuntimeException e) {
									Log.e(TAG, "Front camera failed to open: " + e.getLocalizedMessage());
								}
							}
						}
					}
					return cam;
				}
			}.execute();
		}
		catch (Exception e) {
		}
	}
}