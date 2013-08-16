package com.ejegg.fractaldisplay;

import java.nio.FloatBuffer;

import com.ejegg.fractaldisplay.spatial.Camera;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

public class DisplayActivity extends Activity implements FractalCalculatorTask.ProgressListener {

	private FractalRenderer fractalRenderer;
	private MainRenderer mainRenderer;
	private FractalDisplay appContext;
	private ProgressBar progressBar;
	private FractalStateManager stateManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appContext = (FractalDisplay)getApplicationContext();
		stateManager = appContext.getStateManager();
		Camera camera = appContext.getCamera(); 
		
		fractalRenderer = new FractalRenderer();
		mainRenderer = new MainRenderer(camera, fractalRenderer);
		setContentView(R.layout.activity_display);
		FractalDisplayView view = (FractalDisplayView)findViewById(R.id.fractalCanvas);
		view.setRenderer(mainRenderer);
		
        progressBar = (ProgressBar)findViewById(R.id.computeProgress);
		if (stateManager.getFractalPoints() != null) {
			progressBar.setVisibility(View.GONE);
			fractalRenderer.setPoints(stateManager.getNumPoints(), stateManager.getFractalPoints());
		} else {
			FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(stateManager.getState(), stateManager.getNumPoints());
			new FractalCalculatorTask(this).execute(request);
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
		Log.d("DisplayActivity", "GotProgress");
		this.progressBar.setProgress(progress);
	}

	@Override
	public void finished(FloatBuffer points) {
		Log.d("DisplayActivity", "GotPoints");
		progressBar.setVisibility(View.GONE);
		stateManager.setFractalPoints(points);
		fractalRenderer.setPoints(stateManager.getNumPoints(), points);
	}
}
