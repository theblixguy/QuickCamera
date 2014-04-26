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

public class DebugPrefActivity extends Activity implements OnItemSelectedListener {
	
	/* Variables we require */
	
	private static final String TAG = "TouchlessCamera";
	boolean use_gyro = false;
	boolean use_proximity = false;
	int threshold = 9;
	
	/* Entry point of our activity. Nothing too fancy, just read user preferences and update the fields */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prefs);
		Spinner spinner1 = (Spinner)findViewById(R.id.spinner1);
		spinner1.setOnItemSelectedListener(this);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.spinner_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(adapter);

		EditText ed = (EditText) findViewById(R.id.editText1);
		Switch proximitySwitch = (Switch)  findViewById(R.id.switch1);

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		use_gyro = settings.getBoolean("use_gyro", false);
		use_proximity = settings.getBoolean("use_proximity", false);
		threshold = settings.getInt("threshold", 9);

		if (use_proximity == true) {
			proximitySwitch.setChecked(true);
		}
		else if (use_proximity == false){
			proximitySwitch.setChecked(false);
		}

		ed.setText(Integer.toString(threshold));

		if (use_gyro == false) {
			spinner1.setSelection(0);
		}
		else {
			spinner1.setSelection(1);
		}

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

	}
	
	/* Event handler for spinner */

	public void onItemSelected(AdapterView<?> parent, View view, 
			int pos, long id) {

		switch (pos) {
		case 0:
			use_gyro = false;
			Log.i(TAG, "Use gyroscope disabled");
			break;
		case 1:
			use_gyro = true;
			Log.i(TAG, "Use gyrsocope enabled");
			break;
		}
	}
	
	/* Event handler we don't need but still have to keep its code */

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}
	
	/* Function to save user preferences*/

	public void SaveSettings (View v) {
		EditText ed1 = (EditText) findViewById(R.id.editText1);
		threshold = Integer.parseInt(ed1.getText().toString());
		SharedPreferences app_settings = getSharedPreferences("app_prefs", 0);
		SharedPreferences.Editor settings_editor = app_settings.edit();
		settings_editor.putBoolean("usegyro", use_gyro);
		settings_editor.putBoolean("use_proximity", use_proximity);
		settings_editor.putInt("threshold", threshold);
		Log.i(TAG, "Saving debug prefs");
		settings_editor.commit();
	}


}

