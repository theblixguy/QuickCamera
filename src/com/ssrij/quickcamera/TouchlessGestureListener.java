package com.ssrij.quickcamera;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

@SuppressLint({ "Wakelock", "NewApi" }) @SuppressWarnings("deprecation")
public class TouchlessGestureListener extends Service {

	/* Variables we require */

	private static final String TAG = "TouchlessCamera";
	SharedPreferences settings;
	WakeLock wakeLock;
	PowerManager mgr;
	KeyguardManager kgMgr;
	SensorManager sensorManager;
	Sensor rotationVectorSensor;
	Sensor proximitySensor;
	String sleep_hr;
	String sleep_min;
	String wake_hr;
	String wake_min;
	String sleep_time;
	String wake_time;
	boolean rotationVectorPresent;
	boolean proximityPresent;
	boolean in_pocket = false;
	boolean was_up = false;
	boolean was_down = true;
	boolean use_proximity;
	boolean is_timer_running = false;
	boolean proper_gesture = false;
	boolean launch_from_lockscreen_only = false;
	boolean auto_sleep_wake = false;
	boolean time_to_sleep = false;
	boolean time_to_wake = false;
	boolean use_qc_camera = false;
	int vibration_intensity;
	int up_how_many;
	int down_how_many;
	float twist_back_z = 0.6f;
	float twist_back_y = 0.2f;
	float twist_forward_y = 0.4f;
	Timer timer;
	TimerTask GestureTimerTask;

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
		try {
			unregisterRotationVectorSensor();
			unregisterProximitySensor();
			unregisterReceiver(receiver);
			unregisterReceiver(OnOffGestureRecognition);
			unregisterReceiver(TimeChangeListener);
			wakeLock.release();
		} catch (Exception e) {}
	}

	/* Entry point of our Gesture service. Before we start detecting the gesture, there are some important
	 * things we have to do.
	 * 
	 * 1> Read user preferences
	 * 2> Verify sensor presence
	 * 3> Register sensor interrupts
	 * 4> Acquire service wakelock
	 * 5> Register broadcast recievers
	 * 6> Stop sensors if current system time is >= sleep time AND <= wake time
	 * 
	 */

	@Override
	public void onCreate() {

		settings = getSharedPreferences("app_prefs", 0);
		use_proximity = settings.getBoolean("use_proximity", false);
		launch_from_lockscreen_only = settings.getBoolean("launch_from_lockscreen_only", false);
		auto_sleep_wake = settings.getBoolean("auto_sleep_wake", false);
		vibration_intensity = settings.getInt("vibration_intensity", 150);
		twist_back_z = settings.getFloat("twist_back_z", 0.6f);
		twist_back_y = settings.getFloat("twist_back_y", 0.2f);
		twist_forward_y = settings.getFloat("twist_forward_y", 0.4f);
		sleep_hr = settings.getString("auto_sleep_hr", "9");
		sleep_min = settings.getString("auto_sleep_min", "01");
		wake_hr = settings.getString("auto_wake_hr", "9");
		wake_min = settings.getString("auto_wake_min", "01");
		use_qc_camera = settings.getBoolean("use_qc_camera", false);

		sleep_time = sleep_hr.concat(":").concat(sleep_min);
		wake_time = wake_hr.concat(":").concat(wake_min);

		Log.i(TAG, "Auto wake/sleep: " + auto_sleep_wake);
		Log.i(TAG, "Auto wake time: " + wake_time);
		Log.i(TAG, "Auto sleep time: " + sleep_time);

		Log.i(TAG, "Current gesture values -> Twist Back Z: " + twist_back_z + " Twist Back Y: " + twist_back_y + " Twist Forward Y: " + twist_forward_y);

		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		kgMgr =( KeyguardManager)this.getSystemService(Context.KEYGUARD_SERVICE);
		Log.i(TAG, "Acquiring partial wakelock for background service");
		mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TouchlessCameraServiceWakeLock");
		wakeLock.acquire();

		IntentFilter filter_phone_state = new IntentFilter();
		filter_phone_state.addAction("android.intent.action.PHONE_STATE");
		registerReceiver(receiver, filter_phone_state);

		IntentFilter filter_on_off = new IntentFilter();
		filter_on_off.addAction(Intent.ACTION_SCREEN_ON);
		filter_on_off.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(OnOffGestureRecognition, filter_on_off);

		IntentFilter filter_time_change = new IntentFilter();
		filter_time_change.addAction(Intent.ACTION_TIME_TICK);
		registerReceiver(TimeChangeListener, filter_time_change);

		boolean is_screen_on_now = mgr.isScreenOn();

		if (!launch_from_lockscreen_only && is_screen_on_now) {
			registerRotationVectorSensor();
		}

		registerProximitySensor();

		DateFormat current_hr = new SimpleDateFormat("HH");
		String current_hr_str = current_hr.format(Calendar.getInstance().getTime());
		DateFormat current_min = new SimpleDateFormat("mm");
		String current_min_str = current_min.format(Calendar.getInstance().getTime());

		int current_hr_int = Integer.parseInt(current_hr_str);
		int current_min_int = Integer.parseInt(current_min_str);
		int sleep_hr_int = Integer.parseInt(sleep_hr);
		int sleep_min_int = Integer.parseInt(sleep_min);
		int wake_hr_int = Integer.parseInt(wake_hr);
		int wake_min_int = Integer.parseInt(wake_min);

		if (current_hr_int >= sleep_hr_int && current_min_int >= sleep_min_int){
			if (current_hr_int <= wake_hr_int && current_min_int <= wake_min_int) {
				Log.i(TAG, "Service was restarted but it is sleep time right now, so stopping sensor listeners");
				unregisterRotationVectorSensor();
				unregisterProximitySensor();
				time_to_sleep = true;
				time_to_wake = false;
			}
		}
	}

	/* Function to register rotation vector listener. If no rotation vector is
	 * found then the service stops itself. 
	 */

	public void registerRotationVectorSensor() {

		List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
		if(sensorList.size() > 0){
			rotationVectorPresent = true;
			Log.i(TAG, "Rotation vector detected");
			rotationVectorSensor = sensorList.get(0);  
		}
		else{
			Log.w(TAG, "No Rotation vector detected");
			rotationVectorPresent = false;
			stopSelf();
		}

		if(rotationVectorPresent){
			Log.i(TAG, "Registering rotation vector listener");
			sensorManager.registerListener(RotationVectorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);  
		}
	}

	/* Function to register proximity sensor listener */

	public void registerProximitySensor() {

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

		if (proximityPresent) {
			if (use_proximity) {
				Log.i(TAG, "Registering proximity listener");
				sensorManager.registerListener(ProximityEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
	}

	/* Function to unregister rotation vector sensor listener */

	public void unregisterRotationVectorSensor() {

		if (rotationVectorPresent) {
			Log.i(TAG, "Unregistering rotation vector listener");
			try {
				sensorManager.unregisterListener(RotationVectorEventListener);
			} catch (Exception e) {
				Log.e(TAG, e.toString().toString());
			}
		}
	}

	/* Function to unregister proximity sensor listener */

	public void unregisterProximitySensor() {

		if (use_proximity && proximityPresent) {
			Log.i(TAG, "Unregistering proximity listener");
			try {
				sensorManager.unregisterListener(ProximityEventListener);
			} catch (Exception e) {
				Log.e(TAG, e.toString().toString());
			}
		}
	}

	/* This function validates gesture requirements and launches camera if the requirements are met:
	 * 
	 * 1> Checks whether the phone is in the user's pocket or not, function fails if true.
	 * 2> Checks whether the gesture has been properly constructed or not, function fails if false.
	 * 
	 * There are further checks too: 
	 * 1> Checks whether the user is in a call or not
	 * 2> Checks whether phone is locked or unlocked (incase it's locked it launches a secure version of the camera)
	 * 3> Checks whether QC camera app is enabled in settings or not
	 * 
	 * It also turns the screen on, in case it's off. If all requirements are met then the camera app 
	 * is launched and the state of gesture variables are reset, else nothing happens.
	 * 
	 */

	public void launchCamera() {

		long total_time;
		long start_time = System.currentTimeMillis();

		Log.i(TAG, "Validating camera launch requirements");

		if (in_pocket == false) {
			Log.i(TAG, "1: Phone not in pocket, moving forward");

			if (up_how_many == 2 && down_how_many == 2 && proper_gesture == true) {
				Log.i(TAG, "2: Gesture requirements met, moving forward. Veryfing if screen is already on or not");
				boolean isScreenOn = mgr.isScreenOn();

				if (isScreenOn == false) {
					turnOnScreen();	
				}

				Log.i(TAG, "All requirements met");

				try
				{
					Intent qcIntent = new Intent(this, QuickCameraPreview.class);
					Intent camera_intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
					Intent secure_camera_intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);

					qcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					camera_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					secure_camera_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

					boolean is_camera_already_running = isCameraRunning();
					boolean lockscreen = kgMgr.inKeyguardRestrictedInputMode();

					if (is_camera_already_running == false && lockscreen == false) {

						if (use_qc_camera == true) {
							this.startActivity(qcIntent);
						}
						else {
							this.startActivity(camera_intent);
						}

						Log.i(TAG, "Starting camera app");
						vibratePhone(vibration_intensity);
						total_time = System.currentTimeMillis() - start_time;
						Log.i(TAG, "Total time launchCamera() function (ms): " + total_time);
					}
					else if (is_camera_already_running == false && lockscreen == true) {
						Log.i(TAG, "Starting camera app in secure mode");
						vibratePhone(vibration_intensity);
						this.startActivity(secure_camera_intent);
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
				start_time = 0;
				total_time = 0;
			}

		} else {

			Log.w(TAG, "Camera launch requirements not met (2/2)");
			start_time = 0;
			total_time = 0;
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

	/* 
	 * Function to turn on the display of the device. Simply launching camera when the phone is 
	 * turned off won't turn on the screen with it in most cases, so we have to manually do it. 
	 * Since there is no official method to turn on the display, we have to rely on wake locks. 
	 * 
	 */

	public void turnOnScreen() {
		Log.i(TAG, "Acquiring wakelock to turn on the screen");
		WakeLock wl = mgr.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, "TouchlessCameraWakeLock");
		wl.acquire(5000);
	}

	/* 
	 * We use the rotation vector virtual sensor (which uses three sensors simultaneously to give us a 
	 * rotation vector) so that we dont have to perform certain calculations ourselves and get much higher accuracy.
	 * We also use a timer to prevent accidental gesture activations, so the gesture activates only if you perform the
	 * twists within 2 seconds, else it gets discarded
	 * 
	 */

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
						GestureTimerTask = new GestureDetectionTimerTask();
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
						GestureTimerTask = new GestureDetectionTimerTask();
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

		/* 
		 * Proximity sensor is used by this service to prevent the camera from launching when the
		 * user keeps the phone in his/her pocket and prevent battery drain due to accelerometer. 
		 * We simply check whether the distance reported by the sensor is equal to the sensor's maximum 
		 * range to determine whether it's in the pocket or not. If the distance is equal then we assume 
		 * it's not in the pocket and if it's not then it's in the pocket, easy. The variable in_pocket is 
		 * used by launchCamera() to quickly check whether the phone is in the user's pocket or not.
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
					registerRotationVectorSensor();
				}
				else {
					Log.i(TAG, "Phone is in pocket");
					in_pocket = true;
					unregisterRotationVectorSensor();
				}
			}};

			/* Broadcast receiver for auto sleep/wake */

			private final BroadcastReceiver TimeChangeListener = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					final String action = intent.getAction();

					if (action.equals(Intent.ACTION_TIME_TICK)) {

						if (auto_sleep_wake == true) {

							DateFormat df = new SimpleDateFormat("HH:mm");
							String time = df.format(Calendar.getInstance().getTime());

							sleep_time = sleep_hr.concat(":").concat(sleep_min);
							wake_time = wake_hr.concat(":").concat(wake_min);

							if (time.equals(sleep_time)) {
								Log.i(TAG, "Time to sleep, stopping sensor listeners");
								unregisterRotationVectorSensor();
								unregisterProximitySensor();
								time_to_sleep = true;
								time_to_wake = false;
							}

							if (time.equals(wake_time)) {
								Log.i(TAG, "Time to wake up, starting sensor listeners");
								registerRotationVectorSensor();
								registerProximitySensor();
								time_to_wake = true;
								time_to_sleep = false;
							}
						}
					}
				}
			};

			/* Class that handles gesture timing */

			class GestureDetectionTimerTask extends TimerTask {

				@Override
				public void run() {
					is_timer_running = false;
					proper_gesture = false;
					up_how_many = 0;
					down_how_many = 0;
				}

			}

			/* Broadcast receiver for screen on/off gesture */

			BroadcastReceiver OnOffGestureRecognition = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
						Log.i(TAG, "Screen on detected");
						if (launch_from_lockscreen_only) {
							if (!time_to_sleep) {
								unregisterRotationVectorSensor();
								Log.i(TAG, "Screen on detected: Unregistering sensors");
							}
						}
					}
					else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
						Log.i(TAG, "Screen off detected");
						if (launch_from_lockscreen_only) {
							if (!time_to_sleep) {
								registerRotationVectorSensor();
								Log.i(TAG, "Screen off detected: Registering sensors");
							}
						}
					}
				}
			};

			/* Broadcast receiver for handling pause/resume gesture depending on call state */

			private final BroadcastReceiver receiver = new BroadcastReceiver() {

				TelephonyManager telephony;
				GesturePhoneStateListener gesturePhoneListener;

				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					if(action.equals("android.intent.action.PHONE_STATE")){
						Log.i(TAG, "Registering call state listener");
						gesturePhoneListener = new GesturePhoneStateListener();
						telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
						telephony.listen(gesturePhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
					}   
				}

				/* Class that handles change in call state to pause/resume gesture recognition */

				class GesturePhoneStateListener extends PhoneStateListener {  

					int previous_call_state = 0; 

					@Override  
					public void onCallStateChanged(int state, String incomingNumber){     

						switch(state){  
						case TelephonyManager.CALL_STATE_RINGING:
							previous_call_state = state;
							if (!time_to_sleep) {
								Log.i(TAG, "Phone ringing, stopping sensors");
								unregisterRotationVectorSensor();
								unregisterProximitySensor();
							}
							break;  
						case TelephonyManager.CALL_STATE_OFFHOOK:  
							previous_call_state = state;
							if (!time_to_sleep) {
								Log.i(TAG, "Phone offhook, stopping sensors");
								unregisterRotationVectorSensor();
								unregisterProximitySensor();
							}
							break;  
						case TelephonyManager.CALL_STATE_IDLE:   
							if((previous_call_state == TelephonyManager.CALL_STATE_OFFHOOK)){  
								previous_call_state = state;
								if (!time_to_sleep) {
									Log.i(TAG, "Phone idle (offhook before), starting sensors");
									registerRotationVectorSensor();
									registerProximitySensor();
								}
							}  
							if((previous_call_state == TelephonyManager.CALL_STATE_RINGING)){  
								previous_call_state = state;
								if (!time_to_sleep) {
									Log.i(TAG, "Phone idle (ringing before), starting sensors");
									registerRotationVectorSensor();  
									registerProximitySensor();
								}
							}  
							break;  

						}  
					}  
				} 
			};
}
