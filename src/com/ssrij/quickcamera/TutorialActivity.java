package com.ssrij.quickcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class TutorialActivity extends Activity {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";
	boolean first_run_proximity;
	WebView tutorial;

	/* Entry point of our tutorial activity. Nothing too fancy */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_run_tutorial);

		Log.i(TAG, "Tutorial opened");

		tutorial = (WebView)findViewById(R.id.webView1);
		tutorial.loadUrl("file:///android_asset/tutorial/tutorial.html");
		tutorial.setVerticalScrollBarEnabled(false);
		tutorial.setHorizontalScrollBarEnabled(false);

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		first_run_proximity = settings.getBoolean("first_run_proximity", true);
	}

	/* Remind user of proximity sensor and close tutorial */

	public void closeTutorial(View v) {
		Log.i(TAG, "Tutorial closed");
		SharedPreferences app_settings = getSharedPreferences("app_prefs", 0);
		SharedPreferences.Editor settings_editor = app_settings.edit();
		settings_editor.putBoolean("first_run", false);
		settings_editor.commit();
		
		AlertDialog.Builder DialogBld = new AlertDialog.Builder(this);
		DialogBld.setPositiveButton("Alright, got it!", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				tutorial.destroy();
				finish();
			}
		});

		DialogBld.setMessage("This app takes advantage of the proximity sensor to prevent accidental camera launches and turn off the accelerometer in order to minimize battery usage when the phone is in your pocket.\n\nBy default, this feature is turned off because it consumes little bit of your battery (very less according to my testing). You can manually turn it on in Settings if you want for better experience.");
		DialogBld.setTitle("Proximity Sensor");
		DialogBld.show();
		
	}
}

