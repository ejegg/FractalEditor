package com.ejegg.fractaldisplay;

import android.opengl.Matrix;
import android.util.Log;

public class Camera {
	private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] rotateMatrix = new float[16];
    private final float[] inverseRotateMatrix = new float[16];
    private final float[] inverseProjectionMatrix = new float[16];
	private final float[] transposeRotation = new float[16];
    private final float[] zAxis = new float[4];
    private static final float[] staticZAxis = {0, 0, -1, 1};
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
	}
	
	public void scale(float scaleFactor) {
		zoom *= scaleFactor;
		Log.d("Camera", "zoom is " + zoom);
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

		float[] tempRotate = new float[16];

		Matrix.rotateM(newRotate, 0, -1 * angle, zAxis[0], zAxis[1], zAxis[2]);

		Matrix.multiplyMM(tempRotate, 0, newRotate, 0, rotateMatrix, 0);
		System.arraycopy(tempRotate, 0, rotateMatrix, 0, 16);
		setRotationMatrices();
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
		setViewMatrix();
	}
	
	private void setViewMatrix() {
		
		Matrix.frustumM(mProjMatrix, 0, -ratio / zoom, ratio / zoom, -1 / zoom, 1 / zoom, 5, 12);
		
		Matrix.setLookAtM(mVMatrix, 0, 0, 0, 8, 0f, 0f, 1f, 0f, 1.0f, 0.0f);
        
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
}
