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
import org.movieos.proton.R;
import org.movieos.proton.adapters.MediaAdapter;
import org.movieos.proton.databinding.MainActivityBinding;
import timber.log.Timber;

import java.io.File;

public class MainActivity extends AppCompatActivity implements MediaAdapter.MediaTappedListener {
    static final int RESULT_LOAD_IMAGE = 9001;
    static final int RESULT_READ_PERMISSION = 9002;
    static final String STATE_FILENAME = "filename";
    static final String STATE_DENIED = "denied";

    @Nullable
    private Uri mOutputFileUri;
    private MainActivityBinding mBinding;
    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean mDeniedPermission;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        if (savedInstanceState != null) {
            // the camera tends to force us out of memory, so we need to retain this
            mOutputFileUri = savedInstanceState.getParcelable(STATE_FILENAME);
            mDeniedPermission = savedInstanceState.getBoolean(STATE_DENIED);
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

        // top-pad the recycler view content so that it never overlaps the header buttons
        mBinding.wrapper.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(final View v, final int left, final int top, final int right, final int bottom, final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {
                int side = MainActivity.this.getResources().getDimensionPixelSize(R.dimen.media_padding);
                mBinding.recyclerview.setPaddingRelative(side, bottom, side, 0);
                mBinding.wrapper.removeOnLayoutChangeListener(this);
            }
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

        // We want to lay out images in a grid. We'll re-count the columns if the width
        // changes - we want the images to be about 150dp wide.
        mBinding.recyclerview.setLayoutManager(new GridLayoutManager(MainActivity.this, 1));
        mBinding.recyclerview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(final View v, final int left, final int top, final int right, final int bottom, final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {
                // work out the number of spans that gets us closest to the desired cell width
                int width = right - left - v.getPaddingStart() - v.getPaddingEnd();
                int spans = Math.round((float) width / MainActivity.this.getResources().getDimensionPixelSize(R.dimen.ideal_media_size));
                ((GridLayoutManager)mBinding.recyclerview.getLayoutManager()).setSpanCount(spans);

                // delay this till layout is actually complete, so that the grid cells are the right size.
                // I don't fully understand why I need to do this, but it's neeed or first boot on N fails.
                if (hasReadPermission()) {
                    mBinding.recyclerview.postDelayed(() -> {
                        mBinding.recyclerview.setAdapter(new MediaAdapter(MainActivity.this, MainActivity.this));
                    }, 0);
                }

                mBinding.recyclerview.removeOnLayoutChangeListener(this);
            }
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
        outState.putParcelable(STATE_FILENAME, mOutputFileUri);
        outState.putBoolean(STATE_DENIED, mDeniedPermission);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update cursor on resume, so that if the user changes their media library in the
        // background we'll notice. (TODO can I watch the cursor or something? This will matter more for split screen)

        // If we have media permissions, display images, otherwise ask for them
        if (mDeniedPermission) {
            // If the user explicitly denies us, we won't ask again this session
        } else if (!hasReadPermission()) {
            // Ask for read permissions on M+
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RESULT_READ_PERMISSION);
        } else {
            // We can create the adapter then
            mBinding.recyclerview.setAdapter(new MediaAdapter(this, this));
        }
    }

    private boolean hasReadPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case RESULT_READ_PERMISSION:
                    Timber.i("setting adapter because permission granted");
                    mBinding.recyclerview.setAdapter(new MediaAdapter(this, this));
                    break;
            }
        } else {
            mDeniedPermission = true;
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
                Timber.i("Camera cancelled");
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
