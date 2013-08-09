package com.ejegg.fractaldisplay;

import java.nio.FloatBuffer;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.ProgressBar;

public class DisplayActivity extends Activity implements FractalCalculatorTask.ProgressListener {

	private FractalRenderer fractalRenderer;
	private MainRenderer mainRenderer;
	private FractalDisplay appContext;
	private ProgressBar progress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appContext = (FractalDisplay)getApplicationContext();
		fractalRenderer = new FractalRenderer();
		mainRenderer = new MainRenderer(appContext.getCamera(), fractalRenderer);
		setContentView(R.layout.activity_display);
		GLSurfaceView view = (GLSurfaceView)findViewById(R.id.fractalCanvas);
		view.setRenderer(mainRenderer);
		if (appContext.getFractalPoints() != null) {
			fractalRenderer.setPoints(appContext.getNumPoints(), appContext.getFractalPoints());
		} else {
			FractalCalculatorTask.Request request = new FractalCalculatorTask.Request(appContext.getState(), appContext.getNumPoints());
			new FractalCalculatorTask(this).execute(request);
		}
        progress = (ProgressBar)findViewById(R.id.computeProgress);
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
	}

	@Override
	public void finished(FloatBuffer points) {
		Log.d("DisplayActivity", "GotPoints");
		appContext.setFractalPoints(points);
		fractalRenderer.setPoints(appContext.getNumPoints(), points);
	}
}
