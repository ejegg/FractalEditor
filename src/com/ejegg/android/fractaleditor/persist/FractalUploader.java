package com.ejegg.android.fractaleditor.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;

import org.json.JSONObject;

public class FractalUploader extends AsyncTask<FractalState, String, Boolean>{
	private MessagePasser passer;
	private final ContentResolver resolver;
	private URL url;

	public FractalUploader(MessagePasser passer, ContentResolver resolver, String url) throws MalformedURLException {
		this.passer = passer;
		this.resolver = resolver;
		this.url = new URL(url);
	}

	@Override
	protected Boolean doInBackground(FractalState... params) {
		HttpURLConnection connection = null;
		Boolean success = true;
		FractalState state = params[0];
		String thumbnailPath = state.getThumbnailPath();
		int newSharedId = 0;

		try {
			File thumbnail = new File(thumbnailPath);
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=X*X*X*X*X*X*X*X");

			OutputStream out = connection.getOutputStream();
			String boundary = "X*X*X*X*X*X*X*X";
			String fieldFormat = "--%s\r\n" +
					"Content-Disposition: form-data; name=\"%s\"\r\n" +
					"Content-Type: text/plain; charset=\"UTF-8\"\r\n\r\n" +
					"%s\r\n";
			String fileFormat = "--%s\r\n" +
					"Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n" +
					"Content-Type: image/png\r\n" +
					"Content-Transfer-Encoding: binary\r\n\r\n";
			String formFields = String.format(fieldFormat + fieldFormat + fileFormat,
					boundary,
					"name", state.getName(),
					boundary,
					"serializedTransforms", state.getSerializedTransforms(),
					boundary,
					"thumbnail", thumbnail.getName());
			out.write(formFields.getBytes("UTF-8"));
			out.flush();
			FileInputStream thumbStream = new FileInputStream(thumbnail);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = thumbStream.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				// TODO: compare total read with file size, post progress
			}
			out.write(String.format("\r\n--%s--\r\n", boundary).getBytes("UTF-8"));
			out.flush();
			out.close();
			thumbStream.close();

			// TODO: something with connection.getResponseCode();
			//int len = connection.getContentLength();
			int responseLength = 1000;//Math.min(1000, connection.getContentLength());
			byte[] responseBuffer = new byte[responseLength];
			bytesRead = connection.getInputStream().read(responseBuffer);
			byte[] truncated = new byte[bytesRead];
			System.arraycopy(responseBuffer, 0, truncated, 0, bytesRead);
			String response = new String(truncated, "UTF-8");
			Log.i("Saver", "Attempted upload, got response: " + response);
			if (response.startsWith("Error")) {
				throw new Exception(response);
			}
			JSONObject json = new JSONObject(response);
			int returnedId = json.getInt("id");
			if (state.getSharedId() != returnedId) {
				newSharedId = returnedId;
			}
		}
		catch( Exception e ) {
			Log.e("Uploader", "Exception trying to upload: " + e.getMessage());
			success = false;
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		if (newSharedId > 0) {
			try{
				ContentValues v = new ContentValues();
				v.put(FractalStateProvider.Items.SHARED_ID, newSharedId);
				Uri uri = ContentUris.withAppendedId(
					FractalStateProvider.CONTENT_URI,
					state.getDeviceId()
				);
				int rowsUpdated = resolver.update(uri, v, null, null);
				Log.d("Uploader", String.format("Updated shared id for local ID %d to %d, %d rows updated", state.getDeviceId(), newSharedId, rowsUpdated));
				state.setSharedId(newSharedId);
			}
			catch( Exception e ) {
				Log.e("Uploader", "Exception updating shared ID: " + e.getMessage());
			}
		}
		return success;
	}

	@Override
    protected void onPostExecute(Boolean success){
    	Log.d("Uploader", "Done with result: " + success);
    	passer.SendMessage(MessageType.STATE_UPLOADED, success);
    }
}
