package com.ford.syncV4.demofull.logging;

public class Log { // TODO replace with more detailed log class

    private static final String TAG = "AppLinkTester";

    public static void i(String msg) {
        android.util.Log.i(TAG, msg);
    }

    public static void d(String msg) {
        android.util.Log.d(TAG, msg);
    }

    public static void e(String msg) {
        e(msg, null);
    }

    public static void e(String msg, Throwable e) {
        android.util.Log.e(TAG, msg, e);
    }

    public static void v(String msg) {
        android.util.Log.v(TAG, msg);
    }

    public static void w(String msg) {
        android.util.Log.w(TAG, msg);
    }
}
