package com.ejegg.fractaldisplay.persist;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.provider.BaseColumns;
import com.ejegg.fractaldisplay.R;

public class FractalStateProvider extends ContentProvider {
//lots of this is adapted from the npr android app, apache licensed (can be added to GPLv3 projects)
	private FractalDatabaseHelper helper;
	public static final Uri CONTENT_URI = Uri.parse("content://com.ejegg.fractaldisplay.persist.FractalState");
	private static final String DATABASE_NAME = "fractaleditor";
	private static final String TABLE_NAME = "fractalstates";
    private static final int DATABASE_VERSION = 1;

	@Override
	public boolean onCreate() {
		helper = new FractalDatabaseHelper(getContext());
		//Log.d("fsp", "created fractal state provider");
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
	    public static final String NAME = "name";
	    public static final String TRANSFORM_COUNT = "transformCount";
	    public static final String SERIALIZED_TRANSFORMS = "serializedTransforms";
	    public static final String THUMBNAIL = "thumbnail";
	    public static final String LAST_UPDATED = "lastUpdated";
	    public static final String[] COLUMNS = {NAME, TRANSFORM_COUNT, SERIALIZED_TRANSFORMS, THUMBNAIL, LAST_UPDATED};
	    public static final String[] ALL_COLUMNS = {BaseColumns._ID, NAME, TRANSFORM_COUNT, SERIALIZED_TRANSFORMS, THUMBNAIL, LAST_UPDATED};
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

		private HashMap<String, String> demoFractals;
		
	    private static final String FRACTAL_TABLE_CREATE =
	                "CREATE TABLE " + TABLE_NAME + " (" +
	                Items._ID + " INTEGER PRIMARY KEY, " +
	                Items.NAME + " TEXT UNIQUE, " +
	                Items.TRANSFORM_COUNT + " INTEGER, " +
	                Items.SERIALIZED_TRANSFORMS + " TEXT, " +
	                Items.THUMBNAIL + " BLOB, " + 
	                Items.LAST_UPDATED + " INTEGER);";
	    
	    public FractalDatabaseHelper(Context context) {
	    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    	demoFractals = new HashMap<String, String>();
	    	Resources r = context.getResources();
	    	demoFractals.put(r.getString(R.string.sierpinski_name), r.getString(R.string.sierpinski_transforms));
	    	demoFractals.put(r.getString(R.string.menger_name), r.getString(R.string.menger_transforms));
	    	demoFractals.put(r.getString(R.string.spleenwort_name), r.getString(R.string.spleenwort_transforms));
	    }
	    
	    @Override
	    public void onOpen(SQLiteDatabase db) {
	    	if (db.isReadOnly()) return;
			for (String name : demoFractals.keySet()) {
				insertFractal(db, name, demoFractals.get(name));
			}
	    }
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(FRACTAL_TABLE_CREATE);
		}

		
		private void insertFractal(SQLiteDatabase db, String name, String transforms)
		{
			int numT = transforms.split(" ").length / 16;
			db.execSQL(String.format("INSERT OR IGNORE INTO %s (%s, %s, %s, %s) VALUES(?, ?, ?, ?)", 
					TABLE_NAME, Items.NAME, Items.TRANSFORM_COUNT, Items.SERIALIZED_TRANSFORMS, Items.LAST_UPDATED),
					new Object[] { name, numT, transforms, System.currentTimeMillis()});
			
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// nothing yet!
		}

	}
}
