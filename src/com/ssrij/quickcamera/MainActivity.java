package com.ssrij.quickcamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	/* Variables we require */
	
	private static final String TAG = "TouchlessCamera";
	int voltimes = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	/* Start the gesture listener service */

	public void StartGestureService(View v) {
		startService(new Intent(this, TouchlessGestureListener.class));
		Log.i(TAG, "Service started");
		Toast.makeText(getApplicationContext(), "Service started! You can now close this app", Toast.LENGTH_SHORT).show();
	}
	
	/* Stop the gesture listener service */

	public void StopGestureService(View v) {
		stopService(new Intent(this, TouchlessGestureListener.class));
		Log.i(TAG, "Service stopped");
		Toast.makeText(getApplicationContext(), "Service stopped! You can now close this app", Toast.LENGTH_SHORT).show();
	}
	
	/* Open the secret debugging options page if the user presses the volume down key thrice */
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){ 
			voltimes = voltimes + 1; 
		}

		if (voltimes == 3) { 
			startActivity(new Intent(this, DebugPrefActivity.class));
			Log.i(TAG, "Debug prefs accessed");
			Toast.makeText(getApplicationContext(), "For debugging only", Toast.LENGTH_SHORT).show(); 
			voltimes = 0;
		}
		return true;

	}


}