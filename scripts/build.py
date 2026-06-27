import os
import subprocess
import shutil
import platform
import sys
import urllib.request
import zipfile

SDK_URL = "https://cubism.live2d.com/sdk-native/bin/CubismSdkForNative-5-r.4.1.zip"
SDK_DIR = "sdk"
GLES_HEADERS = {
    "GLES2/gl2.h": "https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/main/api/GLES2/gl2.h",
    "GLES2/gl2ext.h": "https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/main/api/GLES2/gl2ext.h",
    "GLES2/gl2platform.h": "https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/main/api/GLES2/gl2platform.h",
    "KHR/khrplatform.h": "https://raw.githubusercontent.com/KhronosGroup/EGL-Registry/main/api/KHR/khrplatform.h"
}

def find_shader_compiler():
    glslang = shutil.which("glslangValidator")
    if glslang:
        return ("glslangValidator", glslang)
    glslang_bin = shutil.which("glslang")
    if glslang_bin:
        return ("glslang", glslang_bin)
    glslc = shutil.which("glslc")
    if glslc:
        return ("glslc", glslc)
    return (None, None)

def compile_vulkan_shaders(out_dir):
    shader_src_dir = os.path.join(SDK_DIR, "Framework/src/Rendering/Vulkan/Shaders/src")
    if not os.path.isdir(shader_src_dir):
        print(f"Vulkan shader source dir not found: {shader_src_dir}")
        sys.exit(1)

    compiler_kind, compiler_path = find_shader_compiler()
    if compiler_path is None:
        print("Vulkan shader compiler not found. Please install glslangValidator, glslang, or glslc.")
        print("Vulkan backend requires SPIR-V shader binaries.")
        sys.exit(1)

    for f in os.listdir(shader_src_dir):
        if not (f.endswith(".vert") or f.endswith(".frag")):
            continue
        src_file = os.path.join(shader_src_dir, f)
        out_file = os.path.join(out_dir, f.rsplit(".", 1)[0] + ".spv")
        if compiler_kind in ("glslangValidator", "glslang"):
            run_cmd([compiler_path, "-V", src_file, f"-I{shader_src_dir}", "-o", out_file])
        else:
            run_cmd([compiler_path, src_file, f"-I{shader_src_dir}", "-o", out_file])

