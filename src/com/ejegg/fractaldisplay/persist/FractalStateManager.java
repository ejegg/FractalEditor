package com.ejegg.fractaldisplay.persist;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


import android.net.Uri;

public class FractalStateManager {
	private int NumPoints = 200000;
    private FloatBuffer FractalPoints = null;
    private boolean editMode = false;
	private Stack<FractalState> undoStack = new Stack<FractalState>();
	private List<StateChangeSubscriber> undoSubscribers = new ArrayList<StateChangeSubscriber>();
    private FractalState State = new FractalState(4, "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 -0.5 -0.5 -0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.5 -0.5 -0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 -0.5 0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.5 0.0 1.0");
    
	public int getNumPoints() {
		return NumPoints;
	}
	
	public void setNumPoints(int numPoints) {
		NumPoints = numPoints;
	}
	
	public boolean hasPoints() {
		return FractalPoints != null;
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
	
	public boolean save(FractalState fs) {
		return false;
	}
	
	public void load(Uri savedFractalUri) {

	}
	
	public boolean getUndoEnabled() {
		return false;
	}
	
	public boolean getEditMode() {
		return editMode;
	}
	
	public void setEditMode(boolean isEditMode) {
		editMode = isEditMode;
	}
	
	public void undo() {
		
	}
	
	public interface StateChangeSubscriber {
		void updateState(FractalState newState, boolean undoEnabled);
	}
}
