package com.ssrij.quickcamera;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ssrij.quickcamera.TouchlessGestureListener.GestureDetectionTimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class TwistCalibrationActivity extends Activity {

	private static final String TAG = "TouchlessCamera";
	SensorManager sensorManager;
	Sensor rotationVectorSensor;
	Timer timer;
	TimerTask GestureTimerTask;
	SeekBar seekbar_twist_back_z;
	SeekBar seekbar_twist_back_y;
	SeekBar seekbar_twist_forward_y;
	Button try_button;
	Button save_button;
	boolean rotationVectorPresent;
	boolean is_sensor_registered = false;
	boolean was_up = false;
	boolean was_down = true;
	int up_how_many;
	int down_how_many;
	boolean is_timer_running = false;
	boolean proper_gesture = false;

	float twist_back_z = 0.6f;
	float twist_back_y = 0.2f;
	float twist_forward_y = 0.4f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calibrator);

		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		seekbar_twist_back_z =(SeekBar) findViewById(R.id.seekBar1);
		seekbar_twist_back_y = (SeekBar) findViewById(R.id.seekBar2);
		seekbar_twist_forward_y = (SeekBar) findViewById(R.id.seekBar3);

		try_button = (Button)findViewById(R.id.button1);
		save_button = (Button)findViewById(R.id.button2);

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		twist_back_z = settings.getFloat("twist_back_z", 0.6f);
		twist_back_y = settings.getFloat("twist_back_y", 0.2f);
		twist_forward_y = settings.getFloat("twist_forward_y", 0.4f);

		seekbar_twist_back_z.setProgress(Math.round(twist_back_z * 10));
		seekbar_twist_back_y.setProgress(Math.round(twist_back_y * 10));
		seekbar_twist_forward_y.setProgress(Math.round(twist_forward_y * 10));

	}


	public void executeCalibrationValues(View v) {

		int back_z = seekbar_twist_back_z.getProgress();
		int back_y = seekbar_twist_back_y.getProgress();
		int forward_y = seekbar_twist_forward_y.getProgress();

		twist_back_z = (float)back_z/10;
		twist_back_y = (float)back_y/10;
		twist_forward_y = (float)forward_y/10;
		
		AlertDialog.Builder DialogBld = new AlertDialog.Builder(this);
		DialogBld.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		DialogBld.setNegativeButton("Run", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				try_button.setEnabled(false);
				save_button.setEnabled(false);
				registerRotationVectorListener();
				dialog.cancel();
			}
		});

		DialogBld.setMessage("The calibration values you chose are:\n\nTwist Back (Z): " + twist_back_z + "\nTwist Back (Y): " + twist_back_y + "\nTwist Forward (Y): " + twist_forward_y + "\n\nSelect Run if you want to start with the calibration process. \nSelect Cancel if you want to cancel the calibration process");
		DialogBld.setTitle("Confirmation");
		DialogBld.show();
	}

	public void saveCalibrationValues(View v) {
		SharedPreferences app_settings = getSharedPreferences("app_prefs", 0);
		SharedPreferences.Editor settings_editor = app_settings.edit();
		settings_editor.putFloat("twist_back_z", twist_back_z);
		settings_editor.putFloat("twist_back_y", twist_back_y);
		settings_editor.putFloat("twist_forward_y", twist_forward_y);
		Log.i(TAG, "Saving calibration values");
		settings_editor.commit();
		Toast.makeText(getApplicationContext(), "Calibration values were saved!", Toast.LENGTH_SHORT).show();
		finish();
	}

	public void registerRotationVectorListener() {
		List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
		if(sensorList.size() > 0){
			rotationVectorPresent = true;
			Log.i(TAG, "Rotation vector detected");
			rotationVectorSensor = sensorList.get(0);  
		}
		else{
			Log.w(TAG, "No Rotation vector detected");
			rotationVectorPresent = false;
		}

		if(rotationVectorPresent){
			Log.i(TAG, "Registering rotation vector listener");
			sensorManager.registerListener(RotationVectorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);  
			is_sensor_registered = true;
		}
	}

	public void unregisterRotationVectorListener() {
		if (is_sensor_registered && rotationVectorPresent) {
			Log.i(TAG, "Unregistering rotation vector listener");
			try {
				sensorManager.unregisterListener(RotationVectorEventListener);
			} catch (Exception e) {
				Log.e(TAG, e.toString().toString());
			}
		}
	}

	SensorEventListener RotationVectorEventListener = new SensorEventListener(){

		@Override
		public void onSensorChanged(SensorEvent arg0) {
			Log.i(TAG, "Roatation vector detected, computing values");
			float y_value = arg0.values[1];
			float z_value = arg0.values[2];
			Log.i(TAG, "Y axis vector: " + y_value);

			if (y_value > twist_back_y && z_value < twist_back_z){
				if (was_up == false && was_down == true) {
					Log.i(TAG, "Turn up");
					up_how_many = up_how_many + 1;
					was_up = true;
					was_down = false;

					if (is_timer_running == false) {
						timer = new Timer();
						GestureTimerTask = new CalibrationGestureDetectionTimerTask();
						timer.schedule(GestureTimerTask, 1500);
						is_timer_running = true;
						proper_gesture = true;
					}

				}
			}
			else if (y_value > -twist_forward_y) {
				if (was_down == false && was_up == true) {
					Log.i(TAG, "Turn down");
					down_how_many = down_how_many + 1;
					was_down = true;
					was_up = false;

					if (is_timer_running == false) {
						timer = new Timer();
						GestureTimerTask = new CalibrationGestureDetectionTimerTask();
						timer.schedule(GestureTimerTask, 1500);
						is_timer_running = true;
						proper_gesture = true;
					}

				}
			}

			launchCamera();
		}

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// We don't require this interrupt

		}};

		/* Class that handles gesture timing */

		class CalibrationGestureDetectionTimerTask extends TimerTask {

			@Override
			public void run() {
				is_timer_running = false;
				proper_gesture = false;
				up_how_many = 0;
				down_how_many = 0;
			}

		}

		/* Function to reset the state of variables used to construct the gesture by our accelerometer interrupt.
		 * State is reset everytime the gesture is performed
		 */

		public void resetState() {
			Log.i(TAG, "Resetting state since gesture requirements were met");
			up_how_many = 0;
			down_how_many = 0;
			was_up = true;
			was_down = false;
		}

		public void launchCamera() {

			if (up_how_many == 2 && down_how_many == 2 && proper_gesture == true) {
				unregisterRotationVectorListener();
				resetState();
				try_button.setEnabled(true);
				save_button.setEnabled(true);

				AlertDialog.Builder DialogBld = new AlertDialog.Builder(this);
				DialogBld.setPositiveButton("Close", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});


				DialogBld.setMessage("Twist gesture was detected! If you are happy with this sensitivity then you can save the sensitivity now!");
				DialogBld.setTitle("Gesture detected");
				DialogBld.show();

			}
			else {
				Log.w(TAG, "Camera launch requirements not met");
			}

		}

}
