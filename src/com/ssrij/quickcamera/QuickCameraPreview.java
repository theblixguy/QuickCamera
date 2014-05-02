package com.ssrij.quickcamera;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.ssrij.quickcamera.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class QuickCameraPreview extends Activity {

	/* Variables we require */
	private Camera mCamera;
	RelativeLayout preview;
	Timer timer;
	TimerTask OptionsTimerTask;
	private QuickCameraPreviewSurface mCameraPreview;
	int camBackId = Camera.CameraInfo.CAMERA_FACING_BACK;
	int camFrontId = Camera.CameraInfo.CAMERA_FACING_FRONT;
	static final int MIN_DISTANCE = 500;
	static final int MIN_DISTANCE_OPTIONS = 300;
	private PointerCoords mDownPos = new PointerCoords();
	private PointerCoords mUpPos = new PointerCoords();
	int currentCamId;
	boolean is_timer_running = false;

	Handler handler = new Handler();
	Runnable runnable = new Runnable() {
		public void run() {
			fadeOutOptions();
		}
	};

	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quickcamerapreview);
		preview = (RelativeLayout)findViewById(R.id.camera_preview);
		ImageButton settings_btn = (ImageButton)findViewById(R.id.imageButton2);
		initBackCamera();
		handler.postDelayed(runnable, 7000);
		is_timer_running = true;

		settings_btn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "Not implemented!", Toast.LENGTH_SHORT).show();
			}
		});

		String[] projection = new String[]{
				MediaStore.Images.ImageColumns._ID,
				MediaStore.Images.ImageColumns.DATA,
				MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
				MediaStore.Images.ImageColumns.DATE_TAKEN,
				MediaStore.Images.ImageColumns.MIME_TYPE
		};
		final Cursor cursor = getContentResolver()
				.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, 
						null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

		if (cursor.moveToFirst()) {
			ImageButton image_preview_btn = (ImageButton)findViewById(R.id.imageButton1);
			Bitmap bm = BitmapFactory.decodeFile(cursor.getString(1));
			image_preview_btn.setImageBitmap(bm);
		}
	}

	/* Initialize back camera and start previewing, also listen for touch events */

	public void initBackCamera() {
		mCamera = getCameraInstance(camBackId);
		mCameraPreview = new QuickCameraPreviewSurface(this, mCamera, camBackId);
		preview.addView(mCameraPreview);
		mCamera.startPreview();
		mCamera.stopPreview();
		mCamera.startPreview();

		mCameraPreview.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN: {
					event.getPointerCoords(0, mDownPos);
					return true;
				}
				case MotionEvent.ACTION_UP: {
					event.getPointerCoords(0, mUpPos);
					float dx = mDownPos.x - mUpPos.x;
					if (Math.abs(dx) > MIN_DISTANCE) {
						if (dx > 0) {
							switchCameras();
						}
						else {
							if (is_timer_running == false) {
								fadeInOptions();
							}
						}
						return true;
					} 
					else {
						Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
						vibrator.vibrate(100);
						mCamera.takePicture(null, null, mPicture);
						mCamera.stopPreview();
					}
				}
				}
				return false;
			}
		});

	}

	/* Uninitialize back camera and stop previewing */

	public void uninitBackCamera() {
		preview.removeView(mCameraPreview);
		mCamera.stopPreview();
		mCamera.release();
	}

	/* Initialize front camera and start previewing, also listen for touch events */

	public void initFrontCamera() {
		mCamera = getCameraInstance(camFrontId);
		mCameraPreview = new QuickCameraPreviewSurface(this, mCamera, camFrontId);
		preview.addView(mCameraPreview);
		mCamera.startPreview();
		mCamera.stopPreview();
		mCamera.startPreview();

		mCameraPreview.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN: {
					event.getPointerCoords(0, mDownPos);
					return true;
				}
				case MotionEvent.ACTION_UP: {
					event.getPointerCoords(0, mUpPos);
					float dx = mDownPos.x - mUpPos.x;
					if (Math.abs(dx) > MIN_DISTANCE) {
						if (dx > 0) {
							switchCameras();
						}
						else {
							if (is_timer_running == false) {
								fadeInOptions();
							}
						}
						return true;
					} 
					else {
						Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
						vibrator.vibrate(100);
						mCamera.takePicture(null, null, mPicture);
						mCamera.stopPreview();
					}
				}
				}
				return false;
			}
		});
	}

	/* Uninitialize back camera and stop previewing */

	public void uninitFrontCamera() {
		preview.removeView(mCameraPreview);
		mCamera.stopPreview();
		mCamera.release();
	}

	/* Switch between front/back cameras */

	public void switchCameras() {
		if (currentCamId == camBackId) {
			uninitBackCamera();
			initFrontCamera();
			currentCamId = camFrontId;
		}
		else if(currentCamId == camFrontId) {
			uninitFrontCamera();
			initBackCamera();
			currentCamId = camBackId;
		}
	}

	/* Uninit camera to other camera apps can run */

	@Override
	protected void onPause() {
		super.onPause();

		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		if (mCameraPreview != null) {
			mCameraPreview.setOnTouchListener(null);
			preview.removeView(mCameraPreview);
			mCameraPreview = null;
		}

	}

	/* Init camera and resume preview */

	@Override
	protected void onResume() {
		super.onResume();

		if (mCamera == null && mCameraPreview == null) {
			if (currentCamId == camBackId) {
				initBackCamera();
			}
			else if(currentCamId == camFrontId) {
				initFrontCamera();
			}
		}
	}

	/* Get an instance of front or back camera */

	private Camera getCameraInstance(int cameraId) {
		Camera camera = null;
		try {
			camera = Camera.open(cameraId);
		} catch (Exception e) {}
		return camera;
	}
	
	/* Callback for onPictureTaken, save photo and show it on image preview. When user clicks on image preview, launch the gallery
	 * and show the image
	 */

	PictureCallback mPicture = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			final Uri uriTarget = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
			OutputStream imageFileOS;
			try {
				imageFileOS = getContentResolver().openOutputStream(uriTarget);
				imageFileOS.write(data);
				imageFileOS.flush();
				imageFileOS.close();
				Toast.makeText(getApplicationContext(), "Picture taken", Toast.LENGTH_SHORT).show();
			} catch (FileNotFoundException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}
			mCamera.startPreview();
			ImageButton imagePreview = (ImageButton)findViewById(R.id.imageButton1);
			//imagePreview.setImageURI(uriTarget);

			String[] projection = new String[]{
					MediaStore.Images.ImageColumns._ID,
					MediaStore.Images.ImageColumns.DATA,
					MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
					MediaStore.Images.ImageColumns.DATE_TAKEN,
					MediaStore.Images.ImageColumns.MIME_TYPE
			};
			final Cursor cursor = getContentResolver()
					.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, 
							null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

			if (cursor.moveToFirst()) {
				Bitmap bm = BitmapFactory.decodeFile(cursor.getString(1));
				imagePreview.setImageBitmap(bm);
			}

			imagePreview.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(uriTarget, "image/*");
					startActivity(intent);

				}
			});
		}
	};

	/* Fade out settings and image preview buttons */

	public void fadeOutOptions() {
		Animation fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setInterpolator(new AccelerateInterpolator());
		fadeOut.setStartOffset(1000);
		fadeOut.setDuration(1000);
		AnimationSet animation = new AnimationSet(false);
		animation.addAnimation(fadeOut);

		final ImageButton imagePreview = (ImageButton)findViewById(R.id.imageButton1);
		final ImageButton settingsButton = (ImageButton)findViewById(R.id.imageButton2);

		imagePreview.startAnimation(animation);
		settingsButton.startAnimation(animation);

		animation.setAnimationListener(new Animation.AnimationListener(){
			@Override
			public void onAnimationStart(Animation arg0) {
			}           
			@Override
			public void onAnimationRepeat(Animation arg0) {
			}           
			@Override
			public void onAnimationEnd(Animation arg0) {
				imagePreview.setVisibility(View.INVISIBLE);
				settingsButton.setVisibility(View.INVISIBLE);
				is_timer_running = false;
			}
		});
	}
	
	/* Fade in settings and image preview buttons */

	public void fadeInOptions() {
		Animation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new DecelerateInterpolator());
		fadeIn.setDuration(500);

		AnimationSet animation = new AnimationSet(false);
		animation.addAnimation(fadeIn);

		final ImageButton imagePreview = (ImageButton)findViewById(R.id.imageButton1);
		final ImageButton settingsButton = (ImageButton)findViewById(R.id.imageButton2);
		imagePreview.setVisibility(View.VISIBLE);
		settingsButton.setVisibility(View.VISIBLE);
		imagePreview.startAnimation(animation);
		settingsButton.startAnimation(animation);

		animation.setAnimationListener(new Animation.AnimationListener(){
			@Override
			public void onAnimationStart(Animation arg0) {
			}           
			@Override
			public void onAnimationRepeat(Animation arg0) {
			}           
			@Override
			public void onAnimationEnd(Animation arg0) {
				imagePreview.setVisibility(View.VISIBLE);
				settingsButton.setVisibility(View.VISIBLE);
				handler.postDelayed(runnable, 7000);
				is_timer_running = true;
			}
		});

	}


}
