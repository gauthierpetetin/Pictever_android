package com.pict.ever;


import android.annotation.SuppressLint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnSwipeTouchListener implements OnTouchListener {
	
	private static final String LOG_TAG = "OnSwipeTouchListener";
	
	@SuppressWarnings("deprecation")
	private final GestureDetector gestureDetector = new GestureDetector(new GestureListener());
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(final View v, final MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}
	
	private final class GestureListener extends SimpleOnGestureListener {
		
		private static final int SWIPE_THRESHOLD = 100;
		
		private static final int SWIPE_VELOCITY_THRESHOLD = 100;
		
		@Override
		public void onLongPress(MotionEvent motionEvent) {
			super.onLongPress(motionEvent);
			onLongPressDetected(motionEvent);
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// by default, don't consume the event
			boolean consumeEvent = false;
			try {
				if (e1 != null && e2 != null) {
					float diffY = e2.getY() - e1.getY();
					float diffX = e2.getX() - e1.getX();
					if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD
							&& Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
						if (diffX > 0) {
							onSwipeRight();
						} else {
							onSwipeLeft();
						}
						consumeEvent = true;
					}
				}
			} catch (Exception exception) {
				Log.d(LOG_TAG, "onFling", exception);
			}
			return consumeEvent;
		}
	}
	
	public void onSwipeRight() {
	}
	
	public void onSwipeLeft() {
	}
	
	public void onLongPressDetected(MotionEvent motionEvent) {
	}
	
	public void setIsLongpressEnabled(boolean enabled) {
		gestureDetector.setIsLongpressEnabled(enabled);
	}
}