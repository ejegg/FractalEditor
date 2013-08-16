package com.ejegg.fractaldisplay.spatial;

import android.util.FloatMath;

public class Vec {

	public static void cross(float[] result, float[] vec1, float[] vec2)
	{
		result[0] = vec1[1] * vec2[2] - vec2[1] * vec1[2];
		result[1] = vec1[2] * vec2[0] - vec2[2] * vec1[0];
		result[2] = vec1[0] * vec2[1] - vec2[0] * vec1[1];
	}
	
	public static float dot(float[] vec1, float[] vec2)
	{
		return vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2]; 
	}
	
	public static void sub(float[] result, float[] vec1, float[] vec2)
	{
		result[0] = vec1[0] - vec2[0];
		result[1] = vec1[1] - vec2[1];
		result[2] = vec1[2] - vec2[2];
	}
	
	public static void add(float[] result, float[] vec1, float[] vec2)
	{
		result[0] = vec1[0] + vec2[0];
		result[1] = vec1[1] + vec2[1];
		result[2] = vec1[2] + vec2[2];
	}

	public static float magnitude(float[] vector)
	{
		float total = 0f;
		for (int i = 0; i < vector.length && i < 3; i++){
			total += vector[i] * vector[i];
		}
		return FloatMath.sqrt(total);
	}

	public static void normalize(float[] vector)
	{
		float mag = magnitude(vector);
		if (mag == 0.0f) 
			return;
		
		mag = 1/mag;
		
		for (int i = 0; i < vector.length && i < 3; i++){
			vector[i] *= mag;
		}
	}
}
