package org.movieos.proton;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.InputStream;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;

public class CorrectionActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    @InjectView(R.id.image)
    ImageView mImageView;

    @InjectView(R.id.rotation)
    SeekBar mRotation;

    @InjectView(R.id.horizontal_distortion)
    SeekBar mHorizontalSkew;

    @InjectView(R.id.vertical_distortion)
    SeekBar mVerticalSkew;

    @InjectViews({
            R.id.rotation,
            R.id.horizontal_distortion,
            R.id.vertical_distortion,
    })
    List<SeekBar> mSliders;

    //    IplImage mImage;
    Bitmap mBitmap;
    CorrectionManager mCorrection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correction_activity);
        ButterKnife.inject(this);

        // TODO this needs to be a file loader, obviously
        InputStream photo = getResources().openRawResource(R.raw.photo);
        Bitmap source = BitmapFactory.decodeStream(photo);

        Log.i(TAG, "source is " + source.getWidth() + "x" + source.getHeight());


        // Create scaled bitmap the size of the screen. This is probably
        // cheaper than matrixing a huge jpeg.
        int size = getResources().getDisplayMetrics().widthPixels;
        // square crop
        mBitmap = ThumbnailUtils.extractThumbnail(source, size, size);
        //mBitmap = Bitmap.createScaledBitmap(source, size, size, false);

        mCorrection = new CorrectionManager();

        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
        mImageView.setImageBitmap(mBitmap);

        for (SeekBar seek : mSliders) {
            seek.setOnSeekBarChangeListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateImage();
    }

    private void updateImage() {
        mImageView.setImageMatrix(mCorrection.getMatrix(mBitmap));
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float amount = (float)(progress - 500) / 1000;
        if (seekBar == mRotation) {
            mCorrection.setRotation(amount * 180);
        } else if (seekBar == mVerticalSkew) {
            mCorrection.setVerticalSkew(amount);
        } else if (seekBar == mHorizontalSkew) {
            mCorrection.setHorizontalSkew(amount);
        }
        updateImage();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
