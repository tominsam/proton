package org.movieos.proton.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import org.movieos.proton.R;
import org.movieos.proton.databinding.AboutActivityBinding;


public class AboutActivity extends AppCompatActivity {
    private transient static final String TAG = AboutActivity.class.getSimpleName();

    private AboutActivityBinding mBinding;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mBinding = DataBindingUtil.setContentView(this, R.layout.about_activity);

        mBinding.toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        mBinding.toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        mBinding.aboutProton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://movieos.org/code/proton/"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        mBinding.aboutIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://thenounproject.com/term/polygon/17101/"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
