package com.ejegg.android.fractaleditor.persist;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Stack;

import com.ejegg.android.fractaleditor.FractalCalculatorTask;
import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.FractalCalculatorTask.ResultListener;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.ProgressListener;
import com.ejegg.android.fractaleditor.render.GlRenderer;
import com.ejegg.android.fractaleditor.spatial.RayCubeIntersection;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class FractalStateManager implements ResultListener {
	private final static String LOG_TAG = "FractalStateManager";

	// Number of points to calculate per batch
	public final static int BATCH_SIZE = 30000;

	// how many floats that is
	public final static int BATCH_FLOATS = BATCH_SIZE * GlRenderer.COORDS_PER_VERTEX;

	// Maximum number of points to hold on to
	public static final int MAX_POINTS = BATCH_SIZE * 10;

	// Number of batches to calculate
	private static final int MAX_CALCULATION_REPEAT = 36;

	private PointSet fractalPoints;
    private FractalCalculatorTask calculator;
    private boolean editMode = false;
    private boolean recalculating = false;
    private boolean continuousCalculation = false;
    private int calculationRepeatCount = 0;
	private final Stack<FractalState> undoStack = new Stack<>();
	private boolean uniformScaleMode = true;

	
	private final MessagePasser messagePasser;
	private ProgressListener calculationListener;
	private FractalState lastState = null;
    private FractalState State = new FractalState();

	private float[] boundingBox;

	public FractalStateManager(MessagePasser messagePasser) {
		this.messagePasser = messagePasser;
		resetPoints();
	}

	public boolean hasPoints() {
		return fractalPoints.getNumPoints() > 0;
	}
	
	public PointSet getFractalPoints() {
		return fractalPoints;
	}

	public void resetPoints() {
		fractalPoints = new PointSet(null, 0);
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
		State.clearSelection();
		sendMessage(MessageType.EDIT_MODE_CHANGED, editMode);
		if (editMode) {
			cancelCalculation();
		}
	}
	
	public boolean isUniformScaleMode() {
		return uniformScaleMode;
	}
	
	public void toggleScaleMode() {
		uniformScaleMode = !uniformScaleMode;
		sendMessage(MessageType.SCALE_MODE_CHANGED, uniformScaleMode);
	}
	
	public void undo() {
		if (!undoStack.empty()) {
			State = undoStack.pop();
			stateChanged();
		}
		if (undoStack.empty()) {
			sendMessage(MessageType.UNDO_ENABLED_CHANGED, false);
		}
	}

	public void loadStateFromUri(ContentResolver contentResolver, Uri savedFractalUri) {
		Boolean oldEditMode = editMode;
		if (savedFractalUri == null) {
			State = new FractalState();
			// For a new fractal, start in edit mode
			editMode = true;
		} else {
			final Cursor cursor = contentResolver.query(savedFractalUri, FractalStateProvider.Items.ALL_COLUMNS, null, null, null);
			// TODO: handle trying to load non-existent fractal id
			cursor.moveToFirst();
			State = new FractalState(
					cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items._ID)),
					cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items.SHARED_ID)),
					cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.NAME)),
					cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.THUMBNAIL)),
					cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.SERIALIZED_TRANSFORMS))
			);
			cursor.close();
			// For a saved fractal, start in render mode
			editMode = false;
		}

		undoStack.clear();
		if (oldEditMode != editMode) {
			sendMessage(MessageType.EDIT_MODE_CHANGED, editMode);
		}
		sendMessage(MessageType.UNDO_ENABLED_CHANGED, false);
		stateChanged();
	}

	public void setCalculationListener(ProgressListener calculationListener) {
		calculationRepeatCount = 0; //reset redraw count on orientation change 
		this.calculationListener = calculationListener;
		if (calculator != null && !continuousCalculation) {
			calculator.setProgressListener(calculationListener);
		}
	}
	
	public void recalculatePoints() {
		if (recalculating) return;
		
		recalculating = true;
		//Log.d(LOG_TAG, String.format("Have %d points, requesting %d new ones", getPointsRendered(), BATCH_SIZE));
		FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(State, BATCH_SIZE);
		calculator = new FractalCalculatorTask(fractalPoints.getNumPoints() == 0 ? calculationListener : null, this);
		calculator.execute(request);
	}

	@Override
	public void finished(float[] points, float[] boundingBox) {
		this.boundingBox = boundingBox;
		boolean accumulatedPoints = (fractalPoints != null);

		this.fractalPoints = createNewPointSet(points);
		recalculating = false;
		if (continuousCalculation) {
			calculationRepeatCount++;
			if (calculationRepeatCount >= MAX_CALCULATION_REPEAT) {
				calculationRepeatCount = 0;
				setContinuousCalculation(false);
			} else {
				recalculatePoints();
			}
		} else if (getPointsRendered() < MAX_POINTS) {
			recalculatePoints();
		}
		sendMessage(MessageType.NEW_POINTS_AVAILABLE, accumulatedPoints);
	}

	private PointSet createNewPointSet(float[] newPoints) {
		int existingPointCount = getPointsRendered();
		int newPointCount = existingPointCount;
		//Log.d(LOG_TAG, String.format("Had %d points, just got %d new ones", existingPointCount, BATCH_SIZE));
		if (existingPointCount < MAX_POINTS) {
			newPointCount += BATCH_SIZE;
		}
		//Log.d(LOG_TAG, "Allocating new byte buffer");
		ByteBuffer bb = ByteBuffer.allocateDirect(newPointCount * GlRenderer.COORDS_PER_VERTEX * GlRenderer.BYTES_PER_FLOAT);
		bb.order(ByteOrder.nativeOrder());

		// create a floating point buffer from the ByteBuffer
		FloatBuffer pointBuffer = bb.asFloatBuffer();
		if (existingPointCount > 0) {
			if (existingPointCount == MAX_POINTS) {
				// We are at max points, so we should chop off a batch from the beginning.
				int numToKeep = MAX_POINTS - BATCH_SIZE;
				//Log.d(LOG_TAG, String.format("Adding %d existing points to new buffer", numToKeep));
				float[] keep = new float[numToKeep * GlRenderer.COORDS_PER_VERTEX];
				fractalPoints.getPoints().position(BATCH_FLOATS);
				fractalPoints.getPoints().get(keep, 0, keep.length);
				pointBuffer.put(keep);
			} else {
				//Log.d(LOG_TAG, String.format("Adding all existing points to new buffer"));
				pointBuffer.put(fractalPoints.getPoints());
			}
		}
		// add the new coordinates to the FloatBuffer
		//Log.d(LOG_TAG, "Appending new points to new buffer");
		pointBuffer.put(newPoints, 0, BATCH_FLOATS);
		// set the buffer to read the first coordinate
		pointBuffer.position(0);
		return new PointSet(pointBuffer, newPointCount);
	}

	public boolean isRecalculating() {
		return recalculating;
	}
	
	public boolean select(float[] nearPoint, float[] farPoint) {
		float minA = RayCubeIntersection.NO_INTERSECTION;
		float testA;
		int mindex = FractalState.NO_CUBE_SELECTED;
		
		//Log.d("View", String.format("Near point is (%f,  %f, %f)", nearPoint[0], nearPoint[1], nearPoint[2]));
		//Log.d("View", String.format("Far point is (%f,  %f, %f)", farPoint[0], farPoint[1], farPoint[2]));
				
		int transformCount = State.getNumTransforms();
		float[][] transforms = State.getTransforms();
		
		for (int i = 0; i< transformCount; i++) {
			testA = new RayCubeIntersection(nearPoint, farPoint, transforms[i]).getMinA();
			
			//Log.d("View", "testA is " + testA);
			if (testA < minA) {
				minA = testA;
				mindex = i;
			}
		}
		
		State.setSelectedTransform(mindex);
		sendMessage(MessageType.STATE_CHANGING, true);
		return State.anyTransformSelected();
	}
	
	public void startManipulation() {
		//Log.d("FractalStateManager", "starting manipulation");
		lastState = State.clone();
	}

	public void finishManipulation() {
		boolean undoWasEmpty = undoStack.empty();
		if (lastState != null && !State.equals(lastState)) {
			undoStack.push(lastState);
			stateChanged();
		}
		if (undoWasEmpty && !undoStack.empty()) {
			sendMessage(MessageType.UNDO_ENABLED_CHANGED, true);
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
		sendMessage(MessageType.STATE_CHANGING, true);
	}

	public void scaleSelectedTransform(float scaleFactor, float[] focus,
			float[] endPoint) {
		if (uniformScaleMode) {
			State.scaleUniform(scaleFactor);
		} else {
			State.scaleLinear(scaleFactor, focus, endPoint);
		}
		sendMessage(MessageType.STATE_CHANGING, true);
	}

	public void moveSelectedTransform(float[] oldNear, float[] oldFar,
			float[] newNear, float[] newFar) {
		State.moveSelectedTransform(oldNear, oldFar, newNear, newFar);
		sendMessage(MessageType.STATE_CHANGING, true);
	}

	public boolean isContinuousCalculation() {
		return continuousCalculation;
	}

	public void setContinuousCalculation(boolean continuousCalculation) {
		//Log.d("statemgr", "Got call to setContinuousCalculation with value " + continuousCalculation + ", old value was " + this.continuousCalculation);
		if (editMode) {return;}
		if (continuousCalculation != this.continuousCalculation) {
			calculationRepeatCount = 0;
			sendMessage(MessageType.ACCUMULATION_MODE_CHANGED, continuousCalculation);
			if (!continuousCalculation && fractalPoints != null) {
				cancelCalculation();
			}
		}
		this.continuousCalculation = continuousCalculation;
		if (continuousCalculation && !isEditMode()) {
			//Log.d("statemgr", "Going to recalculate points");
			recalculatePoints();
		}
	}
	
	private void stateChanged() {
		sendMessage(MessageType.STATE_CHANGED, true);
		cancelCalculation();
		resetPoints();
	}
	
	private void sendMessage(MessagePasser.MessageType type, boolean value) {
		if (messagePasser != null) {
			messagePasser.SendMessage(type, value);
		}
	}

	public int getPointsRendered() {
		return fractalPoints.getNumPoints();
	}
	
	public void cancelCalculation() {
		if (calculator != null && !calculator.isCancelled()) {
			calculator.cancel(true);
			calculator = null;
			recalculating = false;
		}
	}

	public float[] getBoundingBox() {
		return boundingBox;
	}

	public void loadStateFromUri(Uri intentData) {
		String transforms = intentData.getQueryParameter("transforms");
		Log.d("StateManager", "transforms: " + transforms);
		State = new FractalState(
			0,
			Integer.parseInt(intentData.getQueryParameter("id")),
			intentData.getQueryParameter("name"),
			"https://fractaleditor.com/media/" + intentData.getQueryParameter("thumbnail"),
			intentData.getQueryParameter("transforms")
		);

		undoStack.clear();
		sendMessage(MessageType.UNDO_ENABLED_CHANGED, false);
		stateChanged();
	}

	public static class PointSet {
		private FloatBuffer points;
		private int numPoints;

		public PointSet(FloatBuffer points, int numPoints) {
			this.points = points;
			this.numPoints = numPoints;
		}

		public FloatBuffer getPoints() {
			return points;
		}

		public int getNumPoints() {
			return numPoints;
		}
	}
}
