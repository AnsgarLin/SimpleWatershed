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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
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

	public interface ScaledListener extends OnTouchListener {
		int NONE = 0;
		int DRAG = 1;
		int ZOOM = 2;

		class TransInfo {
			private PointF mTranslate;
			private float mScale;
			private float mRotation;

			/**
			 * Initial the transition state. Use set() method for setting each state
			 */
			public TransInfo() {
				mTranslate = new PointF(0f, 0f);
				mScale = 1;
				mRotation = 0;
			}

			public void reset() {
				mTranslate.set(0f, 0f);
				mScale = 1;
				mRotation = 0;
			}

			public PointF getTranslate() {
				return mTranslate;
			}

			public void setTranslate(float translateX, float translateY) {
				mTranslate.x += (translateX - mTranslate.x);
				mTranslate.y += (translateY - mTranslate.y);
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

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}

				if (!(o instanceof TransInfo)) {
					return false;
				}

				TransInfo target = (TransInfo) o;

				return (mRotation == target.mRotation) && (mScale == target.mScale)
						&& (mTranslate == null ? target.mTranslate == null : mTranslate.equals(target.mTranslate));
			}

			@Override
			public int hashCode() {
				// Start with a non-zero constant.
				int result = 17;

				// Include a hash for each field.
				result = (31 * result) + Float.floatToIntBits(mScale);
				result = (31 * result) + Float.floatToIntBits(mRotation);
				result = (31 * result) + (mTranslate == null ? 0 : mTranslate.hashCode());

				return result;
			}

			@Override
			public String toString() {
				return getClass().getName() + "[" + "Translate = (" + mTranslate.x + ", " + mTranslate.y + ") " + "Scale = " + mScale + " "
						+ "Rotation = " + mRotation + "]";
			}

		}

	}

	/**
	 * A simple class to be extended for developing a listener to handle drag/zoom/rotate event in one.<br>
	 * <strong>Note:</strong> Be aware that a reliable listener for handling drag/zoom/rotate event should always be able to provider user the current
	 * mode and transition state. And always call {@link #onDestroy} to release all references.
	 */
	public abstract static class ScaledTouchListenerImpl implements ScaledListener {
		// The target view
		protected View mView;

		// Record current mode
		protected int mMode;

		// Record the transition state between the current and the begin
		protected TransInfo mTransInfo;

		// Whether drag event should be triggered on one finger, default is true
		protected boolean mSingleDrag;

		// Whether rotate event should be triggered, default is true
		protected boolean mRotatable;

		public ScaledTouchListenerImpl(View view) {
			initConfigure(view);
		}

		/**
		 * Set changeable parameters with default value, and will be changed during run time
		 */
		private void initConfigure(View view) {
			mView = view;
			mMode = NONE;
			mTransInfo = new TransInfo();
			mSingleDrag = true;
			mRotatable = true;
		}

		/**
		 * Release the all references
		 */
		public void onDestroy() {
			mView = null;
			mTransInfo = null;
		}

		/**
		 * Reset transition state
		 */
		public void onReset() {
			setTransInfo(new TransInfo());
		}

		/**
		 * Override this method for doing things <strong>before</strong> handling drag/scale/rotate event.<br>
		 * 
		 * @param v
		 *            The view the touch event has been dispatched to.
		 * @param event
		 *            The MotionEvent object containing full information about the event.
		 * @return False by default to pass the event to {@link #onCustomTouch}, true otherwise.
		 */
		public boolean onTouchBefore(View v, MotionEvent event) {
			return false;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			boolean result = onTouchBefore(v, event);
			if (!result) {
				result = onCustomTouch(v, event);
			}
			onTouchAfter(v, event);
			return result;
		}

		/**
		 * Override this method for doing things <strong>after</strong> {@link #onCustomTouch} or {@link #onTouchBefore}.
		 * 
		 * @param v
		 *            The view the touch event has been dispatched to.
		 * @param event
		 *            The MotionEvent object containing full information about the event.
		 */
		public void onTouchAfter(View v, MotionEvent event) {
		}

		/**
		 * Set the target view to be scaled
		 */
		public void setView(View view) {
			mView = view;
		}

		/**
		 * Get the target view to be scaled
		 */
		public View getView() {
			return mView;
		}

		/**
		 * Get current mode(Drag/Zoom(Scale))
		 */
		public int getMode() {
			return mMode;
		}

		/**
		 * Override this method for setting transition state from outside
		 */
		public abstract void setTransInfo(TransInfo transInfo);

		/**
		 * Get transition state between each step
		 */
		public TransInfo getTransInfo() {
			return mTransInfo;
		}

		/**
		 * Get whether drag event will be triggered on one finger
		 */
		public boolean isSingleDrag() {
			return mSingleDrag;
		}

		/**
		 * Set whether drag event should be triggered on one finger
		 */
		public void setSingleDrag(boolean singleDrag) {
			mSingleDrag = singleDrag;
		}

		/**
		 * Get whether rotate event will be triggered
		 */
		public boolean isRotatable() {
			return mRotatable;
		}

		/**
		 * Set whether rotate event should be triggered
		 */
		public void setRotatable(boolean rotatable) {
			mRotatable = rotatable;
		}

		/**
		 * Get distance between two points in 2-dimension
		 */
		protected float getDistance(float sX, float tX, float sY, float tY) {
			float x = sX - tX;
			float y = sY - tY;
			return (float) Math.sqrt((x * x) + (y * y));
		}

		/**
		 * Get mid-point between two points in 2-dimension
		 */
		protected PointF getMidPoint(float sX, float tX, float sY, float tY) {
			return new PointF((sX + tX) / 2, (sY + tY) / 2);
		}

		/**
		 * Override this method for handling drag/scale/rotate event.
		 */
		public abstract boolean onCustomTouch(View v, MotionEvent event);

		/**
		 * A customized class used for rotation
		 */
		protected class Vector2D extends PointF {
			protected Vector2D(float x, float y) {
				super(x, y);
				normalize();
			}

			protected void normalize() {
				float length = (float) Math.sqrt((x * x) + (y * y));
				x /= length;
				y /= length;
			}

			protected float getAngle(Vector2D vector) {
				return (float) ((180.0 / Math.PI) * (Math.atan2(vector.y, vector.x) - Math.atan2(y, x)));
			}

		}
	}

	/**
	 * A listener for handling drag/scale/rotate event of the bitmap inside the target ImageView.<br>
	 * <strong>Note:</strong> The target ImageView's LayoutParams should be set to {@link LayoutParams#WRAP_CONTENT} and scale type must set to
	 * {@link ScaleType#MATRIX}. After using, be aware to use {@link #onDestroy()} to release all references. <br>
	 * <strong>Usage:</strong> <strong>targetView</strong>.setOnTouchListener(new ScaledImageViewTouchListener())
	 */
	public static class ScaledImageViewTouchListener extends ScaledTouchListenerImpl {
		// Start point of the first finger point
		private PointF startPoint;

		// Record the state while the second finger touch
		// Distance between two finger point
		private float startDistance;
		// Middle point of two finger point
		private PointF startMidPoint;
		private Matrix startMatrix;
		// Vector of the line between fingers
		private Vector2D startVector;

		// Record current transition state
		// Translate
		private PointF tmpTranlate;
		// Scale
		private float tmpScale;
		// Image matrix
		private Matrix tmpMatrix;
		// Rotate
		private float tmpRotate;

		public ScaledImageViewTouchListener(ImageView view) {
			super(view);
			initConfigure();
		}

		private void initConfigure() {
			startPoint = new PointF();
			startMatrix = new Matrix();

			tmpTranlate = new PointF();
			tmpMatrix = new Matrix();
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
		}

		@Override
		public boolean onCustomTouch(View v, MotionEvent event) {
			v = mView;

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (mSingleDrag) {
					mMode = DRAG;
					startPoint.set(event.getX(), event.getY());
					startMatrix.set(((ImageView) v).getImageMatrix());
				}

				break;
			case MotionEvent.ACTION_MOVE:
				if (mMode != NONE) {
					tmpMatrix.set(startMatrix);
					if (mMode == DRAG) {
						// Calculate the current transition state
						// Translate
						tmpTranlate.x = event.getX() - startPoint.x;
						tmpTranlate.y = event.getY() - startPoint.y;
					} else if (mMode == ZOOM) {
						// Calculate the current transition state
						PointF curPivot = getMidPoint(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
						// Translate
						tmpTranlate.x = curPivot.x - startMidPoint.x;
						tmpTranlate.y = curPivot.y - startMidPoint.y;
						// Scale
						tmpScale = getDistance(event.getX(0), event.getX(1), event.getY(0), event.getY(1)) / startDistance;
						// Set the current transition state to target view
						tmpMatrix.postScale(tmpScale, tmpScale, curPivot.x, curPivot.y);

						// Record the current transition state
						mTransInfo.setScale(tmpScale);

						if (mRotatable) {
							// Calculate the current transition state
							// Rotate
							tmpRotate = startVector.getAngle(new Vector2D(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0)));
							// Set the current transition state to target view
							tmpMatrix.postRotate(tmpRotate, v.getWidth() / 2, v.getHeight() / 2);
							// Record the current transition state
							mTransInfo.setRotation(tmpRotate);
						}
					}
					// Set the current transition state to target view
					tmpMatrix.postTranslate(tmpTranlate.x, tmpTranlate.y);
					((ImageView) v).setImageMatrix(tmpMatrix);

					// Record the current transition state
					mTransInfo.setTranslate(tmpTranlate.x, tmpTranlate.y);
				}

				break;
			case MotionEvent.ACTION_UP:
				mMode = NONE;

				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				mMode = ZOOM;

				startDistance = getDistance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
				startMidPoint = getMidPoint(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
				startMatrix.set(((ImageView) v).getImageMatrix());
				if (mRotatable) {
					startVector = new Vector2D(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0));
				}

				tmpMatrix.set(startMatrix);

				break;
			case MotionEvent.ACTION_POINTER_UP:
				if (mSingleDrag) {
					mMode = DRAG;
					// If release the second finger, use first finger point to do drag as normal.
					if (event.getActionIndex() == 1) {
						startPoint.set(event.getX(0), event.getY(0));
					} else {
						startPoint.set(event.getX(1), event.getY(1));
					}
					startMatrix.set(((ImageView) v).getImageMatrix());
				} else {
					mMode = NONE;
				}
				break;
			default:
				break;
			}

			return true;
		}

		@Override
		public void setTransInfo(TransInfo transInfo) {
			// Set the current transition state to target view
			tmpMatrix.reset();
			tmpMatrix.postTranslate(transInfo.getTranslate().x, transInfo.getTranslate().y);
			mTransInfo.setScale(transInfo.getScale());
			if (mRotatable) {
				mTransInfo.setRotation(transInfo.getRotation());
			}
			((ImageView) mView).setImageMatrix(tmpMatrix);

			// Record the current transition state
			mTransInfo.setTranslate(transInfo.getTranslate().x, transInfo.getTranslate().y);
			mTransInfo.setScale(transInfo.getScale());
			mTransInfo.setRotation(transInfo.getRotation());
		}
	}

}