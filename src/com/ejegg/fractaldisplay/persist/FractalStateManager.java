package com.ejegg.fractaldisplay.persist;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.ejegg.fractaldisplay.FractalCalculatorTask;
import com.ejegg.fractaldisplay.FractalCalculatorTask.ResultListener;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class FractalStateManager implements ResultListener {
	private int numPoints = 200000;
    private FloatBuffer fractalPoints = null;
    private FractalCalculatorTask calculator;
    private boolean editMode = false;
    private boolean recalculating = false;    
	private Stack<FractalState> undoStack = new Stack<FractalState>();
	private List<StateChangeSubscriber> changeSubscribers = new ArrayList<StateChangeSubscriber>();
	private FractalCalculatorTask.ProgressListener calculationListener;
    private FractalState State = new FractalState(4, "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 -0.5 -0.5 -0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.5 -0.5 -0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 -0.5 0.5 1.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.5 0.0 1.0");
    
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
	
	public void recalculatePoints() {
		recalculating = true;		
		FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(State, numPoints);
		calculator = new FractalCalculatorTask(calculationListener, this);
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
}
