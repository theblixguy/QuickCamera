package com.ssrij.quickcamera;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

public class DebugPrefActivity extends Activity {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";
	boolean start_service_bootup = false;
	boolean use_proximity = false;
	boolean launch_from_lockscreen_only = false;
	boolean auto_sleep_wake = false;
	boolean use_qc_camera;
	int vibration_intensity = 150;
	String sleep_hr;
	String sleep_min;
	String wake_hr;
	String wake_min;

	/* Entry point of our activity. Nothing too fancy, just read user preferences and update the fields */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prefs);

		EditText ed = (EditText) findViewById(R.id.editText1);
		Switch proximitySwitch = (Switch)findViewById(R.id.switch1);
		Switch bootupSwitch = (Switch)findViewById(R.id.switch2);
		Switch screenOffSwitch = (Switch)findViewById(R.id.switch3);
		Switch autoSleepWakeSwitch = (Switch)findViewById(R.id.switch4);
		Switch qcCameraSwitch = (Switch)findViewById(R.id.switch5);
		final Button wakeBtn = (Button)findViewById(R.id.button_set_time_wake);
		final Button sleepBtn = (Button)findViewById(R.id.button_set_time_sleep);

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		start_service_bootup = settings.getBoolean("start_service_bootup", false);
		launch_from_lockscreen_only = settings.getBoolean("launch_from_lockscreen_only", false);
		use_proximity = settings.getBoolean("use_proximity", false);
		auto_sleep_wake = settings.getBoolean("auto_sleep_wake", false);
		use_qc_camera = settings.getBoolean("use_qc_camera", false);
		vibration_intensity = settings.getInt("vibration_intensity", 150);
		sleep_hr = settings.getString("auto_sleep_hr", "9");
		sleep_min = settings.getString("auto_sleep_min", "01");
		wake_hr = settings.getString("auto_wake_hr", "9");
		wake_min = settings.getString("auto_wake_min", "01");

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

		if (auto_sleep_wake == true) {
			autoSleepWakeSwitch.setChecked(true);
			wakeBtn.setVisibility(View.VISIBLE);
			sleepBtn.setVisibility(View.VISIBLE);
		}
		else if (auto_sleep_wake == false){
			autoSleepWakeSwitch.setChecked(false);
			wakeBtn.setVisibility(View.GONE);
			sleepBtn.setVisibility(View.GONE);
		}
		
		if (use_qc_camera == true) {
			qcCameraSwitch.setChecked(true);
		}
		else if (use_qc_camera == false){
			qcCameraSwitch.setChecked(false);
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

		autoSleepWakeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked == true) {
					Log.i(TAG, "Auto sleep/wake enabled");
					auto_sleep_wake = true;
					wakeBtn.setVisibility(View.VISIBLE);
					sleepBtn.setVisibility(View.VISIBLE);
				}
				else if (isChecked == false) {
					Log.i(TAG, "Auto sleep/wake disabled");
					auto_sleep_wake = false;
					wakeBtn.setVisibility(View.GONE);
					sleepBtn.setVisibility(View.GONE);
				}
			}
		});
		
		qcCameraSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked == true) {
					Log.i(TAG, "Use QC camera enabled");
					use_qc_camera = true;
				}
				else if (isChecked == false) {
					Log.i(TAG, "Use QC camera disabled");
					use_qc_camera = false;
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
		settings_editor.putBoolean("auto_sleep_wake", auto_sleep_wake);
		settings_editor.putBoolean("use_qc_camera", use_qc_camera);
		settings_editor.putInt("vibration_intensity", vibration_intensity);
		settings_editor.putString("auto_wake_hr", wake_hr);
		settings_editor.putString("auto_wake_min", wake_min);
		settings_editor.putString("auto_sleep_hr", sleep_hr);
		settings_editor.putString("auto_sleep_min", sleep_min);
		Log.i(TAG, "Saving debug prefs");
		settings_editor.commit();
		Toast.makeText(getApplicationContext(), "Your preferences were saved!", Toast.LENGTH_SHORT).show();
	}
	
	/* Function to set auto wakeup time */

	public void setWakeTime(View v) {
		TimePickerDialog mTimePicker;
		mTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
				wake_hr = Integer.toString(selectedHour);
				wake_min = Integer.toString(selectedMinute);
			}
		}, Integer.parseInt(wake_hr), Integer.parseInt(wake_min), true);
		mTimePicker.setTitle("Select wake up time (AM)");
		mTimePicker.show();
	}
	
	/* Function to set auto sleep time */

	public void setSleepTime(View v) {
		TimePickerDialog mTimePicker1;
		mTimePicker1 = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
				sleep_hr = Integer.toString(selectedHour);
				sleep_min = Integer.toString(selectedMinute);
			}
		}, Integer.parseInt(sleep_hr), Integer.parseInt(sleep_min), true);
		mTimePicker1.setTitle("Select sleep time (PM)");
		mTimePicker1.show();
	}


}

