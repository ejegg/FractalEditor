package com.ejegg.android.fractaleditor;

import com.ejegg.android.fractaleditor.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class EditButton extends Button{

	private static final int[] STATE_EDIT_MODE = { R.attr.state_edit };
	private static final int[] STATE_RENDER_AVAILABLE = { R.attr.render_available };
	private boolean isEditMode = false;
	private boolean isRenderAvailable = false;

	public void setEditMode(boolean isEditMode) {
		this.isEditMode = isEditMode;
	}
	public void setRenderAvailable(boolean isRenderAvailable) {
		this.isRenderAvailable = isRenderAvailable;
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
		if (isRenderAvailable) {
			mergeDrawableStates(drawableState, STATE_RENDER_AVAILABLE);
		}
	    return drawableState;
	}

}
