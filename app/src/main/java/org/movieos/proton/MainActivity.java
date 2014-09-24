package org.movieos.proton;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends Activity {
    private transient static final String TAG = MainActivity.class.getSimpleName();

    static int RESULT_LOAD_IMAGE = 9001;
    private Uri mOutputFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mOutputFileUri = savedInstanceState.getParcelable("filename");
        }
        setContentView(R.layout.main_activity);
        ButterKnife.inject(this);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void showAbout(MenuItem sender) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("filename", mOutputFileUri);
    }

    @OnClick(R.id.main_activity_camera)
    void onClickCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(Environment.getExternalStorageDirectory(), "capture.jpg");
        mOutputFileUri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutputFileUri);
        startActivityForResult(intent, RESULT_LOAD_IMAGE);
    }

    @OnClick(R.id.main_activity_gallery)
    void onClickGallery() {
        Intent imageGalleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        imageGalleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        imageGalleryIntent.setType("image/*");
        startActivityForResult(imageGalleryIntent, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_LOAD_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, CorrectionActivity.class);
                if (data != null) {
                    Uri selectedImageUri = data.getData();
                    intent.setData(selectedImageUri);
                    Bitmap photo = data.getExtras() == null ? null : (Bitmap) data.getExtras().get("data");
                    intent.putExtra("data", photo);
                } else {
                    // null data, so the camera just wrote the file
                    intent.setData(mOutputFileUri);
                }
                startActivity(intent);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
