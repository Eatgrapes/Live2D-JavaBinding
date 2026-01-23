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

def download_sdk():
    if os.path.exists(SDK_DIR):
        return
    print("Downloading SDK...")
    urllib.request.urlretrieve(SDK_URL, "sdk.zip")
    with zipfile.ZipFile("sdk.zip", 'r') as z: z.extractall("temp")
    inner = [d for d in os.listdir("temp") if os.path.isdir(os.path.join("temp", d))][0]
    shutil.move(os.path.join("temp", inner), SDK_DIR)
    shutil.rmtree("temp")
    os.remove("sdk.zip")
    tpp = os.path.join(SDK_DIR, "Framework/src/Rendering/CubismClippingManager.tpp")
    with open(tpp, "r", encoding="utf-8") as f: content = f.read()
    with open(tpp, "w", encoding="utf-8") as f: 
        f.write(content.replace("_clearedMaskBufferFlags = NULL;", "_clearedMaskBufferFlags.Clear();"))
    
    gles2_hpp = os.path.join(SDK_DIR, "Framework/src/Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp")
    with open(gles2_hpp, "r", encoding="utf-8") as f: content = f.read()
    content = content.replace("void Initialize(Framework::CubismModel* model);", 
                              "void Initialize(Framework::CubismModel* model) override;")
    content = content.replace("void Initialize(Framework::CubismModel* model, csmInt32 maskBufferCount);", 
                              "void Initialize(Framework::CubismModel* model, csmInt32 maskBufferCount) override;")
    content = content.replace("virtual void SaveProfile();", "void SaveProfile() override;")
    content = content.replace("virtual void RestoreProfile();", "void RestoreProfile() override;")
    with open(gles2_hpp, "w", encoding="utf-8") as f: f.write(content)

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
    run_cmd(["cmake", ".."], nb)
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

    sd = os.path.join(classes, "live2d", "shaders")
    os.makedirs(sd)
    ss = os.path.join(SDK_DIR, "Framework/src/Rendering/OpenGL/Shaders/StandardES")
    for f in os.listdir(ss): shutil.copy(os.path.join(ss, f), sd)

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
        tag_dir = os.path.join(res_root, platform_tag)
        if not os.path.isdir(tag_dir): continue
        
        jar_name = f"live2d-native-{platform_tag}.jar"
        # We need to structure it as /<platform_tag>/<lib> inside the jar
        # The current extract logic expects /<platform_tag>/<lib>
        # So we create a temp structure
        tmp_pkg = os.path.join(out, "tmp_pkg", platform_tag)
        os.makedirs(tmp_pkg, exist_ok=True)
        for f in os.listdir(tag_dir):
            shutil.copy(os.path.join(tag_dir, f), tmp_pkg)
        
        run_cmd(["jar", "--create", "--file", os.path.join(out, jar_name), "-C", os.path.join(out, "tmp_pkg"), "."])
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