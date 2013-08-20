package com.ejegg.fractaldisplay.persist;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.ejegg.fractaldisplay.FractalCalculatorTask;
import com.ejegg.fractaldisplay.FractalCalculatorTask.ResultListener;
import com.ejegg.fractaldisplay.spatial.RayCubeIntersection;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.Matrix;
import android.util.Log;

public class FractalStateManager implements ResultListener {
	private int numPoints = 200000;
    private FloatBuffer fractalPoints = null;
    private FractalCalculatorTask calculator;
    private boolean editMode = false;
    private boolean recalculating = false;    
	private Stack<FractalState> undoStack = new Stack<FractalState>();
	private List<StateChangeSubscriber> changeSubscribers = new ArrayList<StateChangeSubscriber>();
	private List<ModeChangeSubscriber> modeSubscribers = new ArrayList<ModeChangeSubscriber>();
	private FractalCalculatorTask.ProgressListener calculationListener;
    private FractalState State = new FractalState(4, "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 -0.5 -0.5 -0.5 1.0 " +
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.5 -0.5 -0.5 1.0 " +
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 -0.5 0.5 1.0 " + 
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.5 0.0 1.0");
    
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
	
	public boolean save(FractalState fs) {
		return false;
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

	public interface ModeChangeSubscriber {
		void updateMode(boolean editMode);
	}
	
	public void loadStateFromUri(ContentResolver contentResolver, Uri savedFractalUri) {
		Cursor cursor = contentResolver.query(savedFractalUri, FractalStateProvider.Items.COLUMNS, null, null, null);
		cursor.moveToFirst();
		State = new FractalState(cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items.TRANSFORM_COUNT)),
				cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.SERIALIZED_TRANSFORMS)));
		cursor.close();
		fractalPoints = null;
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
		float[] invNear = new float[4];
		float[] invFar = new float[4];
		float minA = RayCubeIntersection.NO_INTERSECTION;
		float testA;
		int mindex = FractalState.NO_CUBE_SELECTED;
		
		Log.d("View", String.format("Near point is (%f,  %f, %f)", nearPoint[0], nearPoint[1], nearPoint[2]));
		Log.d("View", String.format("Far point is (%f,  %f, %f)", farPoint[0], farPoint[1], farPoint[2]));
				
		int transformCount = State.getNumTransforms();
		float[][] transforms = State.getTransforms();
		float[] inverse = new float[16];
		
		for (int i = 0; i< transformCount; i++) {
			
			Matrix.invertM(inverse, 0, transforms[i], 0);
			Matrix.multiplyMV(invNear, 0, inverse, 0, nearPoint, 0);
			Matrix.multiplyMV(invFar, 0, inverse, 0, farPoint, 0);
			
			RayCubeIntersection intersect = new RayCubeIntersection(invNear, invFar);  
			
			testA = intersect.getMinA();
			
			Log.d("View", "testA is " + testA);
			if (testA < minA) {
				minA = testA;
				mindex = i;
			}
		}
		
		State.setSelectedTransform(mindex);
	}
}
