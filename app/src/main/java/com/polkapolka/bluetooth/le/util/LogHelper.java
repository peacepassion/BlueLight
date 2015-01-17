package com.polkapolka.bluetooth.le.util;


import android.util.Log;

/**
 * Created by peace_da on 2014/10/29.
 */
public final class LogHelper {

    public static final String DEFAULT_LOG_TAG = "Peace";

    public enum LEVEL {
        v, d, i, w, e;

        void log(String logTag, String content) {
            if (this == v) {
                Log.v(logTag, content);
            } else if (this == d) {
                Log.d(logTag, content);
            } else if (this == i) {
                Log.i(logTag, content);
            } else if (this == w) {
                Log.w(logTag, content);
            } else if (this == e) {
                Log.e(logTag, content);
            }
        }
    }

    public static final String getNativeSimpleLogTag(Class cls, String tag) {
       return tag + " - " + cls.getSimpleName();
    }

    public static final void logWithTID(LEVEL level, String logTag, String content) {
        content = content + ", thread id: " + Thread.currentThread().getId();
        level.log(logTag, content);
    }
}
