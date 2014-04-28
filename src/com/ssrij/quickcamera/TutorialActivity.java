package com.ssrij.quickcamera;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class TutorialActivity extends Activity {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";

	/* Entry point of our tutorial activity. Nothing too fancy */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_run_tutorial);
		
		Log.i(TAG, "Tutorial opened");
		
		WebView tutorial = (WebView)findViewById(R.id.webView1);
		tutorial.loadUrl("file:///android_asset/tutorial/tutorial.html");
		tutorial.setVerticalScrollBarEnabled(false);
		tutorial.setHorizontalScrollBarEnabled(false);
	}
	
	/* Close tutorial */
	
	public void closeTutorial(View v) {
		Log.i(TAG, "Tutorial closed");
		SharedPreferences app_settings = getSharedPreferences("app_prefs", 0);
		SharedPreferences.Editor settings_editor = app_settings.edit();
		settings_editor.putBoolean("first_run", false);
		settings_editor.commit();
		finish();
	}
}

