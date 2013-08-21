package com.ejegg.fractaldisplay.persist;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.ejegg.fractaldisplay.FractalCalculatorTask;
import com.ejegg.fractaldisplay.FractalCalculatorTask.ResultListener;
import com.ejegg.fractaldisplay.spatial.RayCubeIntersection;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class FractalStateManager implements ResultListener {
	private int numPoints = 200000;
    private FloatBuffer fractalPoints = null;
    private FractalCalculatorTask calculator;
    private boolean editMode = false;
    private boolean recalculating = false;    
	private Stack<FractalState> undoStack = new Stack<FractalState>();
	private boolean uniformScaleMode = true;
	
	private List<ModeChangeListener> modeChangeListeners = new ArrayList<ModeChangeListener>();
	private FractalCalculatorTask.ProgressListener calculationListener;
	private FractalState lastState = null;
    private FractalState State = new FractalState(4, "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 -0.5 -0.5 -0.5 1.0 " +
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.5 -0.5 -0.5 1.0 " +
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 -0.5 0.5 1.0 " + 
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.5 0.0 1.0");

	public interface ModeChangeListener {
		void updateMode();
	}
    
	public int getNumPoints() {
		return numPoints;
	}
	
	public void setNumPoints(int numPoints) {
		this.numPoints = numPoints;
	}
	
	public boolean hasPoints() {
		return fractalPoints != null;
	}
	
	public FloatBuffer getFractalPoints() {
		return fractalPoints;
	}
	
	public FractalState getState() {
		return State;
	}
	
	public boolean isUndoEnabled() {
		return !undoStack.empty();
	}
	
	public boolean isEditMode() {
		return editMode;
	}
	
	public void toggleEditMode() {
		editMode = !editMode;
		notifyModeChangeListeners();
	}
	
	public boolean isUniformScaleMode() {
		return uniformScaleMode;
	}
	
	public void toggleScaleMode() {
		uniformScaleMode = !uniformScaleMode;
		notifyModeChangeListeners();
	}
	
	public void undo() {
		if (!undoStack.empty()) {
			State = undoStack.pop();
		}
		if (undoStack.empty()) {
			notifyModeChangeListeners();
		}
	}

	private void notifyModeChangeListeners() {
		for (ModeChangeListener sub : modeChangeListeners) {
			if (sub != null) {
				sub.updateMode();
			}
		}
	}

	public void addModeChangeListener(ModeChangeListener sub) {
		modeChangeListeners.add(sub);
	}

	public void clearModeChangeListeners() {
		modeChangeListeners.clear();
	}
	
	public void loadStateFromUri(ContentResolver contentResolver, Uri savedFractalUri) {
		Cursor cursor = contentResolver.query(savedFractalUri, FractalStateProvider.Items.COLUMNS, null, null, null);
		cursor.moveToFirst();
		State = new FractalState(cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items.TRANSFORM_COUNT)),
				cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.SERIALIZED_TRANSFORMS)));
		cursor.close();
		undoStack.clear();
		fractalPoints = null;
	}

	public boolean save(ContentResolver contentResolver, String saveName) {
		try {
			ContentValues val = new ContentValues();
			val.put(FractalStateProvider.Items.NAME, saveName);
			val.put(FractalStateProvider.Items.TRANSFORM_COUNT, State.getNumTransforms());
			val.put(FractalStateProvider.Items.SERIALIZED_TRANSFORMS, State.getSerializedTransforms());
			val.put(FractalStateProvider.Items.LAST_UPDATED, System.currentTimeMillis());
			contentResolver.insert(FractalStateProvider.CONTENT_URI, val);
		}
		catch (Exception e) {
			Log.d("fview", "Error saving: " + e.getMessage() + e.getStackTrace());
			return false;
		}
		return true;
	}

	public void setCalculationListener(FractalCalculatorTask.ProgressListener calculationListener) {
		this.calculationListener = calculationListener;
		if (calculator != null) {
			calculator.setProgressListener(calculationListener);
		}
	}
	
	public void recalculatePoints(boolean showProgress) {
		recalculating = true;		
		FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(State, numPoints);
		calculator = new FractalCalculatorTask(showProgress ? calculationListener : null, this);
		calculator.execute(request);
	}

	@Override
	public void finished(FloatBuffer points) {
		this.fractalPoints = points;
		recalculating = false;		
	}
	
	public boolean isRecalculating() {
		return recalculating;
	}
	
	public void select(float[] nearPoint, float[] farPoint) {
		float minA = RayCubeIntersection.NO_INTERSECTION;
		float testA;
		int mindex = FractalState.NO_CUBE_SELECTED;
		
		Log.d("View", String.format("Near point is (%f,  %f, %f)", nearPoint[0], nearPoint[1], nearPoint[2]));
		Log.d("View", String.format("Far point is (%f,  %f, %f)", farPoint[0], farPoint[1], farPoint[2]));
				
		int transformCount = State.getNumTransforms();
		float[][] transforms = State.getTransforms();
		
		for (int i = 0; i< transformCount; i++) {
			testA = new RayCubeIntersection(nearPoint, farPoint, transforms[i]).getMinA();
			
			Log.d("View", "testA is " + testA);
			if (testA < minA) {
				minA = testA;
				mindex = i;
			}
		}
		
		State.setSelectedTransform(mindex);
	}
	
	public void startManipulation() {
		Log.d("FractalStateManager", "starting manipulation");
		lastState = State.clone();
	}

	public void finishManipulation() {
		boolean needsModeNotification = false;
		if (lastState != null && !State.equals(lastState)) {
			if (undoStack.empty()) {
				needsModeNotification = true;
			}
			undoStack.push(lastState);
			fractalPoints = null;
		}
		if (needsModeNotification) {
			notifyModeChangeListeners();
		}
	}
	
	public void addTransform() {
		startManipulation();
		State.addTransform();
		finishManipulation();
	}

	public void removeSelectedTransform() {
		startManipulation();
		State.removeSelectedTransform();
		finishManipulation();
	}
	
	public void rotateSelectedTransform(float angle, float[] axis) {
		State.rotateSelectedTransform(angle, axis);
	}

	public void scaleSelectedTransform(float scaleFactor, float[] focus,
			float[] endPoint) {
		if (uniformScaleMode) {
			State.scaleUniform(scaleFactor);
		} else {
			State.scaleLinear(scaleFactor, focus, endPoint);
		}
	}

	public void moveSelectedTransform(float[] oldNear, float[] oldFar,
			float[] newNear, float[] newFar) {
		State.moveSelectedTransform(oldNear, oldFar, newNear, newFar);
	}
}
