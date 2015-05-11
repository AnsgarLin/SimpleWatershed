package com.example.simplewatershed.view.imagecontainer;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.simplewatershed.R;
import com.example.simplewatershed.util.Util;

public class ImageContainer extends RelativeLayout {
	// General
	private ImageTouchDispatcher mImageTouchDispatcher;
	private int mThickness;
	private int mEraserThickness;
	private float mScale;
	private float mTransX;
	private float mTransY;
	private Toast mToast;

	// States of edit mode
	public static enum STATE {
		FG, ERASER, ZOOMER, STEP, SHAPE, BORDER, SPREAD, COLOR
	};

	private STATE mState;

	// Save the images matrix state
	private Matrix mMatrix;

	// BaseImage
	private Mat mOriginMat;
	private ImageView mBaseImage;

	// PreviewImage
	private Mat mTransMatForPreview;
	private ImageView mPreviewImage;

	// LineImage
	private Mat mTransMatForLine;
	private ImageView mLineImage;

	// Watershed
	private Mat mWatershedMask;

	public ImageContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		initConfigure();
	}

	public ImageContainer(Context context) {
		super(context);
		initConfigure();
	}

	// ============================================================
	// Initial functions
	/**
	 * Set changeable parameters with default value, and will be changed during run time
	 */
	public void initConfigure() {
		mState = STATE.FG;
		mThickness = ImageProcessor.BASIC_THICKNESS;
		mEraserThickness = mThickness * 4;
		mScale = ImageProcessor.BASIC_SCALE;
		mTransX = 0;
		mTransY = 0;
		mMatrix = new Matrix();
	}

	/**
	 * Set view or listener, should always call after setContentView() in onCreate()
	 */
	public void initView() {
		mBaseImage = (ImageView) findViewById(R.id.base_image);
		setOnTouchListener(mImageTouchDispatcher = new ImageTouchDispatcher());
	}

	public void setImage(Bitmap bitmap) {
		initConfigure();

		bitmap = Util.getScaleBitmap(bitmap, ((View) getParent()).getWidth(), ((View) getParent()).getHeight(), true);

		// BaseImage
		if (mBaseImage != null) {
			ImageProcessor.recycle(mBaseImage);
		}
		mBaseImage.setImageBitmap(bitmap);
		mBaseImage.setImageMatrix(new Matrix());

		if (mOriginMat != null) {
			mOriginMat.release();
		}
		Utils.bitmapToMat(bitmap, mOriginMat = new Mat());

		// Convert to 3 channel for later usage
		Imgproc.cvtColor(mOriginMat, mOriginMat, Imgproc.COLOR_BGRA2BGR);

		// Initital layout params for Line/PreviewImage
		LayoutParams imageLayoutParams = (LayoutParams) mBaseImage.getLayoutParams();
		imageLayoutParams.width = bitmap.getWidth();
		imageLayoutParams.height = bitmap.getHeight();

		// PreviewImage
		if (mPreviewImage != null) {
			Util.recycle(mPreviewImage);
			mPreviewImage.setLayoutParams(imageLayoutParams);
		} else {
			// Add PreviewImage
			mPreviewImage = new ImageView(getContext());
			mPreviewImage.setScaleType(ScaleType.MATRIX);
			addView(mPreviewImage, imageLayoutParams);
		}
		mPreviewImage.setImageMatrix(new Matrix());

		if (mTransMatForPreview != null) {
			mTransMatForPreview.release();
		} // Generate PreviewImage's transparent background mat
		mTransMatForPreview = new Mat(mOriginMat.size(), CvType.CV_8UC4);
		mTransMatForPreview.setTo(ImageProcessor.sTrans);
		ImageProcessor.showMatAsImage(mTransMatForPreview, mPreviewImage);

		// LineImage
		if (mLineImage != null) {
			Util.recycle(mLineImage);
			mLineImage.setLayoutParams(imageLayoutParams);
		} else {
			// Add LineImage
			mLineImage = new ImageView(getContext());
			mLineImage.setScaleType(ScaleType.MATRIX);
			addView(mLineImage, imageLayoutParams);
		}
		mLineImage.setImageMatrix(new Matrix());

		if (mTransMatForLine != null) {
			mTransMatForLine.release();
		}
		// Generate lineImage's transparent background mat
		mTransMatForLine = new Mat(mOriginMat.size(), CvType.CV_8UC4);
		mTransMatForLine.setTo(ImageProcessor.sTrans);
		ImageProcessor.showMatAsImage(mTransMatForLine, mLineImage);

		// Watershed
		if (mWatershedMask != null) {
			mWatershedMask.release();
		}
		// Initial watershed mask
		mWatershedMask = new Mat(mOriginMat.size(), CvType.CV_8UC1);
		mWatershedMask.setTo(ImageProcessor.sBlack);

		// Push the initial state of cutout into history
		Bitmap tmpBitmap = Bitmap.createBitmap(mOriginMat.width(), mOriginMat.height(), Config.ARGB_8888);
		Utils.matToBitmap(mTransMatForLine, tmpBitmap);
	}

	// ============================================================
	// Life cycle
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mImageTouchDispatcher.onDestroy();
		setOnTouchListener(mImageTouchDispatcher = null);

		// BaseImage
		if (mOriginMat != null) {
			mOriginMat.release();
			mOriginMat = null;
		}

		if (mBaseImage != null) {
			Util.recycle(mBaseImage);
			mBaseImage = null;
		}

		// PreviewImage
		if (mTransMatForPreview != null) {
			mTransMatForPreview.release();
			mTransMatForPreview = null;
		}
		if (mPreviewImage != null) {
			Util.recycle(mPreviewImage);
			mPreviewImage = null;
		}

		// LineImage
		if (mTransMatForLine != null) {
			mTransMatForLine.release();
			mTransMatForLine = null;
		}

		if (mLineImage != null) {
			Util.recycle(mLineImage);
			mLineImage = null;
		}

		// CutOutImage
		if (mLineImage != null) {
			Util.recycle(mLineImage);
			mLineImage = null;
		}

		// Watershed
		if (mWatershedMask != null) {
			mWatershedMask.release();
			mWatershedMask = null;
		}
		removeAllViews();
	}

	// ============================================================
	// Override functions from super
	// ============================================================
	// Listeners
	/**
	 * Dispatch touch event base on navbar button
	 */
	public class ImageTouchDispatcher implements OnTouchListener {
		private ContainerTouchListener mContainerTouchListener;
		private ScaledImageViewTouchListener mScaledImageViewTouchListener;

		public ImageTouchDispatcher() {
			mContainerTouchListener = new ContainerTouchListener();
			mScaledImageViewTouchListener = new ScaledImageViewTouchListener().initConfigure(mBaseImage);
		}

		public void onDestroy() {
			mContainerTouchListener = null;
			mScaledImageViewTouchListener.onDestroy();
			mScaledImageViewTouchListener = null;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// Dispatch touch event based on state
			if ((mState == STATE.FG) || (mState == STATE.ERASER)) {
				return mContainerTouchListener.onTouch(v, event);
			} else if ((mState == STATE.ZOOMER) || (mState == STATE.SHAPE)) {
				if (mScaledImageViewTouchListener.onTouch(v, event)) {
					setAllImageMatrix(null);
					// Make stick container can be drag/scale with image
					getTransitionState();
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * For FG/BG/Eraser event
	 */
	private class ContainerTouchListener implements OnTouchListener {
		private Point prePoint;
		private Point curPoint;

		public ContainerTouchListener() {
			super();
			prePoint = new Point(0.0, 0.0);
			curPoint = new Point(0.0, 0.0);
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mTransMatForLine = mTransMatForLine.clone();
				mTransMatForPreview = mTransMatForPreview.clone();
				mWatershedMask = mWatershedMask.clone();

				prePoint.x = (event.getX() - mTransX) / mScale;
				prePoint.y = (event.getY() - mTransY) / mScale;
				break;
			case MotionEvent.ACTION_MOVE:
				curPoint.x = (event.getX() - mTransX) / mScale;
				curPoint.y = (event.getY() - mTransY) / mScale;

				if (mState == STATE.FG) {
					Core.line(mTransMatForLine, prePoint, curPoint, ImageProcessor.sWhite, mThickness, Core.LINE_8, 0);
					Core.line(mWatershedMask, prePoint, curPoint, ImageProcessor.sForeground, mThickness, Core.LINE_8, 0);
				} else if (mState == STATE.ERASER) {
					Core.line(mTransMatForLine, prePoint, curPoint, ImageProcessor.sTrans, mEraserThickness, Core.LINE_8, 0);
					Core.line(mTransMatForPreview, prePoint, curPoint, ImageProcessor.sTrans, mEraserThickness, Core.LINE_8, 0);
					ImageProcessor.showMatAsImage(mTransMatForPreview, mPreviewImage);
				}
				ImageProcessor.showMatAsImage(mTransMatForLine, mLineImage);

				prePoint.x = curPoint.x;
				prePoint.y = curPoint.y;

				break;
			case MotionEvent.ACTION_UP:
				if ((mState == STATE.FG) || (mState == STATE.ERASER)) {
					watershed();
				}
				break;
			}
			return true;
		}
	}

	/**
	 * A listener for handling drag/scale/rotate event of ImageView's bitmap.<br>
	 * <h3>Note:</h3> The target ImageView's layout should match parent, then set the touch event to it or its parent.<br>
	 * <h3>Usage:</h3> ScaledImageViewTouchListener scaledImageViewTouchListener = new getScaledImageViewTouchListener().initConfigure(view);
	 */
	public class ScaledImageViewTouchListener implements OnTouchListener {
		int NONE = 0;
		int DRAG = 1;
		int ZOOM = 2;

		private int mMode;

		private ImageView mTargetView;

		private Matrix tempMatrix;
		private Matrix startMatrix;

		private TransInfo transInfo;

		private PointF startPoint;
		private float startDistance;

		public class TransInfo {
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

		/**
		 * The target ImageView's scale type must set to ScaleType.MATRIX. After using, call onDestroy() to make sure listener will not keep the
		 * target ImageView's reference.
		 * 
		 * @param imageView
		 *            The target ImageView's bitmap will be changed by touch event.
		 */
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

		/**
		 * Release the target ImageView's reference
		 */
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
	}

	// ============================================================
	// Main functions
	public Mat watershed() {
		Mat resultOld = new Mat();
		Mat resultNew = new Mat(mWatershedMask.size(), CvType.CV_8U, ImageProcessor.sBlack);

		// Find user's forground contour
		Mat foreground = mWatershedMask.clone();
		Core.inRange(foreground, ImageProcessor.sForeground, ImageProcessor.sForeground, foreground);
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(foreground, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

		// Simulate the edge of foreground
		if (contours.size() > 0) {
			contours = ImageProcessor.combineContour(contours);

			Rect rect = Imgproc.boundingRect(contours.get(0));
			// Core.rectangle(mImageContainer.getTransdMatForLine(), new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
			// sWhite);
			// ImageProcessor.showMatAsImage(mImageContainer.getTransdMatForLine(), mImageContainer.getLineImage());
			double i = 0;
			double distance = 5;
			double interval = mThickness;
			double space;

			// Set timer
			double startTime, endTime, totTime;
			startTime = System.currentTimeMillis();
			// Start iterate procedure to find the minimum background bounds
			do {
				resultOld.release();
				resultOld = resultNew.clone();

				resultNew.setTo(ImageProcessor.sBlack);
				space = (i + distance) * interval;
				Point lt = new Point(rect.x - space, rect.y - space);
				Point br = new Point(rect.x + rect.width + space, rect.y + rect.height + space);
				Core.polylines(resultNew, contours, false, ImageProcessor.sForeground, mThickness);
				Core.rectangle(resultNew, lt, br, ImageProcessor.sBackground, mThickness);

				// Convert to 32SC1
				resultNew.convertTo(resultNew, CvType.CV_32S);

				// Watershed
				Imgproc.watershed(mOriginMat, resultNew);

				// Convert watershed result back to 8U
				resultNew.convertTo(resultNew, CvType.CV_8U);

				// Filter out the foreground and filled with white
				Core.compare(resultNew, ImageProcessor.sForeground, resultNew, Core.CMP_EQ);

				i++;
				if (i == 2) {
					break;
				}
			} while (Core.countNonZero(resultNew) > Core.countNonZero(resultOld));
			endTime = System.currentTimeMillis();
			totTime = endTime - startTime;
		}

		// Draw white(foreground) area
		// mImageContainer.getTransdMatForPreview().setTo(sTrans);
		mTransMatForPreview.setTo(ImageProcessor.sRed, resultNew);

		// Find erased area
		Mat background = mWatershedMask.clone();
		Core.inRange(background, ImageProcessor.sBackground, ImageProcessor.sBackground, background);
		mTransMatForPreview.setTo(ImageProcessor.sTrans, background);

		ImageProcessor.showMatAsImage(mTransMatForPreview, mPreviewImage);

		// Release all temporally reference/resource
		foreground.release();
		background.release();
		resultOld.release();
		resultNew.release();

		// Reset the watershed mask
		mWatershedMask.setTo(ImageProcessor.sTrans);

		return mTransMatForPreview;
	}

	// ============================================================
	// Get/Set functions

	/**
	 * Reset Base/Preview/LineImageView in the same matrix. Be always aware to use before {@link #setTransitionStateToStickMaskContainer}.
	 * 
	 * @param matrix
	 *            The target matrix that will be set to all image. If null, use BaseImage's matrix instead.
	 */
	private void setAllImageMatrix(Matrix matrix) {
		if (mBaseImage != null) {
			if (matrix == null) {
				matrix = mBaseImage.getImageMatrix();
			} else {
				mBaseImage.setImageMatrix(matrix);
			}
		}
		if (mPreviewImage != null) {
			mPreviewImage.setImageMatrix(matrix);
		}
		if (mLineImage != null) {
			mLineImage.setImageMatrix(matrix);
		}
	}

	/**
	 * Save the transition state which was changed by drag/zoom.
	 */
	public void getTransitionState() {
		mMatrix.set(mBaseImage.getImageMatrix());

		float[] values = new float[9];
		mMatrix.getValues(values);
		// Save the transition state
		mTransX = values[Matrix.MTRANS_X];
		mTransY = values[Matrix.MTRANS_Y];
		if (mScale > values[Matrix.MSCALE_X]) {
			mThickness = (int) (ImageProcessor.BASIC_THICKNESS * values[Matrix.MSCALE_X]);
		} else {
			mThickness = (int) (ImageProcessor.BASIC_THICKNESS / values[Matrix.MSCALE_X]);
		}
		mEraserThickness = mThickness * 4;
		mScale = values[Matrix.MSCALE_X];
	}

	public void setState(STATE state) {
		mState = state;
	}

	public Mat getWatershedMask() {
		return mTransMatForPreview;
	}
	// ============================================================
}
