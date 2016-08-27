package org.movieos.proton.activities;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
import org.movieos.proton.ELog;
import org.movieos.proton.R;
import org.movieos.proton.databinding.MainActivityBinding;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private transient static final String TAG = MainActivity.class.getSimpleName();

    static int RESULT_LOAD_IMAGE = 9001;
    @Nullable private Uri mOutputFileUri;
    @SuppressWarnings("FieldCanBeLocal")
    private MainActivityBinding mBinding;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        if (savedInstanceState != null) {
            mOutputFileUri = savedInstanceState.getParcelable("filename");
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.main_activity);
        mBinding.toolbar.inflateMenu(R.menu.main);

        mBinding.toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_about:
                    startActivity(new Intent(this, AboutActivity.class));
                    return true;
            }
            return false;
        });

        mBinding.mainActivityCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(getExternalFilesDir("capture"), "capture.jpg");
            mOutputFileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutputFileUri);
            startActivityForResult(intent, RESULT_LOAD_IMAGE);
        });

        mBinding.mainActivityGallery.setOnClickListener(v -> {
            Intent imageGalleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            imageGalleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
            imageGalleryIntent.setType("image/*");
            startActivityForResult(imageGalleryIntent, RESULT_LOAD_IMAGE);
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("filename", mOutputFileUri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RESULT_LOAD_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                Intent intent = new Intent(this, CorrectionActivity.class);
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    intent.setData(selectedImageUri);
                    Bitmap photo = data.getExtras() == null ? null : (Bitmap) data.getExtras().get("data");
                    intent.putExtra("data", photo);
                } else {
                    // null data, so the camera just wrote the file
                    intent.setData(mOutputFileUri);
                }
                startActivity(intent);
            } else {
                ELog.i(TAG, "Camera cancelled");
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
