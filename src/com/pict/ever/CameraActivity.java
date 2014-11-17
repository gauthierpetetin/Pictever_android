package com.pict.ever;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends PicteverActivity {

	private CameraPreview preview;
	private ImageButton ibFromGallery,button_center,button_right,button_left,button_flash,button_switch_camera;
	private FrameLayout camera_preview;
	Controller controller;
	private static final int SELECT_PHOTO = 100;
	Context context;
	public static final String TAG = "CameraActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_preview2);
		controller = ((PicteverApp) getApplication()).getController();
		controller.current_activity = getClass().getSimpleName();
		context = CameraActivity.this;		
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (controller.tvRetry!=null && controller.tvRetry.getVisibility() == View.VISIBLE)
			controller.upload_is_active = "failed";
		if(preview.camera!=null){
			preview.camera.stopPreview();
			preview.camera.setPreviewCallback(null);
			preview.camera.release();
			preview.camera = null;
		}
		if(controller.analytics != null) {
			controller.analytics.getSessionClient().pauseSession();
			controller.analytics.getEventClient().submitEvents();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if(controller.analytics != null) 
			controller.analytics.getSessionClient().resumeSession();
		setContentView(R.layout.camera_preview2);
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE );
		setup();
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {}
	};

	PictureCallback postviewCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {			
			String photo_path = controller.storePictureAfterTaken(data);
			Intent intent = new Intent(CameraActivity.this,AfterPictureTaken.class);
			intent.putExtra("from","camera");
			controller.picture_from="camera";
			intent.putExtra("photo_path", photo_path);
			startActivity(intent);
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 
		switch(requestCode) { 
		case SELECT_PHOTO:
			if(resultCode == RESULT_OK){  
				Uri selectedImage = imageReturnedIntent.getData();
				InputStream imageStream;
				try {
					imageStream = getContentResolver().openInputStream(selectedImage);
					Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
					File imageFileFolder = new File(Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_PICTURES),"Pictever");
					FileOutputStream out = null;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss",Locale.US);
					String photo_path = "photoPath" + sdf.format(new Date());
					File imageFileName = new File(imageFileFolder, photo_path);
					try {
						out = new FileOutputStream(imageFileName);
						yourSelectedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
						out.flush();
						out.close();
						out = null;
					} catch (Exception e) {
						e.printStackTrace();
					}
					Intent intent = new Intent(CameraActivity.this,AfterPictureTaken.class);
					intent.putExtra("from","gallery");
					controller.picture_from="gallery";
					intent.putExtra("photo_path", photo_path);
					startActivity(intent);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setup() {
		ibFromGallery = (ImageButton) findViewById(R.id.ibFromGallery);
		camera_preview = (FrameLayout) findViewById(R.id.camera_preview);
		button_center = (ImageButton) findViewById(R.id.button_center);
		button_left = (ImageButton) findViewById(R.id.button_left);
		button_right = (ImageButton) findViewById(R.id.button_right);
		button_flash = (ImageButton) findViewById(R.id.button_flash);
		button_switch_camera = (ImageButton) findViewById(R.id.button_switch_camera);
		controller.ll_progress = (LinearLayout) findViewById(R.id.ll_progress);
		controller.tvRetry = (TextView) findViewById(R.id.tvRetryUpload);
		ibFromGallery.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!controller.sortedlistContacts.isEmpty()) {
					Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
					photoPickerIntent.setType("image/*");
					startActivityForResult(photoPickerIntent, SELECT_PHOTO);
				}
			}
		});

		controller.tvRetry.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (cm!=null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
					controller.tvRetry.setVisibility(View.INVISIBLE);
					controller.sendToAmazon();
				}
				else 
					Toast.makeText(context,"There is not enough network right now. Please try again later",
							Toast.LENGTH_SHORT).show();
			}
		});

		controller.manage_progress_bar();

		// CREATE CAMERA PREVIEW
		preview = new CameraPreview(this,controller);
		if (preview==null)
			Log.v(TAG,"preview is null. Can't add it to the layout");
		else {
			int surface_width = controller.prefs.getInt(controller.CAMERA_FACING
					+ "_frame_height",controller.SCREEN_HEIGHT);
			int surface_height = controller.prefs.getInt(controller.CAMERA_FACING
					+ "_frame_width",controller.SCREEN_WIDTH);
			Log.v(TAG,"surface WxH = " + Integer.toString(surface_width) + "x" + Integer.toString(surface_height));
			preview.setLayoutParams(new FrameLayout.LayoutParams(surface_width,surface_height));
			// ADD IT TO THE VIEW
			camera_preview.addView(preview);
			Log.v(TAG, "Screen size = " + Integer.toString(controller.SCREEN_HEIGHT)+ 
					" x " + Integer.toString(controller.SCREEN_WIDTH));
			float xpreview = (float) (controller.SCREEN_HEIGHT - surface_width)/2;
			float ypreview = (float) (controller.SCREEN_WIDTH - surface_height)/2;
			Log.v(TAG,"set x and y :" + Float.toString(xpreview) +" , " + Float.toString(ypreview));
			camera_preview.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (preview.camera != null ) {
						try {
							preview.camera.autoFocus(new AutoFocusCallback() {
								@Override
								public void onAutoFocus(boolean success, Camera camera) {
								}
							});
						}
						catch (Exception e ) {
							e.printStackTrace();
						}
					}
				}
			});

			button_left.setVisibility(View.VISIBLE);
			button_left.setEnabled(true);
			button_left.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Animation anim = AnimationUtils.loadAnimation(CameraActivity.this,R.animator.anim_on_click);
					button_left.startAnimation(anim);
					startActivity(new Intent(CameraActivity.this,NewMessage.class));
					CameraActivity.this.overridePendingTransition(
							R.animator.animation_enter_on_left,R.animator.animation_leave_on_right);
				}
			});
			// BUTTON CENTER
			button_center.setOnClickListener( new OnClickListener() {
				public void onClick(View v) {
					if (!controller.sortedlistContacts.isEmpty()) {
						Animation anim = AnimationUtils.loadAnimation(CameraActivity.this,R.animator.anim_on_click);
						button_center.startAnimation(anim);
						preview.camera.takePicture(shutterCallback, rawCallback, postviewCallback, jpegCallback);
					}
				}
			});
			// RIGHT BUTTON
			button_right.setVisibility(View.VISIBLE);
			button_right.setEnabled(true);
			button_right.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Animation anim = AnimationUtils.loadAnimation(CameraActivity.this,R.animator.anim_on_click);
					button_right.startAnimation(anim);
					startActivity(new Intent(CameraActivity.this,Timeline.class));
					CameraActivity.this.overridePendingTransition(
							R.animator.animation_enter_on_right,R.animator.animation_leave_on_left);
				}
			});
			// FLASH BUTTON
			if (controller.CAMERA_FACING.equals("front")){
				button_flash.setVisibility(View.INVISIBLE);
				button_flash.setEnabled(false);
			}
			else {
				button_flash.setVisibility(View.VISIBLE);
				button_flash.setEnabled(true);
				if (controller.FLASH_MODE.equals(Parameters.FLASH_MODE_TORCH)){
					button_flash.setImageResource(R.drawable.flash_on);
				}
				else {
					button_flash.setImageResource(R.drawable.flash_off);
				}
				button_flash.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						try{
							if (controller.FLASH_MODE.equals(Parameters.FLASH_MODE_OFF)) {
								button_flash.setImageResource(R.drawable.flash_on);
								Parameters p = preview.camera.getParameters();
								p.setFlashMode(Parameters.FLASH_MODE_TORCH);
								preview.camera.setParameters(p);
								controller.FLASH_MODE = Parameters.FLASH_MODE_TORCH;}
							else {
								button_flash.setImageResource(R.drawable.flash_off);
								Parameters p = preview.camera.getParameters();
								p.setFlashMode(Parameters.FLASH_MODE_OFF);
								preview.camera.setParameters(p);
								controller.FLASH_MODE = Parameters.FLASH_MODE_OFF;}
						}
						catch (Exception e ) {
							e.printStackTrace();
						}
					}
				});
			}

			// SWITCH CAMERA BUTTON
			button_switch_camera = (ImageButton) findViewById(R.id.button_switch_camera);
			button_switch_camera.setVisibility(View.VISIBLE);
			button_switch_camera.setEnabled(true);
			button_switch_camera.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (controller.CAMERA_FACING.equals("front")){
						controller.CAMERA_FACING = "back";
						recreate();
					}
					else {
						controller.CAMERA_FACING = "front";
						recreate();
					}
				}
			});
		}
	}
}