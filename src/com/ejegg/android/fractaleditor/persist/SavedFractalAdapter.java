package com.ejegg.android.fractaleditor.persist;

import com.ejegg.android.fractaleditor.FractalEditActivity;
import com.ejegg.android.fractaleditor.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class SavedFractalAdapter extends RecyclerView.Adapter<SavedFractalAdapter.SavedFractalViewHolder> {
	private Cursor data;
	private Activity context;
	private int count = 0;

	public SavedFractalAdapter(Activity context) {
		this.context = context;
	}

	@Override
	public int getItemCount() {
		return count;
	}

	@Override
	public void onBindViewHolder(SavedFractalViewHolder viewHolder, int i) {
		if (i == count - 1) {
			viewHolder.id = -1;
			viewHolder.thumbnailView.setImageDrawable(
				context.getResources().getDrawable(R.drawable.ic_menu_add)
			);
			return;
		}
		data.moveToPosition(i);
		viewHolder.id = data.getInt(data.getColumnIndex(FractalStateProvider.Items._ID));
		String name = data.getString(data.getColumnIndex(FractalStateProvider.Items.NAME));
		viewHolder.nameView.setText(name);
		String thumb = data.getString(data.getColumnIndex(FractalStateProvider.Items.THUMBNAIL));
		if (thumb.indexOf("http") != 0) {
			Bitmap thumbBitmap = BitmapFactory.decodeFile(thumb);
			Log.d("Adapter", String.format("%s thumb bitmap dimensions: %d width, %d height", name, thumbBitmap.getWidth(), thumbBitmap.getHeight()));
			viewHolder.thumbnailView.setImageBitmap(thumbBitmap);
		} else {
			// do a thing with the remote path (or maybe we should download it as soon as we open it from the site? )
		}
	}

	@Override
	public SavedFractalViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View fractalView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.saved_fractal, viewGroup, false);
		return new SavedFractalViewHolder(fractalView);
	}

	public void setData(Cursor data) {
		this.data = data;
		count = data.getCount() + 1; // Add one for the 'create new' button
		notifyDataSetChanged();
	}

	public class SavedFractalViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		protected ImageView thumbnailView;
		protected TextView nameView;
		protected int id;
	
		public SavedFractalViewHolder(View v) {
			super(v);
			thumbnailView = (ImageView) v.findViewById(R.id.saved_fractal_thumbnail);
			nameView = (TextView) v.findViewById(R.id.saved_fractal_name);
			v.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			Intent result = new Intent(
				context, FractalEditActivity.class
			);
			if (id != 0) {
				result.setData(Uri.parse(FractalStateProvider.CONTENT_URI.toString() + "/" + id));
			}
			context.startActivity(result);
		}
	}

}