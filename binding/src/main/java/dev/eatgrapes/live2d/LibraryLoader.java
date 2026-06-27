package dev.eatgrapes.live2d;

import java.io.*;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.file.*;
import java.util.Locale;
import java.util.Optional;

/**
 * Handles loading of the native Live2D JNI library.
 * <p>
 * This class automatically detects the current operating system and architecture,
 * extracts the appropriate shared library from the JAR, and loads it.
 * On Android, it delegates to the system's library loader.
 * <p>
 * Supports both classpath-based and JPMS modular environments.
 */
public class LibraryLoader {
    private static boolean loaded = false;
    private static final String[] VULKAN_SHADER_FILES = {
            "VertShaderSrcSetupMask.spv",
            "FragShaderSrcSetupMask.spv",
            "VertShaderSrc.spv",
            "FragShaderSrc.spv",
            "VertShaderSrcMasked.spv",
            "FragShaderSrcMask.spv",
            "FragShaderSrcMaskInverted.spv",
            "FragShaderSrcPremultipliedAlpha.spv",
            "FragShaderSrcMaskPremultipliedAlpha.spv",
            "FragShaderSrcMaskInvertedPremultipliedAlpha.spv"
    };

    /**
     * Resolves a resource as an {@link InputStream}, working in both classpath and JPMS environments.
     * <p>
     * Tries the following strategies in order:
     * <ol>
     *   <li>{@code Class.getResourceAsStream} — works in non-modular (classpath) setups.</li>
     *   <li>{@code ClassLoader.getResourceAsStream} — works across modules on the module-path
     *       because the system class loader can see resources from all layers.</li>
     *   <li>Scanning {@link ModuleLayer#boot()} for the resource — explicit JPMS cross-module access
     *       when the native resources live in a separate named module.</li>
     * </ol>
     *
     * @param resourcePath absolute resource path, e.g. {@code "/windows-x64/live2d_jni.dll"}.
     * @return an open {@code InputStream}, or {@code null} if the resource was not found.
     */
    private static InputStream getResourceStream(String resourcePath) {
        // 1. Same-module lookup (classic classpath)
        InputStream is = LibraryLoader.class.getResourceAsStream(resourcePath);
        if (is != null) return is;

        // 2. ClassLoader-level lookup (crosses automatic-module boundaries)
        String classLoaderPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        is = ClassLoader.getSystemResourceAsStream(classLoaderPath);
        if (is != null) return is;

        // Also try the thread-context class loader (useful in frameworks like OSGi / app servers)
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        if (tcl != null) {
            is = tcl.getResourceAsStream(classLoaderPath);
            if (is != null) return is;
        }

        // 3. Explicit JPMS ModuleLayer scan — handles named modules that encapsulate resources
        ModuleLayer bootLayer = ModuleLayer.boot();
        for (ResolvedModule rm : bootLayer.configuration().modules()) {
            Optional<ModuleReference> refOpt = Optional.of(rm.reference());
            if (refOpt.isPresent()) {
                try {
                    Module module = bootLayer.findModule(rm.name()).orElse(null);
                    if (module != null) {
                        is = module.getResourceAsStream(resourcePath);
                        if (is != null) return is;
                    }
                } catch (IOException ignored) {
                    // Continue searching other modules
                }
            }
        }

        return null;
    }

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

        try (InputStream is = getResourceStream(resourcePath)) {
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
        if (name == null || name.isEmpty()) return null;
        String normalized = name.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        String shaderGroup = fileName.endsWith(".spv") ? "vulkan" : "opengl";
        String internalPath = "/live2d/shaders/" + shaderGroup + "/" + fileName;
        try (InputStream is = getResourceStream(internalPath)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Ensures Vulkan SPIR-V shaders exist in the runtime working directory under {@code FrameworkShaders/}.
     * This keeps compatibility with Cubism Vulkan shader loading paths.
     */
    public static synchronized void prepareVulkanShadersToDisk() {
        Path shaderDir = Paths.get("FrameworkShaders");
        try {
            Files.createDirectories(shaderDir);
            for (String fileName : VULKAN_SHADER_FILES) {
                Path target = shaderDir.resolve(fileName);
                if (Files.exists(target) && Files.size(target) > 0) {
                    continue;
                }
                String internalPath = "/live2d/shaders/vulkan/" + fileName;
                try (InputStream is = getResourceStream(internalPath)) {
                    if (is == null) {
                        throw new RuntimeException("Vulkan shader not found in resources: " + internalPath);
                    }
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare Vulkan shaders on disk", e);
        }
    }
}
