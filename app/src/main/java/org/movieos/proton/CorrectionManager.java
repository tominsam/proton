package org.movieos.proton;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import org.parceler.Parcel;

@Parcel
public class CorrectionManager {
    private transient static final String TAG = CorrectionManager.class.getSimpleName();

    double mRotation;
    double mVerticalSkew;
    double mHorizontalSkew;
    boolean mCrop;

    public Matrix getMatrix(Bitmap source) {
        Matrix matrix = new Matrix();

        float hskew = (float) (mHorizontalSkew * source.getWidth() * 0.02f);
        float vskew = (float) (mVerticalSkew * source.getWidth() * 0.02f);
        float[] bounds = new float[] {
                0, 0,
                source.getWidth(), 0,
                0, source.getHeight(),
                source.getWidth(), source.getHeight()
        };
        float[] dest = new float[] {
                0, 0,
                source.getWidth(), 0,
                0, source.getHeight(),
                source.getWidth(), source.getHeight()
        };
        if (hskew > 0) {
            dest[4] -= hskew;
            dest[5] += hskew;
            dest[6] += hskew;
            dest[7] += hskew;
        } else {
            dest[0] += hskew;
            dest[1] += hskew;
            dest[2] -= hskew;
            dest[3] += hskew;
        }
        if (vskew > 0) {
            dest[0] -= vskew;
            dest[1] -= vskew;
            dest[4] -= vskew;
            dest[5] += vskew;
        } else {
            dest[2] -= vskew;
            dest[3] += vskew;
            dest[6] -= vskew;
            dest[7] -= vskew;
        }

        matrix.setPolyToPoly(bounds, 0, dest, 0, 4);
        matrix.preRotate((float) mRotation, source.getWidth() / 2, source.getHeight() / 2);

        // if mCrop, we want the largest image of the same aspect as the original that
        // fits entirely inside our new poly. If not mCrop, we'll return the smallest
        // rectangle of the same aspect that entirely encloses the target poly.
        RectF start = new RectF(0, 0, source.getWidth(), source.getHeight());
        RectF end;
        float[] targetPoly = new float[8];
        matrix.mapPoints(targetPoly, bounds);

        if (mCrop) {
            end = new RectF(start);
        } else {
            // bounding rectangle
            RectF boundsRect = new RectF(
                    Math.min(Math.min(targetPoly[0], targetPoly[2]), Math.min(targetPoly[4], targetPoly[6])),
                    Math.min(Math.min(targetPoly[1], targetPoly[3]), Math.min(targetPoly[5], targetPoly[7])),
                    Math.max(Math.max(targetPoly[0], targetPoly[2]), Math.max(targetPoly[4], targetPoly[6])),
                    Math.max(Math.max(targetPoly[1], targetPoly[3]), Math.max(targetPoly[5], targetPoly[7]))
            );
            float scale = Math.max(boundsRect.width() / start.width(), boundsRect.height() / start.height());
            end = new RectF(0, 0, scale * start.width(), scale * start.height());
            end.offsetTo(boundsRect.left, boundsRect.top);
            end.offset((boundsRect.width() - end.width()) / 2, (boundsRect.height() - end.height()) / 2);
        }

        Matrix shrink = new Matrix();
        shrink.setRectToRect(start, end, Matrix.ScaleToFit.FILL);
        shrink.invert(shrink);
        matrix.postConcat(shrink);
        return matrix;
    }


    public double getRotation() {
        return mRotation;
    }

    public void setRotation(double rotation) {
        mRotation = rotation;
    }

    public double getVerticalSkew() {
        return mVerticalSkew;
    }

    public void setVerticalSkew(double verticalSkew) {
        mVerticalSkew = verticalSkew;
    }

    public double getHorizontalSkew() {
        return mHorizontalSkew;
    }

    public void setHorizontalSkew(double horizontalSkew) {
        mHorizontalSkew = horizontalSkew;
    }

    public boolean isCrop() {
        return mCrop;
    }

    public void setCrop(boolean crop) {
        mCrop = crop;
    }
}
