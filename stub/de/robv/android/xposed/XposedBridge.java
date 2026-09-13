package de.robv.android.xposed;

public class XposedBridge {
    public static void log(String text) {
        throw new UnsupportedOperationException("stub");
    }

    public static void log(Throwable t) {
        throw new UnsupportedOperationException("stub");
    }
}
