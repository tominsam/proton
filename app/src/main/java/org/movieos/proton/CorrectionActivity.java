package org.movieos.proton;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import java.io.InputStream;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;

public class CorrectionActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    enum Mode {
        ROTATE, VERTICAL_SKEW, HORIZONTAL_SKEW
    };

    @InjectView(R.id.image)
    ImageView mImageView;

    @InjectView(R.id.grid)
    GridOverlay mGrid;

    @InjectView(R.id.seekbar)
    SeekBar mSeekBar;

    @InjectViews({
            R.id.rotate_button,
            R.id.vertical_skew_button,
            R.id.horizontal_skew_button
    })
    List<Button> mButtons;

    Bitmap mBitmap;
    CorrectionManager mCorrection;
    Mode mMode = Mode.ROTATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correction_activity);
        ButterKnife.inject(this);

        // TODO this needs to be a file loader, obviously
        InputStream photo = getResources().openRawResource(R.raw.photo);
        Bitmap source = BitmapFactory.decodeStream(photo);

        // Create scaled bitmap the size of the screen. This is probably
        // cheaper than matrixing a huge jpeg.
        int size = getResources().getDisplayMetrics().widthPixels;
        mBitmap = ThumbnailUtils.extractThumbnail(source, size, size);

        mCorrection = new CorrectionManager();
        updateControls();
        activate((Button) findViewById(R.id.rotate_button));

        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
        mImageView.setImageBitmap(mBitmap);

        mGrid.setVisibility(View.INVISIBLE);

        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateImage();
    }

    @OnClick(R.id.rotate_button)
    void onRotate(Button v) {
        mMode = Mode.ROTATE;
        updateControls();
        activate(v);
    }

    @OnClick(R.id.vertical_skew_button)
    void onVertical(Button v) {
        mMode = Mode.VERTICAL_SKEW;
        updateControls();
        activate(v);
    }

    @OnClick(R.id.horizontal_skew_button)
    void onHorizontal(Button v) {
        mMode = Mode.HORIZONTAL_SKEW;
        updateControls();
        activate(v);
    }

    @OnClick(R.id.reset_button)
    void onReset() {
        mSeekBar.setProgress(500);
    }

    @OnClick(R.id.grid_button)
    void onGrid(ToggleButton b) {
        mGrid.setVisibility(b.isChecked() ? View.VISIBLE : View.INVISIBLE);
    }

    void activate(Button v) {
        for (Button b : mButtons) {
            b.setActivated(false);
        }
        v.setActivated(true);
    }

    private void updateImage() {
        mImageView.setImageMatrix(mCorrection.getMatrix(mBitmap));
    }

    private void updateControls() {
        float progress = 0;
        switch (mMode) {
            case ROTATE:
                progress = mCorrection.getRotation() / 180;
                break;
            case VERTICAL_SKEW:
                progress = mCorrection.getVerticalSkew();
                break;
            case HORIZONTAL_SKEW:
                progress = mCorrection.getHorizontalSkew();
                break;
        }
        mSeekBar.setProgress((int)(progress * 1000 + 500));
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float amount = (float)(progress - 500) / 1000; // -1 to +1
        switch (mMode) {
            case ROTATE:
                mCorrection.setRotation(amount * 180);
                break;
            case VERTICAL_SKEW:
                mCorrection.setVerticalSkew(amount);
                break;
            case HORIZONTAL_SKEW:
                mCorrection.setHorizontalSkew(amount);
                break;
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
