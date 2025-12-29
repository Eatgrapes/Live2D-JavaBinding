#include <jni.h>
#include <CubismFramework.hpp>
#include <ICubismAllocator.hpp>
#include <cstdlib>
#include <cstdio>
#include <string>

#ifdef _WIN32
#include <malloc.h>
extern "C" void init_gles2_shim();
#endif

using namespace Live2D::Cubism::Framework;

static JavaVM* g_jvm = nullptr;

static csmByte* LoadFile(const std::string filePath, csmSizeInt* outSize) {
    JNIEnv* env;
    bool attached = false;
    if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        g_jvm->AttachCurrentThread((void**)&env, nullptr);
        attached = true;
    }

    csmByte* buffer = nullptr;
    jclass loaderClass = env->FindClass("dev/eatgrapes/live2d/LibraryLoader");
    jmethodID loadMethod = env->GetStaticMethodID(loaderClass, "loadResource", "(Ljava/lang/String;)[B");
    jstring jPath = env->NewStringUTF(filePath.c_str());
    jbyteArray bytes = (jbyteArray)env->CallStaticObjectMethod(loaderClass, loadMethod, jPath);
    
    if (bytes) {
        jsize len = env->GetArrayLength(bytes);
        *outSize = static_cast<csmSizeInt>(len);
        buffer = (csmByte*)malloc(len);
        env->GetByteArrayRegion(bytes, 0, len, (jbyte*)buffer);
    }

    if (attached) g_jvm->DetachCurrentThread();
    return buffer;
}

static void ReleaseBytes(csmByte* byteData) { free(byteData); }

class JniAllocator : public ICubismAllocator {
public:
    void* Allocate(const csmSizeType size) override { return malloc(size); }
    void Deallocate(void* memory) override { free(memory); }
    void* AllocateAligned(const csmSizeType size, const csmUint32 alignment) override {
#ifdef _WIN32
        return _aligned_malloc(size, alignment);
#else
        void* ptr = nullptr;
        posix_memalign(&ptr, alignment, size);
        return ptr;
#endif
    }
    void DeallocateAligned(void* alignedMemory) override {
#ifdef _WIN32
        _aligned_free(alignedMemory);
#else
        free(alignedMemory);
#endif
    }
};

static JniAllocator allocator;
static CubismFramework::Option option;
static bool g_useJavaLogger = false;

static void LogFunction(const char* message) {
    if (!g_useJavaLogger) return;
    
    JNIEnv* env;
    bool attached = false;
    if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        g_jvm->AttachCurrentThread((void**)&env, nullptr);
        attached = true;
    }

    jclass cls = env->FindClass("dev/eatgrapes/live2d/CubismFramework");
    if (cls) {
        jmethodID mid = env->GetStaticMethodID(cls, "onLog", "(Ljava/lang/String;)V");
        if (mid) {
            jstring str = env->NewStringUTF(message);
            env->CallStaticVoidMethod(cls, mid, str);
            env->DeleteLocalRef(str);
        }
    }

    if (attached) g_jvm->DetachCurrentThread();
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
#ifdef _WIN32
    init_gles2_shim();
#endif
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismFramework_startUpNative(JNIEnv* env, jclass clazz, jboolean hasCallback, jint logLevel) {
    g_useJavaLogger = hasCallback;
    option.LogFunction = LogFunction;
    option.LoggingLevel = static_cast<CubismFramework::Option::LogLevel>(logLevel);
    option.LoadFileFunction = LoadFile;
    option.ReleaseBytesFunction = ReleaseBytes;
    CubismFramework::StartUp(&allocator, &option);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismFramework_initialize(JNIEnv* env, jclass clazz) {
    CubismFramework::Initialize();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismFramework_dispose(JNIEnv* env, jclass clazz) {
    CubismFramework::Dispose();
}

JNIEXPORT jboolean JNICALL Java_dev_eatgrapes_live2d_CubismFramework_isStarted(JNIEnv* env, jclass clazz) {
    return CubismFramework::IsStarted();
}

JNIEXPORT jboolean JNICALL Java_dev_eatgrapes_live2d_CubismFramework_isInitialized(JNIEnv* env, jclass clazz) {
    return CubismFramework::IsInitialized();
}

}