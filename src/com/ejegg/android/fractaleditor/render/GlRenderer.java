package com.ejegg.android.fractaleditor.render;

import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.spatial.Camera;

import android.opengl.GLES20;
import android.util.Log;

public abstract class GlRenderer {

	protected String vertexShaderCode;
	protected String fragmentShaderCode;
	
	protected int programHandle;
	protected int vertexShaderHandle;
	protected int fragmentShaderHandle;
	protected final Camera camera;
	protected final FractalStateManager stateManager;

	protected boolean valid = true; // if false, this instance needs to be destroyed and recreated

	public static final int COORDS_PER_VERTEX = 3;
	public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
	
	protected GlRenderer(Camera camera, FractalStateManager stateManager) {
		this.camera = camera;
		this.stateManager = stateManager;
	}
	
	protected void setShaders() {
		vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, vertexShaderHandle);
        GLES20.glAttachShader(programHandle, fragmentShaderHandle);
        GLES20.glLinkProgram(programHandle);  
	}

	private static int loadShader(int type, String shaderCode){
	    int shader = GLES20.glCreateShader(type);
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);
	    return shader;
	}
	
	public void destroy() {
		GLES20.glDeleteShader(vertexShaderHandle);
		GLES20.glDeleteShader(fragmentShaderHandle);
		GLES20.glDeleteProgram(programHandle);
	}

	protected void checkGlError(String TAG, String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            valid = false;
        }
    }

	public boolean isValid() {
		return valid;
	}

}
