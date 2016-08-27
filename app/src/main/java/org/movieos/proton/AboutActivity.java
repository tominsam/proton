package org.movieos.proton;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.movieos.proton.databinding.AboutActivityBinding;


public class AboutActivity extends AppCompatActivity {
    private transient static final String TAG = AboutActivity.class.getSimpleName();

    private AboutActivityBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.about_activity);

        mBinding.toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        mBinding.toolbar.inflateMenu(R.menu.main);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
