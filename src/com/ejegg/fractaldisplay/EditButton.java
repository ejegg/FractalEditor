package com.ejegg.fractaldisplay;

import com.ejegg.fractaldisplay.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class EditButton extends Button{

	private static final int[] STATE_EDIT_MODE = { R.attr.state_edit }; 
	private boolean isEditMode = false;
	
	public void setEditMode(boolean isEditMode) {
		this.isEditMode = isEditMode;
	}
	
	public EditButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
	    if (isEditMode) {
	        mergeDrawableStates(drawableState, STATE_EDIT_MODE);
	    }
	    return drawableState;
	}

}
