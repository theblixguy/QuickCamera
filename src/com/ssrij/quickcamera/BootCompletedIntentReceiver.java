package com.ssrij.quickcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootCompletedIntentReceiver extends BroadcastReceiver {

	boolean start_service_bootup = false;
	
	/* Start the background service when the system boots up */

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

			SharedPreferences settings;
			settings = context.getSharedPreferences("app_prefs", 0);
			start_service_bootup = settings.getBoolean("start_service_bootup", false);

			if (start_service_bootup == true) {
				Intent serviceIntent = new Intent(context, TouchlessGestureListener.class);
				context.startService(serviceIntent);
			}
		}
	}
}
