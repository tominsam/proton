package org.movieos.proton;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

public class CorrectionManager {
    private transient static final String TAG = CorrectionManager.class.getSimpleName();

    private float mRotation;
    private float mVerticalSkew;
    private float mHorizontalSkew; // TODO

    public Matrix getMatrix(Bitmap source) {
        Matrix matrix = new Matrix();

        float skew = mHorizontalSkew * source.getWidth() * 0.5f;
        float[] bounds = new float[] {
                0, 0,
                source.getWidth(), 0,
                0, source.getHeight(),
                source.getWidth(), source.getHeight()
        };
        float[] dest;
        if (skew > 0) {
            dest = new float[] {
                    0, 0,
                    source.getWidth(), 0,
                    -skew, source.getHeight(),
                    source.getWidth() + skew, source.getHeight()
            };
        } else {
            dest = new float[] {
                    skew, 0,
                    source.getWidth() - skew, 0,
                    0, source.getHeight(),
                    source.getWidth(), source.getHeight()
            };
        }
        matrix.setPolyToPoly(bounds, 0, dest, 0, 4);

        // rotate and scale the image to avoid write corners
        matrix.postRotate(mRotation, source.getWidth() / 2, source.getHeight() / 2);
        double cos = Math.abs(Math.sin(Math.toRadians(mRotation) * 2));
        Log.i(TAG, "cos is " + cos);
        double scale = cos * (Math.sqrt(2) - 1) + 1;
        Log.i(TAG, "scale is " + scale);
        matrix.postScale((float)scale, (float)scale, source.getWidth() / 2, source.getHeight() / 2);

        return matrix;
    }


    public float getRotation() {
        return mRotation;
    }

    public void setRotation(float rotation) {
        mRotation = rotation;
    }

    public float getVerticalSkew() {
        return mVerticalSkew;
    }

    public void setVerticalSkew(float verticalSkew) {
        mVerticalSkew = verticalSkew;
    }

    public float getHorizontalSkew() {
        return mHorizontalSkew;
    }

    public void setHorizontalSkew(float horizontalSkew) {
        mHorizontalSkew = horizontalSkew;
    }
}
