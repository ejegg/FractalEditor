package com.ejegg.android.fractaleditor.persist;

import com.ejegg.android.fractaleditor.spatial.RayCubeIntersection;
import com.ejegg.android.fractaleditor.spatial.Vec;

import android.opengl.Matrix;

public class FractalState {
	private int deviceId; //ID local to device (0 for unsaved)
	private int sharedId; //ID on shared site (0 if never uploaded)
	private String name;
	private String thumbnailPath;
	private int selectedTransform = NO_CUBE_SELECTED;
	private float[][] transforms;
	private final static float[][] axes = {{ 1, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 1}};
	
	public static final int NO_CUBE_SELECTED = -1;
	
	public FractalState(int deviceId, int sharedId, String name, String thumbnailPath, int numTransforms, float[][] transforms) {
		this.transforms = new float[numTransforms][16];
		
		this.deviceId = deviceId;
		this.sharedId = sharedId;
		this.name = name;
		this.thumbnailPath = thumbnailPath;
		
		if (transforms != null) {
			for (int i = 0; i < numTransforms; i++) {
				System.arraycopy(transforms[i], 0, this.transforms[i], 0, 16);
			}
		}
	}
	
	public FractalState clone() {
		FractalState state = new FractalState(getDeviceId(), getSharedId(), getName(), getThumbnailPath(), getNumTransforms(), transforms);
		state.setSelectedTransform(selectedTransform);
		return state;
	}
	
	public FractalState(int deviceId, int sharedId, String name, String thumbnailPath, int numTransforms, String serializedTransforms) {
		this(deviceId, sharedId, name, thumbnailPath, numTransforms, (float[][]) null);
		
		String[] splitTransforms = serializedTransforms.split(" ");
		transforms = new float[numTransforms][16];
		for (int t = 0; t< numTransforms; t++) {
			for (int u = 0; u < 16; u++) {
				transforms[t][u] = Float.parseFloat(splitTransforms[t * 16 + u]);
			}
		}
	}
	
	public boolean equals(FractalState other) {
		return this.getSerializedTransforms().equals(other.getSerializedTransforms());
	}

	public String getSerializedTransforms() {
		int numTransforms = getNumTransforms();
		StringBuilder builder = new StringBuilder();
		for (int t = 0; t < numTransforms; t++) {
			builder.append(serializeMatrix(transforms[t]));
			if (t != numTransforms - 1) {
				builder.append(" ");
			}
		}
		return builder.toString();
	}
	
	public static String serializeMatrix(float[] matrix) {
		StringBuilder builder = new StringBuilder();
		for (int u = 0; u < 16; u++) {
			builder.append(matrix[u]);
			if (u != 15) {
				builder.append(" ");
			}
		}
		return builder.toString();
	}

	public float[][] getTransforms() {
		return transforms;
	}
	
	public int getNumTransforms() {
		return transforms.length;
	}

	public int getSelectedTransform() {
		return selectedTransform;
	}

	public void setSelectedTransform(int selectedTransform) {
		this.selectedTransform = selectedTransform;
	}
	
	public boolean anyTransformSelected() {
		return selectedTransform != NO_CUBE_SELECTED;
	}
	
	public void clearSelection() {
		selectedTransform = NO_CUBE_SELECTED;
	}
	
	public void addTransform() {
		int transformCount = getNumTransforms();

		float[][] newTransforms = new float[transformCount + 1][16];
		
		for (int i = 0; i < transformCount; i++) {
			System.arraycopy(transforms[i], 0, newTransforms[i], 0, 16);
		}
		Matrix.setIdentityM(newTransforms[transformCount], 0);
		Matrix.scaleM(newTransforms[transformCount], 0, .5f, .5f, .5f);
		transforms = newTransforms;
	}
	
	public void removeSelectedTransform() {
		int transformCount = getNumTransforms();
		if (transformCount == 0 || ! anyTransformSelected()) {
			return;
		}
		if (transformCount == 1) {
			transforms = new float[0][16];
			return;
		}
		
		float[][] newTransforms = new float[transformCount - 1][16];

		int newI = 0;
		for (int i = 0; i < transformCount; i++) {
			if (i!= selectedTransform) {
				System.arraycopy(transforms[i], 0, newTransforms[newI++], 0, 16);
			}
		}
		transforms = newTransforms;
		clearSelection();
	}
	
