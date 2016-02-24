package com.ejegg.android.fractaleditor;

import com.ejegg.android.fractaleditor.persist.FractalStateProvider;
import com.ejegg.android.fractaleditor.persist.SavedFractalAdapter;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Surface;
import android.view.View;

public class GalleryActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	private RecyclerView fractalGallery;
	static final String[] PROJECTION = new String[] {
			FractalStateProvider.Items.NAME,
			FractalStateProvider.Items.THUMBNAIL,
			FractalStateProvider.Items._ID
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery);
		fractalGallery = (RecyclerView) findViewById(R.id.fractal_gallery);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, FractalStateProvider.CONTENT_URI,
                PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		SavedFractalAdapter adapter = new SavedFractalAdapter(this, cursor);
		fractalGallery.setAdapter(adapter);
		int numCols = 2;
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
			numCols = 4;
		}
		GridLayoutManager gridMgr = new GridLayoutManager(this, numCols);
		gridMgr.setOrientation(GridLayoutManager.VERTICAL);
		fractalGallery.setLayoutManager(gridMgr);
		findViewById(R.id.loadingText).setVisibility(View.GONE);
		fractalGallery.setVisibility(View.VISIBLE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// nothing!
	}
}
