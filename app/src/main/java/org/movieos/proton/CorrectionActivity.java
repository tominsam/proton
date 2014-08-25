package org.movieos.proton;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class CorrectionActivity extends Activity {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    @InjectView(R.id.image)
    ImageView mImage;

    @InjectView(R.id.seek)
    SeekBar mSeek;

    Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correction_activity);
        ButterKnife.inject(this);

        InputStream photo = getResources().openRawResource(R.raw.photo);

        mBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(photo), 300, 300, false);
        mBitmap.setDensity(Bitmap.DENSITY_NONE);

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
        int value = mSeek.getProgress();
        Matrix matrix = new Matrix();
        float amount = (float)(value - 100) / 100;
        matrix.preSkew(amount, amount, amount, amount);
        Bitmap converted = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
        mImage.setImageDrawable(new BitmapDrawable(getResources(), converted));
    }


}
