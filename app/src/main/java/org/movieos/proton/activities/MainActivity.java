package org.movieos.proton.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import org.movieos.proton.ELog;
import org.movieos.proton.R;
import org.movieos.proton.adapters.MediaAdapter;
import org.movieos.proton.databinding.MainActivityBinding;

import java.io.File;

public class MainActivity extends AppCompatActivity implements MediaAdapter.MediaTappedListener {
    private transient static final String TAG = MainActivity.class.getSimpleName();

    static final int RESULT_LOAD_IMAGE = 9001;
    static final int RESULT_READ_PERMISSION = 9002;

    @Nullable
    private Uri mOutputFileUri;
    private MainActivityBinding mBinding;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

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

        // offset the recycler view content so that it never overlaps the header buttons
        mBinding.wrapper.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int side = getResources().getDimensionPixelSize(R.dimen.media_padding);
            mBinding.recyclerview.setPaddingRelative(side, bottom, side, 0);
        });

        // makes the recyclerview tap-through, so that the buttons in the main view still work
        // only works if the recycler view has the exact same frame as the wrapper view!
        mBinding.recyclerview.setOnTouchListener((v, event) -> {
            if (mBinding.wrapper.getAlpha() > 0.2) {
                MotionEvent e = MotionEvent.obtain(event);
                mBinding.wrapper.dispatchTouchEvent(e);
                e.recycle();
            }
            return false;
        });

        // Lay out images in a grid.
        // re-count the columns if the width changes - we want the images to be about 150dp wide
        GridLayoutManager layout = new GridLayoutManager(this, 2);
        mBinding.recyclerview.setLayoutManager(layout);
        mBinding.recyclerview.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left - v.getPaddingStart() - v.getPaddingEnd();
            int spans = Math.round((float)width / getResources().getDimensionPixelSize(R.dimen.ideal_media_size));
            layout.setSpanCount(spans);
        });

        // as the view is scrolled, fade out the main content
        mBinding.recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
                View firstChild = recyclerView.getLayoutManager().getChildAt(0);
                float scroll = mBinding.wrapper.getBottom() - firstChild.getTop();
                float compare = scroll / (mBinding.wrapper.getHeight() / 4);
                mBinding.wrapper.setAlpha(1 - Math.max(Math.min(compare, 1), 0));
            }
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("filename", mOutputFileUri);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we have media permissions, display images, otherwise ask for them
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RESULT_READ_PERMISSION);
        } else {
            mBinding.recyclerview.setAdapter(new MediaAdapter(this, this));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case RESULT_READ_PERMISSION:
                    mBinding.recyclerview.setAdapter(new MediaAdapter(this, this));
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RESULT_LOAD_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                Uri uri = (data != null && data.getData() != null) ? data.getData() : mOutputFileUri;
                startCorrection(uri);
            } else {
                ELog.i(TAG, "Camera cancelled");
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startCorrection(final Uri uri) {
        Intent intent = new Intent(this, CorrectionActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onMediaTapped(final Uri contentUri) {
        startCorrection(contentUri);
    }

}
