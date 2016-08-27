package org.movieos.proton.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import org.movieos.proton.CorrectionManager;
import org.movieos.proton.DialControl;
import org.movieos.proton.ELog;
import org.movieos.proton.R;
import org.movieos.proton.databinding.CorrectionActivityBinding;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.movieos.proton.R.menu.correction;


public class CorrectionActivity extends AppCompatActivity implements DialControl.OnDialChangeListener {
    private transient static final String TAG = CorrectionActivity.class.getSimpleName();

    static final int REQUEST_STORAGE_WRITE = 4000;

    private CorrectionActivityBinding mBinding;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Toast mToast;

    enum Mode {
        ROTATE, VERTICAL_SKEW, HORIZONTAL_SKEW
    };

    // derived from launch state
    @Nullable
    Bitmap mSource;
    Bitmap mPreview;
    float mOriginalLatitude;
    float mOrigainlLongitude;

    // view state
    @NonNull
    CorrectionManager mCorrection = new CorrectionManager();
    Mode mMode = Mode.ROTATE;
    boolean mGrid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mBinding = DataBindingUtil.setContentView(this, R.layout.correction_activity);

        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        mBinding.toolbar.setNavigationOnClickListener(v -> {
            finish();
        });
        mBinding.toolbar.inflateMenu(correction);
        mBinding.toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        mBinding.rotateButton.setOnClickListener(v -> {
            mMode = Mode.ROTATE;
            updateControls();
        });

        float d = getResources().getDisplayMetrics().density;
        Matrix parallelogram = new Matrix();
        parallelogram.setPolyToPoly(new float[]{
            0 * d, 0 * d,
            0 * d, 24 * d,
            24 * d, 24 * d,
            24 * d, 0 * d,
        }, 0, new float[]{
            11 * d, 11 * d,
            11 * d, 37 * d,
            37 * d, 32 * d,
            37 * d, 16 * d,
        }, 0, 4);

        mBinding.verticalSkewButton.setScaleType(ImageView.ScaleType.MATRIX);
        mBinding.verticalSkewButton.setImageMatrix(parallelogram);
        mBinding.verticalSkewButton.setOnClickListener(v -> {
            mMode = Mode.VERTICAL_SKEW;
            updateControls();
        });

        parallelogram.postRotate(-90, 24 * d, 24 * d);
        mBinding.horizontalSkewButton.setScaleType(ImageView.ScaleType.MATRIX);
        mBinding.horizontalSkewButton.setImageMatrix(parallelogram);
        mBinding.horizontalSkewButton.setOnClickListener(v -> {
            mMode = Mode.HORIZONTAL_SKEW;
            updateControls();
        });

        mBinding.resetButton.setOnClickListener(v -> {
            mBinding.dial.setValue(0, true);
        });

        mBinding.cropButton.setOnClickListener(v -> {
            mCorrection.setCrop(!v.isActivated());
            updateControls();
            updateImage();
        });

        mBinding.gridButton.setOnClickListener(v -> {
            mGrid = !v.isActivated();
            updateControls();
        });

        // start paths:
        // (a) opened with URI (MainActivity document picker)
        // (b) (legacy/gone) Opened with bitmap in data (MainActivity camera)
        // (c) Opened with share intent

        mSource = null;

        // MainActivity document picker / camera written data to file on disk
        Uri selectedImageUri = getIntent().getData();
        if (selectedImageUri != null) {
            try {
                buildSourceFromContentUri(selectedImageUri);
            } catch (IOException e) {
                ELog.e(TAG, "java.io.IOException", e);
            }
        }