def run_cmd(cmd, cwd=None):
    print(f"Executing: {' '.join(cmd)}")
    shell = platform.system().lower() == "windows"
    process = subprocess.Popen(cmd, cwd=cwd, shell=shell, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in process.stdout:
        print(line, end="")
    process.wait()
    if process.returncode != 0:
        print(f"Command failed with exit code {process.returncode}")
        sys.exit(process.returncode)

def patch_text_file(path, replacements):
    if not os.path.exists(path):
        return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    original = content
    for old, new in replacements:
        content = content.replace(old, new)
    if content != original:
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

def apply_sdk_patches():
    tpp = os.path.join(SDK_DIR, "Framework/src/Rendering/CubismClippingManager.tpp")
    patch_text_file(tpp, [
        ("_clearedMaskBufferFlags = NULL;", "_clearedMaskBufferFlags.Clear();")
    ])

    gles2_hpp = os.path.join(SDK_DIR, "Framework/src/Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp")
    patch_text_file(gles2_hpp, [
        ("void Initialize(Framework::CubismModel* model);", "void Initialize(Framework::CubismModel* model) override;"),
        ("void Initialize(Framework::CubismModel* model, csmInt32 maskBufferCount);", "void Initialize(Framework::CubismModel* model, csmInt32 maskBufferCount) override;"),
        ("virtual void SaveProfile();", "void SaveProfile() override;"),
        ("virtual void RestoreProfile();", "void RestoreProfile() override;")
    ])

    opengl_cpp = os.path.join(SDK_DIR, "Framework/src/Rendering/OpenGL/CubismRenderer_OpenGLES2.cpp")
    patch_text_file(opengl_cpp, [
        (
            "CubismRenderer* CubismRenderer::Create()\n{\n    return CSM_NEW CubismRenderer_OpenGLES2();\n}\n\nvoid CubismRenderer::StaticRelease()\n{\n    CubismRenderer_OpenGLES2::DoStaticRelease();\n}",
            "#ifndef LIVE2D_CUSTOM_RENDERER_FACTORY\nCubismRenderer* CubismRenderer::Create()\n{\n    return CSM_NEW CubismRenderer_OpenGLES2();\n}\n\nvoid CubismRenderer::StaticRelease()\n{\n    CubismRenderer_OpenGLES2::DoStaticRelease();\n}\n#endif"
        )
    ])

    vulkan_cpp = os.path.join(SDK_DIR, "Framework/src/Rendering/Vulkan/CubismRenderer_Vulkan.cpp")
    patch_text_file(vulkan_cpp, [
        (
            "CubismRenderer* CubismRenderer::Create()\n{\n    return CSM_NEW CubismRenderer_Vulkan;\n}\n\nvoid CubismRenderer::StaticRelease()\n{\n    CubismRenderer_Vulkan::DoStaticRelease();\n}",
            "#ifndef LIVE2D_CUSTOM_RENDERER_FACTORY\nCubismRenderer* CubismRenderer::Create()\n{\n    return CSM_NEW CubismRenderer_Vulkan;\n}\n\nvoid CubismRenderer::StaticRelease()\n{\n    CubismRenderer_Vulkan::DoStaticRelease();\n}\n#endif"
        )
    ])

def download_sdk():
    if not os.path.exists(SDK_DIR):
        print("Downloading SDK...")
        urllib.request.urlretrieve(SDK_URL, "sdk.zip")
        with zipfile.ZipFile("sdk.zip", 'r') as z: z.extractall("temp")
        inner = [d for d in os.listdir("temp") if os.path.isdir(os.path.join("temp", d))][0]
        shutil.move(os.path.join("temp", inner), SDK_DIR)
        shutil.rmtree("temp")
        os.remove("sdk.zip")
    apply_sdk_patches()

def download_headers():
    inc_dir = os.path.abspath("native/include")
    for path, url in GLES_HEADERS.items():
        full_path = os.path.join(inc_dir, path)
        if not os.path.exists(full_path):
            os.makedirs(os.path.dirname(full_path), exist_ok=True)
            print(f"Downloading {path}...")
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req) as response, open(full_path, 'wb') as out_file:
                shutil.copyfileobj(response, out_file)

def get_platform_tag():
    os_name = platform.system().lower()
    arch = platform.machine().lower()
    p = "macos" if "darwin" in os_name else os_name
    if "arm" in arch or "aarch64" in arch:
        a = "arm64"
    else:
        a = "x64"
    return f"{p}-{a}"

def build_desktop():
    tag = get_platform_tag()
    root = os.getcwd()
    nb = os.path.join(root, "native/build")
    os.makedirs(nb, exist_ok=True)
    cmake_cmd = ["cmake", ".."]
    require_vulkan = os.environ.get("LIVE2D_REQUIRE_VULKAN", "").strip().lower()
    if require_vulkan in ("1", "true", "on", "yes"):
        cmake_cmd.append("-DLIVE2D_REQUIRE_VULKAN=ON")
    run_cmd(cmake_cmd, nb)
    run_cmd(["cmake", "--build", ".", "--config", "Release"], nb)
    
    out_res = os.path.join(root, "out", "native_res", tag)
    os.makedirs(out_res, exist_ok=True)
    for p in [nb, f"{nb}/Release", f"{nb}/Debug"]:
        if not os.path.exists(p): continue
        for f in os.listdir(p):
            if f.endswith((".so", ".dll", ".dylib")) and "live2d_jni" in f:
                shutil.copy(os.path.join(p, f), out_res)

