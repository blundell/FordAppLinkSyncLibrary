package com.ford.syncV4.demofull.console;

import android.app.Activity;

import com.ford.syncV4.demofull.logging.Log;

import java.util.List;

public class LogAdapter extends MessageAdapter {
    public enum Type {
        D, E, W, V
    }

    private final boolean fullUIDebug;
    private final Activity activity;

    public LogAdapter(boolean fullUIDebug, Activity activity, int textViewResourceId, List<Object> items) {
        super(activity, textViewResourceId, items);
        this.fullUIDebug = fullUIDebug;
        this.activity = activity;
    }

    private void addMessageToUI(final Object m) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                addMessage(m);
            }
        });
    }

    public void logMessage(final Object m) {
        Log.i(m.toString());
        if (fullUIDebug) {
            addMessageToUI(m);
        }
    }

    public void logMessage(final Object m, boolean addToUI) {
        Log.i(m.toString());
        if (addToUI) {
            addMessageToUI(m);
        }
    }

    public void logMessage(final Object m, Type type) {
        if (m instanceof String) {
            switch (type) {
                case D:
                    Log.d(m.toString());
                    break;
                case E:
                    Log.e(m.toString());
                    break;
                case V:
                    Log.v(m.toString());
                    break;
                case W:
                    Log.w(m.toString());
                    break;
                default:
                    Log.i(m.toString());
                    break;
            }
        }
        if (fullUIDebug) {
            addMessageToUI(m);
        }
    }

    public void logMessage(final Object m, Type type, boolean addToUI) {
        if (m instanceof String) {
            switch (type) {
                case D:
                    Log.d(m.toString());
                    break;
                case E:
                    Log.e(m.toString());
                    break;
                case V:
                    Log.v(m.toString());
                    break;
                case W:
                    Log.w(m.toString());
                    break;
                default:
                    Log.i(m.toString());
                    break;
            }
        }
        if (addToUI) {
            addMessageToUI(m);
        }
    }

    public void logMessage(final Object m, Type type, Throwable tr) {
        if (m instanceof String) {
            switch (type) {
                case D:
                    Log.d(m.toString());
                    break;
                case E:
                    Log.e(m.toString(), tr);
                    break;
                case V:
                    Log.v(m.toString());
                    break;
                case W:
                    Log.w(m.toString());
                    break;
                default:
                    Log.i(m.toString());
                    break;
            }
        }
        if (fullUIDebug) {
            addMessageToUI(m);
        }
    }

    public void logMessage(final Object m, Type type, Throwable tr, boolean addToUI) {
        if (m instanceof String) {
            switch (type) {
                case D:
                    Log.d(m.toString());
                    break;
                case E:
                    Log.e(m.toString(), tr);
                    break;
                case V:
                    Log.v(m.toString());
                    break;
                case W:
                    Log.w(m.toString());
                    break;
                default:
                    Log.i(m.toString());
                    break;
            }
        }
        if (addToUI) {
            addMessageToUI(m);
        }
    }
}
