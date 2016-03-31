package com.ejegg.android.fractaleditor.render;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.spatial.Camera;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

public class MainRenderer implements GLSurfaceView.Renderer, MessagePasser.MessageListener{

	private static final int THUMBNAIL_MAX_WIDTH = 250;
	private static final int THUMBNAIL_MAX_HEIGHT = 300;
	private FractalRenderer fractalRenderer;
	private CubeRenderer cubeRenderer;
	private TextureRenderer textureRenderer;
	private final Camera camera;
	private final FractalStateManager stateManager;
	private boolean accumulatePoints = false;
	private boolean editMode = false;
	private boolean renderThumbnail = false;
	private final MessagePasser passer;
	private long lastCameraPosition;
	private long newCameraPosition;
	private float minDist = 100;
	private float maxDist = -100;
	private int[] thumbnailBuffer;
	private int thumbWidth;
	private int thumbHeight;
	private IntBuffer wrappedThumbnailBuffer;

	public MainRenderer(Camera camera, FractalStateManager stateManager, MessagePasser passer) {
		this.camera = camera;
		this.stateManager = stateManager;
		this.passer = passer;
		createThumbnailBuffers();
		calculateMinMaxDist();
		lastCameraPosition = camera.getLastMoveId();
		passer.Subscribe(this, MessageType.NEW_POINTS_AVAILABLE, MessageType.ACCUMULATION_MODE_CHANGED, MessageType.CAMERA_MOTION_CHANGED, MessageType.STATE_CHANGED);
	}

	private void createThumbnailBuffers() {
		thumbWidth = camera.getWidth();
		thumbHeight = camera.getHeight();
		Log.d("MainRenderer", String.format("Creating buffers, width=%s, height=%s", thumbWidth, thumbHeight));
		thumbnailBuffer = new int[thumbWidth * thumbHeight];
		wrappedThumbnailBuffer = IntBuffer.wrap(thumbnailBuffer);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		editMode = stateManager.isEditMode();
		//Log.d("MainRenderer", "Requested frame, edit mode is " + editMode);
		if (editMode) {
			clear();
			if (!cubeRenderer.isValid())  {
				cubeRenderer.destroy();
				cubeRenderer = new CubeRenderer(camera, stateManager);
			}
			cubeRenderer.draw();
		} else {
			if (!fractalRenderer.isValid())  {
				fractalRenderer.destroy();
				fractalRenderer = new FractalRenderer(camera, stateManager);
			}
			newCameraPosition = camera.getLastMoveId();
			if (newCameraPosition == lastCameraPosition) {
				if (!textureRenderer.isValid()) {
					textureRenderer.destroy();
					textureRenderer = new TextureRenderer(camera, stateManager, passer);
				}
				textureRenderer.preRender();
				fractalRenderer.draw(true, minDist, maxDist);
				textureRenderer.draw();
			} else {
				clear();
				fractalRenderer.draw(false, 0, 0);	
			}
			lastCameraPosition = newCameraPosition;
		}
		if (renderThumbnail) {
			grabPixels();
			renderThumbnail = false;
		}
		if (camera.isMoving()) {
			camera.spinStep();
		} 
	}
	
	private void clear() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.d("MainRenderer", "onSurfaceChanged");
		GLES20.glViewport(0, 0, width, height);
		camera.setScreenDimensions(width,height);
		createThumbnailBuffers();
		if (textureRenderer != null) {
			textureRenderer.freeResources();
		}
		textureRenderer = new TextureRenderer(camera, stateManager, passer);
		
		//Log.d("MainRenderer", "Done onSurfaceChanged");
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//Log.d("MainRenderer", "Starting onSurfaceCreated");
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glClearDepthf(1.0f);
		GLES20.glEnable( GLES20.GL_DEPTH_TEST );
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
    	GLES20.glEnable(GLES20.GL_CULL_FACE);
    	GLES20.glCullFace(GLES20.GL_BACK);
		GLES20.glDepthMask(true);
    	GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		fractalRenderer = new FractalRenderer(camera, stateManager);
		cubeRenderer = new CubeRenderer(camera, stateManager);
		
