package com.ejegg.android.fractaleditor;

import java.io.File;
import java.util.Arrays;

import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.persist.FractalSaver;
import com.ejegg.android.fractaleditor.persist.FractalState;
import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.render.MainRenderer;
import com.ejegg.android.fractaleditor.spatial.Camera;
import com.ejegg.android.fractaleditor.R;

import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class FractalEditActivity extends Activity implements
		ProgressListener, OnClickListener,
		DialogInterface.OnClickListener, MessagePasser.MessageListener {

	private ProgressBar progressBar;
	private FractalStateManager stateManager;
	private MessagePasser passer;
	private RenderModeManager renderManager;
	private FractalEditView view;
	private MainRenderer mainRenderer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		setButtons();

		FractalEditor appContext = (FractalEditor) getApplicationContext();
		passer = appContext.getMessagePasser();
		passer.Subscribe(this, MessageType.EDIT_MODE_CHANGED,
				MessageType.SCALE_MODE_CHANGED,
				MessageType.UNDO_ENABLED_CHANGED,
				MessageType.STATE_SAVED,
				MessageType.STATE_UPLOADED);

		stateManager = appContext.getStateManager();
		stateManager.setCalculationListener(this);

		Uri intentData = getIntent().getData();
		if (intentData != null) {
			if (intentData.getScheme().equals("content")) {
				// locally saved fractal
				stateManager.loadStateFromUri(getContentResolver(), intentData);
			} else {
				// web link opened in app
				stateManager.loadStateFromUri(intentData);
			}
		}
		setButtonStates();

		Camera camera = appContext.getCamera();

		mainRenderer = new MainRenderer(camera, stateManager, passer);

		view = (FractalEditView) findViewById(R.id.fractalCanvas);
		view.setRenderer(mainRenderer);
		view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		renderManager = new RenderModeManager(passer, stateManager, camera, view);

		progressBar = (ProgressBar) findViewById(R.id.computeProgress);

	}

	@Override
	protected void onPause() {
		super.onPause();
		view.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		view.onResume();
		renderManager.checkAccumulate();
	}

	private void setButtons() {
		for (int id : Arrays.asList(R.id.saveButton,
				R.id.undoButton, R.id.addButton, R.id.removeButton,
				R.id.modeButton, R.id.scaleModeButton)) {
			Button button = (Button) findViewById(id);// who's got the button?
			if (button == null) {
				//Log.d("DisplayActivity", "button " + id + " is null, can set click listener");
			} else {
				button.setOnClickListener(this);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}

	// ProgressListener callbacks
	public void started() {
		progressBar.setVisibility(View.VISIBLE);
	}

	public void progressed(int progress) {
		progressBar.setVisibility(View.VISIBLE);
		this.progressBar.setProgress(progress);
	}

	public void finished() {
		progressBar.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.modeButton:
			stateManager.toggleEditMode();
			break;
		case R.id.addButton:
			stateManager.addTransform();
			break;
		case R.id.removeButton:
			stateManager.removeSelectedTransform();
			break;
		case R.id.scaleModeButton:
			stateManager.toggleScaleMode();
			break;
		case R.id.undoButton:
			stateManager.undo();
			break;
		case R.id.saveButton:
			Dialog d = new Dialog(this);
			d.setContentView(R.layout.dialog_save);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// TODO: localize
			builder.setTitle(R.string.title_activity_save)
					.setView(
							getLayoutInflater().inflate(R.layout.dialog_save,
									(ViewGroup) getCurrentFocus()))
					.setPositiveButton(R.string.save_ok, this)
					.setNegativeButton(R.string.dialog_cancel, this);
			AlertDialog ad = builder.create();
			ad.show();
			EditText t = (EditText) ad.findViewById(R.id.save_name_entry);
			String currentName = stateManager.getState().getName();
			t.setText(currentName);
			t.setSelection(0, currentName.length());
			ad.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			break;
		default:
			break;
		}
	}

	private void setButtonStates() {
		boolean editMode = stateManager.isEditMode();
		((EditButton) findViewById(R.id.modeButton)).setEditMode(editMode);
		((ScaleModeButton) findViewById(R.id.scaleModeButton))
				.setScaleMode(stateManager.isUniformScaleMode());
		((Button) findViewById(R.id.undoButton)).setEnabled(editMode
				&& stateManager.isUndoEnabled());
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		if (which == DialogInterface.BUTTON_NEGATIVE) {
			Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
			return;
		}
		
		EditText t = (EditText) ((AlertDialog) dialog).findViewById(R.id.save_name_entry);
		CheckBox cb = (CheckBox) ((AlertDialog) dialog).findViewById(R.id.upload_checkbox);
		String name = t.getText().toString();
		File saveDir = getApplicationContext().getFilesDir();
		String uploadUrl = null;
		FractalState state = stateManager.getState();
		state.setName(name);

		try {
			if (cb.isChecked()) {
				uploadUrl = getResources().getString(R.string.upload_url);
			}
			FractalSaver saver = new FractalSaver(
					getContentResolver(),
					passer,
					mainRenderer,
					saveDir,
					uploadUrl
			);
			saver.execute(state);
		} catch (NotFoundException e) {
			Toast.makeText( this, "Upload failed - URL not found", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void ReceiveMessage(MessageType type, boolean value) {
		switch(type) {
		case STATE_SAVED:
			Toast.makeText(this, value ? "Fractal saved" : "Error saving fractal", Toast.LENGTH_LONG).show();
			break;
		case STATE_UPLOADED:
			Toast.makeText(this, value ? "Fractal uploaded" : "Error uploading fractal", Toast.LENGTH_LONG).show();
			break;
		default:
			setButtonStates();
		}
	}
}
