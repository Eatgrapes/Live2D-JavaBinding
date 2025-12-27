package dev.eatgrapes.live2d;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;

public class LibraryLoader {
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;

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
