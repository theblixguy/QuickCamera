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
	boolean use_rotation_vector;
	boolean use_proximity = false;
	int vibration_intensity = 150;

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
		use_rotation_vector = settings.getBoolean("use_rotation_vector", true);
		use_proximity = settings.getBoolean("use_proximity", false);
		vibration_intensity = settings.getInt("vibration_intensity", 150);

		if (use_proximity == true) {
			proximitySwitch.setChecked(true);
		}
		else if (use_proximity == false){
			proximitySwitch.setChecked(false);
		}

		ed.setText(Integer.toString(vibration_intensity));

		if (use_rotation_vector == false) {
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
			use_rotation_vector = false;
			Log.i(TAG, "Use rotation vector disabled");
			break;
		case 1:
			use_rotation_vector = true;
			Log.i(TAG, "Use rotation vector enabled");
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
		vibration_intensity = Integer.parseInt(ed1.getText().toString());
		SharedPreferences app_settings = getSharedPreferences("app_prefs", 0);
		SharedPreferences.Editor settings_editor = app_settings.edit();
		settings_editor.putBoolean("use_rotation_vector", use_rotation_vector);
		settings_editor.putBoolean("use_proximity", use_proximity);
		settings_editor.putInt("vibration_intensity", vibration_intensity);
		Log.i(TAG, "Saving debug prefs");
		settings_editor.commit();
	}


}

