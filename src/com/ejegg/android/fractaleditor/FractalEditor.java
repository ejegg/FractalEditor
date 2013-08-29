package com.ejegg.android.fractaleditor;

import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.spatial.Camera;

import android.app.Application;

public class FractalEditor extends Application {
	
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
