package dev.eatgrapes.live2d;

public class CubismFramework {
    static {
        LibraryLoader.load();
    }

    public static native void startUp();
    public static native void initialize();
    public static native void dispose();
    public static native boolean isStarted();
    public static native boolean isInitialized();
}