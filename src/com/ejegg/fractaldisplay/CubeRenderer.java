package com.ejegg.fractaldisplay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.ejegg.fractaldisplay.persist.FractalState;
import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.opengl.GLES20;
import android.util.Log;

public class CubeRenderer extends GlRenderer {

    private static final float[] cubeTriangleVertices = {
    	-1f, -1f, -1f,
    	-1f, 1f, -1f,
    	-1f, -1f, 1f,//counterclock
    	-1f, 1f, 1f,
    	-1f, -1f, 1f,
    	-1f, 1f, -1f,//left face done
    	1f, -1f, -1f,
    	1f, -1f, 1f,
    	1f, 1f, -1f,//ccw
    	1f, 1f, 1f,
    	1f, 1f, -1f,
    	1f, -1f, 1f,//right face done
    	-1f, -1f, -1f,
    	1f, -1f, -1f,
    	-1f, 1f, -1f,//ccw
    	1f, 1f, -1f,
    	-1f, 1f, -1f,
    	1f, -1f, -1f,//back face done
    	-1f, -1f, 1f,
    	-1f, 1f, 1f,
    	1f, -1f, 1f, //ccw
    	1f, 1f, 1f,
    	1f, -1f, 1f,
    	-1f, 1f, 1f,//front face done
    	-1f, -1f, -1f,
    	-1f, -1f, 1f,
    	1f, -1f, -1f,//ccw
    	1f, -1f, 1f,
    	1f, -1f, -1f,
    	-1f, -1f, 1f,//bottom face done
    	-1f, 1f, -1f,
    	1f, 1f, -1f,
    	-1f, 1f, 1f,//ccw
    	1f, 1f, 1f,
    	-1f, 1f, 1f,
    	1f, 1f, -1f//top face done
    };
    private static final float color[] = { 1, 0, 0, 0.9f,
   		 1, 0.647f, 0, 0.9f,
   		 1, 1, 0, 0.9f,
   		 0, 0.5f, 0, 0.9f,
   		 0, 0, 1, 0.9f,
   		 0.294f, 0, 0.505f, 0.9f};
	private FloatBuffer pointBuffer, colorBuffer;
    private int mPointCount = 36;
    private int mSelectedHandle;
	private int mvpMatrixHandle;
	private int transformMatrixHandle;	
	private int colorHandle;
	private int positionHandle;
	private FractalStateManager stateManager;
	private Camera camera;

    public CubeRenderer(Camera camera, FractalStateManager stateManager) {
    	this.camera = camera;
    	this.stateManager = stateManager;
    	
    	vertexShaderCode =
        		"uniform mat4 uMVPMatrix;" +
        	    "uniform mat4 transformMatrix;" +
        	    "attribute vec4 vColor;" +
        	    "attribute vec4 vPosition;" +
        	    "varying vec4 varColor;" +
        	    "void main() {" +
        	    "  gl_Position = uMVPMatrix * (transformMatrix * vPosition);" +
        	    "  varColor = vColor;" +
        	    "}";

        fragmentShaderCode =
        	    "precision mediump float;" +
        	    "uniform int selected;" +
        	    "varying vec4 varColor;" +
        	    "void main() {" +
        	    "  gl_FragColor = varColor;" +
        	    "  if (selected == 0) { " +
        	    "    gl_FragColor /= 1.3; " +
        	    "  } " +
        	    //"  gl_FragColor.w = 1.0;" +
        	    "}";
    }
    
    public void initialize() {
    	setColors();
    	setShaders();
    	Log.d("FractalRenderer", "Called setShaders, programHandle is " + programHandle);
		mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
		transformMatrixHandle = GLES20.glGetUniformLocation(programHandle, "transformMatrix");
    	colorHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
    	positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
    	mSelectedHandle = GLES20.glGetUniformLocation(programHandle, "selected");
    }
    
    public CubeRenderer(float[] transform, float alpha) {
    	ByteBuffer bb = ByteBuffer.allocateDirect(cubeTriangleVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        pointBuffer = bb.asFloatBuffer();
    }
    
    private void setColors() {
        float[] moreColors = new float[color.length * 6];
        for (int i = 0; i < 6; i++) {
        	for (int j = 0; j < 6; j++) {
        		for (int k = 0; k < 3; k++) {
        			moreColors[i * 24 + j * 4 + k] = color[i * 4 + k] * 0.75f;
        		}
        		moreColors[i * 24 + j * 4 + 3] = 1;
        	}
        }
        ByteBuffer bb2 = ByteBuffer.allocateDirect(moreColors.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        colorBuffer = bb2.asFloatBuffer();
        
        colorBuffer.put(moreColors);
        colorBuffer.position(0);
    }
    
    public void draw() {
       	float[] mvpMatrix = camera.getMVPMatrix();
       	FractalState state = stateManager.getState();
       	int numTransforms = state.getNumTransforms();
       	int selectedTransform = state.getSelectedTransform();
       	float[][] transforms = state.getTransforms();
       	
    	GLES20.glUseProgram(programHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

    	GLES20.glEnableVertexAttribArray(positionHandle);

    	GLES20.glEnableVertexAttribArray(colorHandle);
    	
    	GLES20.glVertexAttribPointer(positionHandle, 
    			COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, pointBuffer);

    	GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 16, colorBuffer);
    	
    	for (int i = 0; i < numTransforms; i++) {
        	GLES20.glUniformMatrix4fv(transformMatrixHandle, 1, false, transforms[i], 0);
        	GLES20.glUniform1i(mSelectedHandle, selectedTransform == i ? 1 : 0);
        	GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mPointCount);
    	}
    	
    	GLES20.glDisableVertexAttribArray(positionHandle);    	
    	GLES20.glDisableVertexAttribArray(colorHandle);
    }
}
