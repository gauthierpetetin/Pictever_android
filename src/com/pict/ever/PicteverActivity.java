package com.pict.ever;

import android.app.Activity;
import android.os.Bundle;

public class PicteverActivity extends Activity {
    protected PicteverApp mPicteverApp;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPicteverApp = (PicteverApp) this.getApplicationContext();
    }
    protected void onResume() {
        super.onResume();
        mPicteverApp.setCurrentActivity(this);
    }
    protected void onPause() {
        clearReferences();
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
