package com.ejegg.android.fractaleditor;

import com.ejegg.android.fractaleditor.persist.FractalStateProvider;
import com.ejegg.android.fractaleditor.R;

import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LoadActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, OnItemLongClickListener, OnClickListener {

	private SimpleCursorAdapter adapter;
    static final String[] PROJECTION = new String[] {FractalStateProvider.Items.NAME, FractalStateProvider.Items._ID};
	private long selectedId = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        adapter = new SimpleCursorAdapter(
                this, 
                R.layout.saved_fractal,
                null,                                              // Pass in the cursor to bind to.
                PROJECTION,           // Array of cursor columns to bind to.
                new int[] {R.id.saved_fractal_name}, 0);
        ListView lv = (ListView)findViewById(R.id.load_list);
        lv.setAdapter(adapter);
        lv.setClickable(true);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);
        getSupportLoaderManager().initLoader(0, null, (LoaderManager.LoaderCallbacks<Cursor>) this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, FractalStateProvider.CONTENT_URI,
                PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
		Intent result = new Intent("com.ejegg.android.fractaleditor.LoadActivity", getUri(id));
		//Log.d("load", "clicked item, sending result: " + FractalStateProvider.CONTENT_URI.toString() + "/" + id);
		setResult(RESULT_OK, result);
		finish();
	}

	private Uri getUri(long id) {
		return Uri.parse(FractalStateProvider.CONTENT_URI.toString() + "/" + id);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> a, View v, int position, long id) {
		selectedId = id;
		TextView clickedText = (TextView)v.findViewById(R.id.saved_fractal_name);
				
		Dialog d = new Dialog(this);
		d.setContentView(R.layout.dialog_save);
		
		View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_delete, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_activity_delete)
				.setView(dialogLayout)
				.setPositiveButton(R.string.delete_ok, this)
				.setNegativeButton(R.string.dialog_cancel, this);
		AlertDialog ad = builder.create();
		ad.show();
		TextView deletePrompt = (TextView) ad.findViewById(R.id.delete_dialog_prompt);
		deletePrompt.setText(String.format(deletePrompt.getText().toString(), clickedText.getText()));
		
		return false;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		if (which == DialogInterface.BUTTON_NEGATIVE) {
			Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
			return;
		}
		
		getContentResolver().delete(getUri(selectedId), null, null);
		getSupportLoaderManager().restartLoader(0, null, this);
		adapter.notifyDataSetChanged();
		Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
	}
}
