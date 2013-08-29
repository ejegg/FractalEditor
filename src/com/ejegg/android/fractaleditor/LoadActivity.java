package com.ejegg.android.fractaleditor;

import com.ejegg.android.fractaleditor.persist.FractalStateProvider;
import com.ejegg.android.fractaleditor.R;

import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ListView;

public class LoadActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

	private SimpleCursorAdapter adapter;
    static final String[] PROJECTION = new String[] {FractalStateProvider.Items.NAME, FractalStateProvider.Items._ID};
	
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
		Intent result = new Intent("com.ejegg.android.fractaleditor.LoadActivity", Uri.parse(FractalStateProvider.CONTENT_URI.toString() + "/" + id));
		//Log.d("load", "clicked item, sending result: " + FractalStateProvider.CONTENT_URI.toString() + "/" + id);
		setResult(RESULT_OK, result);
		finish();
	}
}
