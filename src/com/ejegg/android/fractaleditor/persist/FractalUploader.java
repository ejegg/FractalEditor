package com.ejegg.android.fractaleditor.persist;

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
		try {
			FractalState state = params[0];
			connection = (HttpURLConnection)url.openConnection();
			byte[] postBytes = String.format(
					"name=%s&serializedTransforms=%s",
					URLEncoder.encode( state.getName(), "UTF-8" ),
					URLEncoder.encode( state.getSerializedTransforms(), "ASCII" )
			).getBytes("UTF-8");
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
