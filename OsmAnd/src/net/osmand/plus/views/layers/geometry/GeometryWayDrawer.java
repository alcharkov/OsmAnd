package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import net.osmand.data.RotatedTileBox;

import java.util.ArrayList;
import java.util.List;

public class GeometryWayDrawer<T extends GeometryWayContext> {

	private T context;

	public GeometryWayDrawer(T context) {
		this.context = context;
	}

	public T getContext() {
		return context;
	}

	public void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, List<Float> tx, List<Float> ty,
								   List<Double> angles, List<Double> distances, double distPixToFinish, List<GeometryWayStyle<?>> styles) {
		List<PathPoint> arrows = new ArrayList<>();

		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int left = -w / 4;
		int right = w + w / 4;
		int top = -h / 4;
		int bottom = h + h / 4;

		boolean hasStyles = styles != null && styles.size() == tx.size();
		double zoomCoef = tb.getZoomAnimation() > 0 ? (Math.pow(2, tb.getZoomAnimation() + tb.getZoomFloatPart())) : 1f;
		Bitmap arrow = context.getArrowBitmap();
		int arrowHeight = arrow.getHeight();
		double defaultPxStep;
		if (hasStyles && styles.get(0) != null) {
			defaultPxStep = styles.get(0).getPointStepPx(zoomCoef);
		} else {
			defaultPxStep = arrowHeight * 4f * zoomCoef;
		}
		double pxStep = defaultPxStep;
		double dist = 0;
		if (distPixToFinish != 0) {
			dist = distPixToFinish - pxStep * ((int) (distPixToFinish / pxStep)); // dist < 1
		}
		for (int i = tx.size() - 2; i >= 0; i--) {
			GeometryWayStyle<?> style = hasStyles ? styles.get(i) : null;
			float px = tx.get(i);
			float py = ty.get(i);
			float x = tx.get(i + 1);
			float y = ty.get(i + 1);
			double distSegment = distances.get(i + 1);
			double angle = angles.get(i + 1);
			if (distSegment == 0) {
				continue;
			}
			pxStep = style != null ? style.getPointStepPx(zoomCoef) : defaultPxStep;
			if (dist >= pxStep) {
				dist = 0;
			}
			double percent = 1 - (pxStep - dist) / distSegment;
			dist += distSegment;
			while (dist >= pxStep) {
				double pdx = (x - px) * percent;
				double pdy = (y - py) * percent;
				float iconx = (float) (px + pdx);
				float icony = (float) (py + pdy);
				if (GeometryWay.isIn(iconx, icony, left, top, right, bottom)) {
					arrows.add(getArrowPathPoint(iconx, icony, style, angle));
				}
				dist -= pxStep;
				percent -= pxStep / distSegment;
			}
		}
		for (int i = arrows.size() - 1; i >= 0; i--) {
			PathPoint a = arrows.get(i);
			if (!tb.isZoomAnimated() || a.style.isVisibleWhileZooming()) {
				a.draw(canvas, context);
			}
		}
	}

	protected PathPoint getArrowPathPoint(float iconx, float icony, GeometryWayStyle<?> style, double angle) {
		return new PathPoint(iconx, icony, angle, style);
	}

	public void drawPath(Canvas canvas, Path path, GeometryWayStyle<?> style) {
		context.getAttrs().customColor = style.getColor();
		context.getAttrs().drawPath(canvas, path);
	}

	public static class PathPoint {
		float x;
		float y;
		double angle;
		GeometryWayStyle<?> style;

		private Matrix matrix = new Matrix();

		public PathPoint(float x, float y, double angle, GeometryWayStyle<?> style) {
			this.x = x;
			this.y = y;
			this.angle = angle;
			this.style = style;
		}

		protected Matrix getMatrix() {
			return matrix;
		}

		void draw(Canvas canvas, GeometryWayContext context) {
			if (style != null && style.getPointBitmap() != null) {
				Bitmap bitmap = style.getPointBitmap();
				Integer pointColor = style.getPointColor();
				float paintH2 = bitmap.getHeight() / 2f;
				float paintW2 = bitmap.getWidth() / 2f;

				matrix.reset();
				matrix.postRotate((float) angle, paintW2, paintH2);
				matrix.postTranslate(x - paintW2, y - paintH2);
				if (pointColor != null) {
					Paint paint = context.getPaintIconCustom();
					paint.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.SRC_IN));
					canvas.drawBitmap(bitmap, matrix, paint);
				} else {
					if (style.hasPaintedPointBitmap()) {
						Paint paint = context.getPaintIconCustom();
						paint.setColorFilter(null);
						canvas.drawBitmap(bitmap, matrix, paint);
					} else {
						canvas.drawBitmap(bitmap, matrix, context.getPaintIcon());
					}
				}
			}
		}
	}
}
