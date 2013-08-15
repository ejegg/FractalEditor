package com.ejegg.fractaldisplay.spatial;

import android.opengl.Matrix;
import android.util.Log;

import com.ejegg.fractaldisplay.touch.MotionEventSubscriber;

public class Camera implements MotionEventSubscriber{
	private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] rotateMatrix = new float[16];
    private final float[] inverseRotateMatrix = new float[16];
    private final float[] inverseProjectionMatrix = new float[16];
	private final float[] transposeRotation = new float[16];
    private final float[] zAxis = new float[4];
    private final float[] eyePosition = {0, 0, 8};
    private final float[] lookAt = {0, 0, 1};
    private final float[] up = {0, 1, 0, 1};
    private static final float[] staticZAxis = {0, 0, -1, 1};
    private static final float[] staticYAxis = {0, 1, 0, 1};
    private float[] mRotationAxis = new float[4];
    private float mRotationVelocity = 0f;
    private float zoom = 1;
    private float ratio;
    private static final float DECCELERATION = 0.95f;
	private static final float MIN_ROTATE_SPEED = 0.01f;
	
	public Camera() {
        Matrix.setIdentityM(rotateMatrix, 0);
        Matrix.transposeM(transposeRotation, 0, rotateMatrix, 0);
		Matrix.invertM(inverseRotateMatrix,  0, transposeRotation, 0);
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
		Matrix.rotateM(rotateMatrix, 0, mRotationVelocity, mRotationAxis[0], mRotationAxis[1], mRotationAxis[2]);

		setRotationMatrices();
        	        
        mRotationVelocity *= DECCELERATION;
        
        if (Math.abs(mRotationVelocity) < MIN_ROTATE_SPEED) {
        	stop();
        }
	}
	
	public void rotate(float angle)
	{
		float[] newRotate = new float[16];
		Matrix.setIdentityM(newRotate, 0);
		float[] axis = new float[3];
		Vec.sub(axis, lookAt, eyePosition);
		Vec.normalize(axis);
		Matrix.rotateM(newRotate, 0, -1 * angle, axis[0], axis[1], axis[2]);
		float[] oldUp = new float[4];
		System.arraycopy(up, 0, oldUp, 0, 4);
		Matrix.multiplyMV(up, 0, newRotate, 0, oldUp, 0);
		Log.d("Camera", String.format("Up is %f, %f, %f", up[0], up[1], up[2] ));
		setViewMatrix();
	}
		
	public void stop() {
		mRotationVelocity = 0;
	}
	
	public float[] getInverseProjectionMatrix() {
		return inverseProjectionMatrix;
	}
	public float[] getRotateMatrix() {
		return rotateMatrix;
	}
	public float[] getInverseRotateMatrix() {
		return inverseRotateMatrix;
	}
	public float[] getRotatedMVPMatrix() {
		float[] rotatedMatrix = new float[16];
		Matrix.multiplyMM(rotatedMatrix, 0, mMVPMatrix, 0, rotateMatrix, 0);
		return rotatedMatrix;
	}
	public float[] getMVPMatrix() {
		return mMVPMatrix;
	}
	public float[] getTransposeRotation() {
		return transposeRotation;
	}
	
	public void setRatio(float ratio){
		this.ratio = ratio;
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
        
        Matrix.invertM(inverseProjectionMatrix, 0, mMVPMatrix, 0);
        
	}
	
	private void setRotationMatrices() {
		Matrix.transposeM(transposeRotation, 0, rotateMatrix, 0);
		Matrix.invertM(inverseRotateMatrix,  0, transposeRotation, 0); //compute this once instead of at each drag segment;
		Matrix.multiplyMV(zAxis, 0, inverseRotateMatrix, 0, staticZAxis, 0);
	}

	public float[] getZAxis() {
		return zAxis;
	}

	@Override
	public void tap(float screenX, float screenY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void longPress(float screenX, float screenY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drag(float oldScreenX, float oldScreenY, float newScreenX,
			float newScreenY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scale(float scaleFactor, float spanX, float spanY) {
		scale(scaleFactor);		
	}

	@Override
	public void rotate(float angle, float focusScreenX, float focusScreenY) {
		Log.d("Camera", "Rotating " + angle);
		rotate(angle);
	}

	@Override
	public void fling(float oldScreenX, float oldScreenY, float newScreenX,
			float newScreenY, float velocityX, float velocityY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void down() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void up() {
		// TODO Auto-generated method stub
		
	}
}
