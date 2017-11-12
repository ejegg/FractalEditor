package com.ejegg.android.fractaleditor.render;

import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.spatial.Camera;

import android.opengl.GLES20;
import android.util.Log;

public class FractalRenderer extends GlRenderer {
    	
	private final int mvpMatrixHandle;
	private final int colorHandle;
	private final int fadeHandle;
	private final int minDistHandle;
	private final int distFacHandle;
	private final int positionHandle;
	private final float color[] = { 0.35f, 1.0f, 0.3f, 1.0f };
    
    public FractalRenderer(Camera camera, FractalStateManager stateManager) {
    	super(camera, stateManager);
    	vertexShaderCode =
    	        	"uniform mat4 uMVPMatrix;" +
    	    	    "attribute vec4 vPosition;" +
    	    	    "varying float dist; " +
    	    	    "void main() {" +
    	    	    "  gl_Position = uMVPMatrix * vPosition;" +
    	    	    "  dist = gl_Position.z;" +
    	    	    "}";

    	fragmentShaderCode =
    	    	    "precision mediump float;" +
    	    	    "uniform vec4 vColor;" +
    	    	    "uniform int fade;" +
    	    	    "uniform float minDist;" +
    	    	    "uniform float distFac;" +    	    	    
    	    	    "varying float dist; " +
    	    	    "void main() {" +
    	    	    "  if (fade > 0) {" +
    	    	    "    float k = (dist - distFac) / (minDist - distFac); " +
    	    	    "	 gl_FragColor = vColor * k;" +
    	    	    "    gl_FragColor.w *= k;" +
    	    	    "  } else {" +
    	    	    "    gl_FragColor = vColor; " +
    	    	    "  }" +
    	    	    "  if (gl_FragColor.w < 0.1) gl_FragColor.w = 0.1;" +
    	    	    "}";

    	setShaders();
    	Log.d("FractalRenderer", "Called setShaders, programHandle is " + programHandle);
    	mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
    	colorHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
    	positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
    	fadeHandle = GLES20.glGetUniformLocation(programHandle, "fade");
    	minDistHandle = GLES20.glGetUniformLocation(programHandle, "minDist");
    	distFacHandle = GLES20.glGetUniformLocation(programHandle, "distFac");
    }

	// TODO: in accumulatePoints mode, only render each batch once.
    public void draw(boolean accumulatePoints, float minDist, float maxDist) {
    	if (!stateManager.hasPoints()) {
    		Log.v("FractalRenderer", "State manager has no points, requesting recalculation");
        	stateManager.recalculatePoints();
        	return;
    	}
    	float[] mvpMatrix = camera.getMVPMatrix();
    	GLES20.glUseProgram(programHandle);
    	checkGlError("FractalRenderer", "glUseProgram");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
    	checkGlError("FractalRenderer", "glUniformMatrix4fv");
    	GLES20.glEnableVertexAttribArray(positionHandle);
    	checkGlError("FractalRenderer", "glEnableVertexAttribArray");
		FractalStateManager.PointSet points = stateManager.getFractalPoints();
    	GLES20.glVertexAttribPointer(positionHandle,
				COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, points.getPoints());
    	checkGlError("FractalRenderer", "glVertexAttribPointer");
    	GLES20.glUniform4fv(colorHandle, 1, color, 0);
    	checkGlError("FractalRenderer", "glUniform4fv - color");
    	GLES20.glUniform1i(fadeHandle, accumulatePoints ? 1 : 0);
    	checkGlError("FractalRenderer", "glUniform1i - fade");
    	GLES20.glUniform1f(minDistHandle, minDist);
    	checkGlError("FractalRenderer", "glUniform1f - mindist");
    	GLES20.glUniform1f(distFacHandle, 2 * (maxDist - minDist));
    	checkGlError("FractalRenderer", "glUniform1f - distfac");
		int totalPoints = points.getNumPoints();
		//Log.d("FractalRenderer", String.format("State manager reports %d total points", totalPoints));
		for ( int idx = 0; idx < totalPoints; idx += FractalStateManager.BATCH_SIZE) {
			//Log.d("FractalRenderer", String.format("Rendering %d points starting at index %d", FractalStateManager.BATCH_SIZE, idx));
			GLES20.glDrawArrays(GLES20.GL_POINTS, idx, FractalStateManager.BATCH_SIZE);
			checkGlError("FractalRenderer", "glDrawArrays");
		}
		GLES20.glDisableVertexAttribArray(positionHandle);
    	checkGlError("FractalRenderer", "glDisableVertexAttribArray");
    }
}
