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
		FileOutputStream thumbStream = null;

		try {
			Log.d("Saver", saveName);
			File tempFile = File.createTempFile("frac_", ".png", saveDir);
			thumbStream = new FileOutputStream(tempFile);
			thumbnail.compress(CompressFormat.PNG, 100, thumbStream);
			thumbStream.close();
			String thumbnailPath = tempFile.getAbsolutePath();
			state.setThumbnailPath(thumbnailPath);

			ContentValues val = new ContentValues();
			val.put(FractalStateProvider.Items.NAME, saveName);
			val.put(FractalStateProvider.Items.TRANSFORM_COUNT, state.getNumTransforms());
			val.put(FractalStateProvider.Items.SERIALIZED_TRANSFORMS, state.getSerializedTransforms());
			val.put(FractalStateProvider.Items.LAST_UPDATED, System.currentTimeMillis());
			val.put(FractalStateProvider.Items.THUMBNAIL, thumbnailPath);
			contentResolver.insert(FractalStateProvider.CONTENT_URI, val);

			if (url != null) {
				FractalUploader uploader = new FractalUploader(messagePasser, url);
				uploader.execute(state);
			}
		}
		catch (Exception e) {
			Log.d("Saver", "Error saving: " + e.getMessage() + e.getStackTrace());
			return false;
		}
		return true;
	}

    protected void onPostExecute(Boolean success){
    	Log.d("Saver", "Done with result: " + success);
    	messagePasser.SendMessage(MessageType.STATE_SAVED, success);
    }
}