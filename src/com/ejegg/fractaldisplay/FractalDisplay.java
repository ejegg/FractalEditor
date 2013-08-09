package com.ejegg.fractaldisplay;

import java.nio.FloatBuffer;

import android.app.Application;

public class FractalDisplay extends Application {
	
    private int NumPoints = 100000;
    private FloatBuffer FractalPoints = null;
    private FractalState State = new FractalState(4, "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 -0.5 -0.5 -0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.5 -0.5 -0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 -0.5 0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.5 0.0 1.0");
    private Camera Camera = new Camera();
    
	public int getNumPoints() {
		return NumPoints;
	}
	public void setNumPoints(int numPoints) {
		NumPoints = numPoints;
	}
	public FloatBuffer getFractalPoints() {
		return FractalPoints;
	}
	public void setFractalPoints(FloatBuffer fractalPoints) {
		FractalPoints = fractalPoints;
	}
	public FractalState getState() {
		return State;
	}
	public void setState(FractalState state) {
		State = state;
	}
	public Camera getCamera() {
		return Camera;
	}
	public void setCamera(Camera camera) {
		Camera = camera;
	}
    
}
