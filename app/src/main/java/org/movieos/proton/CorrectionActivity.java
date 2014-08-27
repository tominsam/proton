package org.movieos.proton;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class CorrectionActivity extends Activity {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    @InjectView(R.id.image)
    ImageView mImageView;

    @InjectView(R.id.seek)
    SeekBar mSeek;

//    IplImage mImage;
    Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correction_activity);
        ButterKnife.inject(this);

        InputStream photo = getResources().openRawResource(R.raw.photo);

        int size = getResources().getDisplayMetrics().widthPixels;
        mBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(photo), size, size, false);
        mBitmap.setDensity(Bitmap.DENSITY_NONE);

//        mImage = IplImage.create(mBitmap.getWidth(), mBitmap.getHeight(), IPL_DEPTH_8U, 4);
//        mBitmap.copyPixelsToBuffer(mImage.getByteBuffer());
        mImageView.setImageBitmap(mBitmap);

        mSeek.setProgress(100);
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImage();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateImage();
    }

    private void updateImage() {
//        IplImage output = cvCloneImage(mImage);

//        int size = (mSeek.getProgress() / 50) * 2 - 1;
//        Log.i(TAG, "size is " + size);
        //cvSmooth(output, output, CV_GAUSSIAN, size, 0, 0, 0);

//        float offset = (float)mSeek.getProgress() / 100 - 1;
//
//        CvPoint2D32f srcQuad = new CvPoint2D32f(4);
//        srcQuad.put(0, 0, 20, 0, 20, 20, 0, 20);
//        CvPoint2D32f dstQuad = new CvPoint2D32f(4);
//        dstQuad.put(0, 0, 20 + offset, 0, 20, 20, 0, 20);
//
//        CvMat warp = cvCreateMat(3, 3, CV_32FC1);
//        cvGetPerspectiveTransform(srcQuad, dstQuad, warp);
//        cvWarpPerspective(output, output, warp);
//
//        Bitmap bitmapOut = Bitmap.createBitmap(mImage.width(), mImage.height(), Bitmap.Config.ARGB_8888);
//        bitmapOut.copyPixelsFromBuffer(output.getByteBuffer());


        Matrix matrix = new Matrix();

        float skew = (float)(mSeek.getProgress() - 100)/100 * mBitmap.getWidth() * 0.2f;

        float[] bounds = new float[] {
                0, 0,
                mBitmap.getWidth(), 0,
                0, mBitmap.getHeight(),
                mBitmap.getWidth(), mBitmap.getHeight()
        };
        float[] dest;
        if (skew > 0) {
            dest = new float[] {
                    0, 0,
                    mBitmap.getWidth(), 0,
                    -skew, mBitmap.getHeight(),
                    mBitmap.getWidth() + skew, mBitmap.getHeight()
            };
        } else {
            dest = new float[] {
                    skew, 0,
                    mBitmap.getWidth() - skew, 0,
                    0, mBitmap.getHeight(),
                    mBitmap.getWidth(), mBitmap.getHeight()
            };
        }
        matrix.setPolyToPoly(bounds, 0, dest, 0, 4);

        mImageView.setImageMatrix(matrix);
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
    }


}
