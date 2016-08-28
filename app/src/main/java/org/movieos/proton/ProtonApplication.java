package org.movieos.proton;

import android.app.Application;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import timber.log.Timber;

public class ProtonApplication extends Application {

    public final static transient String TAG = ProtonApplication.class.getSimpleName();


    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new FirebaseTree());
        }
    }

    private class FirebaseTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }
            FirebaseAnalytics firebase = FirebaseAnalytics.getInstance(ProtonApplication.this);
            firebase.logEvent(message, null);
        }
    }
}
