package dev.eatgrapes.live2d;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;

/**
 * Handles loading of the native Live2D JNI library.
 * <p>
 * This class automatically detects the current operating system and architecture,
 * extracts the appropriate shared library from the JAR, and loads it.
 * On Android, it delegates to the system's library loader.
 */
public class LibraryLoader {
    private static boolean loaded = false;

    /**
     * Loads the native library.
     * <p>
     * This method is idempotent; calling it multiple times has no effect.
     * It attempts to load 'live2d_jni'. On desktop platforms, it extracts
     * the library to a temporary location first.
     *
     * @throws RuntimeException if the operating system or architecture is unsupported,
     *                          or if the native library cannot be extracted/loaded.
     */
    public static synchronized void load() {
        if (loaded) return;

        // Check for Android
        String vendor = System.getProperty("java.vendor", "").toLowerCase(Locale.ROOT);
        String vmName = System.getProperty("java.vm.name", "").toLowerCase(Locale.ROOT);
        if (vendor.contains("android") || vmName.contains("dalvik")) {
            System.loadLibrary("live2d_jni");
            loaded = true;
            return;
        }

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        
        String os;
        if (osName.contains("win")) os = "windows";
        else if (osName.contains("mac")) os = "macos";
        else if (osName.contains("linux")) os = "linux";
        else throw new RuntimeException("Unsupported OS: " + osName);

        String platformArch;
        if (arch.contains("aarch64") || arch.contains("arm64")) platformArch = "arm64";
        else if (arch.contains("64")) platformArch = "x64";
        else throw new RuntimeException("Unsupported arch: " + arch);

        String platformTag = os + "-" + platformArch;
        String libName = System.mapLibraryName("live2d_jni");
        String resourcePath = "/" + platformTag + "/" + libName;

        try (InputStream is = LibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Native lib not found: " + resourcePath);

            Path tempDir = Files.createTempDirectory("live2d_native");
            File tempFile = tempDir.resolve(libName).toFile();
            tempFile.deleteOnExit();

            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.getAbsolutePath());
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract native lib", e);
        }
    }

    /**
     * Loads a shader resource from the classpath.
     *
     * @param name The name of the shader file (e.g., "vert_shader.glsl").
     * @return The byte content of the shader file, or null if not found.
     */
    public static byte[] loadResource(String name) {
        String internalPath = "/live2d/shaders/" + name.substring(name.lastIndexOf("/") + 1);
        try (InputStream is = LibraryLoader.class.getResourceAsStream(internalPath)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}