package com.pict.ever;

import android.app.Activity;
import android.os.Bundle;

import com.facebook.AppEventsLogger;

public class PicteverActivity extends Activity {
    protected PicteverApp mPicteverApp;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPicteverApp = (PicteverApp) this.getApplicationContext();
    }
    protected void onResume() {
        super.onResume();
        AppEventsLogger.activateApp(this);
        mPicteverApp.setCurrentActivity(this);
    }
    protected void onPause() {
        clearReferences();
        AppEventsLogger.deactivateApp(this);
        super.onPause();
    }
    protected void onDestroy() {        
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = mPicteverApp.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            mPicteverApp.setCurrentActivity(null);
    }
}
