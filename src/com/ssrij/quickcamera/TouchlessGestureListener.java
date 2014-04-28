package com.ssrij.quickcamera;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;

@SuppressLint("Wakelock") @SuppressWarnings("deprecation")
public class TouchlessGestureListener extends Service {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";
	WakeLock wakeLock;
	PowerManager mgr;
	SensorManager sensorManager;
	Sensor accelerometerSensor;
	Sensor linearAccelerometerSensor;
	Sensor proximitySensor;
	boolean accelerometerPresent;
	boolean linearAccelerometerPresent;
	boolean proximityPresent;
	boolean in_pocket = false;
	boolean was_up = true;
	boolean was_down = false;
	boolean use_linear_accelerometer;
	boolean use_proximity;
	int threshold;
	int up_how_many;
	int down_how_many;

	/* Send a value to the OS, indicating we want the service to be restarted if it gets killed 
	 * due to a thermonuclear explosion 
	 */

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Service start command received");
		return START_STICKY;
	}

	/* A random method appears! */

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/* Time to say goodbye! Unregister interrupts and exit */

	@Override
	public void onDestroy() {

		super.onDestroy();
		Log.i(TAG, "Service destroyted, unregistering listeners");

		if (accelerometerPresent && !use_linear_accelerometer) {
			Log.i(TAG, "Unregistering accelerometer listener");
			sensorManager.unregisterListener(AccelerometerEventListener);
		}

		if (use_proximity&& proximityPresent) {
			Log.i(TAG, "Unregistering proximity listener");
			sensorManager.unregisterListener(ProximityEventListener);
		}

		if (use_linear_accelerometer && linearAccelerometerPresent) {
			Log.i(TAG, "Unregistering linear accelerometer listener");
			sensorManager.unregisterListener(linearAccelerometerEventListener);
		}
	}

	/* Entry point of our Gesture service. Before we start detecting the gesture, there are some important
	 * things we have to do.
	 * 
	 * 1> Read user preferences
	 * 2> Verify sensor presence
	 * 3> Register sensor interrupts
	 * 4> Acquire service wakelock
	 * 
	 */

	@Override
	public void onCreate() {

		SharedPreferences settings;
		settings = getSharedPreferences("app_prefs", 0);
		use_proximity = settings.getBoolean("use_proximity", false);
		use_linear_accelerometer = settings.getBoolean("use_linear_accelerometer", false);
		threshold = settings.getInt("threshold", 5);

		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if(sensorList.size() > 0){
			Log.i(TAG, "Accelerometer sensor detected");
			accelerometerPresent = true;
			accelerometerSensor = sensorList.get(0);  
		}
		else{
			Log.e(TAG, "No accelerometer sensor detected, stopping service");
			accelerometerPresent = false;
			this.stopSelf();
		}

		List<Sensor> sensorList1 = sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);
		if(sensorList1.size() > 0){
			proximityPresent = true;
			Log.i(TAG, "Proximity sensor detected");
			proximitySensor = sensorList1.get(0);  
		}
		else{
			Log.w(TAG, "No proximity sensor detected");
			proximityPresent = false;
		}

		List<Sensor> sensorList2 = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		if(sensorList2.size() > 0){
			linearAccelerometerPresent = true;
			Log.i(TAG, "Linear accelerometer detected");
			linearAccelerometerSensor = sensorList2.get(0);  
		}
		else{
			Log.w(TAG, "No Linear accelerometer detected");
			linearAccelerometerPresent = false;
		} 

		if(accelerometerPresent){
			if (!use_linear_accelerometer) {
				Log.i(TAG, "Registering accelerometer listener");
				sensorManager.registerListener(AccelerometerEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);  
			}
		}

		if(linearAccelerometerPresent){
			if (use_linear_accelerometer) {
				Log.i(TAG, "Registering linear accelerometer listener");
				sensorManager.registerListener(linearAccelerometerEventListener, linearAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);  
			}
		}

		if (proximityPresent) {
			if (use_proximity) {
				Log.i(TAG, "Registering proximity listener");
				sensorManager.registerListener(ProximityEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
			}

		}

		Log.i(TAG, "Acquiring partial wakelock for background service");
		mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TouchlessCameraServiceWakeLock");
		wakeLock.acquire();

	}

	/* This function validates gesture requirements and launches camera if the requirements are met:
	 * 
	 * 1> Checks whether the phone is in the user's pocket or not, function fails if true.
	 * 2> Checks whether the gesture has been properly constructed or not, function fails if false.
	 * 
	 * It also turns the screen on, in case it's off. If all requirements are met then the camera app 
	 * is launched and the state of gesture variables are reset, else nothing happens.
	 * 
	 */

	public void launchCamera() {

		Log.i(TAG, "Validating camera launch requirements");

		if (in_pocket == false) {
			Log.i(TAG, "1: Phone not in pocket, moving forward");

			if (up_how_many == 2 && down_how_many == 2) {
				Log.i(TAG, "2: Gesture requirements met, moving forward");

				Log.i(TAG, "Veryfing if screen is already on or not");
				PowerManager pwrmgr = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
				boolean isScreenOn = pwrmgr.isScreenOn();

				if (isScreenOn == false) {
					turnOnScreen(this);	
				}

				Log.i(TAG, "All requirements met");

				try
				{
					Intent it = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
					it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					boolean is_camera_already_running = isCameraRunning();
					if (null != it && is_camera_already_running == false) {
						Log.i(TAG, "Starting camera app");
						vibratePhone(150);
						this.startActivity(it);
					}
				}

				catch (ActivityNotFoundException e)
				{
					// Do nothing
				}
				resetState();

			}
			else {
				Log.w(TAG, "Camera launch requirements not met (1/2)");
			}

		} else {

			Log.w(TAG, "Camera launch requirements not met (2/2)");
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

	/* Function to vibrate the phone */

	public void vibratePhone(int seconds) {
		Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(seconds);
	}

	/* 
	 * Function to check whether the camera is running or not in order to prevent stupid users
	 * from crashing or hanging the system by performing the gesture over and over again, after the
	 * camera app starts. We don't actually check if the app is running or not, we deduce this 
	 * information from the success or failure of the function, which merely tries to acquire a 
	 * camera object and fails if another app has already acquired it, which indicates a camera app 
	 * is already running in the foreground. The function might also fail if there is no rear camera or
	 * if there is no camera at all, but we don't check for that.
	 * 
	 */

	public boolean isCameraRunning() {
		Log.i(TAG, "Verifying if camera app is already running or not");
		Camera c = null;
		try {
			c = Camera.open();
		} catch (RuntimeException e) {
			Log.i(TAG, "Verification succeeded, camera app is already running");
			return true;
		} finally {
			if (c != null) c.release(); Log.i(TAG, "Releasing camera object");
		}
		Log.i(TAG, "Verification succeeded, camera app is not running");
		return false;
	}

	/* Function to check whether the user is in call or not, in order to prevent the camera from
	 * accidentally launching during a call.
	 */

	public boolean isUserInCall(){
		AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		if (manager.getMode()==AudioManager.MODE_IN_CALL){
			return true;
		}
		else{
			return false;
		}
	}

	/* 
	 * Function to turn on the display of the device. Simply launching camera when the phone is 
	 * turned off won't turn on the screen with it in most cases, so we have to manually do it. 
	 * Since there is no official method to turn on the display, we have to rely on wake locks. 
	 * We release this wakelock after 5 seconds, in order to reduce battery consumption. There is 
	 * also a possibility that the camera app is being launched from the lockscreen so we disable 
	 * keyguard as well, although disabling has no effect on protected devices.
	 * 
	 */

	public void turnOnScreen(Context context) {
		Log.i(TAG, "Acquiring wakelock to turn on the screen");
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, "TouchlessCameraWakeLock");
		wl.acquire(5000);

		Log.i(TAG, "Disabling keyguard");
		KeyguardManager keyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
		lock.disableKeyguard();
	}

	/* 
	 * Accelerometer is used to detect the launch gesture. Accelerometer is a bit tricky to use
	 * since the force of gravity has an effect on the values and in order to get the actual, aka
	 * linear acceleration data, we have to isolate the gravity from the values. A simple low-pass
	 * filter can help, so we use that. Once we have the optimized data, we check the linear acceleration 
	 * across the X axis in x-y-z space to determine whether the phone is facing the sky or the ground. 
	 * We use the values to construct our gesture. Usually, a value of > 5 indicates the phone is facing 
	 * towards the user and a value of > -5 indicates the phone is facing the opposite side. If the user 
	 * twists the phone twice, the linear acceleration goes from > 5 to > -5 four times. We record the values 
	 * and increment up_how_many everytime the acceleration hits > 5 and increment down_how_many everytime 
	 * acceleration hits > -5. We also do some conditional checks before incrementing to make sure the device 
	 * was previously facing the opposite side. launchCamera() is called everytime there is a change
	 * in acceleration and the camera is invoked only when both up_how_many and down_how_many are 
	 * equal to 2.
	 * 
	 * Some devices have both accelerometer & gyroscope, so we can use the linear accelerometer virtual sensor
	 * (which uses both sensors simultaneously to give us linear accelerometer data) so that we dont have to perform 
	 * certain calculations ourselfs to extract linear acceleration data from normal acceleration data.
	 * 
	 * A gyroscope is more suitable for this task, but consumes more battery while giving better accuracy. Gyro
	 * support is planned for a future version of this app and it's currently work-in-progress. It's quite 
	 * complicated to implement gyro support because it involves a lot of calculations, but support
	 * will eventually be added very soon.
	 * 
	 */

	SensorEventListener AccelerometerEventListener = new SensorEventListener(){

		@Override
		public void onSensorChanged(SensorEvent arg0) {
			Log.i(TAG, "Acceleration detected, computing values");
			float constant = 0.8f;
			float x_value = arg0.values[0];
			float linear_x_value;
			float gravity = 0;
			gravity = constant * gravity + (1 - constant) * arg0.values[0];
			linear_x_value = x_value - gravity;

			Log.i(TAG, "Acceleration: " + x_value);
			Log.i(TAG, "Gravity: " + gravity);
			Log.i(TAG, "Linear Acceleration: " + linear_x_value);

			if (linear_x_value > threshold){
				if (was_up == false && was_down == true) {
					Log.i(TAG, "Turn up");
					up_how_many = up_how_many + 1;
					was_up = true;
					was_down = false;
				}
			}
			else if (linear_x_value > -threshold) {
				if (was_down == false && was_up == true) {
					Log.i(TAG, "Turn down");
					down_how_many = down_how_many + 1;
					was_down = true;
					was_up = false;
				}
			}
			launchCamera();
		}

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// We don't require this interrupt

		}};

		SensorEventListener linearAccelerometerEventListener = new SensorEventListener(){

			@Override
			public void onSensorChanged(SensorEvent arg0) {
				Log.i(TAG, "Linear acceleration detected, computing values");
				float x_value = arg0.values[0];

				Log.i(TAG, "Linear Acceleration: " + x_value);

				if (x_value > threshold){
					if (was_up == false && was_down == true) {
						Log.i(TAG, "Turn up");
						up_how_many = up_how_many + 1;
						was_up = true;
						was_down = false;
					}
				}
				else if (x_value > -threshold) {
					if (was_down == false && was_up == true) {
						Log.i(TAG, "Turn down");
						down_how_many = down_how_many + 1;
						was_down = true;
						was_up = false;
					}
				}
				launchCamera();
			}

			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				// We don't require this interrupt

			}};

			/* 
			 * Proximity sensor is used by this service to prevent the camera from launching when the
			 * user keeps the phone in his/her pocket. We simply check whether the distance reported by 
			 * the sensor is equal to the sensor's maximum range to determine whether it's in the pocket
			 * or not. If the distance is equal then we assume it's not in the pocket and if it's not
			 * then it's in the pocket, easy. The variable in_pocket is used by launchCamera() to quickly 
			 * check whether the phone is in the user's pocket or not.
			 * 
			 */

			SensorEventListener ProximityEventListener = new SensorEventListener(){

				@Override
				public void onAccuracyChanged(Sensor arg0, int arg1) {
					// We don't require this interrupt

				}

				@Override
				public void onSensorChanged(SensorEvent arg0) {
					float distance = arg0.values[0];
					if (distance == arg0.sensor.getMaximumRange()) {
						Log.i(TAG, "Phone is outside pocket");
						in_pocket = false;
					}
					else {
						Log.i(TAG, "Phone is in pocket");
						in_pocket = true;
					}
				}};

}
