package com.ejegg.fractaldisplay;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class ScaleModeButton extends Button{

	private static final int[] STATE_UNIFORM_SCALE_MODE = { R.attr.state_uniform_scale }; 
	private boolean isUniformScaleMode = true;
	
	public void setScaleMode(boolean isUniformScaleMode) {
		this.isUniformScaleMode = isUniformScaleMode;
	}
	
	public ScaleModeButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
	    if (isUniformScaleMode) {
	        mergeDrawableStates(drawableState, STATE_UNIFORM_SCALE_MODE);
	    }
	    return drawableState;
	}

}