		//Log.d("MainRenderer", "Done onSurfaceCreated");
	}

	@Override
	public void ReceiveMessage(MessageType type, boolean value) {
		switch(type){
			case NEW_POINTS_AVAILABLE:
				accumulatePoints = value;
				if (minDist > maxDist) {
					Log.d("MainRenderer", String.format("MinDist is %f, MaxDist is %f", minDist, maxDist));
					calculateMinMaxDist();
				}
				if (stateManager.getPointsRendered() == FractalStateManager.MAX_POINTS) {
					renderThumbnail = true;
				}
				break;
			case ACCUMULATION_MODE_CHANGED:
				accumulatePoints = accumulatePoints || value;
				break;
			case STATE_CHANGED:
				minDist = 100;
				maxDist = -100;
				textureRenderer.clear();
				break;
			case CAMERA_MOTION_CHANGED:
				if (value) {
					calculateMinMaxDist();
				}
		}
	}

	private void calculateMinMaxDist() {
		minDist = 100;
		maxDist = -100;
		if (!stateManager.hasPoints()) {return;}
		float[] bbox = stateManager.getBoundingBox();
		float[] mvpMatrix = camera.getMVPMatrix();
		float[] vec = new float[4];
		Log.d("MainRenderer", String.format("Calculating max/min distances: X: [%f, %f], Y: [%f, %f], Z: [%f, %f] ", 
				bbox[0], bbox[3], bbox[1], bbox[4], bbox[2], bbox[5]));
		float dist;
		for (int i = 0; i < 8 ; i++ ) {
			int xInd = i < 4 ? 0 : 3;
			int yInd = i % 4 < 2 ? 1 : 4;
			int zInd = i % 2 == 0 ? 2 : 5;
			float[] vertex = new float[] {bbox[xInd], bbox[yInd], bbox[zInd], 1.0f};

			Matrix.multiplyMV(vec, 0, mvpMatrix, 0, vertex, 0);

			dist = vec[2];
			Log.d("MainRenderer", String.format("Vertex %d (%f, %f, %f): Distance is %f ", 
					i, vertex[0], vertex[1], vertex[2], dist));
			minDist = Math.min(minDist, dist);
			maxDist = Math.max(maxDist, dist);
		}
	}

	private void grabPixels() {
		wrappedThumbnailBuffer.position(0);
		GLES20.glReadPixels(
				0,
				0,
				camera.getWidth(),
				camera.getHeight(),
				GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE,
				wrappedThumbnailBuffer);
		Log.d("MainRenderer", "Grabbed thumbnail pixels");
	}

	public Bitmap getRender() {
		// Cribbed from http://stackoverflow.com/questions/20606295/when-using-gles20-glreadpixels-on-android-the-data-returned-by-it-is-not-exactl
		Log.d("MainRenderer", String.format("Creating bitmap, width=%s, height=%s", thumbWidth, thumbHeight));
		int bt[] = new int[thumbWidth * thumbHeight];
		Log.d("MainRenderer", "Created buffer");

		for(int i = 0, k = 0; i < thumbHeight; i++, k++) {
			// OpenGL bitmap is incompatible with Android bitmap
			// and so, some correction is needed.
			for(int j = 0; j < thumbWidth; j++) {
				int pix = thumbnailBuffer[i * thumbWidth + j];
				int pb = (pix >> 16) & 0xff;
				int pr = (pix << 16) & 0x00ff0000;
				int pix1 = (pix & 0xff00ff00) | pr | pb;
				bt[(thumbHeight - k - 1) * thumbWidth + j] = pix1;
			}
		}
		Log.d("MainRenderer", "Transformed buffer to android format");
		return Bitmap.createBitmap(bt, thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888);
	}

	public Bitmap getThumbnail() {
		Bitmap fullSize = this.getRender();
		int width = fullSize.getWidth();
		int height = fullSize.getHeight();
		float widthRatio = width / THUMBNAIL_MAX_WIDTH;
		float heightRatio = height / THUMBNAIL_MAX_HEIGHT;
		float ratio = 2.0f;//Math.max(widthRatio, heightRatio);
		Bitmap thumb = Bitmap.createScaledBitmap(
				fullSize,
				(int) (width / ratio),
				(int) (height / ratio),
				false
		);
		fullSize.recycle();
		return thumb;
	}
}
