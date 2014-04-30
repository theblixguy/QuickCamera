package com.ssrij.quickcamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";
	int voltimes = 0;
	boolean first_run;

	/* Entry point of our activity */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		first_run = settings.getBoolean("first_run", true);

		if (first_run) {
			startActivity(new Intent(this, TutorialActivity.class));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:

			moveTaskToBack(true);

			return true;
		}
		return false;
	}

	/* You know what this does */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	/* Open the debug settings */

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.action_settings:
			startActivity(new Intent(this, DebugPrefActivity.class));
			Log.i(TAG, "Debug prefs accessed");
			Toast.makeText(getApplicationContext(), "For debugging only", Toast.LENGTH_SHORT).show(); 
			return true;

		case R.id.action_calibration:
			boolean is_service_running = isServiceAlreadyRunning();
			if (!is_service_running) {
				startActivity(new Intent(this, TwistCalibrationActivity.class));
				Log.i(TAG, "Calibration accessed");
			} else {
				Toast.makeText(getApplicationContext(), "Service is currently running. Please stop it first", Toast.LENGTH_SHORT).show();
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}

	}

	/* Checks if service is already running */

	private boolean isServiceAlreadyRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (TouchlessGestureListener.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/* Start the gesture listener service */

	public void StartGestureService(View v) {
		boolean is_service_running = isServiceAlreadyRunning();
		if (!is_service_running) {
			startService(new Intent(this, TouchlessGestureListener.class));
			Log.i(TAG, "Service started");
			Toast.makeText(getApplicationContext(), "Service started! You can now close this app", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "Service is already running.", Toast.LENGTH_SHORT).show();
		}
	}

	/* Stop the gesture listener service */

	public void StopGestureService(View v) {
		stopService(new Intent(this, TouchlessGestureListener.class));
		Log.i(TAG, "Service stopped");
		Toast.makeText(getApplicationContext(), "Service stopped! You can now close this app", Toast.LENGTH_SHORT).show();
	}


}