	public void rotateSelectedTransform(float angle, float[] axis) {
		if (!anyTransformSelected()) {
			return;
		}
		
		float[] newRotate = new float[16];
		Matrix.setIdentityM(newRotate, 0);
	
		float[] tempRotate = new float[16];
				
		//Rotate identity matrix around the line going into the screen
		Matrix.rotateM(newRotate, 0, angle, axis[0], axis[1], axis[2]);
	
		float[] targetTransform = transforms[selectedTransform];
		
		float[] prevTranslation = new float[3];
		for (int i = 0; i < 3; i++) {
			prevTranslation[i] = targetTransform[12 + i];
		}
		
		Matrix.multiplyMM(tempRotate, 0, newRotate, 0, targetTransform, 0);
		for (int i = 0; i < 3; i++) {
			tempRotate[12 + i] = prevTranslation[i];
		}
		transforms[selectedTransform] = tempRotate;
	}

	public void scaleUniform(float scaleFactor) {
		Matrix.scaleM(transforms[selectedTransform], 0, scaleFactor, scaleFactor, scaleFactor);	
	}

	public void scaleLinear(float scaleFactor, float[] focus, float[] endPoint) {
		float[] rayInSpace = new float[3];
		Vec.sub(rayInSpace, endPoint, focus);
		
		float[] prevTranslation = new float[3];
		float[][] transformedAxes = new float[3][4];
		
		float[] targetTransform = new float[16];
		System.arraycopy(transforms[selectedTransform], 0, targetTransform, 0, 16);
		
		for (int i = 0; i < 3; i++) {
			//temporarily store translation components of selected transform and move it back to origin				
			prevTranslation[i] = targetTransform[12 + i];
			targetTransform[12 + i] = 0;
		}
		for (int i = 0; i < 3; i++) {
			//apply selected transform to x,y,z axes.
			Matrix.multiplyMV(transformedAxes[i], 0, targetTransform, 0, axes[i], 0);
			Vec.normalize(transformedAxes[i]);
		}

		int matchingAxisIndex = -1;
		float maxDot = 0;
		//find axis most closely aligned to scale direction line
		for (int i = 0; i < 3; i++) {
			float dot = Math.abs(Vec.dot(transformedAxes[i], rayInSpace));
			if (dot > maxDot) {
				maxDot = dot;
				matchingAxisIndex = i;
			}
		}
		
		//scale along matching axis.
		Matrix.scaleM(targetTransform, 0, 1 + (scaleFactor - 1) * axes[matchingAxisIndex][0], 
				1 + (scaleFactor - 1) * axes[matchingAxisIndex][1], 1 + (scaleFactor - 1) * axes[matchingAxisIndex][2]);
		
		//restore original cube translation
		for (int i = 0; i < 3; i++) {
			targetTransform[12 + i] = prevTranslation[i];
		}
		transforms[selectedTransform] = targetTransform;
	}

	public void moveSelectedTransform(float[] oldNear, float[] oldFar, float[] newNear, float[] newFar) {
		
		float[] targetTransform = new float[16];
		System.arraycopy(transforms[selectedTransform], 0, targetTransform, 0, 16);
		
		float minA = new RayCubeIntersection(oldNear, oldFar, targetTransform).getMinA();
		
		if (minA == RayCubeIntersection.NO_INTERSECTION) {
			return;
		}
		
		for (int i = 0; i < 3; i++) {
			targetTransform[12 + i] += newNear[i] - oldNear[i] + minA * ((newFar[i] - newNear[i]) - (oldFar[i] - oldNear[i]));  
		}
		transforms[selectedTransform] = targetTransform;
	}

	public int getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}

	public int getSharedId() {
		return sharedId;
	}

	public void setSharedId(int sharedId) {
		this.sharedId = sharedId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}

	public void setThumbnailPath(String thumbnailPath) {
		this.thumbnailPath = thumbnailPath;
	}
}
