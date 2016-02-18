package com.ejegg.android.fractaleditor.persist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

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
	private final URL url;

	public FractalSaver(ContentResolver contentResolver,
			MessagePasser messagePasser,
			MainRenderer renderer,
			File saveDir,
			String url) throws MalformedURLException {

		this.contentResolver = contentResolver;
		this.messagePasser = messagePasser;
		this.renderer = renderer;
		this.saveDir = saveDir;
		if (url == null) {
			this.url = null;
		} else {
			this.url = new URL(url);
		}
	}

	@Override
	protected Boolean doInBackground(FractalState... params) {
		FractalState state = params[0];
		String saveName = state.getName();
		Bitmap thumbnail = renderer.getThumbnail();

		String transforms = state.getSerializedTransforms();
		FileOutputStream thumbStream = null;

		try {
			Log.d("Saver", saveName);
			File tempFile = File.createTempFile("frac_", ".png", saveDir);
			thumbStream = new FileOutputStream(tempFile);
			thumbnail.compress(CompressFormat.PNG, 100, thumbStream);

			ContentValues val = new ContentValues();
			val.put(FractalStateProvider.Items.NAME, saveName);
			val.put(FractalStateProvider.Items.TRANSFORM_COUNT, state.getNumTransforms());
			val.put(FractalStateProvider.Items.SERIALIZED_TRANSFORMS, transforms);
			val.put(FractalStateProvider.Items.LAST_UPDATED, System.currentTimeMillis());
			val.put(FractalStateProvider.Items.THUMBNAIL, tempFile.getAbsolutePath());
			contentResolver.insert(FractalStateProvider.CONTENT_URI, val);

			if (url != null) {
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				byte[] postBytes = String.format(
						"name=%s&serializedTransforms=%s",
						URLEncoder.encode( saveName, "UTF-8" ),
						URLEncoder.encode( transforms, "ASCII" )
				).getBytes("UTF-8");
				try {
					connection.setDoOutput(true);
					connection.setRequestProperty("Accept-Charset", "UTF-8");
					connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					connection.setRequestProperty("Content-Length", Integer.toString(postBytes.length));

					OutputStream out = connection.getOutputStream();
					out.write(postBytes);

					int responseLength = Math.min(1000, Integer.parseInt(connection.getHeaderField("Content-Length")));
					byte[] responseBuffer = new byte[responseLength];
					connection.getInputStream().read(responseBuffer);
					String response = new String(responseBuffer, "UTF-8");
					Log.d("Saver", "Attempted upload, got response: " + response);
					if (response.startsWith("Error")) {
						throw new Exception(response);
					}
				}
				catch( Exception e ) {
					Log.e("Saver", "Exception trying to upload: " + e.getMessage());
				}
				finally {
					connection.disconnect();
				}
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