def build_android():
    ndk = os.environ.get("ANDROID_NDK_HOME") or os.environ.get("ANDROID_NDK_ROOT")
    if not ndk:
        print("ANDROID_NDK_HOME not set, skipping Android build")
        return

    root = os.getcwd()
    abis = ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]
    toolchain = os.path.join(ndk, "build/cmake/android.toolchain.cmake")

    core_lib_dir = os.path.join(SDK_DIR, "Core/lib/android")
    if os.path.exists(core_lib_dir):
        print(f"Listing {core_lib_dir}:")
        for item in os.listdir(core_lib_dir):
            print(f"  {item}")
    else:
        print(f"Directory not found: {core_lib_dir}")

    for abi in abis:
        lib_path = os.path.join(SDK_DIR, "Core/lib/android", abi, "libLive2DCubismCore.a")
        if not os.path.exists(lib_path):
            print(f"Skipping {abi}: Library not found at {lib_path}")
            continue

        print(f"Building for Android {abi}...")
        nb = os.path.join(root, f"native/build-android-{abi}")
        os.makedirs(nb, exist_ok=True)
        
        cmd = [
            "cmake", "..",
            f"-DCMAKE_TOOLCHAIN_FILE={toolchain}",
            f"-DANDROID_ABI={abi}",
            "-DANDROID_PLATFORM=android-21",
            "-DCSM_TARGET_ANDROID_ES2=ON"
        ]
        if platform.system().lower() == "windows":
             cmd.append("-GMinGW Makefiles") 

        run_cmd(cmd, nb)
        run_cmd(["cmake", "--build", "."], nb)

        out_res = os.path.join(root, "out", "native_res", f"android-{abi}")
        os.makedirs(out_res, exist_ok=True)
        
        for f in os.listdir(nb):
             if f.endswith(".so") and "live2d_jni" in f:
                shutil.copy(os.path.join(nb, f), out_res)

def compile_java():
    root = os.getcwd()
    out = os.path.join(root, "out")
    classes = os.path.join(out, "classes")
    if os.path.exists(classes): shutil.rmtree(classes)
    os.makedirs(classes)

    shader_root = os.path.join(classes, "live2d", "shaders")
    gl_shader_dir = os.path.join(shader_root, "opengl")
    vk_shader_dir = os.path.join(shader_root, "vulkan")
    os.makedirs(gl_shader_dir)
    os.makedirs(vk_shader_dir)

    opengl_src = os.path.join(SDK_DIR, "Framework/src/Rendering/OpenGL/Shaders/StandardES")
    for f in os.listdir(opengl_src):
        shutil.copy(os.path.join(opengl_src, f), gl_shader_dir)

    compile_vulkan_shaders(vk_shader_dir)

    src = os.path.join(root, "binding/src/main/java")
    j_files = [os.path.join(dp, f) for dp, dn, fn in os.walk(src) for f in fn if f.endswith('.java')]
    run_cmd(["javac", "-d", classes, "--source-path", src] + j_files)
    run_cmd(["jar", "--create", "--file", os.path.join(out, "live2d-shared.jar"), "-C", classes, "."])

def package_jars():
    root = os.getcwd()
    out = os.path.join(root, "out")
    res_root = os.path.join(out, "native_res")
    
    if not os.path.exists(res_root): return

    for platform_tag in os.listdir(res_root):
        if "-" not in platform_tag:
            continue
        tag_dir = os.path.join(res_root, platform_tag)
        if not os.path.isdir(tag_dir): continue
        
        jar_name = f"live2d-native-{platform_tag}.jar"
        # We need to structure it as /<platform_tag>/<lib> inside the jar
        # The current extract logic expects /<platform_tag>/<lib>
        # So we create a temp structure
        tmp_pkg = os.path.join(out, "tmp_pkg", platform_tag)
        os.makedirs(tmp_pkg, exist_ok=True)
        for f in os.listdir(tag_dir):
            src = os.path.join(tag_dir, f)
            if not os.path.isfile(src):
                continue
            shutil.copy(src, tmp_pkg)

        # Create MANIFEST.MF with Automatic-Module-Name for JPMS compatibility.
        # Without this, the JAR filename 'live2d-native-...' maps to the module
        # name 'live2d.native' which is invalid because 'native' is a reserved keyword.
        manifest_dir = os.path.join(out, "tmp_pkg", "META-INF")
        os.makedirs(manifest_dir, exist_ok=True)
        manifest_path = os.path.join(manifest_dir, "MANIFEST.MF")
        with open(manifest_path, "w", encoding="utf-8") as mf:
            mf.write("Manifest-Version: 1.0\n")
            mf.write("Automatic-Module-Name: dev.eatgrapes.live2d.natives\n")
            mf.write("\n")

        run_cmd(["jar", "--create", "--file", os.path.join(out, jar_name),
                 "--manifest", manifest_path,
                 "-C", os.path.join(out, "tmp_pkg"), "."])
        shutil.rmtree(os.path.join(out, "tmp_pkg"))

if __name__ == "__main__":
    download_sdk()
    download_headers()
    compile_java()
    
    targets = sys.argv[1:]
    if not targets:
        build_desktop()
        build_android()
    else:
        if "desktop" in targets: build_desktop()
        if "android" in targets: build_android()

    package_jars()
