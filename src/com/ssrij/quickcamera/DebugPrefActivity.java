package com.ssrij.quickcamera;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class DebugPrefActivity extends Activity {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";
	boolean start_service_bootup = false;
	boolean use_proximity = false;
	boolean launch_from_lockscreen_only = false;
	int vibration_intensity = 150;

	/* Entry point of our activity. Nothing too fancy, just read user preferences and update the fields */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prefs);

		EditText ed = (EditText) findViewById(R.id.editText1);
		Switch proximitySwitch = (Switch)findViewById(R.id.switch1);
		Switch bootupSwitch = (Switch)findViewById(R.id.switch2);
		Switch screenOffSwitch = (Switch)findViewById(R.id.switch3);

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		start_service_bootup = settings.getBoolean("start_service_bootup", false);
		launch_from_lockscreen_only = settings.getBoolean("launch_from_lockscreen_only", false);
		use_proximity = settings.getBoolean("use_proximity", false);
		vibration_intensity = settings.getInt("vibration_intensity", 150);

		if (use_proximity == true) {
			proximitySwitch.setChecked(true);
		}
		else if (use_proximity == false){
			proximitySwitch.setChecked(false);
		}
		
		if (start_service_bootup == true) {
			bootupSwitch.setChecked(true);
		}
		else if (start_service_bootup == false){
			bootupSwitch.setChecked(false);
		}
		
		if (launch_from_lockscreen_only == true) {
			screenOffSwitch.setChecked(true);
		}
		else if (launch_from_lockscreen_only == false){
			screenOffSwitch.setChecked(false);
		}

		ed.setText(Integer.toString(vibration_intensity));

		proximitySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked == true) {
					Log.i(TAG, "Use proximity enabled");
					use_proximity = true;
				}
				else if (isChecked == false) {
					Log.i(TAG, "Use proximity disabled");
					use_proximity = false;
				}
			}
		});
		
		bootupSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked == true) {
					Log.i(TAG, "Start service on bootup enabled");
					start_service_bootup = true;
				}
				else if (isChecked == false) {
					Log.i(TAG, "Start service on bootup disabled");
					start_service_bootup = false;
				}
			}
		});
		
		screenOffSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked == true) {
					Log.i(TAG, "Screen off only enabled");
					launch_from_lockscreen_only = true;
				}
				else if (isChecked == false) {
					Log.i(TAG, "Screen off only disabled");
					launch_from_lockscreen_only = false;
				}
			}
		});
	}


	/* Function to save user preferences*/

	public void SaveSettings (View v) {
		EditText ed1 = (EditText) findViewById(R.id.editText1);
		vibration_intensity = Integer.parseInt(ed1.getText().toString());
		SharedPreferences app_settings = getSharedPreferences("app_prefs", 0);
		SharedPreferences.Editor settings_editor = app_settings.edit();
		settings_editor.putBoolean("start_service_bootup", start_service_bootup);
		settings_editor.putBoolean("use_proximity", use_proximity);
		settings_editor.putBoolean("launch_from_lockscreen_only", launch_from_lockscreen_only);
		settings_editor.putInt("vibration_intensity", vibration_intensity);
		Log.i(TAG, "Saving debug prefs");
		settings_editor.commit();
		Toast.makeText(getApplicationContext(), "Your preferences were saved!", Toast.LENGTH_SHORT).show();
	}


}

