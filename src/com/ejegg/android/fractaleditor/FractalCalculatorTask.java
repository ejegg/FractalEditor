package com.ejegg.android.fractaleditor;

import java.lang.ref.SoftReference;
import java.util.Random;

import com.ejegg.android.fractaleditor.persist.FractalState;
import com.ejegg.android.fractaleditor.render.GlRenderer;

import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;

public class FractalCalculatorTask extends AsyncTask<FractalCalculatorTask.Request, Integer, float[]> {

    private final Random r = new Random();
    private ProgressListener progressListener;
    private final ResultListener resultListener;
    private static final int DISCARD_COUNT = 10;
    private static SoftReference<float[]> pointsRef;
    private final float[] boundingBox = new float[6];
    
    public FractalCalculatorTask(ProgressListener progressListener, ResultListener resultListener) {
    	this.progressListener  = progressListener;
    	this.resultListener = resultListener;
    }
    
    @Override
    protected float[] doInBackground(Request... fractalRequest) {
        //Log.d("FractalCalculatorTask", "Starting calculation");
        int numPoints = fractalRequest[0].getNumPoints();
        int numTransforms = fractalRequest[0].getFractal().getNumTransforms();
        float[][] transforms = fractalRequest[0].getFractal().getTransforms();
        float[] transform;
        float[] fractalPoints = null;
        boundingBox[0] = boundingBox[1] = boundingBox[2] = 10; // minima
        boundingBox[3] = boundingBox[4] = boundingBox[5] = -10; //maxima
        if (pointsRef != null) {
                fractalPoints = pointsRef.get(); //assuming numPoints does not change!!
        }
        
        if (fractalPoints == null) {
                Log.d("FractalCalculatorTask", "Allocating new points array");
                fractalPoints = new float[numPoints * GlRenderer.COORDS_PER_VERTEX + 1];
                pointsRef = new SoftReference<>(fractalPoints);
        }

        for(int i = 0; i < 3; i++) {
                fractalPoints[i] = r.nextFloat() * 2.0f - 1.0f;
        }
        fractalPoints[3] = 1.0f;

        int progressStep = numPoints / 100;
                int lastPos = 0;
                int curPos;
                int j;
                float f;

        // Calculate some initial points to throw away
        for (int i = 1; i< DISCARD_COUNT; i++) {
            transform = transforms[r.nextInt(numTransforms)];
            curPos = i * GlRenderer.COORDS_PER_VERTEX;
            Matrix.multiplyMV(fractalPoints, curPos, transform, 0, fractalPoints, lastPos);
            lastPos = curPos;
        }
        // Now start over from the zero-th position
        for (int i = 0; i< numPoints; i++) {
                transform = transforms[r.nextInt(numTransforms)];
                curPos = i * GlRenderer.COORDS_PER_VERTEX;
                Matrix.multiplyMV(fractalPoints, curPos, transform, 0, fractalPoints, lastPos);
                lastPos = curPos;
                
                if (i % progressStep == 0) {
                        //Log.d("FractalCalculatorTask", "progress at " + (i * 100 / numPoints));
                        publishProgress(i * 100 / numPoints);
                        for(j = 0; j < 3; j++) {
                                f = fractalPoints[curPos + j];
                                if (f < boundingBox[j]) {
                                        boundingBox[j] = f;
                                }
                                if (f > boundingBox[j + 3]) {
                                        boundingBox[j + 3] = f;
                                }
                        }
                        if (isCancelled()) {
                                return null;
                        }
                }
        }

        return fractalPoints;
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
    
    protected void onPostExecute(float[] points){
            Log.d("FractalCalculatorTask", "onPostExecute, publishing points");
            if (progressListener != null) {
                    progressListener.finished();
            }
            if (resultListener != null) {
                    resultListener.finished(points, boundingBox);
            }
    }
    
    public void setProgressListener(ProgressListener listener) {
            this.progressListener = listener;
    }
    
    public interface ResultListener {
            void finished(float[] points, float[] boundingBox);
    }
}
