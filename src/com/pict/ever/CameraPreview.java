package com.pict.ever;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder holder;
	public Camera camera;
	public static String TAG="CameraPreview";
	Controller controller;

	public CameraPreview (Context context, Controller control_her) {
		super(context);
		holder=getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		controller = control_her;
		holder.setFixedSize(controller.SCREEN_WIDTH, Math.round((float) 
				controller.SCREEN_HEIGHT- controller.SCREEN_HEIGHT/10));
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (controller.CAMERA_FACING.equals("front")) {
			camera = openFrontFacingCamera();
			if (camera == null) {
				Log.v(TAG,"openFrontFacingCamera returns null");
				camera = openBackFacingCamera();
			}
		}
		else 
			camera = openBackFacingCamera();
		if (camera!=null) {
			try {
				camera.setPreviewDisplay(holder);
				camera.setDisplayOrientation(90);
			} catch (IOException e) {
				Log.v(TAG,"IOException : " + e.getMessage());
			}
		}

	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if(camera!=null){
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}

	}
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (holder.getSurface() == null)
			return;
		if (camera!=null) {
			camera.stopPreview();
			try {
				Camera.Parameters parameters = camera.getParameters();
				parameters.setFlashMode(controller.FLASH_MODE);
				String facing = controller.CAMERA_FACING;
				int preview_width = controller.prefs.getInt(facing+"_preview_optisize_width",0);
				int preview_height = controller.prefs.getInt(facing+"_preview_optisize_height",0);
				int picture_width = controller.prefs.getInt(facing+"_picture_optisize_width",0);
				int picture_height = controller.prefs.getInt(facing+"_picture_optisize_height",0);
				if (preview_width!=0 && preview_height!=0)
					parameters.setPreviewSize(preview_width,preview_height);
				if (picture_width!=0 && picture_height!=0)
					parameters.setPictureSize(picture_width,picture_height);
				camera.setParameters(parameters);
			} catch (Exception e){
				e.printStackTrace();
			}
			//start preview of camera
			camera.startPreview();
		}
	}

	public static Camera openBackFacingCamera(){
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
}