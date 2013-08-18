package com.ejegg.fractaldisplay.spatial;

import android.opengl.Matrix;

public class RayCaster {
	
	private int width, height;
	private float[] inverseProjectionMatrix;
	
	public RayCaster(int width, int height, float[] inverseProjectionMatrix) {
		this.width = width;
		this.height = height;
		this.inverseProjectionMatrix = inverseProjectionMatrix;
	}
	
	public void getTouchRay(float[] nearPoint, float[] farPoint, float x, float y)
	{
		getTouchPoint(nearPoint, x, y, 0);
		getTouchPoint(farPoint, x, y, 1);	
	}
	
	public void getTouchPoint(float[] point, float x, float y, float depth)
	{
		float[] screenTouchPoint = {2.0f * x / width - 1.0f, -2.0f * y / height + 1.0f, depth, 1};

		Matrix.multiplyMV(point, 0, inverseProjectionMatrix, 0, screenTouchPoint, 0);
		
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
