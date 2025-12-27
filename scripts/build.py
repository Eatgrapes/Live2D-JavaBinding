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
    # Capture output to show on error, but also print it
    process = subprocess.Popen(cmd, cwd=cwd, shell=shell, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in process.stdout:
        print(line, end="")
    process.wait()
    if process.returncode != 0:
        print(f"Command failed with exit code {process.returncode}")
        sys.exit(process.returncode)

def setup_env():
    if not os.path.exists(SDK_DIR):
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

    inc_dir = os.path.abspath("native/include")
    for path, url in GLES_HEADERS.items():
        full_path = os.path.join(inc_dir, path)
        if not os.path.exists(full_path):
            os.makedirs(os.path.dirname(full_path), exist_ok=True)
            print(f"Downloading {path}...")
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req) as response, open(full_path, 'wb') as out_file:
                shutil.copyfileobj(response, out_file)

def get_tag():
    os_name = platform.system().lower()
    arch = platform.machine().lower()
    p = "macos" if "darwin" in os_name else os_name
    a = "arm64" if ("arm" in arch or "aarch64" in arch) else "x64"
    return f"{p}-{a}"

def build():
    tag = get_tag()
    root = os.getcwd()
    nb = os.path.join(root, "native/build")
    os.makedirs(nb, exist_ok=True)
    run_cmd(["cmake", ".."], nb)
    run_cmd(["cmake", "--build", ".", "--config", "Release"], nb)

    out = os.path.join(root, "out")
    shutil.rmtree(out, ignore_errors=True)
    classes = os.path.join(out, "classes")
    os.makedirs(classes)
    
    # Shaders
    sd = os.path.join(classes, "live2d", "shaders")
    os.makedirs(sd)
    ss = os.path.join(SDK_DIR, "Framework/src/Rendering/OpenGL/Shaders/StandardES")
    for f in os.listdir(ss): shutil.copy(os.path.join(ss, f), sd)

    src = os.path.join(root, "binding/src/main/java")
    j_files = [os.path.join(dp, f) for dp, dn, fn in os.walk(src) for f in fn if f.endswith('.java')]
    run_cmd(["javac", "-d", classes, "--source-path", src] + j_files)
    run_cmd(["jar", "--create", "--file", os.path.join(out, "live2d-shared.jar"), "-C", classes, "."])

    res = os.path.join(out, "native_res", tag)
    os.makedirs(res)
    for p in [nb, f"{nb}/Release", f"{nb}/Debug"]:
        if not os.path.exists(p): continue
        for f in os.listdir(p):
            if f.endswith((".so", ".dll", ".dylib")) and "live2d_jni" in f:
                shutil.copy(os.path.join(p, f), res)
    run_cmd(["jar", "--create", "--file", os.path.join(out, f"live2d-native-{tag}.jar"), "-C", os.path.join(out, "native_res"), "."])

if __name__ == "__main__":
    setup_env()
    build()
