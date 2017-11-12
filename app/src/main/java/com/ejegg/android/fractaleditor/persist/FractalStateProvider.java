package com.ejegg.android.fractaleditor.persist;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.provider.BaseColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ejegg.android.fractaleditor.R;

public class FractalStateProvider extends ContentProvider {
//lots of this is adapted from the npr android app, apache licensed (can be added to GPLv3 projects)
	private FractalDatabaseHelper helper;
	public static final Uri CONTENT_URI = Uri.parse("content://com.ejegg.android.fractaleditor.persist.FractalState");
	private static final String DATABASE_NAME = "fractaleditor";
	private static final String TABLE_NAME = "fractalstates";
    private static final int DATABASE_VERSION = 2;

	@Override
	public boolean onCreate() {
		helper = new FractalDatabaseHelper(getContext());
		return true;
	}
    
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
		String realSelection = getSelectionFromId(uri, selection);
		return db.delete(TABLE_NAME, realSelection, selectionArgs);
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = helper.getWritableDatabase();
	    long id = db.insertWithOnConflict(TABLE_NAME, Items.NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
	    return ContentUris.withAppendedId(uri, id);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = helper.getReadableDatabase();
	    String realSelection = getSelectionFromId(uri, selection);

	    Cursor result = db.query(TABLE_NAME, projection, realSelection,
	        selectionArgs, null, null, sortOrder);
	    return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
	    String realSelection = getSelectionFromId(uri, selection);
	    return db.update(TABLE_NAME, values, realSelection, selectionArgs);
	}
	
	public static class Items implements BaseColumns {
	    public static final String SHARED_ID = "sharedId";
	    public static final String NAME = "name";
	    public static final String TRANSFORM_COUNT = "transformCount";
	    public static final String SERIALIZED_TRANSFORMS = "serializedTransforms";
	    public static final String THUMBNAIL = "thumbnail";
	    public static final String LAST_UPDATED = "lastUpdated";
	    public static final String[] COLUMNS = {SHARED_ID, NAME, TRANSFORM_COUNT, SERIALIZED_TRANSFORMS, THUMBNAIL, LAST_UPDATED};
	    public static final String[] ALL_COLUMNS = {BaseColumns._ID, SHARED_ID, NAME, TRANSFORM_COUNT, SERIALIZED_TRANSFORMS, THUMBNAIL, LAST_UPDATED};
	}

	private String getSelectionFromId(Uri uri, String selection) {
	    long id = ContentUris.parseId(uri);
	    if (id == -1) {
	      return selection;
	    }
	    String realSelection = selection == null ? "" : selection + " and ";
	    realSelection += Items._ID + " = " + id;
	    return realSelection;
	}

	protected static class FractalDatabaseHelper extends SQLiteOpenHelper {

		private String[][] demoFractals;
		private AssetManager manager;
		private File saveDir;

		private static final String FRACTAL_TABLE_CREATE =
				"CREATE TABLE " + TABLE_NAME + " (" +
						Items._ID + " INTEGER PRIMARY KEY, " +
						Items.SHARED_ID + " INTEGER, " +
						Items.NAME + " TEXT UNIQUE, " +
						Items.TRANSFORM_COUNT + " INTEGER, " +
						Items.SERIALIZED_TRANSFORMS + " TEXT, " +
						Items.THUMBNAIL + " TEXT, " +
						Items.LAST_UPDATED + " INTEGER);";

		public FractalDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			manager = context.getAssets();
			saveDir = context.getFilesDir();
			Resources r = context.getResources();
			demoFractals = new String[][]{
				{
					r.getString(R.string.sierpinski_name),
					r.getString(R.string.sierpinski_transforms),
					"sierpinski.png"
				},
				{
					r.getString(R.string.maple_name),
					r.getString(R.string.maple_transforms),
					"maple.png"
				},
				{
					r.getString(R.string.spleenwort_name),
					r.getString(R.string.spleenwort_transforms),
					"spleenwort.png"
				},
				{
					r.getString(R.string.menger_name),
					r.getString(R.string.menger_transforms),
					"menger.png"
				},
			};
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			if (db.isReadOnly()) return;
		}
	    
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(FRACTAL_TABLE_CREATE);
			for (int i = 0; i < demoFractals.length; i++) {
				insertFractal(db, i+1,  demoFractals[i][0], demoFractals[i][1], demoFractals[i][2]);
			}
		}
		
		private void insertFractal(SQLiteDatabase db, int sharedId, String name, String transforms, String thumbnail) {
			Log.d("Provider", "insertFractal");
			int numT = transforms.split(" ").length / 16;
			String thumbPath = saveDir.getAbsolutePath() + "/" + thumbnail;
			db.execSQL(String.format("INSERT OR IGNORE INTO %s (%s, %s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?, ?)",
					TABLE_NAME, Items.SHARED_ID, Items.NAME, Items.TRANSFORM_COUNT,
					Items.SERIALIZED_TRANSFORMS, Items.THUMBNAIL, Items.LAST_UPDATED),
					new Object[] { sharedId, name, numT, transforms, thumbPath, System.currentTimeMillis()});
			try {
				InputStream in = manager.open(thumbnail);
				OutputStream out = new FileOutputStream(thumbPath);
				byte[] buffer = new byte[4096];
				int read;
				while((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch (IOException e) {
				Log.e("Provider", "Could not write thumbnail: " + e.getMessage());
				if (e.getCause() != null) {
					Log.e("Provider", "thumbnail error cause: " + e.getCause().getMessage());
				}
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("Provider", "updating from " + oldVersion + " to " + newVersion);
			if (oldVersion < 2 && newVersion > 2) {
				String addSharedId = String.format("ALTER TABLE %s ADD COLUMN %s INTEGER", TABLE_NAME, Items.SHARED_ID);
				Log.d("Provider", addSharedId);
				db.execSQL(addSharedId);
			}
		}
	}
}
