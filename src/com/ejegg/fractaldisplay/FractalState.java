package com.ejegg.fractaldisplay;

public class FractalState {
	private int id;
	private int numTransforms;
	private float[][] transforms;
	
	public FractalState(int numTransforms, float[][] transforms) {
		this.numTransforms = numTransforms;
		this.transforms = new float[numTransforms][16];
		if (transforms != null) {
			for (int i = 0; i < numTransforms; i++) {
				System.arraycopy(transforms[i], 0, this.transforms[i], 0, 16);
			}
		}
	}
	
	public FractalState(int numTransforms, String serializedTransforms) {
		this(numTransforms, (float[][]) null);
		String[] splitTransforms = serializedTransforms.split(" ");
		transforms = new float[numTransforms][16];
		for (int t = 0; t< numTransforms; t++) {
			for (int u = 0; u < 16; u++) {
				transforms[t][u] = Float.parseFloat(splitTransforms[t * 16 + u]);
			}
		}
	}
	
	public boolean equals(FractalState other) {
		return this.numTransforms == other.numTransforms &&
				this.getSerializedTransforms().equals(other.getSerializedTransforms());
	}

	public String getSerializedTransforms() {
		StringBuilder builder = new StringBuilder();
		for (int t = 0; t< numTransforms; t++) {
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

	public void getTransforms(float[][] transforms) {
		System.arraycopy(this.transforms, 0, transforms, 0, this.transforms.length);
	}
	
	public int getNumTransforms() {
		return numTransforms;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int value) {
		id = value;
	}
}
