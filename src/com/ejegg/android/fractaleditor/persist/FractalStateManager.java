package com.ejegg.android.fractaleditor.persist;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.ejegg.android.fractaleditor.FractalCalculatorTask;
import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.FractalCalculatorTask.ResultListener;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.spatial.RayCubeIntersection;

import android.R;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class FractalStateManager implements ResultListener {
	private int numPoints = 200000;
    private FloatBuffer fractalPoints = null;
    private FractalCalculatorTask calculator;
    private boolean editMode = false;
    private boolean recalculating = false;
    private boolean continuousCalculation = false;
    private int calculationRepeatCount = 0;
    private static final int MAX_CALCULATION_REPEAT = 25;
	private Stack<FractalState> undoStack = new Stack<FractalState>();
	private boolean uniformScaleMode = true;
	
	private static final int BUFFER_MULTIPLE = 3;
	private int bufferIndex = 0;
	
	private MessagePasser messagePasser;
	private FractalCalculatorTask.ProgressListener calculationListener;
	private FractalState lastState = null;
    private FractalState State = new FractalState(0, 0, "Sierpinski Pyramid",
    											  4, "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 -0.5 -0.5 -0.5 1.0 " +
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.5 -0.5 -0.5 1.0 " +
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 -0.5 0.5 1.0 " + 
    												 "0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.5 0.0 1.0");
	private float[] boundingBox;

	public FractalStateManager(MessagePasser messagePasser) {
		this.messagePasser = messagePasser;
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
		State.clearSelection();
		sendMessage(MessagePasser.MessageType.EDIT_MODE_CHANGED, editMode);
		if (editMode) {
			cancelCalculation();
		}
	}
	
	public boolean isUniformScaleMode() {
		return uniformScaleMode;
	}
	
	public void toggleScaleMode() {
		uniformScaleMode = !uniformScaleMode;
		sendMessage(MessagePasser.MessageType.SCALE_MODE_CHANGED, uniformScaleMode);
	}
	
	public void undo() {
		if (!undoStack.empty()) {
			State = undoStack.pop();
			stateChanged();
		}
		if (undoStack.empty()) {
			sendMessage(MessagePasser.MessageType.UNDO_ENABLED_CHANGED, false);
		}
	}
	
	public void loadStateFromUri(ContentResolver contentResolver, Uri savedFractalUri) {
		Cursor cursor = contentResolver.query(savedFractalUri, FractalStateProvider.Items.ALL_COLUMNS, null, null, null);
		cursor.moveToFirst();
		State = new FractalState(
				cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items._ID)),
				cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items.SHARED_ID)),
				cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.NAME)),
				cursor.getInt(cursor.getColumnIndex(FractalStateProvider.Items.TRANSFORM_COUNT)),
				cursor.getString(cursor.getColumnIndex(FractalStateProvider.Items.SERIALIZED_TRANSFORMS))
				);

		cursor.close();
		undoStack.clear();
		sendMessage(MessagePasser.MessageType.UNDO_ENABLED_CHANGED, false);
		stateChanged();
	}

	protected class Saver extends AsyncTask<String, Integer, Boolean> {

		private ContentResolver contentResolver;
		private String url;

		public Saver(ContentResolver contentResolver) {
			this.contentResolver = contentResolver;
		}

		public Saver(ContentResolver contentResolver, String url) {
			this(contentResolver);
			this.url = url;
		}

		@Override
		protected Boolean doInBackground(String... params) {
			String saveName = params[0];
			
			try {
				Log.d("Saver", saveName);
				ContentValues val = new ContentValues();
				val.put(FractalStateProvider.Items.NAME, saveName);
				val.put(FractalStateProvider.Items.TRANSFORM_COUNT, State.getNumTransforms());
				val.put(FractalStateProvider.Items.SERIALIZED_TRANSFORMS, State.getSerializedTransforms());
				val.put(FractalStateProvider.Items.LAST_UPDATED, System.currentTimeMillis());
				contentResolver.insert(FractalStateProvider.CONTENT_URI, val);
				
				if (url != null) {
					HttpClient client = new DefaultHttpClient();
					HttpPost post = new HttpPost("http://fractaleditor.ejegg.com/fractal/save");
					List<NameValuePair> pairs = new ArrayList<NameValuePair>(2);
					pairs.add(new BasicNameValuePair("name", saveName));
					pairs.add(new BasicNameValuePair("serializedTransforms", State.getSerializedTransforms()));
					post.setEntity(new UrlEncodedFormEntity(pairs));
					
					HttpResponse response = client.execute(post);
					String result = EntityUtils.toString(response.getEntity());
					Log.d("FractalStateManager", "Saved, got response: " + result);
				}
			}
			catch (Exception e) {
				Log.d("FractalStateManager", "Error saving: " + e.getMessage() + e.getStackTrace());
				return false;
			}
			return true;
		}
		
	    protected void onPostExecute(Boolean success){
	    	Log.d("Saver", "Done with result: " + success);
	    	FractalStateManager.this.sendMessage(MessageType.STATE_SAVED, success);
	    }
	}
	
	public void save(ContentResolver contentResolver, String saveName) {
		new Saver(contentResolver).execute(saveName);
	}

	public void save(ContentResolver contentResolver, String saveName, String url) {
		new Saver(contentResolver, url).execute(saveName);
	}

	public void setCalculationListener(FractalCalculatorTask.ProgressListener calculationListener) {
		calculationRepeatCount = 0; //reset redraw count on orientation change 
		this.calculationListener = calculationListener;
		if (calculator != null && !continuousCalculation) {
			calculator.setProgressListener(calculationListener);
		}
	}
	
	public void recalculatePoints() {
		if (recalculating) return;
		
		recalculating = true;		
		FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(State, numPoints * BUFFER_MULTIPLE);
		calculator = new FractalCalculatorTask(fractalPoints == null ? calculationListener : null, this);
		calculator.execute(request);
	}

	@Override
	public void finished(FloatBuffer points, float[] boundingBox) {
		this.boundingBox = boundingBox;
		boolean accumulatedPoints = (fractalPoints != null);
		this.fractalPoints = points;
		recalculating = false;
		if (continuousCalculation) {
			calculationRepeatCount++;
			if (calculationRepeatCount >= MAX_CALCULATION_REPEAT) {
				calculationRepeatCount = 0;
				setContinuousCalculation(false);
			} else {
				recalculatePoints();
			}
		}
		bufferIndex = 0;
		sendMessage(MessagePasser.MessageType.NEW_POINTS_AVAILABLE, accumulatedPoints);
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
			sendMessage(MessagePasser.MessageType.UNDO_ENABLED_CHANGED, true);
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
		sendMessage(MessagePasser.MessageType.STATE_CHANGED, true);
		cancelCalculation();
		fractalPoints = null;
	}
	
	private void sendMessage(MessagePasser.MessageType type, boolean value) {
		if (messagePasser != null) {
			messagePasser.SendMessage(type, value);
		}
	}

	public int getBufferIndex() {
		return bufferIndex;
	}

	public void incrementBufferIndex() {
		bufferIndex = (bufferIndex + 1) % BUFFER_MULTIPLE;
		if (bufferIndex > 0) {
			sendMessage(MessagePasser.MessageType.NEW_POINTS_AVAILABLE, true);
		}
	}
	
	private void cancelCalculation() {
		if (calculator != null && !calculator.isCancelled()) {
			calculator.cancel(true);
			recalculating = false;
		}
	}

	public float[] getBoundingBox() {
		return boundingBox;
	}
}
