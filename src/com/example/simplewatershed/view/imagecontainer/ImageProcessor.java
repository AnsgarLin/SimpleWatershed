package com.example.simplewatershed.view.imagecontainer;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

public class ImageProcessor {
	public static final int MIN_SCALAR_FOR_WATERSHED = 0;
	public static final int MAX_SCALAR_FOR_WATERSHED = 3;
	public static final int BASIC_THICKNESS = 8;
	public static final int MAX_THICKNESS = 20;
	public static final int BASIC_SCALE = 1;
	public static final int MAX_SCALE = 10;
	public static final int BASIC_BORDER_THICKNESS = 11;
	public static final int MAX_BORDER_THICKNESS = 21;
	public static final int BASIC_SPREAD = 1;
	public static final int MAX_SPREAD = 21;
	public static final int BASIC_COLOR = 8421504;
	public static final int MAX_COLOR = 16777215;
	public static final int TIMES_BWTWEEN_THICKNESS = 4;
	public static final Scalar sRed = new Scalar(255.0, 0.0, 0.0, 255.0 * 0.5d);
	public static final Scalar sTrans = new Scalar(0.0, 0.0, 0.0, 0.0);
	public static final Scalar sBlack = new Scalar(0.0, 0.0, 0.0, 255.0);
	public static final Scalar sWhite = new Scalar(255.0, 255.0, 255.0, 255.0);
	public static final Scalar sForeground = new Scalar(1.0);
	public static final Scalar sBackground = new Scalar(2.0);

	/**
	 * Show mat as bitmap on image view
	 * 
	 * @param targetMat
	 *            The mat that will be show
	 * @param targetView
	 *            The ImageView will show the mat
	 */
	public static Bitmap showMatAsImage(Mat targetMat, ImageView targetView) {
		Bitmap bitmap = null;
		if (targetMat != null) {
			bitmap = Bitmap.createBitmap(targetMat.width(), targetMat.height(), Config.ARGB_8888);
			Utils.matToBitmap(targetMat, bitmap);
		}
		recycle(targetView);
		targetView.setImageBitmap(bitmap);
		return bitmap;
	}

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

	/**
	 * Combine all MatOfPoint in list into a single one.
	 * 
	 * @param contours
	 *            The target list of MatOfPoint.
	 * @return The original list with one MatOfPoint.
	 */
	public static List<MatOfPoint> combineContour(List<MatOfPoint> contours) {
		List<org.opencv.core.Point> mixPoints = new ArrayList<org.opencv.core.Point>();
		for (int k = 0; k < contours.size(); k++) {
			mixPoints.addAll(contours.get(k).toList());
		}
		contours.clear();
		contours.add(new MatOfPoint(mixPoints.toArray(new org.opencv.core.Point[mixPoints.size()])));
		mixPoints.clear();
		mixPoints = null;
		return contours;
	}

	/**
	 * Combine all MatOfPoint's {@link Rect} in list.
	 * 
	 * @param contours
	 *            The target list of MatOfPoint.
	 * @return The {@link Rect} contains all contours, null if the target contour list is empty.
	 */
	public static Rect combineContourRect(List<MatOfPoint> contours) {
		if (contours.size() == 0) {
			return null;
		}

		Point tl = Imgproc.boundingRect(contours.get(0)).tl();
		Point br = Imgproc.boundingRect(contours.get(0)).br();

		for (int i = 1; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));
			if (tl.x > rect.tl().x) {
				tl.x = rect.tl().x;
			}

			if (tl.y > rect.tl().y) {
				tl.y = rect.tl().y;
			}

			if (br.x < rect.br().x) {
				br.x = rect.br().x;
			}

			if (br.y < rect.br().y) {
				br.y = rect.br().y;
			}
		}
		return new Rect(tl, br);
	}

	/**
	 * Convert color from int to Scala with alpha is 255
	 * 
	 * @param scalar
	 *            The scalar to save the value from color, null if want a new one
	 * @param color
	 *            The target color
	 * @return Scalar of the target color
	 */
	public static Scalar intToScalar(Scalar scalar, int color) {
		double r = Color.red(color);
		double g = Color.green(color);
		double b = Color.blue(color);
		if (scalar == null) {
			return new Scalar(r, g, b, 255.0);
		}
		scalar.set(new double[] { r, g, b, 255.0 });
		return scalar;
	}
}
