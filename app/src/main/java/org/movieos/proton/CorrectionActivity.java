package org.movieos.proton;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;

public class CorrectionActivity extends Activity implements DialControl.OnDialChangeListener {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    private static final boolean ALWAYS_SAVE = true; // TODO setting or something

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
    List<ImageButton> mButtons;

    Bitmap mSource;
    Bitmap mBitmap;
    CorrectionManager mCorrection;
    Mode mMode = Mode.ROTATE;
    private ShareActionProvider mShareActionProvider;
    float mLatitude;
    float mLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correction_activity);
        ButterKnife.inject(this);

        // start paths:
        // (a) opened with URI (MainActivity document picker)
        // (b) Opened with bitmap in data (MainActivity camera)
        // (c) Opened with share intent


        // look for raw bitmap in data (TODO anything still use this?)
        mSource = getIntent().getExtras() == null ? null : (Bitmap) getIntent().getExtras().get("data");

        // MainActivity document picker / camera
        Uri selectedImageUri = getIntent().getData();
        if (selectedImageUri != null) {
            try {
                buildSourceFromContentUri(selectedImageUri);
            } catch (IOException e) {
                ELog.e(TAG, "java.io.IOException", e);
            }
        }

        // mime type share - share intent
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_SEND) && getIntent().getType() != null) {
            Uri send = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            try {
                buildSourceFromContentUri(send);
            } catch (IOException e) {
                ELog.e(TAG, "java.io.IOException", e);
            }
        }

        if (mSource == null) {
            Toast.makeText(this, R.string.read_error, Toast.LENGTH_LONG).show();
            finish();
        }

        // Create scaled bitmap the size of the screen. This is probably
        // cheaper than matrixing a huge jpeg.

        mImageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                float width = right - left;
                float height = bottom - top;
                float scale = Math.min(width / mSource.getWidth(), height / mSource.getHeight());
                ELog.i(TAG, "layout changed scale now " + scale);
                mBitmap = Bitmap.createScaledBitmap(mSource, (int) (mSource.getWidth() * scale), (int) (mSource.getHeight() * scale), true);
                ELog.i(TAG, String.format("view is %s by %s and image is %s by %s", width, height, mBitmap.getWidth(), mBitmap.getHeight()));
                mImageView.setImageBitmap(mBitmap);
                updateImage();
            }
        });

        mCorrection = new CorrectionManager();
        updateControls();
        activate((ImageButton)findViewById(R.id.rotate_button));

        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
        // temporary bitmap to force scaling
        mImageView.setImageBitmap(mSource);

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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                save();
                Toast.makeText(this, R.string.save_complete, Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_share:
                share();
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    @OnClick(R.id.rotate_button)
    void onRotate(ImageButton v) {
        mMode = Mode.ROTATE;
        updateControls();
        activate(v);
    }

    @OnClick(R.id.vertical_skew_button)
    void onVertical(ImageButton v) {
        mMode = Mode.VERTICAL_SKEW;
        updateControls();
        activate(v);
    }

    @OnClick(R.id.horizontal_skew_button)
    void onHorizontal(ImageButton v) {
        mMode = Mode.HORIZONTAL_SKEW;
        updateControls();
        activate(v);
    }

    @OnClick(R.id.reset_button)
    void onReset() {
        mDial.setValue(0, true);
    }

    @OnClick(R.id.grid_button)
    void onGrid(ImageButton b) {
        b.setActivated(!b.isActivated());
        mGrid.setVisibility(b.isActivated() ? View.VISIBLE : View.INVISIBLE);
    }

    void activate(ImageButton v) {
        for (ImageButton b : mButtons) {
            b.setActivated(false);
        }
        v.setActivated(true);
    }

    private void updateImage() {
        if (mBitmap != null) {
            Matrix matrix = mCorrection.getMatrix(mBitmap);
            // center the image in the view
            int xdiff = mImageView.getWidth() - mBitmap.getWidth();
            int ydiff = mImageView.getHeight() - mBitmap.getHeight();
            matrix.postTranslate(xdiff / 2, ydiff / 2);
            mImageView.setImageMatrix(matrix);
        }
    }

    private void updateControls() {
        switch (mMode) {
            case ROTATE:
                mDial.setValue(mCorrection.getRotation(), 45, 0.1, 10);
                break;
            case VERTICAL_SKEW:
                mDial.setValue(mCorrection.getVerticalSkew(), 40, 0.1, 10);
                break;
            case HORIZONTAL_SKEW:
                mDial.setValue(mCorrection.getHorizontalSkew(), 40, 0.1, 10);
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
            ELog.e(TAG, "Directory not created");
        }
        return file;
    }

    void writeToFile(File file) {
        Bitmap output = Bitmap.createBitmap(mSource.getWidth(), mSource.getHeight(), mSource.getConfig());
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(mSource, mCorrection.getMatrix(mSource), paint);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            output.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            stream.close();

            if (mLatitude != 0 && mLongitude != 0) {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                ELog.i(TAG, "writing " + String.valueOf(mLatitude) + " / " + String.valueOf(mLongitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, gpsToString(mLatitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, mLatitude > 0 ? "N" : "S");
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, gpsToString(mLongitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, mLongitude > 0 ? "E" : "W");
                exif.saveAttributes();
            }

        } catch (IOException e) {
            ELog.e(TAG, "java.io.IOException", e);
            Toast.makeText(this, R.string.write_error, Toast.LENGTH_LONG).show();
        } finally {
            output.recycle();
        }
    }

    File save() {
        File album = getAlbumStorageDir(getString(R.string.app_name));
        File file = new File(album, filename());
        writeToFile(file);
        return file;
    }

    String filename() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return format.format(new Date()) + ".jpg";
    }

    void share() {
        File saved;
        if (ALWAYS_SAVE) {
            saved = save();
        } else {
            saved = getTempFile();
            writeToFile(saved);
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(saved));
        startActivity(Intent.createChooser(intent, getString(R.string.share_title)));
    }

    private File getTempFile() {
        File saved;
        File folder = new File(getExternalFilesDir("share"), "temp");
        folder.mkdirs();
        // cleaning up after share is hard, we'll clean up before share
        // which is 90% as good and way easier to follow.
        for (String filename : folder.list()) {
            new File(folder, filename).delete();
        }
        saved = new File(folder, filename());
        return saved;
    }

    private String gpsToString(double latitude) {
        latitude=Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude*1000.0d);

        StringBuilder sb = new StringBuilder(20);
        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }

    private void buildSourceFromContentUri(Uri selectedImageUri) throws IOException {
        // pull the stream into a file so I can read the EXIF off it
        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
        File temp = getTempFile();
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(temp));
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        for (int len=0; len != -1; len = inputStream.read(buffer)) {
            stream.write(buffer, 0, len);
        }
        stream.close();

        // read jpeg
        Bitmap bitmap = BitmapFactory.decodeFile(temp.getAbsolutePath());

        // read exif
        ExifInterface exif = new ExifInterface(temp.getAbsolutePath());
        float[] latlng = new float[]{0, 0};
        if (exif.getLatLong(latlng)) {
            mLatitude = latlng[0];
            mLongitude = latlng[1];
            ELog.i(TAG, "latlng is " + mLatitude + "/" + mLongitude);
        }

        // respect EXIF rotation
        Matrix matrix = new Matrix();
        switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
            case ExifInterface.ORIENTATION_NORMAL:
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }
        mSource = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }
}
