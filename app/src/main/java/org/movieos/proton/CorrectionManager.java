package org.movieos.proton;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

public class CorrectionManager implements Parcelable {
    private transient static final String TAG = CorrectionManager.class.getSimpleName();

    double mRotation = 0;
    double mVerticalSkew = 0;
    double mHorizontalSkew = 0;
    boolean mCrop = true;

    public CorrectionManager() {
    }

    protected CorrectionManager(@NonNull Parcel in) {
        mRotation = in.readDouble();
        mVerticalSkew = in.readDouble();
        mHorizontalSkew = in.readDouble();
        mCrop = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(mRotation);
        dest.writeDouble(mVerticalSkew);
        dest.writeDouble(mHorizontalSkew);
        dest.writeByte((byte) (mCrop ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CorrectionManager> CREATOR = new Creator<CorrectionManager>() {
        @NonNull
        @Override
        public CorrectionManager createFromParcel(@NonNull Parcel in) {
            return new CorrectionManager(in);
        }

        @NonNull
        @Override
        public CorrectionManager[] newArray(int size) {
            return new CorrectionManager[size];
        }
    };

    @Override
    public String toString() {
        return String.format(Locale.US, "CorrectionManager{%f %f %f %s}", mRotation, mHorizontalSkew, mVerticalSkew, mCrop ? "cropped" : "not cropped");
    }

    @NonNull
    public Matrix getMatrix(@Nullable Bitmap source) {
        Matrix matrix = new Matrix();
        if (source == null) {
            return matrix;
        }

        int w = source.getWidth();
        int h = source.getHeight();
        double hcos = Math.cos(Math.toRadians(Math.abs(mHorizontalSkew)));
        double hsin2 = Math.sin(Math.toRadians(Math.abs(mHorizontalSkew * 2)));

        float hskew = (float) (mHorizontalSkew * w * 0.02f);
        float vskew = (float) (mVerticalSkew * w * 0.02f);

        float[] bounds = new float[]{
            0, 0,
            w, 0,
            0, h,
            w, h
        };
        float[] dest = new float[]{
            0, 0,
            w, 0,
            0, h,
            w, h
        };
        double perspectiveCorrection = 0.3;
        if (hskew > 0) {
            dest[4] = (float) (hsin2 * w * perspectiveCorrection);
            dest[5] = (float) (hcos * h);
            dest[6] = (float) (w - hsin2 * w * perspectiveCorrection);
            dest[7] = (float) (hcos * h);
        } else {
            dest[0] = (float) (hsin2 * w * perspectiveCorrection);
            dest[1] = (float) (h - hcos * h);
            dest[2] = (float) (w - hsin2 * w * perspectiveCorrection);
            dest[3] = (float) (h - hcos * h);
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
        matrix.preRotate((float) mRotation, w / 2, h / 2);

        // if mCrop, we want the largest image of the same aspect as the original that
        // fits entirely inside our new poly. If not mCrop, we'll return the smallest
        // rectangle of the same aspect that entirely encloses the target poly.
        RectF start = new RectF(0, 0, w, h);
        float[] targetPoly = new float[8];
        matrix.mapPoints(targetPoly, bounds);
        // bounding rectangle
        RectF boundsRect;
        float scale;
        if (mCrop) {
            // in theory this should be the largest rectangle that fits. This is hard given that
            // rotation is a thing. So we do the simplest thing that mostly works.
            boundsRect = new RectF(
                Math.max(targetPoly[0], targetPoly[4]),
                Math.max(targetPoly[1], targetPoly[3]),
                Math.min(targetPoly[2], targetPoly[6]),
                Math.min(targetPoly[5], targetPoly[7])
            );
            scale = Math.min(boundsRect.width() / start.width(), boundsRect.height() / start.height());
        } else {
            boundsRect = new RectF(
                Math.min(Math.min(targetPoly[0], targetPoly[2]), Math.min(targetPoly[4], targetPoly[6])),
                Math.min(Math.min(targetPoly[1], targetPoly[3]), Math.min(targetPoly[5], targetPoly[7])),
                Math.max(Math.max(targetPoly[0], targetPoly[2]), Math.max(targetPoly[4], targetPoly[6])),
                Math.max(Math.max(targetPoly[1], targetPoly[3]), Math.max(targetPoly[5], targetPoly[7]))
            );
            scale = Math.max(boundsRect.width() / start.width(), boundsRect.height() / start.height());
        }

        Matrix shrink = new Matrix();
        //shrink.setPolyToPoly()
        matrix.postConcat(shrink);

//        // build the aspect-correct rectangle that has the bounding rectangle at its center
//        RectF end = new RectF(0, 0, scale * start.width(), scale * start.height());
//        end.offsetTo(boundsRect.left, boundsRect.top);
//        end.offset((boundsRect.width() - end.width()) / 2, (boundsRect.height() - end.height()) / 2);
//        Matrix shrink = new Matrix();
//        shrink.setRectToRect(start, end, Matrix.ScaleToFit.FILL);
//        shrink.invert(shrink);
//        matrix.postConcat(shrink);

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