        // mime type share - share intent from other app
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_SEND) && getIntent().getType() != null) {
            Uri send = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            try {
                buildSourceFromContentUri(send);
            } catch (IOException e) {
                ELog.e(TAG, "java.io.IOException", e);
            }
        }

        if (mSource == null) {
            toast(R.string.read_error);
            finish();
        }

        // Create scaled bitmap the size of the screen. This is probably
        // cheaper than matrixing a huge jpeg.
        mBinding.frame.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                float width = right - left;
                float height = bottom - top;
                float scale = Math.min(width / mSource.getWidth(), height / mSource.getHeight());
                ELog.i(TAG, "preview scale is " + scale);
                mPreview = Bitmap.createScaledBitmap(mSource, (int) (mSource.getWidth() * scale), (int) (mSource.getHeight() * scale), true);
                mBinding.image.setImageBitmap(mPreview);
                updateControls();
                updateImage();
            }
        });

        mBinding.dial.setOnChangeListener(this);

        if (savedInstanceState != null) {
            CorrectionManager correction = savedInstanceState.getParcelable("correction");
            if (correction != null) {
                mCorrection = correction;
            }
            mGrid = savedInstanceState.getBoolean("grid");
            mMode = Mode.valueOf(savedInstanceState.getString("mode"));
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("correction", mCorrection);
        outState.putBoolean("grid", mGrid);
        outState.putString("mode", mMode.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ELog.i(TAG, "Correction Manager is " + mCorrection);
        updateControls();
        updateImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(correction, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_save:
                save();
                return true;
            case R.id.action_share:
                share();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void activate(@NonNull ImageButton v) {
        mBinding.rotateButton.setActivated(false);
        mBinding.verticalSkewButton.setActivated(false);
        mBinding.horizontalSkewButton.setActivated(false);
        v.setActivated(true);
    }

    private void updateImage() {
        Matrix matrix = mCorrection.getMatrix(mPreview);
        mBinding.image.setScaleType(ImageView.ScaleType.MATRIX);
        mBinding.image.setImageMatrix(matrix);
    }

    private void updateControls() {
        switch (mMode) {
            case ROTATE:
                activate(mBinding.rotateButton);
                mBinding.dial.setValue(mCorrection.getRotation(), 45, 0.1, 10);
                break;
            case VERTICAL_SKEW:
                activate(mBinding.verticalSkewButton);
                mBinding.dial.setValue(mCorrection.getVerticalSkew(), 40, 0.1, 10);
                break;
            case HORIZONTAL_SKEW:
                activate(mBinding.horizontalSkewButton);
                mBinding.dial.setValue(mCorrection.getHorizontalSkew(), 40, 0.1, 10);
                break;
        }
        mBinding.grid.setVisibility(mGrid ? View.VISIBLE : View.INVISIBLE);
        mBinding.gridButton.setActivated(mGrid);
        mBinding.cropButton.setActivated(mCorrection.isCrop());
    }

    @Override
    public void onDialValueChanged(double value) {
        if (Double.isNaN(value)) {
            return;
        }
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

    @NonNull
    File getAlbumStorageDir(@NonNull String albumName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            ELog.e(TAG, "Directory not created");
        }
        return file;
    }

    void writeToFile(@NonNull File file) {
        toast(R.string.saving);
        Bitmap output = Bitmap.createBitmap(mSource.getWidth(), mSource.getHeight(), mSource.getConfig());
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(mSource, mCorrection.getMatrix(mSource), paint);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            output.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            stream.close();

            if (mOriginalLatitude != 0 && mOrigainlLongitude != 0) {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, gpsToString(mOriginalLatitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, mOriginalLatitude > 0 ? "N" : "S");
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, gpsToString(mOrigainlLongitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, mOrigainlLongitude > 0 ? "E" : "W");
                exif.saveAttributes();
            }

        } catch (IOException e) {
            ELog.e(TAG, "java.io.IOException", e);
            toast(R.string.write_error);
        } finally {
            output.recycle();
        }
    }

    private void save() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_WRITE);
            return;
        }

        File album = getAlbumStorageDir(getString(R.string.album_name));
        File file = new File(album, filename());
        writeToFile(file);
        toast(R.string.save_complete);
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            Intent data = new Intent();
            data.setData(Uri.parse("file://" + file.getAbsolutePath()));
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_STORAGE_WRITE:
                    save();
                    break;
            }
        }
    }

    @NonNull
    String filename() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZ", Locale.US);
        return format.format(new Date()) + ".jpg";
    }

    void share() {
        File saved = getTempFile();
        writeToFile(saved);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.setType("image/jpeg");
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", saved);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, getString(R.string.share_title)));
    }

    private void toast(final int message) {
        untoast();
        mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        mToast.show();
    }

    private void untoast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    private void buildSourceFromContentUri(@NonNull Uri selectedImageUri) throws IOException {
        // pull the stream into a file so I can read the EXIF off it
        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
        if (inputStream == null) {
            throw new IOException("null input");
        }
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
            mOriginalLatitude = latlng[0];
            mOrigainlLongitude = latlng[1];
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
