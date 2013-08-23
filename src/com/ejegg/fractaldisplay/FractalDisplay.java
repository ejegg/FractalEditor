package com.ejegg.fractaldisplay;

import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;
import android.app.Application;

public class FractalDisplay extends Application {
	
    private Camera camera;
    private MessagePasser messagePasser = new MessagePasser();
    private FractalStateManager stateManager;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	camera = new Camera(messagePasser);
    	stateManager = new FractalStateManager(messagePasser);
    }
    
	public Camera getCamera() {
		return camera;
	}
	
	public FractalStateManager getStateManager() {
		return stateManager;
	}
	
	public MessagePasser getMessagePasser() {
		return messagePasser;
	}
    
}
