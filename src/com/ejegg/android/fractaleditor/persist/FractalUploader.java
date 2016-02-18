package com.ejegg.android.fractaleditor.persist;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.os.AsyncTask;
import android.util.Log;

import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;

public class FractalUploader extends AsyncTask<FractalState, String, Boolean>{
	private MessagePasser passer;
	private URL url;

	public FractalUploader(MessagePasser passer, String url) throws MalformedURLException {
		this.passer = passer;
		this.url = new URL(url);
	}

	@Override
	protected Boolean doInBackground(FractalState... params) {
		HttpURLConnection connection = null;
		Boolean success = true;
		FractalState state = params[0];
		String thumbnailPath = state.getThumbnailPath();

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
			Log.e("Uploader", "Exception trying to upload: " + e.getMessage());
			success = false;
		}
		finally {
			if (connection != null) {
				connection.disconnect();
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
