package com.ejegg.fractaldisplay;

import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.opengl.GLES20;
import android.util.Log;

public class FractalRenderer extends GlRenderer {
		
	private int mvpMatrixHandle;
	private int colorHandle;
	private int positionHandle;
	private Camera camera;
	private FractalStateManager stateManager;
	
    private float color[] = { 0.35f, 1.0f, 0.3f, 1.0f };
    
    public FractalRenderer(Camera camera, FractalStateManager stateManager) {
    	this.camera = camera;
    	this.stateManager = stateManager;
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
    	    	    "varying float dist; " +
    	    	    "void main() {" +
    	    	    //"  if (fade > 0) {" +
    	    	    "	 gl_FragColor = vColor;" +
    	    	    "    gl_FragColor.w = 1.0 - dist / 3.0;" +
    	    	    //"  } else {" +
    	    	    //"    gl_FragColor = vColor; " +
    	    	    //"  }" +
    	    	    "  if (gl_FragColor.w < 0.1) gl_FragColor.w = 0.3;" +
    	    	    "}";

    	setShaders();
    	Log.d("FractalRenderer", "Called setShaders, programHandle is " + programHandle);
		mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
    	colorHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
    	positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
    }
        
    public void draw() {
    	if (!stateManager.hasPoints()) return;
    	
    	float[] mvpMatrix = camera.getMVPMatrix();
    	
    	GLES20.glUseProgram(programHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

    	GLES20.glEnableVertexAttribArray(positionHandle);
    	
    	GLES20.glVertexAttribPointer(positionHandle, 
    			COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, stateManager.getFractalPoints());

    	GLES20.glUniform4fv(colorHandle, 1, color, 0);
    	
    	GLES20.glDrawArrays(GLES20.GL_POINTS, 0, stateManager.getNumPoints());
    	
    	GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
