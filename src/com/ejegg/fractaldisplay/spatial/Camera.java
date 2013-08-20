package com.ejegg.fractaldisplay.spatial;

import android.opengl.Matrix;
import android.util.Log;

public class Camera {
	private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mInverseProjectionMatrix = new float[16];
    private boolean inverseProjectionNeedsRecalculating = true;
    private final float[] mVMatrix = new float[16];
    private final float[] eyePosition = {0, 0, 9};
    private final float[] lookAt = {0, 0, 0};
    private final float[] up = {0, 1, 0, 1};
    private float[] mRotationAxis = new float[4];
    private float mRotationVelocity = 0f;
    private float zoom = 1;
    private float ratio;
    private static final float DECELERATION = 0.95f;
	private static final float MIN_ROTATE_SPEED = 0.01f;
	private int width;
	private int height;
	private long lastMoveId = 0;
	
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
		
        mRotationVelocity *= DECELERATION;
        
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
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setScreenDimensions(int width, int height){
		this.width = width;
		this.height = height;
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
        inverseProjectionNeedsRecalculating = true;
        lastMoveId++;
	}
	
	public long getLastMoveId() {
		return lastMoveId;
	}

	public float[] getInverseProjectionMatrix() {
		if (inverseProjectionNeedsRecalculating) {
			Matrix.invertM(mInverseProjectionMatrix, 0, mMVPMatrix, 0);
			inverseProjectionNeedsRecalculating = false;
		}
		return mInverseProjectionMatrix;
	}
	
	public void spin(float dX, float dY, float velocity) {
		float[] intoScreen = intoScreen();
		float[] right = new float[3];
		Vec.cross(right,  intoScreen, up);
		float normX = dX / width;
		float normY = dY / height;
		float[] gestureVector = new float[] {normX * right[0] + normY * up[0], normX * right[1] + normY * up[1], normX * right[2] + normY * up[2]};
		Vec.cross(mRotationAxis, intoScreen, gestureVector);
		Vec.normalize(mRotationAxis);
		mRotationVelocity = velocity;
		Log.d("Camera", String.format("Flung! axis is (%f, %f, %f), velocity is %f",mRotationAxis[0], mRotationAxis[1], mRotationAxis[2], mRotationVelocity));
	}
	
	public void getTouchRay(float[] nearPoint, float[] farPoint, float x, float y)
	{
		getTouchPoint(nearPoint, x, y, 0);
		getTouchPoint(farPoint, x, y, 1);	
	}
	
	public void getTouchPoint(float[] point, float x, float y, float depth)
	{
		float[] screenTouchPoint = {2.0f * x / width - 1.0f, -2.0f * y / height + 1.0f, depth, 1};

		Matrix.multiplyMV(point, 0, getInverseProjectionMatrix(), 0, screenTouchPoint, 0);
		
		float scale = 1.0f / point[3];
		
		for (int i = 0; i < 4; i++) {
			point[i] *= scale;
		}
	}
	
	public static void getInterpolatedCoordinates(float[] coords, float[] nearPoint, float[] farPoint, float z)
	{
		float z1 = nearPoint[2];
		float z2 = farPoint[2];
		
		float t = (z -z1) / (z2 - z1);
		
		coords[0] = nearPoint[0] + t * (farPoint[0] - nearPoint[0] );
		coords[1] = nearPoint[1] + t * (farPoint[1] - nearPoint[1] );
		coords[2] = z;
		coords[3] = 1.0f;
	}
}
