package com.ejegg.fractaldisplay;

import java.util.Arrays;

import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class DisplayActivity extends Activity implements FractalCalculatorTask.ProgressListener, FractalStateManager.ModeChangeListener, OnClickListener {

	private ProgressBar progressBar;
	private FractalStateManager stateManager;
	private static final int LOAD_REQUEST = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display);
		setButtons();
		
		FractalDisplay appContext = (FractalDisplay)getApplicationContext();
		stateManager = appContext.getStateManager();
		stateManager.setCalculationListener(this);
		stateManager.addModeChangeListener(this);
		setButtonStates();
		
		Camera camera = appContext.getCamera();
		MainRenderer mainRenderer = new MainRenderer(camera, stateManager);
		
		FractalDisplayView view = (FractalDisplayView)findViewById(R.id.fractalCanvas);
		view.setRenderer(mainRenderer);
        progressBar = (ProgressBar)findViewById(R.id.computeProgress);
        
	}
	
	@Override
	protected void onDestroy() {
		stateManager.clearModeChangeListeners();
		super.onDestroy();
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
		progressBar.setVisibility(View.VISIBLE);
		this.progressBar.setProgress(progress);
	}

	@Override
	public void finished() {
		progressBar.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.modeButton: 
				stateManager.toggleEditMode();
				break;
			case R.id.loadButton:
				Intent loadTntent = new Intent(this, LoadActivity.class);
            	startActivityForResult(loadTntent, LOAD_REQUEST);
            	break;
			case R.id.addButton:
				stateManager.addTransform();
				break;
			case R.id.removeButton:
				stateManager.removeSelectedTransform();
				break;
			case R.id.scaleModeButton:
				stateManager.toggleScaleMode();
				break;
			case R.id.undoButton:
				stateManager.undo();
				break;
			default:
				break;
		}
	}
	
	private void setButtonStates() {
		((EditButton)findViewById(R.id.modeButton)).setEditMode(stateManager.isEditMode());
		((ScaleModeButton)findViewById(R.id.scaleModeButton)).setScaleMode(stateManager.isUniformScaleMode());
		((Button)findViewById(R.id.undoButton)).setEnabled(stateManager.isUndoEnabled());
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d("main", "onActivityResult");
		switch(requestCode) {
			case LOAD_REQUEST: 
				if (resultCode != RESULT_OK) {
					Toast.makeText(this, "No saved fractal loaded", Toast.LENGTH_LONG).show();
					return;
				}
				Uri savedFractalUri = data.getData();
				Log.d("main", "trying to load fractal with URI: " + savedFractalUri);
				stateManager.loadStateFromUri(getContentResolver(), savedFractalUri);
				break;
		}

	}

	@Override
	public void updateMode() {
		setButtonStates();
	}
}
