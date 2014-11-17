package com.pict.ever;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;


public class GcmIntentService extends IntentService {
	static final String TAG = "GcmIntentService";
	Controller controller;

	public GcmIntentService() {
		super(TAG);
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		if (!extras.isEmpty()) { 
			controller = ((PicteverApp) getApplication()).getController();
			controller.current_activity = getClass().getSimpleName();
			controller.receive_all();
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
}