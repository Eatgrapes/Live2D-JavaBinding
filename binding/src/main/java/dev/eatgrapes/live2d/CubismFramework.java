package dev.eatgrapes.live2d;

public class CubismFramework {
    static {
        LibraryLoader.load();
    }

    public enum LogLevel {
        VERBOSE(0),
        DEBUG(1),
        INFO(2),
        WARNING(3),
        ERROR(4),
        OFF(5);
        
        final int value;
        LogLevel(int value) { this.value = value; }
    }

    public interface LogCallback {
        void log(String message);
    }

    private static LogCallback logCallback;

    public static void startUp() {
        startUp(null, LogLevel.OFF);
    }

    public static void startUp(LogCallback callback, LogLevel level) {
        logCallback = callback;
        startUpNative(callback != null, level.value);
    }

    private static native void startUpNative(boolean hasCallback, int logLevel);
    public static native void initialize();
    public static native void dispose();
    public static native boolean isStarted();
    public static native boolean isInitialized();

    private static void onLog(String message) {
        if (logCallback != null) {
            logCallback.log("[Live2D Native] " + message);
        }
    }
}