package com.ejegg.fractaldisplay.spatial;

import android.opengl.Matrix;
import android.util.Log;

public class RayCubeIntersection {
	private float minA = NO_INTERSECTION;
	public static final float NO_INTERSECTION = 999999f;
	
	public RayCubeIntersection(float[] origNear, float[] origFar, float[] transform) {
		float[] inverse = new float[16];
		float[] near = new float[4];
		float[] far = new float[4];
		Matrix.invertM(inverse, 0, transform, 0);
		Matrix.multiplyMV(near, 0, inverse, 0, origNear, 0);
		Matrix.multiplyMV(far, 0, inverse, 0, origFar, 0);
		
		intersect(near[0], far[0], near[1], far[1], near[2], far[2], -1.0f);
		intersect(near[0], far[0], near[1], far[1], near[2], far[2], 1.0f);
		intersect(near[1], far[1], near[2], far[2], near[0], far[0], -1.0f);
		intersect(near[1], far[1], near[2], far[2], near[0], far[0], 1.0f);
		intersect(near[2], far[2], near[0], far[0], near[1], far[1], -1.0f);
		intersect(near[2], far[2], near[0], far[0], near[1], far[1], 1.0f);
	}
	
	public float getMinA() {
		return minA;
	}
	
	private void intersect(float n1, float f1, float n2, float f2, float n3, float f3, float dist) {
		float d1 = n1 - f1;
		if (d1 == 0.0f) {
			//Log.d("intersection", "d1 is zero");
			return;
		}
		float a = (n1 - dist) / d1;
		//Log.d("intersection", "a is " + a);
		float z2 = n2 + a * (f2 - n2);
		if (z2 > 1.0f || z2 < -1.0f) {
			//Log.d("intersection", "z2 is " + z2);
			return;
		}
		float z3 = n3 + a * (f3 - n3);
		if (z3 > 1.0f || z3 < -1.0f) {
			//Log.d("intersection", "z3 is " + z3);
			return;
		}
		if (a < minA) {
			minA = a;
		}
	}
}
