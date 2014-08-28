package org.movieos.proton;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;

public class CorrectionActivity extends Activity implements DialControl.OnDialChangeListener {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    enum Mode {
        ROTATE, VERTICAL_SKEW, HORIZONTAL_SKEW
    };

    @InjectView(R.id.image)
    ImageView mImageView;

    @InjectView(R.id.grid)
    GridOverlay mGrid;

    @InjectView(R.id.dial)
    DialControl mDial;

    @InjectViews({
            R.id.rotate_button,
            R.id.vertical_skew_button,
            R.id.horizontal_skew_button
    })
    List<Button> mButtons;

    Bitmap mBitmap;
    CorrectionManager mCorrection;
    Mode mMode = Mode.ROTATE;
    private ShareActionProvider mShareActionProvider;

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

        mDial.setOnChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.correction, menu);
        MenuItem item = menu.findItem(R.id.action_share);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                save();
                return true;
            case R.id.action_share:
                share();
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
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
        mDial.setValue(0);
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
        switch (mMode) {
            case ROTATE:
                mDial.setRange(-90, 90);
                mDial.setValue(mCorrection.getRotation());
                break;
            case VERTICAL_SKEW:
                mDial.setRange(-1, 1);
                mDial.setValue(mCorrection.getVerticalSkew());
                break;
            case HORIZONTAL_SKEW:
                mDial.setRange(-1, 1);
                mDial.setValue(mCorrection.getHorizontalSkew());
                break;
        }
    }


    @Override
    public void onDialValueChanged(double value) {
        switch (mMode) {
            case ROTATE:
                mCorrection.setRotation(value);
                break;
            case VERTICAL_SKEW:
                mCorrection.setVerticalSkew(value);
                break;
            case HORIZONTAL_SKEW:
                mCorrection.setHorizontalSkew(value);
                break;
        }
        updateImage();
    }

    File getAlbumStorageDir(String albumName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    void writeToFile(File file) {
        Bitmap output = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(mBitmap, mCorrection.getMatrix(mBitmap), paint);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            output.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            stream.close();
        } catch (IOException e) {
            Log.e(TAG, "java.io.IOException", e);
            Toast.makeText(this, R.string.write_error, Toast.LENGTH_LONG).show();
            return;
        } finally {
            output.recycle();
        }
    }

    void save() {
        File album = getAlbumStorageDir(getString(R.string.app_name));
        writeToFile(new File(album, filename()));
        Toast.makeText(this, R.string.save_complete, Toast.LENGTH_LONG).show();
    }

    String filename() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return format.format(new Date()) + ".jpg";
    }

    void share() {
        File folder = new File(getExternalFilesDir("share"), "temp");
        folder.mkdirs();
        // cleaning up after share is hard, we'll clean up before share
        // which is 90% as good and way easier to follow.
        for (String filename : folder.list()) {
            new File(folder, filename).delete();
        }
        File temp = new File(folder, filename());
        writeToFile(temp);
        Log.i(TAG, "tmep is " + temp.toURI());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(temp));
        startActivity(Intent.createChooser(intent, getString(R.string.share_title)));
    }
}
