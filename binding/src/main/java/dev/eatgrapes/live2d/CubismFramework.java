package dev.eatgrapes.live2d;

/**
 * Entry point for the Live2D Cubism Framework.
 * <p>
 * This class manages the lifecycle of the Cubism native framework (startup, initialization, disposal)
 * and provides logging capabilities.
 */
public class CubismFramework {
    static {
        LibraryLoader.load();
    }

    /**
     * Represents the logging level for the framework.
     */
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

    /**
     * Callback interface for receiving log messages from the native framework.
     */
    public interface LogCallback {
        /**
         * Called when a log message is generated.
         * @param message The log message.
         */
        void log(String message);
    }

    private static LogCallback logCallback;

    /**
     * Starts the Cubism framework with default settings (no logging).
     * Must be called before {@link #initialize()}.
     */
    public static void startUp() {
        startUp(null, LogLevel.OFF);
    }

    /**
     * Starts the Cubism framework with a specific logger and log level.
     * Must be called before {@link #initialize()}.
     *
     * @param callback The callback to handle log messages (can be null).
     * @param level    The minimum log level to capture.
     */
    public static void startUp(LogCallback callback, LogLevel level) {
        logCallback = callback;
        startUpNative(callback != null, level.value);
    }

    private static native void startUpNative(boolean hasCallback, int logLevel);

    /**
     * Initializes the framework resources.
     * Must be called after {@link #startUp()} and before using any other framework features.
     */
    public static native void initialize();

    /**
     * Disposes of the framework resources.
     * Should be called when Live2D is no longer needed to free native memory.
     */
    public static native void dispose();

    /**
     * Checks if the framework has been started.
     * @return true if started, false otherwise.
     */
    public static native boolean isStarted();

    /**
     * Checks if the framework has been initialized.
     * @return true if initialized, false otherwise.
     */
    public static native boolean isInitialized();

    // Called from JNI
    private static void onLog(String message) {
        if (logCallback != null) {
            logCallback.log("[Live2D Native] " + message);
        }
    }
}
