package com.ejegg.fractaldisplay;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class FractalDisplayView extends GLSurfaceView{

	public FractalDisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEGLContextClientVersion(2);
	}
}
