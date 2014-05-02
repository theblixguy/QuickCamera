package com.ssrij.quickcamera;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class QuickCameraPreviewSurface extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	int _cameraId;
	Context ctx;
	
	/* Initialize our surface view for camera */

	@SuppressWarnings("deprecation")
	public QuickCameraPreviewSurface(Context context, Camera camera, int cameraId) {
		super(context);
		this.mCamera = camera;
		_cameraId = cameraId;
		ctx = context;
		this.mSurfaceHolder = this.getHolder();
		this.mSurfaceHolder.addCallback(this);
		this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	/* Set proper camera parameters before camera preview starts */

	@SuppressLint("NewApi") @Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		Camera.Parameters parameters = mCamera.getParameters();

		final DisplayMetrics metrics = new DisplayMetrics(); 
		WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();   

		int viewfinder_height;
		int viewfinder_width;

		display.getRealMetrics(metrics);
		viewfinder_width = metrics.widthPixels;
		viewfinder_height = metrics.heightPixels;

		if (_cameraId == CameraInfo.CAMERA_FACING_BACK) {
			Camera.Size size = getBestPreviewSize(viewfinder_width, viewfinder_height, parameters);
			mCamera.setDisplayOrientation(90);
			parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			parameters.setPreviewSize(size.width, size.height);
		}

		else if (_cameraId == CameraInfo.CAMERA_FACING_FRONT) {
			Camera.Size size1 = getBestPreviewSize(viewfinder_height, viewfinder_width, parameters);
			mCamera.setDisplayOrientation(90);
			parameters.setPreviewSize(size1.width, size1.height);
		}

		try {
			mCamera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.setParameters(parameters);
		mCamera.startPreview();

	}
	
	/* Remove callbacks because the surface view was destroyed */

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		this.getHolder().removeCallback(this);
	}
	
	/* Update picture size */

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
			int width, int height) {

		try {
			Camera.Parameters parameters = mCamera.getParameters();
			List<Size> sizes = parameters.getSupportedPictureSizes();
			parameters.setPictureSize(sizes.get(0).width, sizes.get(0).height);
			mCamera.setParameters(parameters);
			mCamera.setPreviewDisplay(surfaceHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			// intentionally left blank for a test
		}
	}
	
	/* Function to get the best preview size for viewfinder */

	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result=size;
				}
				else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return(result);
	}
}
