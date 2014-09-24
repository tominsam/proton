package org.movieos.proton;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class AboutActivity extends Activity {
    private transient static final String TAG = AboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        setContentView(R.layout.about_activity);
        ButterKnife.inject(this);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @OnClick(R.id.about_proton)
    public void onClickProton() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://movieos.org/code/proton/"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @OnClick(R.id.about_icon)
    public void onClickIcon() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://thenounproject.com/term/polygon/17101/"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
