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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.simplewatershed.R;
import com.example.simplewatershed.util.Util;
import com.example.simplewatershed.util.Util.ScaledImageViewTouchListener;

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
		private boolean ZOOM = false;

		private ContainerTouchListener mContainerTouchListener;
		private ScaledImageViewTouchListener mScaledImageViewTouchListener;

		public ImageTouchDispatcher() {
			mContainerTouchListener = new ContainerTouchListener();
			mScaledImageViewTouchListener = new ScaledImageViewTouchListener(mBaseImage);
			// Set the listener will handle drag event only on two fingers on the screen
			mScaledImageViewTouchListener.setSingleDrag(false);
			// Set the listener will ignore any rotate event
			mScaledImageViewTouchListener.setRotatable(false);
		}

		public void onDestroy() {
			mContainerTouchListener = null;
			mScaledImageViewTouchListener.onDestroy();
			mScaledImageViewTouchListener = null;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			boolean result = false;

			// Zoom mode will be turned on only when the second finger touch on the screen, and turned off when all fingers take off<br>
			// If not in ZOOM mode, the flow will be DOWN -> MOVE -> UP
			// If in ZOOM mode, the flow will be POINTER_DOWN -> MOVE -> POINTER_UP
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (!ZOOM) {
					result = mContainerTouchListener.onTouch(v, event);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (!ZOOM) {
					result = mContainerTouchListener.onTouch(v, event);
				} else {
					result = mScaledImageViewTouchListener.onTouch(v, event);
					if (mScaledImageViewTouchListener.onTouch(v, event)) {
						setAllImageMatrix(null);
						// Make stick container can be drag/scale with image
						getTransitionState();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				if (!ZOOM) {
					result = mContainerTouchListener.onTouch(v, event);
				} else {
					ZOOM = false;
					result = mScaledImageViewTouchListener.onTouch(v, event);
				}
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				ZOOM = true;
				result = mScaledImageViewTouchListener.onTouch(v, event);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				result = mScaledImageViewTouchListener.onTouch(v, event);
				break;
			}

			return result;
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

				if (isMove()) {
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
				}

				break;
			case MotionEvent.ACTION_UP:
				if ((mState == STATE.FG) || (mState == STATE.ERASER)) {
					watershed();
					getWatershedMaskRect();
				}
				break;
			}
			return true;
		}

		public boolean isMove() {
			return (Math.abs(curPoint.x - prePoint.x) >= 10) || (Math.abs(curPoint.y - prePoint.y) >= 10);
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

	/**
	 * Get watershed mask's outer rect.
	 */
	public Rect getWatershedMaskRect() {
		Mat tmpTransMatForPreview = new Mat(mTransMatForPreview.size(), CvType.CV_8UC1);
		Imgproc.cvtColor(mTransMatForPreview, tmpTransMatForPreview, Imgproc.COLOR_BGRA2GRAY);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(tmpTransMatForPreview, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

		return ImageProcessor.combineContourRect(contours);
	}

	public void setState(STATE state) {
		mState = state;
	}

	public Mat getWatershedMask() {
		return mTransMatForPreview;
	}
	// ============================================================
}
