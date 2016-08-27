package org.movieos.proton;

import android.support.annotation.NonNull;
import android.util.Log;

public class ELog {
    private static boolean printLogs = BuildConfig.DEBUG;

    @NonNull private static String BASE_TAG = "Movieos::";

    public static void d(String tag, String msg) {
        if (printLogs) {
            Log.d(BASE_TAG + tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (printLogs) {
            Log.d(BASE_TAG + tag, msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(BASE_TAG + tag, msg, tr);
    }

    public static void i(String tag, String msg) {
        if (printLogs) {
            Log.i(BASE_TAG + tag, msg);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (printLogs) {
            Log.i(BASE_TAG + tag, msg, tr);
        }
    }

    public static void v(String tag, String msg) {
        if (printLogs) {
            Log.v(BASE_TAG + tag, msg);
        }
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (printLogs) {
            Log.v(BASE_TAG + tag, msg, tr);
        }
    }

    public static void w(String tag, Throwable tr) {
        if (printLogs) {
            Log.w(BASE_TAG + tag, tr);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (printLogs) {
            Log.w(BASE_TAG + tag, msg, tr);
        }
    }

    public static void w(String tag, String msg) {
        if (printLogs) {
            Log.w(BASE_TAG + tag, msg);
        }
    }
}
