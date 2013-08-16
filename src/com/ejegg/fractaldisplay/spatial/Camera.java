package com.ejegg.fractaldisplay.spatial;

import android.opengl.Matrix;
import android.util.Log;

public class Camera {
	private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] eyePosition = {0, 0, 8};
    private final float[] lookAt = {0, 0, 0};
    private final float[] up = {0, 1, 0, 1};
    private float[] mRotationAxis = new float[4];
    private float mRotationVelocity = 0f;
    private float zoom = 1;
    private float ratio;
    private static final float DECCELERATION = 0.95f;
	private static final float MIN_ROTATE_SPEED = 0.01f;
	
	public Camera() {
		mRotationVelocity = 0;
		Log.d("Camera", String.format("Up is %f, %f, %f", up[0], up[1], up[2] ));
	}
	
	public void scale(float scaleFactor) {
		zoom *= scaleFactor;
		Log.d("Camera", "zoom is " + zoom);
		setProjection();
		setViewMatrix();
	}
	
	public boolean isMoving() {
		return mRotationVelocity != 0;
	}
	
	public void setRotation(float[] rotationAxis, float speed) {
		mRotationAxis = rotationAxis;
		mRotationVelocity = speed;
	}
	
	public void spinStep() { //TODO: use time-based rotation instead of step-based
		float[] newRotate = new float[16];
		Matrix.setIdentityM(newRotate, 0);
		
		Matrix.rotateM(newRotate, 0, mRotationVelocity, mRotationAxis[0], mRotationAxis[1], mRotationAxis[2]);
		float[] newVec = new float[4];
		
		Matrix.multiplyMV(newVec, 0, newRotate, 0, up, 0);
		System.arraycopy(newVec, 0, up, 0, 4);
		
		float[] relativeEye = new float[] {eyePosition[0] - lookAt[0], eyePosition[1] - lookAt[1], eyePosition[2] - lookAt[2], 1};
		Matrix.multiplyMV(newVec, 0, newRotate, 0, relativeEye, 0);
		
		Vec.add(eyePosition, lookAt, newVec);
		setViewMatrix();
		
        mRotationVelocity *= DECCELERATION;
        
        if (Math.abs(mRotationVelocity) < MIN_ROTATE_SPEED) {
        	stop();
        }
	}
	
	private float[] intoScreen() {
		return new float[] {lookAt[0] - eyePosition[0], lookAt[1] - eyePosition[1], lookAt[2] - eyePosition[2]};
	}
	
	public void rotate(float angle)
	{
		float[] newRotate = new float[16];
		Matrix.setIdentityM(newRotate, 0);
		float[] axis = intoScreen();
		Vec.normalize(axis);
		Matrix.rotateM(newRotate, 0, -1 * angle, axis[0], axis[1], axis[2]);
		float[] oldUp = new float[4];
		System.arraycopy(up, 0, oldUp, 0, 4);
		Matrix.multiplyMV(up, 0, newRotate, 0, oldUp, 0);
		Log.d("Camera", String.format("Up is %f, %f, %f", up[0], up[1], up[2] ));
		setViewMatrix();
	}
		
	public void stop() {
		Log.d("Camera", "Stopping");
		mRotationVelocity = 0;
	}
	
	public float[] getMVPMatrix() {
		return mMVPMatrix;
	}
	
	public void setScreenDimensions(int width, int height){
		this.ratio = (float)width / (float)height;
		setProjection();
		setViewMatrix();
	}
	
	private void setProjection() {
		Matrix.frustumM(mProjMatrix, 0, -ratio / zoom, ratio / zoom, -1 / zoom, 1 / zoom, 5, 12);	
	}
	
	private void setViewMatrix() {
		
		Matrix.setLookAtM(mVMatrix, 0, 
				eyePosition[0], eyePosition[1], eyePosition[2], 
				lookAt[0], lookAt[1], lookAt[2], 
				up[0], up[1], up[2]);
        
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
	}

	public void spin(float dX, float dY, float velocity) {
		float[] intoScreen = intoScreen();
		float[] right = new float[3];
		Vec.cross(right,  intoScreen, up);
		float[] gestureVector = new float[] {dX * right[0] + dY * up[0], dX * right[1] + dY * up[1], dX * right[2] + dY * up[2]};
		Vec.cross(mRotationAxis, intoScreen, gestureVector);
		Vec.normalize(mRotationAxis);
		mRotationVelocity = velocity;
		Log.d("Camera", String.format("Flung! axis is (%f, %f, %f), velocity is %f",mRotationAxis[0], mRotationAxis[1], mRotationAxis[2], mRotationVelocity));
	}
}
