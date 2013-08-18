package com.ejegg.fractaldisplay;

import java.nio.FloatBuffer;

import com.ejegg.fractaldisplay.persist.FractalState;
import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

public class DisplayActivity extends Activity implements FractalCalculatorTask.ProgressListener, FractalStateManager.StateChangeSubscriber {

	private FractalDisplay appContext;
	private ProgressBar progressBar;
	private FractalStateManager stateManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appContext = (FractalDisplay)getApplicationContext();
		stateManager = appContext.getStateManager();
		Camera camera = appContext.getCamera();
		MainRenderer mainRenderer = new MainRenderer(camera, stateManager);
		
		setContentView(R.layout.activity_display);
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
		Log.d("DisplayActivity", "GotProgress");
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
}
