package com.pict.ever;

import android.app.Activity;
import android.app.Application;

public class PicteverApp extends Application {
	
	Controller controller;
	private Activity mCurrentActivity = null;
	
	@Override
	public void onCreate(){
		super.onCreate();
		controller = new Controller(getApplicationContext());
	}
	
    public Activity getCurrentActivity(){
          return mCurrentActivity;
    }
    public void setCurrentActivity(Activity mCurrentActivity){
          this.mCurrentActivity = mCurrentActivity;
    }
	
	public Controller getController() {
		return controller;
	}
}
