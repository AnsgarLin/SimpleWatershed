package com.example.simplewatershed.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class Util {
	public static boolean LOG = true;

	/**
	 * Recycle bitmap in ImageView
	 */
	public static void recycle(ImageView imageView) {
		if ((BitmapDrawable) imageView.getDrawable() != null) {
			Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
			if ((bitmap != null) && !bitmap.isRecycled()) {
				bitmap.recycle();
			}
			imageView.setImageBitmap(bitmap = null);
		}
	}

	public static android.graphics.Point measureSmallSize(float dstW, float dstH, float srcW, float srcH) {
		float scaleHeight = srcH / dstH;
		float scaleWidth = srcW / dstW;
		float finalW = srcW;
		float finalH = srcH;
		// Log.d("Util:measureSmallSize", "0, Dst H: " + dstH + ", Dst W: " + dstW + ", Scale H: " + scaleHeight + ", Scale W: " + scaleWidth);

		// To get a smaller size, either scaleHeight or scaleWidth need to be greater than 0 as int
		if (((srcW / scaleHeight) > 0f) && (scaleHeight > 0f) && (scaleHeight > scaleWidth)) {
			// Log.d("Util:measureSmallSize", "Scale by height");
			finalW = srcW / scaleHeight;
			finalH = srcH / scaleHeight;
		} else if (((srcH / scaleWidth) > 0f) && (scaleWidth > 0f) && (scaleWidth > scaleHeight)) {
			// Log.d("Util:measureSmallSize", "Scale by width");
			finalW = srcW / scaleWidth;
			finalH = srcH / scaleWidth;
		}
		// Log.d("Util:measureSmallSize", "1, Src H: " + srcH + ", Src W: " + srcW + ", Final H: " + finalH + ", Final W: " + finalW);

		// Too make the small size is absolutely smaller than the target size, resize again if necessary.
		if (dstW < finalW) {
			float scale = finalW / dstW;
			finalW = dstW;
			finalH = finalH / scale;
		} else if (dstH < finalH) {
			float scale = finalH / dstH;
			finalH = dstH;
			finalW = finalW / scale;
		}
		// Log.d("Util:measureSmallSize", "2, Src H: " + srcH + ", Src W: " + srcW + ", Final H: " + finalH + ", Final W: " + finalW);
		return new android.graphics.Point((int) finalW, (int) finalH);
	}

	/**
	 * Get a bitmap with target size
	 * 
	 * @param bitmap
	 *            The original bitmap
	 * @param dstW
	 *            The target width
	 * @param dstH
	 *            The target height
	 * @param recycle
	 *            true if recycle the origin, false otherwise
	 * @return The small bitmap with target size
	 */
	public static Bitmap getScaleBitmap(Bitmap bitmap, float dstW, float dstH, boolean recycle) {
		Bitmap cloneBitmap = createMutableBitmap(bitmap);

		android.graphics.Point smallSize = measureSmallSize(dstW, dstH, cloneBitmap.getWidth(), cloneBitmap.getHeight());
		Bitmap smallBitmap = Bitmap.createScaledBitmap(cloneBitmap, smallSize.x, smallSize.y, false);
		if (recycle && !bitmap.isRecycled()) {
			bitmap.recycle();
		}
		cloneBitmap.recycle();

		return smallBitmap;
	}

	public static Bitmap createMutableBitmap(Bitmap bitmap) {
		Bitmap mutableBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_4444);
		Canvas canvas = new Canvas(mutableBitmap);
		canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG));

		return mutableBitmap;
	}

	public static Bitmap resizeDownToPixels(Bitmap bitmap, int targetPixels, boolean recycle) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float scale = (float) Math.sqrt((double) targetPixels / (width * height));
		if (scale >= 1.0f) {
			return bitmap;
		}
		return resizeBitmapByScale(bitmap, scale, recycle);
	}

	public static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean recycle) {
		int width = Math.round(bitmap.getWidth() * scale);
		int height = Math.round(bitmap.getHeight() * scale);
		if ((width == bitmap.getWidth()) && (height == bitmap.getHeight())) {
			return bitmap;
		}
		Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
		Canvas canvas = new Canvas(target);
		canvas.scale(scale, scale);
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
		canvas.drawBitmap(bitmap, 0, 0, paint);
		if (recycle) {
			bitmap.recycle();
		}
		return target;
	}

	private static Bitmap.Config getConfig(Bitmap bitmap) {
		Bitmap.Config config = bitmap.getConfig();
		if (config == null) {
			config = Bitmap.Config.ARGB_8888;
		}
		return config;
	}

	/**
	 * Get a scaled bitmap base on width
	 * 
	 * @param bitmap
	 *            The original bitmap
	 * @param width
	 *            The target width
	 * @return The scaled bitmap
	 */
	public static Bitmap scaleBitmapByWidth(Bitmap bitmap, int width) {
		float scale = (float) bitmap.getWidth() / (float) width;
		Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, width, (int) (bitmap.getHeight() / scale), false);
		bitmap.recycle();
		return newBitmap;
	}

	/**
	 * Swap LayoutParams with target view
	 * 
	 * @param leftLayoutParams
	 *            The LayoutParams that will be set to the target view.
	 * @param right
	 *            The target view.
	 * @return The target view's old LayoutParams.
	 */
	public static android.view.ViewGroup.LayoutParams swapLayoutParam(android.view.ViewGroup.LayoutParams leftLayoutParams, View right) {
		android.view.ViewGroup.LayoutParams rightLayoutParams = right.getLayoutParams();
		right.setLayoutParams(leftLayoutParams);
		return rightLayoutParams;
	}

	/**
	 * Get bitmap left/top/right/bottom/scaleX/scaleY in ImageView
	 */
	public static float[] getBitmapTransStateInImageView(ImageView imageView) {
		float[] values = new float[9];
		imageView.getImageMatrix().getValues(values);
		float[] bounds = new float[6];
		bounds[0] = values[Matrix.MTRANS_X];
		bounds[1] = values[Matrix.MTRANS_Y];
		bounds[2] = (imageView.getWidth() * values[Matrix.MSCALE_X]) + bounds[0];
		bounds[3] = (imageView.getHeight() * values[Matrix.MSCALE_Y]) + bounds[1];
		bounds[4] = values[Matrix.MSCALE_X];
		bounds[5] = values[Matrix.MSCALE_Y];
		return bounds;
	}

	/**
	 * Get view left/top/right/bottom in parent
	 */
	public static float[] getViewBoundsInParent(View view) {
		float[] values = new float[4];
		values[0] = view.getX() + (view.getWidth() * (1 - view.getScaleX()));
		values[1] = view.getY() + (view.getHeight() * (1 - view.getScaleY()));
		values[2] = values[0] + (view.getWidth() * view.getScaleX());
		values[3] = values[1] + (view.getHeight() * view.getScaleY());
		return values;
	}

	public static Intent getBasePickerIntent(Context context, int selectLimit) {
		Intent intent = new Intent();
		// intent.setClass(context.getApplicationContext(), PickerActivity.class);
		// intent.putExtra(PickerActivity.ALLOW_MULTISELECT, selectLimit == 1 ? false : true);
		// intent.putExtra(PickerActivity.MAX_PHOTO_LIMIT, selectLimit);
		return intent;
	}

	public interface ScaledImageViewTouchListener extends OnTouchListener {
		int NONE = 0;
		int DRAG = 1;
		int ZOOM = 2;

		/**
		 * The target ImageView's scale type must set to ScaleType.MATRIX. After using, call onDestroy() to make sure listener will not keep the
		 * target ImageView's reference.
		 * 
		 * @param imageView
		 *            The target ImageView's bitmap will be changed by touch event.
		 */
		ScaledImageViewTouchListener initConfigure(ImageView imageView);

		/**
		 * Release the target ImageView's reference
		 */
		void onDestroy();

		/**
		 * Get current mode(Drag/Zoom)
		 */
		public int getMode();

		/**
		 * Get transition state between each step
		 */
		public TransInfo getTranslate();

		public static class TransInfo {
			private PointF mTranlate;
			private float mScale;
			private PointF mPivots;

			public TransInfo() {
				mTranlate = new PointF();
				mPivots = new PointF();
			}

			public PointF getTranlate() {
				return mTranlate;
			}

			public void setTranlate(float tranlateX, float tranlateY) {
				mTranlate.x = tranlateX;
				mTranlate.y = tranlateY;
			}

			public float getScale() {
				return mScale;
			}

			public void setScale(float scale) {
				mScale = scale;
			}

			public PointF getPivots() {
				return mPivots;
			}

			public void setPivots(PointF pivot) {
				mPivots.x = pivot.x;
				mPivots.y = pivot.y;
			}

			public void setPivots(float pivotX, float pivotY) {
				mPivots.x = pivotX;
				mPivots.y = pivotY;
			}
		}
	}

	/**
	 * A listener for handling drag/scale/rotate event of ImageView's bitmap. <h3>Note:</h3> The target ImageView's layout should match parent, then
	 * set the touch event to it or its parent. <h3>Usage:</h3> ScaledImageViewTouchListener scaledImageViewTouchListener = new
	 * getScaledImageViewTouchListener().initConfigure(view);
	 */
	public static ScaledImageViewTouchListener getScaleTouchListener() {
		return new ScaledImageViewTouchListener() {
			private int mMode;

			private ImageView mTargetView;

			private Matrix tempMatrix;
			private Matrix startMatrix;

			private TransInfo transInfo;

			private PointF startPoint;
			private float startDistance;

			@Override
			public ScaledImageViewTouchListener initConfigure(ImageView imageView) {
				mMode = NONE;
				mTargetView = imageView;
				mTargetView.setScaleType(ScaleType.MATRIX);

				tempMatrix = new Matrix();
				startMatrix = new Matrix();

				transInfo = new TransInfo();

				startPoint = new PointF();

				return this;
			}

			@Override
			public void onDestroy() {
				mTargetView = null;

			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					mMode = DRAG;
					startPoint.set(event.getX(), event.getY());
					startMatrix.set(mTargetView.getImageMatrix());

					break;
				case MotionEvent.ACTION_MOVE:
					if (mMode != NONE) {
						tempMatrix.set(startMatrix);
						if (mMode == DRAG) {
							transInfo.setTranlate(event.getX() - startPoint.x, event.getY() - startPoint.y);
							tempMatrix.postTranslate(transInfo.getTranlate().x, transInfo.getTranlate().y);
						} else if (mMode == ZOOM) {
							transInfo.setScale((float) getDistance(event.getX(0), event.getX(1), event.getY(0), event.getY(1)) / startDistance);
							tempMatrix.postScale(transInfo.getScale(), transInfo.getScale(), transInfo.getPivots().x, transInfo.getPivots().y);
						}
					}
					mTargetView.setImageMatrix(tempMatrix);

					break;
				case MotionEvent.ACTION_UP:
					mMode = NONE;

					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					mMode = ZOOM;
					startDistance = (float) getDistance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
					transInfo.setPivots(getMidPoint(event.getX(0), event.getX(1), event.getY(0), event.getY(1)));

					startMatrix.set(mTargetView.getImageMatrix());
					tempMatrix.set(startMatrix);

					Logger.d(getClass(), "ACTION_POINTER_DOWN" + event.getAction());

					break;
				case MotionEvent.ACTION_POINTER_UP:
					mMode = NONE;

					break;
				default:
					break;
				}

				return true;
			}

			/**
			 * Get distance between two points in 2-dimension
			 */
			private double getDistance(float sX, float tX, float sY, float tY) {
				float x = sX - tX;
				float y = sY - tY;
				return Math.sqrt((x * x) + (y * y));
			}

			/**
			 * Get mid-point between two points in 2-dimension
			 */
			private PointF getMidPoint(float sX, float tX, float sY, float tY) {
				return new PointF((sX + tX) / 2, (sY + tY) / 2);
			}

			@Override
			public int getMode() {
				return mMode;
			}

			@Override
			public TransInfo getTranslate() {
				return transInfo;
			}

		};
	}

	/**
	 * A basic structure for a touch listener which can handle drag/scale/rotate event.
	 */
	public interface ScaledTouchListener extends OnTouchListener {
		int NONE = 0;
		int DRAG = 1;
		int ZOOM = 2;

		/**
		 * Set target view and initialize everything here. if don't, use setView instead at run time.<br>
		 * <Strong>Note:</Strong> After using, call onDestroy() to make sure listener will not keep the target view's reference.
		 */
		ScaledTouchListener initConfigure(View view);

		/**
		 * Release the target view's reference.
		 */
		void onDestroy();

		/**
		 * Reset th target view's reference and transition info
		 */
		void onReset();

		/**
		 * Override this method for doing things <strong>before</strong> handling drag/scale/rotate event.
		 */
		boolean beforeTouch(View v, MotionEvent event);

		/**
		 * Override this method for doing things <strong>after</strong> handling drag/scale/rotate event.
		 */
		boolean aferTouch(View v, MotionEvent event);

		/**
		 * Reset the target view that will be transmitted.
		 */
		void setView(View view);

		/**
		 * Get the target view that will be transmitted.
		 */
		View getView();

		/**
		 * Get the target view's transmition state.
		 */
		TransInfo getTransInfo();

		/**
		 * A customized class used for storing the current transition state.
		 */
		class TransInfo {
			private float mWidth;
			private float mHeight;
			private PointF mTopLeft;
			private PointF mPivots;
			private float mScale;
			private float mRotation;

			/**
			 * Initial the transition state, use set method for setting each state
			 */
			public TransInfo() {
				mTopLeft = new PointF();
				mPivots = new PointF();
				mScale = 1;
			}

			public void setWidth(float width) {
				mWidth = width;
			}

			public float getWidth() {
				return mWidth;
			}

			public void setHeight(float height) {
				mHeight = height;
			}

			public float getHeight() {
				return mHeight;
			}

			public void setTopLeft(float left, float top) {
				mTopLeft.x = left;
				mTopLeft.y = top;
			}

			public PointF getTopLeft() {
				return mTopLeft;
			}

			public void setPivots(float x, float y) {
				mPivots.x = x;
				mPivots.y = y;
			}

			public PointF getPivots() {
				return mPivots;
			}

			public void setScale(float scale) {
				mScale = scale;
			}

			public float getScale() {
				return mScale;
			}

			public void setRotation(float rotation) {
				mRotation = rotation;
			}

			public float getRotation() {
				return mRotation;
			}
		}

		/**
		 * A customized class used for rotation
		 */
		class Vector2D extends PointF {
			public Vector2D(float x, float y) {
				super(x, y);
				normalize();
			}

			public void normalize() {
				float length = (float) Math.sqrt((x * x) + (y * y));
				x /= length;
				y /= length;
			}

			public float getAngle(Vector2D vector) {
				return (float) ((180.0 / Math.PI) * (Math.atan2(vector.y, vector.x) - Math.atan2(y, x)));
			}

		}

	}

	/**
	 * A listener for handling drag/scale/rotate event of view.<br>
	 * <h3>Note:</h3> The target view should be contained in a <strong>FrameLayout</strong>, which will be bigger than the target view, or the size
	 * will be limited by right and bottom bounds while scaling.<br>
	 * <h3>Usage:</h3> <strong>TargetFrameLayout</strong>.setOnTouchListener(new ScaledLayoutListener().initConfigure(<strong>targetView</strong>));
	 */
	public static abstract class ScaledLayoutListener implements ScaledTouchListener {
		protected int mMode;

		protected View mView;
		protected TransInfo mTransInfo;

		private float preWidth;
		private float preHeight;

		private float preLeft;
		private float preTop;

		private float preX;
		private float preY;

		private PointF preMidPoint;
		private float preDistance;

		private float preRotate;
		private Vector2D preVector;

		public ScaledLayoutListener() {
			mTransInfo = new TransInfo();
		}

		/**
		 * @param view
		 *            The target view's layout will be changed by touch event.
		 */
		@Override
		public ScaledLayoutListener initConfigure(View view) {
			mView = view;

			return this;
		}

		@Override
		public void onDestroy() {
			mView = null;
			mTransInfo = null;
		}

		@Override
		public void onReset() {
			mView = null;
			mTransInfo = new TransInfo();
		}

		/**
		 * Return false by default to pass the event to onTouch, true otherwise.
		 */
		@Override
		public boolean beforeTouch(View v, MotionEvent event) {
			return false;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (beforeTouch(v, event)) {
				return aferTouch(v, event);
			}

			// No view to control, just ignore.
			if (mView == null) {
				Log.e("ScaledLayoutListener", "Need to use initConfigure(View view) to assign a view to control");
				return aferTouch(v, event);
			}

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mMode = DRAG;

				recordLayoutState();

				preX = event.getX();
				preY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				if (mMode != NONE) {
					if (mMode == DRAG) {
						mTransInfo.setTopLeft(preLeft + (event.getX() - preX), preTop + (event.getY() - preY));
						mTransInfo.setWidth(preWidth);
						mTransInfo.setHeight(preHeight);
					} else if (mMode == ZOOM) {
						PointF curPivot = getMidPoint(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
						mTransInfo.setScale(getDistance(event.getX(0), event.getX(1), event.getY(0), event.getY(1)) / preDistance);
						mTransInfo.setWidth(preWidth * mTransInfo.getScale());
						mTransInfo.setHeight(preHeight * mTransInfo.getScale());
						mTransInfo.setTopLeft(preLeft + ((preWidth - mTransInfo.getWidth()) * ((preMidPoint.x - preLeft) / preWidth))
								+ (curPivot.x - preMidPoint.x), preTop
								+ ((preHeight - mTransInfo.getHeight()) * ((preMidPoint.y - preTop) / preHeight)) + (curPivot.y - preMidPoint.y));

						mTransInfo.setRotation(preRotate
								+ preVector.getAngle(new Vector2D(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))));
						mView.setRotation(mTransInfo.getRotation());
					}
					mView.setLayoutParams(generateLayoutParam(mTransInfo));
				}

				break;
			case MotionEvent.ACTION_UP:
				mMode = NONE;

				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				mMode = ZOOM;

				recordLayoutState();

				preDistance = getDistance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
				preMidPoint = getMidPoint(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
				preRotate = mView.getRotation();
				preVector = new Vector2D(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0));
				break;
			case MotionEvent.ACTION_POINTER_UP:
				mMode = NONE;

				break;
			default:
				break;
			}

			return aferTouch(v, event);
		}

		/**
		 * Return true by default if the listener has consumed the event, false otherwise.
		 */
		@Override
		public boolean aferTouch(View v, MotionEvent event) {
			return true;
		}

		/**
		 * Get distance between two points in 2-dimension
		 */
		private float getDistance(float sX, float tX, float sY, float tY) {
			float x = sX - tX;
			float y = sY - tY;
			return (float) Math.sqrt((x * x) + (y * y));
		}

		/**
		 * Get mid-point between two points in 2-dimension
		 */
		private PointF getMidPoint(float sX, float tX, float sY, float tY) {
			return new PointF((sX + tX) / 2, (sY + tY) / 2);
		}

		/**
		 * New a layout params for target view based current transition state
		 */
		private LayoutParams generateLayoutParam(TransInfo layoutInfo) {
			ViewGroup.LayoutParams tmpLayoutParams = mView.getLayoutParams();
			tmpLayoutParams.width = (int) layoutInfo.getWidth();
			tmpLayoutParams.height = (int) layoutInfo.getHeight();
			((FrameLayout.LayoutParams) tmpLayoutParams).leftMargin = (int) mTransInfo.getTopLeft().x;
			((FrameLayout.LayoutParams) tmpLayoutParams).topMargin = (int) mTransInfo.getTopLeft().y;
			return tmpLayoutParams;
		}

		/**
		 * Record target view's current layout state
		 */
		private void recordLayoutState() {
			preWidth = ((FrameLayout.LayoutParams) mView.getLayoutParams()).width;
			preHeight = ((FrameLayout.LayoutParams) mView.getLayoutParams()).height;
			preLeft = ((FrameLayout.LayoutParams) mView.getLayoutParams()).leftMargin;
			preTop = ((FrameLayout.LayoutParams) mView.getLayoutParams()).topMargin;
		}

		public int getMode() {
			return mMode;
		}

		public TransInfo getLayoutInfo() {
			return mTransInfo;
		}

		@Override
		public void setView(View view) {
			mView = view;
		}

		@Override
		public View getView() {
			return mView;
		}

		@Override
		public TransInfo getTransInfo() {
			return mTransInfo;
		}
	}

}