package com.ejegg.fractaldisplay.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.ejegg.fractaldisplay.MessagePasser;
import com.ejegg.fractaldisplay.MessagePasser.MessageType;
import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

public class TextureRenderer extends GlRenderer implements MessagePasser.MessageListener {

	private int[] frameBuffer;
	private int[] depthRenderBuffer;
	private int[] textures;
	private int textureWidth, textureHeight;
	private int drawToTexture = 0, numCleared = 0;
	private int lastFrameTexture = 1;
	private boolean clearLastFrame = true;
    private final float[] projectionMatrix = new float[16];
    private int mPositionHandle, mMVPMatrixHandle, textureHandle, texturePositionHandle, alphaHandle;

	private FloatBuffer pointBuffer, textureCoordinateBuffer;
	private long lastCameraMoveId = 0;

    public TextureRenderer(Camera camera, FractalStateManager stateManager, MessagePasser passer) {
    	super(camera, stateManager);
    	
    	passer.Subscribe(this, MessagePasser.MessageType.STATE_CHANGED, MessagePasser.MessageType.EDIT_MODE_CHANGED);
    	
    	vertexShaderCode =
        	    "attribute vec4 vPosition; " +
        	    "attribute vec2 aTexture; " +
        	    "varying vec2 vTexture; " +
        	    "uniform mat4 uMVPMatrix;" +
        	    "void main() {" +
        	    "  gl_Position = uMVPMatrix * vPosition;" +
        	    "  vTexture = aTexture.xy; " +
        	    "}";
    	fragmentShaderCode =
        	    "precision mediump float; " +
        	    "uniform sampler2D texSample; " +
        	    "varying highp vec2 vTexture; " +
        	    "uniform float alpha; " +
        	    "void main() {" +
        	    "  gl_FragColor = texture2D(texSample, vTexture);" +
        	    "  gl_FragColor.w = alpha;" +
        	    "}";
    	setShaders();
    	mPositionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
       	textureHandle = GLES20.glGetUniformLocation(programHandle, "texSample");
    	texturePositionHandle = GLES20.glGetAttribLocation(programHandle, "aTexture");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        alphaHandle  = GLES20.glGetUniformLocation(programHandle, "alpha");
        
    	textureWidth = camera.getWidth();
		textureHeight = camera.getHeight();
		
        float[] quadVertices = {
                0f,  0f, 0f,
                textureWidth,  0, 0f,
                textureWidth,  textureHeight, 0f,
        		0f,  textureHeight, 0f
        };
        float[] textureVertices = {
                0, 0,
        		1, 0,
                1, 1,
                0, 1
            };
        frameBuffer = new int[1];
        depthRenderBuffer = new int[1];
        textures = new int[2];
        
        Matrix.orthoM(projectionMatrix,  0, 0, textureWidth, 0, textureHeight, -2, 2);

        // generate
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glGenRenderbuffers(1, depthRenderBuffer, 0);
        GLES20.glGenTextures(2, textures, 0);
       
        clearTextures();
        
        // create render buffer and bind 16-bit depth buffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRenderBuffer[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, textureWidth, textureHeight);
       
    	ByteBuffer bb = ByteBuffer.allocateDirect(quadVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        pointBuffer = bb.asFloatBuffer();
        pointBuffer.put(quadVertices);
        pointBuffer.position(0);
        
        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        // use the device hardware's native byte order
        bb2.order(ByteOrder.nativeOrder());
        // create a floating point buffer from the ByteBuffer
        textureCoordinateBuffer = bb2.asFloatBuffer();
        textureCoordinateBuffer.put(textureVertices);
        textureCoordinateBuffer.position(0);
    }
    
    private void clearTextures() {
	    for (int i = 0; i < 2; i++)
        {
	        // generate color texture
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
	
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
	                        GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
	                        GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
	                        GLES20.GL_NEAREST);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
	                        GLES20.GL_NEAREST);

	        int[] buf = new int[textureWidth * textureHeight];
	        IntBuffer texBuffer = ByteBuffer.allocateDirect(buf.length  * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
	        
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, textureWidth, textureHeight, 0, 
	        		GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, texBuffer);
        }	
	}
    
    public void preRender() {
    	long newCameraMoveId = camera.getLastMoveId();
    	if (newCameraMoveId != lastCameraMoveId) {
    		lastCameraMoveId = newCameraMoveId;
    		clearLastFrame = true;
    	}
    	
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[drawToTexture], 0);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRenderBuffer[0]);
		GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glDisable( GLES20.GL_DEPTH_TEST );

		if (clearLastFrame) {
			Log.d("TextureRenderer", "clearLastFrame is true, numCleared = " + numCleared);
			numCleared++;
			if (numCleared == 2) {
				numCleared = 0;
				clearLastFrame = false;
			}
		}
		else {
			drawTextureQuad(lastFrameTexture,1.0f);
		}
	}
    
	public void draw() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		drawTextureQuad(drawToTexture, 1.0f);
		drawToTexture = 1 - drawToTexture;
		lastFrameTexture = 1 - lastFrameTexture;
	}
	
	private void drawTextureQuad(int index, float alpha) {
		
		GLES20.glUseProgram(programHandle);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

    	GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 
    			VERTEX_STRIDE, pointBuffer);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, projectionMatrix, 0);
 	    
    	GLES20.glEnableVertexAttribArray(texturePositionHandle);
    	
    	GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 8, textureCoordinateBuffer);
   
    	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[index]);

    	GLES20.glUniform1i(textureHandle, 0);

    	GLES20.glUniform1f(alphaHandle, alpha);
    	
    	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
    	
    	GLES20.glDisableVertexAttribArray(mPositionHandle);

    	GLES20.glDisableVertexAttribArray(texturePositionHandle);
	}
	
	public void clear() {
		clearLastFrame = true;
	}
	
	public void freeResources() {
		GLES20.glDeleteTextures(2, textures, 0);
		GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
		destroy();
	}

	@Override
	public void ReceiveMessage(MessageType t, boolean value) {
		clear();
	}
}
