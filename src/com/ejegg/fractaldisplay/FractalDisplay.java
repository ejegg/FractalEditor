package com.ejegg.fractaldisplay;

import com.ejegg.fractaldisplay.spatial.Camera;
import android.app.Application;

public class FractalDisplay extends Application {
	
    private Camera Camera = new Camera();
    private FractalStateManager stateManager = new FractalStateManager();
    
	public Camera getCamera() {
		return Camera;
	}
	
	public FractalStateManager getStateManager() {
		return stateManager;
	}
    
}
