package com.ejegg.android.fractaleditor.persist;

import java.io.File;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.util.Log;

import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.render.MainRenderer;

public class FractalSaver extends AsyncTask<FractalState, Integer, Boolean> {

	private final ContentResolver contentResolver;
	private final MessagePasser messagePasser;
	private final MainRenderer renderer;
	private final File saveDir;
	private final String url;

	public FractalSaver(ContentResolver contentResolver,
			MessagePasser messagePasser,
			MainRenderer renderer,
			File saveDir,
			String url) {

		this.contentResolver = contentResolver;
		this.messagePasser = messagePasser;
		this.renderer = renderer;
		this.saveDir = saveDir;
		this.url = url;
	}

	@Override
	protected Boolean doInBackground(FractalState... params) {
		FractalState state = params[0];
		String saveName = state.getName();
		Bitmap thumbnail = renderer.getThumbnail();

		try {
			Log.d("Saver", saveName);
			if ( thumbnail != null ) {
				File tempFile = File.createTempFile("frac_", ".png", saveDir);
				FileOutputStream thumbStream = new FileOutputStream(tempFile);
				thumbnail.compress(CompressFormat.PNG, 100, thumbStream);
				thumbStream.close();
				state.setThumbnailPath(tempFile.getAbsolutePath());
			}
			ContentValues val = new ContentValues();
			val.put(FractalStateProvider.Items.NAME, saveName);
			val.put(FractalStateProvider.Items.SHARED_ID, state.getSharedId());
			val.put(FractalStateProvider.Items.TRANSFORM_COUNT, state.getNumTransforms());
			val.put(FractalStateProvider.Items.SERIALIZED_TRANSFORMS, state.getSerializedTransforms());
			val.put(FractalStateProvider.Items.LAST_UPDATED, System.currentTimeMillis());
			val.put(FractalStateProvider.Items.THUMBNAIL, state.getThumbnailPath());
			contentResolver.insert(FractalStateProvider.CONTENT_URI, val);

			if (url != null) {
				// Think I need to post this as a runnable to the main thread
				FractalUploader uploader = new FractalUploader(messagePasser, contentResolver, url);
				uploader.execute(state);
			}
		}
		catch (Exception e) {
			Log.d("Saver", "Error saving: " + e.getMessage() + e.getStackTrace());
			return false;
		}
		thumbnail.recycle();
		return true;
	}

    protected void onPostExecute(Boolean success){
    	Log.d("Saver", "Done with result: " + success);
    	messagePasser.SendMessage(MessageType.STATE_SAVED, success);
    }
}