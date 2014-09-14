package org.movieos.proton;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class CorrectionManager {
    private transient static final String TAG = CorrectionManager.class.getSimpleName();

    private double mRotation;
    private double mVerticalSkew;
    private double mHorizontalSkew; // TODO

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

        // rotate and scale the image to avoid write corners. Math
        // here is wrong, but almost right, I'll deal with this when I care more.
        matrix.postRotate((float) mRotation, source.getWidth() / 2, source.getHeight() / 2);
        double cos = Math.abs(Math.sin(Math.toRadians(mRotation) * 2));
        double scale = cos * (Math.sqrt(2) - 1) + 1;
        matrix.postScale((float)scale, (float)scale, source.getWidth() / 2, source.getHeight() / 2);

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
}
