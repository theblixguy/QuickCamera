package com.ssrij.quickcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver {

	/* Start the background service when the system boots up */

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			Intent serviceIntent = new Intent(context, TouchlessGestureListener.class);
			context.startService(serviceIntent);
		}
	}
}
