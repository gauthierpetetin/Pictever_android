package com.pict.ever;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class WelcomeActivity extends Activity {

	Button button_sign_up, button_login;
	Controller controller;
	public static final String TAG = "WelcomeActivity";
	Typeface font;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_full_screen);

		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		controller.api = android.os.Build.VERSION.SDK_INT;

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
		font = Typeface.createFromAsset(getAssets(), "gabriola.ttf");
		TextView tvPicteverTitle = (TextView) findViewById(R.id.tvPicteverTitle);
		tvPicteverTitle.setTypeface(font,Typeface.NORMAL);

		camera_sizes();

		button_sign_up = (Button) findViewById(R.id.button_welcome_sign_up);
		button_sign_up.setTypeface(font,Typeface.NORMAL);
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
		button_login.setTypeface(font,Typeface.NORMAL);
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