package com.ejegg.android.fractaleditor.persist;

import com.ejegg.android.fractaleditor.FractalEditActivity;
import com.ejegg.android.fractaleditor.GalleryActivity;
import com.ejegg.android.fractaleditor.R;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class SavedFractalAdapter extends RecyclerView.Adapter<SavedFractalAdapter.SavedFractalViewHolder> {
	private Cursor data;
	private GalleryActivity context;

	public SavedFractalAdapter(GalleryActivity context) {
		this.context = context;
	}

	@Override
	public int getItemCount() {
		return data.getCount() + 1;
	}

	@Override
	public void onBindViewHolder(SavedFractalViewHolder viewHolder, int i) {
		if (i == 0) {
			viewHolder.state = null;
			viewHolder.thumbnailView.setImageDrawable(
				context.getResources().getDrawable(R.drawable.ic_menu_add)
			);
			viewHolder.nameView.setTypeface(null, Typeface.BOLD_ITALIC);
			viewHolder.nameView.setText(context.getResources().getString(R.string.create_new));
			return;
		}
		data.moveToPosition(data.getCount() - i);
		FractalState state = new FractalState(
			data.getInt(data.getColumnIndex(FractalStateProvider.Items._ID)),
			0,
			data.getString(data.getColumnIndex(FractalStateProvider.Items.NAME)),
			data.getString(data.getColumnIndex(FractalStateProvider.Items.THUMBNAIL)),
			""
		);
		viewHolder.state = state;
		String name = state.getName();
		viewHolder.nameView.setText(name);
		String thumb = state.getThumbnailPath();
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
		notifyDataSetChanged();
	}

	public class SavedFractalViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
		protected ImageView thumbnailView;
		protected TextView nameView;
		protected Button deleteButton;
		protected FractalState state;

		public SavedFractalViewHolder(View v) {
			super(v);
			thumbnailView = (ImageView) v.findViewById(R.id.saved_fractal_thumbnail);
			thumbnailView.setOnClickListener(this);
			thumbnailView.setLongClickable(true);
			thumbnailView.setOnLongClickListener(this);
			nameView = (TextView) v.findViewById(R.id.saved_fractal_name);
			deleteButton = (Button)v.findViewById(R.id.delete_button);
			deleteButton.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			Uri uri = null;
			if (state != null) {
				uri = Uri.parse(FractalStateProvider.CONTENT_URI.toString() + "/" + state.getDeviceId());
			}
			if (v.getId() == R.id.delete_button) {
				deleteButton.setVisibility(View.GONE);
				// FIXME: get this off the UI thread
				context.getContentResolver().delete(uri, null, null);
				context.reloadCursor();
				return;
			}
			if (deleteButton.getVisibility() == View.VISIBLE) {
				deleteButton.setVisibility(View.GONE);
				return;
			}
			Intent result = new Intent(
				context, FractalEditActivity.class
			);
			if (uri != null) {
				result.setData(uri);
			}

			context.startActivity(result);
		}

		@Override
		public boolean onLongClick(View v) {
			if (state == null) {
				return false;
			}
			deleteButton.setVisibility(View.VISIBLE);
			return true;
		}
	}

}