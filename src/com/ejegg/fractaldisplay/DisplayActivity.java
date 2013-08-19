package com.ejegg.fractaldisplay;

import java.nio.FloatBuffer;
import java.util.Arrays;

import com.ejegg.fractaldisplay.persist.FractalState;
import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

public class DisplayActivity extends Activity implements FractalCalculatorTask.ProgressListener, FractalStateManager.StateChangeSubscriber, OnClickListener {

	private ProgressBar progressBar;
	private FractalStateManager stateManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display);
		setButtons();
		
		FractalDisplay appContext = (FractalDisplay)getApplicationContext();
		stateManager = appContext.getStateManager();
		setEditButtonState();
		
		Camera camera = appContext.getCamera();
		MainRenderer mainRenderer = new MainRenderer(camera, stateManager);
		
		FractalDisplayView view = (FractalDisplayView)findViewById(R.id.fractalCanvas);
		view.setRenderer(mainRenderer);
		
        progressBar = (ProgressBar)findViewById(R.id.computeProgress);
		if (stateManager.getFractalPoints() != null) {
			progressBar.setVisibility(View.GONE);
		} else {
			recalculatePoints(stateManager.getState());
		}
	}

	private void recalculatePoints(FractalState state) {
		FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(state, stateManager.getNumPoints());
		new FractalCalculatorTask(this).execute(request);
	}

	private void setButtons() {
	   	for (int id : Arrays.asList(R.id.loadButton, R.id.saveButton, R.id.undoButton, R.id.addButton, R.id.removeButton, R.id.modeButton, R.id.scaleModeButton)) {
	   		Button button = (Button) findViewById(id);//who's got the button?
	   		if ( button == null ) {
	   			Log.d("DisplayActivity", "button " + id + " is null, can set click listener");	   			
	   		} else {
	   			button.setOnClickListener(this);
	   		}
	   	}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display, menu);
		return true;
	}

	@Override
	public void started() {
		Log.d("DisplayActivity", "GotStartedSignal");
		progressBar.setVisibility(View.VISIBLE);
	}

	public void progressed(int progress) {
		this.progressBar.setProgress(progress);
	}

	@Override
	public void finished(FloatBuffer points) {
		Log.d("DisplayActivity", "GotPoints");
		progressBar.setVisibility(View.GONE);
		stateManager.setFractalPoints(points);
	}

	@Override
	public void updateState(FractalState newState, boolean undoEnabled) {
		recalculatePoints(newState);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.modeButton:
				boolean newEditMode = !stateManager.getEditMode(); 
				stateManager.setEditMode(newEditMode);
				setEditButtonState();
				break;
			default:
				break;
		}
	}
	
	private void setEditButtonState() {
		((EditButton)findViewById(R.id.modeButton)).setEditMode(stateManager.getEditMode());
	}
}
