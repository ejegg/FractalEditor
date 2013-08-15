package com.ejegg.fractaldisplay.touch;

public interface MotionEventSubscriber {
	void tap(float screenX, float screenY);
	void longPress(float screenX, float screenY);
	void drag(float oldScreenX, float oldScreenY, float newScreenX, float newScreenY);
	void scale(float scaleFactor, float spanX, float spanY);
	void rotate(float angle, float focusScreenX, float focusScreenY);
	void fling(float oldScreenX, float oldScreenY, float newScreenX, float newScreenY, float velocityX, float velocityY);
	void down();
	void up();
}
