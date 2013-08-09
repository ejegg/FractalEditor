package com.ejegg.fractaldisplay;

import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.util.Log;

public class FractalRenderer extends GlRenderer {
		
	private FloatBuffer pointBuffer;
	private int mvpMatrixHandle;
	//private int mFadeHandle;
	private int colorHandle;
	private int positionHandle;
	private int numPoints;
	
    private float color[] = { 0.35f, 1.0f, 0.3f, 1.0f };
    
    public FractalRenderer() {
    	 vertexShaderCode =
    	    		"uniform mat4 uMVPMatrix;" +
    	    	    "attribute vec4 vPosition;" +
    	    	    "varying float dist; " +
    	    	    "void main() {" +
    	    	    "  gl_Position = uMVPMatrix * vPosition;" +
    	    	    "  dist = 2.0 - gl_Position.z;" +
    	    	    "}";

    	 fragmentShaderCode =
    	    	    "precision mediump float;" +
    	    	    "uniform vec4 vColor;" +
    	    	    "uniform int fade;" +
    	    	    "varying float dist; " +
    	    	    "void main() {" +
    	    	    "  if (fade > 0) {" +
    	    	    "	 gl_FragColor = vColor / (1.0 + (dist / 3.0 ));" +
    	    	    "    gl_FragColor.w = 1.0 - (dist / 2.0);" +
    	    	    "  } else {" +
    	    	    "    gl_FragColor = vColor; " +
    	    	    "  }" +
    	    	    "  if (gl_FragColor.w < 0.1) gl_FragColor.w = 0.1;" +
    	    	    "}";
    }
    
    @Override
    public void initialize() {
    	setShaders();
    	Log.d("FractalRenderer", "Called setShaders, programHandle is " + programHandle);
		mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
    	colorHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
    	positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
    }
    
    public boolean ready() {
    	return numPoints > 0 && pointBuffer != null;
    }
    
    public void setPoints(int numPoints, FloatBuffer points) {
    	this.numPoints = numPoints;
    	this.pointBuffer = points;
    }
    
    public void draw(Camera camera) {
    	float[] mvpMatrix = camera.getMVPMatrix();
    	if (numPoints == 0) return;
    	if (mvpMatrix == null) {
    		Log.d("Fractal", "mvpMatrix is null");
    	}
    	
    	GLES20.glUseProgram(programHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

    	GLES20.glEnableVertexAttribArray(positionHandle);
    	
    	GLES20.glVertexAttribPointer(positionHandle, 
    			COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, pointBuffer);

    	GLES20.glUniform4fv(colorHandle, 1, color, 0);
    	
    	GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);
    	
    	GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
