package com.ejegg.fractaldisplay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import com.ejegg.fractaldisplay.persist.FractalState;

import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;

public class FractalCalculatorTask extends AsyncTask<FractalCalculatorTask.Request, Integer, FloatBuffer> {

    private final Random r = new Random();
    private ProgressListener progressListener;
    private ResultListener resultListener;
    
    public FractalCalculatorTask(ProgressListener progressListener, ResultListener resultListener) {
    	this.progressListener  = progressListener;
    	this.resultListener = resultListener;
    }
    
	@Override
	protected FloatBuffer doInBackground(Request... fractalRequest) {
		Log.d("FractalCalculatorTask", "Starting calculation");
		int numPoints = fractalRequest[0].getNumPoints();
		int numTransforms = fractalRequest[0].getFractal().getNumTransforms();
		float[][] transforms = fractalRequest[0].getFractal().getTransforms();
		
    	float[] currentPoint = new float[4];
    	for(int i = 0; i < 3; i++) {
    		currentPoint[i] = r.nextFloat() * 2.0f - 1.0f;
    	}
    	currentPoint[3] = 1.0f;
		float[] fractalPoints = new float[numPoints * GlRenderer.COORDS_PER_VERTEX + 1];
        int progressStep = numPoints / 25;
        
    	for (int i = 0; i< numPoints; i++) {
    		float[] transform = transforms[r.nextInt(numTransforms)];
    		int srpPos = i * GlRenderer.COORDS_PER_VERTEX;
    		Matrix.multiplyMV(fractalPoints, srpPos, transform, 0, currentPoint, 0);
    		System.arraycopy(fractalPoints, srpPos, currentPoint, 0, GlRenderer.COORDS_PER_VERTEX);
    		
    		if (i % progressStep == 0) {
    			//Log.d("FractalCalculatorTask", "progress at " + (i * 100 / numPoints));
    			publishProgress(i * 100 / numPoints);
    			if (isCancelled()) {
    				return null;
    			}
    		}
    	}
    	FloatBuffer pointBuffer = allocateBuffer(fractalPoints.length);
        // add the coordinates to the FloatBuffer
        pointBuffer.put(fractalPoints);
        // set the buffer to read the first coordinate
        pointBuffer.position(0);
        Log.d("FractalCalculatorTask", "returning pointBuffer");
        return pointBuffer;
	}
		
	private FloatBuffer allocateBuffer(int length) {
    	ByteBuffer bb = ByteBuffer.allocateDirect(length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());
        // create a floating point buffer from the ByteBuffer
        return bb.asFloatBuffer();
    }
	
	public static class Request {
		private FractalState fractal;
		private int numPoints;
		
		public Request(FractalState fractal, int numPoints) {
			this.fractal = fractal;
			this.numPoints = numPoints;
		}
		
		public FractalState getFractal() {
			return fractal;
		}
		
		public int getNumPoints() {
			return numPoints;
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (progressListener != null) {
			progressListener.progressed(values[0]);
		}
	}
	
	protected void onPostExecute(FloatBuffer points){
		Log.d("FractalCalculatorTask", "onPostExecute, publishing points");
		if (progressListener != null) {
			progressListener.finished();
		}
		if (resultListener != null) {
			resultListener.finished(points);
		}
	}
	
	public void setProgressListener(ProgressListener listener) {
		this.progressListener = listener;
	}
	
	public interface ProgressListener {
		void started();
		void progressed(int progress);
		void finished();
	}
	
	public interface ResultListener {
		void finished(FloatBuffer points);
	}
}
