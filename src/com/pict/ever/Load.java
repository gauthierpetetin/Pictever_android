package com.pict.ever;


import java.util.List;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class Load extends PicteverActivity {

	Controller controller;
	public static final String TAG = "Load";
	TextView tvRemember,tvSendFuture;
	Typeface font;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		controller.api = android.os.Build.VERSION.SDK_INT;
		controller.editor = controller.prefs.edit();
		controller.editor.putInt("api_level", controller.api);
		controller.editor.commit();
		Log.v(TAG,Integer.toString(controller.api));
		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");
		
		// IF THIS IS THE FIRST TIME ON THE APP
		if (controller.prefs.getString("is_connected", "").isEmpty()) {
			startActivity(new Intent(Load.this,WelcomeActivity.class));
			finish();
		}
		else {
			// IF THERE IS A NEW UPDATE TO INSTALL
			if (controller.prefs.getString("force_update","").equals("true")) {
				if (controller.api < 17) {
					requestWindowFeature(Window.FEATURE_NO_TITLE);
					getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
					setContentView(R.layout.update_app);
				}
				else {
					setContentView(R.layout.update_app);
					View decorView = getWindow().getDecorView();
					decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
							View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
				}

				((Button) findViewById(R.id.button_update)).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						controller.editor = controller.prefs.edit();
						controller.editor.putString("force_update", "false");
						controller.editor.commit();
						final String appPackageName = getPackageName();
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW, 
									Uri.parse("market://details?id=" + appPackageName));
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
						} catch (android.content.ActivityNotFoundException anfe) {
							Intent intent = new Intent(Intent.ACTION_VIEW, 
									Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));											intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(intent);
						}
					}
				});
			}
			// ELSE SHOW SPLASH SCREEN, LAUNCH ASYNC REQUESTS AND START CAMERA ACTIVITY
			else {
				if (controller.api < 17) {
					requestWindowFeature(Window.FEATURE_NO_TITLE);
					getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
					setContentView(R.layout.init_layout);
				}
				else {
					setContentView(R.layout.init_layout);
					View decorView = getWindow().getDecorView();
					decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
							| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
				}

				tvRemember = (TextView) findViewById(R.id.tvRemember);
				tvRemember.setTextSize(30);
				tvRemember.setTypeface(font,Typeface.BOLD);
				tvSendFuture = (TextView) findViewById(R.id.tvSendFuture);
				tvSendFuture.setTextSize(30);
				tvSendFuture.setTypeface(font,Typeface.BOLD);
				
				if (controller.wait > 1000) {
					tvRemember.setVisibility(View.INVISIBLE);
					tvRemember.postDelayed(new Runnable() {
						@Override
						public void run() {
							tvRemember.setVisibility(View.VISIBLE);
							Animation anim = AnimationUtils.loadAnimation(Load.this,R.animator.anim_fade_in);
							tvRemember.startAnimation(anim);							
						}
					}, 1000);
					tvSendFuture.setVisibility(View.INVISIBLE);
					tvSendFuture.postDelayed(new Runnable() {
						@Override
						public void run() {
							tvSendFuture.setVisibility(View.VISIBLE);
							Animation anim = AnimationUtils.loadAnimation(Load.this,R.animator.anim_fade_in);
							tvSendFuture.startAnimation(anim);
						}
					}, 3000);
				}

				if (controller.prefs.getInt("back"+ "_preview_optisize_width",0)==0 ||
						controller.prefs.getInt("back"+ "_frame_width",0)==0) {
					Display display = getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					Log.v(TAG, "Screen size = " + Integer.toString(size.x)+ " x " + Integer.toString(size.y));
					controller.editor = controller.prefs.edit();
					controller.editor.putInt("SCREEN_WIDTH", size.y);
					controller.editor.putInt("SCREEN_HEIGHT",size.x);
					controller.SCREEN_WIDTH = size.y;
					controller.SCREEN_HEIGHT = size.x;
					controller.editor.commit();
					Log.v(TAG,"no back camera sizes stored in prefs");
					get_camera_sizes();
				}
				else {
					controller.contacts_from_phone();
					new Handler().postDelayed(new Runnable() {
						public void run() {
							startActivity(new Intent(Load.this, CameraActivity.class));
							finish();
						}
					}, controller.wait);
				}
			}
		}
	}

	public void get_camera_sizes() {
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
				@Override
				protected void onPostExecute(String result) {
					controller.contacts_from_phone();
					new Handler().postDelayed(new Runnable() {
						public void run() {
							startActivity(new Intent(Load.this, CameraActivity.class));
							finish();
						}
					}, controller.wait);
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
							if (picture_sizes.contains(preview_size))
								picture_size=preview_size;
							else {
								PREVIEW_ASPECT_TOLERANCE  = Controller.ASPECT_TOLERANCE;
								while (picture_size==null && PREVIEW_ASPECT_TOLERANCE < 1) {
									picture_size = getOptimalPreviewSize(picture_sizes,
											controller.SCREEN_WIDTH,controller.SCREEN_HEIGHT,PREVIEW_ASPECT_TOLERANCE);
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

								Log.v(TAG,facing +"_display" + " stored = " + Integer.toString(display_width) + " x "+ 
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

				private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h, double aspect_tolerance) {

					try {
						double targetRatio=(double) w / h; 
						Log.v(TAG, "target ratio = " + Double.toString(targetRatio));

						if (sizes == null)
							return null;

						Camera.Size optimalSize = null;
						double old_pixels = 0.0;

						for (Camera.Size size : sizes) {
							double ratio = (double) size.width / size.height;
							Log.v(TAG, Integer.toString(size.width) + " x "+ Integer.toString(size.height) + " : " + Double.toString(ratio));
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
