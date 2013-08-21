package com.ejegg.fractaldisplay.render;

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
        -1f, -1f, 1f,
        -1f, 1f, -1f,
        -1f, 1f, 1f,
        -1f, 1f, -1f,
        -1f, -1f, 1f,//left face done
        1f, -1f, -1f,
        1f, 1f, -1f,
        1f, -1f, 1f,//ccw
        1f, 1f, 1f,
        1f, -1f, 1f,
        1f, 1f, -1f,//right face done
        -1f, -1f, -1f,
        -1f, 1f, -1f,
        1f, -1f, -1f,//ccw
        1f, 1f, -1f,
        1f, -1f, -1f,
        -1f, 1f, -1f,//back face done
        -1f, -1f, 1f,
        1f, -1f, 1f,
        -1f, 1f, 1f, //ccw
        1f, 1f, 1f,
        -1f, 1f, 1f,
        1f, -1f, 1f,//front face done
        -1f, -1f, -1f,
        1f, -1f, -1f,
        -1f, -1f, 1f,//ccw
        1f, -1f, 1f,
        -1f, -1f, 1f,
        1f, -1f, -1f,//bottom face done
        -1f, 1f, -1f,
        -1f, 1f, 1f,
        1f, 1f, -1f,//ccw
        1f, 1f, 1f,
        1f, 1f, -1f,
        -1f, 1f, 1f//top face done
    };
    private static final float color[] = { 1, 0, 0, 0.9f,
                 1, 0.647f, 0, 0.9f,
                 1, 1, 0, 0.9f,
                 0, 0.5f, 0, 0.9f,
                 0, 0, 1, 0.9f,
                 0.294f, 0, 0.505f, 0.9f};
    
    private FloatBuffer pointBuffer, colorBuffer;
    private int vertexCount = 36;
    private int selectedHandle;
    private int mvpMatrixHandle;
    private int transformMatrixHandle;      
    private int colorHandle;
    private int positionHandle;
    public CubeRenderer(Camera camera, FractalStateManager stateManager) {
        super(camera, stateManager);
        setColors();
        setPoints();
        
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
                    "}";

        Log.d("CubeRenderer", "started initialize");
        setShaders();
        Log.d("CubeRenderer", "Program handle is " + programHandle);
        mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        Log.d("CubeRenderer", "uMVPMatrix handle is " + mvpMatrixHandle);
        transformMatrixHandle = GLES20.glGetUniformLocation(programHandle, "transformMatrix");
        Log.d("CubeRenderer", "transformMatrix handle is " + transformMatrixHandle);
        colorHandle = GLES20.glGetAttribLocation(programHandle, "vColor");
        Log.d("CubeRenderer", "vColor handle is " + colorHandle);
        positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
        Log.d("CubeRenderer", "vPosition handle is " + positionHandle);
        selectedHandle = GLES20.glGetUniformLocation(programHandle, "selected");
        Log.d("CubeRenderer", "selected handle is " + selectedHandle);
        Log.d("CubeRenderer", "finished initialize");
    }
    
    private void setPoints() {
        ByteBuffer bb = ByteBuffer.allocateDirect(cubeTriangleVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        
        pointBuffer = bb.asFloatBuffer();
        pointBuffer.put(cubeTriangleVertices);
        pointBuffer.position(0);
    }
    
    private void setColors() {
        float[] moreColors = new float[color.length * 6];
        for (int i = 0; i < 6; i++) {
        	for (int j = 0; j < 6; j++) {
            	for (int k = 0; k < 3; k++) {
                	moreColors[i * 24 + j * 4 + k] = color[i * 4 + k] * 0.75f;
                }
                moreColors[i * 24 + j * 4 + 3] = 1.0f;
                //Log.d("cubeRenderer", String.format("Color %d is (%f, %f, %f, %f)", i*6 + j, moreColors[i * 24 + j * 4], moreColors[i * 24 + j * 4 + 1], moreColors[i * 24 + j * 4 + 2], moreColors[i * 24 + j * 4 + 3]));
        	}
        }
        ByteBuffer bb2 = ByteBuffer.allocateDirect(moreColors.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        colorBuffer = bb2.asFloatBuffer();
        
        colorBuffer.put(moreColors);
        colorBuffer.position(0);
    }
    
    public void draw() {
        //Log.d("CubeRenderer", "draw");
        float[] mvpMatrix = camera.getMVPMatrix();
        FractalState state = stateManager.getState();
        int numTransforms = state.getNumTransforms();
        int selectedTransform = state.getSelectedTransform();
        float[][] transforms = state.getTransforms();
        GLES20.glEnable( GLES20.GL_DEPTH_TEST );
        GLES20.glUseProgram(programHandle);
        //checkGlError("CubeRenderer", "glUseProgram");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        //checkGlError("CubeRenderer", "glUniformMatrix4fv mvpMatrix");
        GLES20.glEnableVertexAttribArray(positionHandle);
        //checkGlError("CubeRenderer", "glEnableVertexAttribArray position");
        GLES20.glEnableVertexAttribArray(colorHandle);
        //checkGlError("CubeRenderer", "glEnableVertexAttribArray color");
        GLES20.glVertexAttribPointer(positionHandle, 
                        COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, pointBuffer);
        //checkGlError("CubeRenderer", "glVertexAttribPointer position");
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 16, colorBuffer);
        //checkGlError("CubeRenderer", "glVertexAttribPointer color");
        
        for (int i = 0; i < numTransforms; i++) {
	        GLES20.glUniformMatrix4fv(transformMatrixHandle, 1, false, transforms[i], 0);
	        //checkGlError("CubeRenderer", "glUniformMatrix4fv");
	        GLES20.glUniform1i(selectedHandle, selectedTransform == i ? 1 : 0);
	        //checkGlError("CubeRenderer", "glUniform1i");
	        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
	        //checkGlError("CubeRenderer", "glDrawArrays");
        }
        
        GLES20.glDisableVertexAttribArray(positionHandle);      
        GLES20.glDisableVertexAttribArray(colorHandle);
    }
}
