#include <jni.h>
#include <CubismFramework.hpp>
#include <ICubismAllocator.hpp>
#include "RendererBackend.hpp"
#include <cstdlib>
#include <cstdio>
#include <cstdint>
#include <string>

#ifdef _WIN32
#include <malloc.h>
extern "C" void init_gles2_shim();
#endif

#if defined(LIVE2D_HAS_VULKAN)
#include <Rendering/Vulkan/CubismRenderer_Vulkan.hpp>
#endif

using namespace Live2D::Cubism::Framework;
using namespace Live2D::Cubism::Framework::Rendering;

static void ThrowRuntimeException(JNIEnv* env, const char* message)
{
    jclass ex = env->FindClass("java/lang/RuntimeException");
    if (ex)
    {
        env->ThrowNew(ex, message);
    }
}

#if defined(LIVE2D_HAS_VULKAN)
template<typename T>
static T JLongToVkHandle(jlong value)
{
    return (T)(uintptr_t)value;
}
#endif

static JavaVM* g_jvm = nullptr;
static jclass g_libraryLoaderClass = nullptr;
static jmethodID g_loadResourceMethod = nullptr;
static jclass g_cubismFrameworkClass = nullptr;
static jmethodID g_onLogMethod = nullptr;

static csmByte* LoadFile(const std::string filePath, csmSizeInt* outSize) {
    JNIEnv* env;
    bool attached = false;
    jint res = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
#ifdef __ANDROID__
        g_jvm->AttachCurrentThread(&env, nullptr);
#else
        g_jvm->AttachCurrentThread((void**)&env, nullptr);
#endif
        attached = true;
    } else if (res != JNI_OK) {
        return nullptr;
    }

    csmByte* buffer = nullptr;
    if (g_libraryLoaderClass && g_loadResourceMethod) {
        jstring jPath = env->NewStringUTF(filePath.c_str());
        jbyteArray bytes = (jbyteArray)env->CallStaticObjectMethod(g_libraryLoaderClass, g_loadResourceMethod, jPath);
        
        if (bytes) {
            jsize len = env->GetArrayLength(bytes);
            *outSize = static_cast<csmSizeInt>(len);
            buffer = (csmByte*)malloc(len);
            env->GetByteArrayRegion(bytes, 0, len, (jbyte*)buffer);
        }
        env->DeleteLocalRef(jPath);
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
    jint res = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
#ifdef __ANDROID__
        g_jvm->AttachCurrentThread(&env, nullptr);
#else
        g_jvm->AttachCurrentThread((void**)&env, nullptr);
#endif
        attached = true;
    } else if (res != JNI_OK) {
        return;
    }

    if (g_cubismFrameworkClass && g_onLogMethod) {
        jstring str = env->NewStringUTF(message);
        env->CallStaticVoidMethod(g_cubismFrameworkClass, g_onLogMethod, str);
        env->DeleteLocalRef(str);
    }

    if (attached) g_jvm->DetachCurrentThread();
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass loader = env->FindClass("dev/eatgrapes/live2d/LibraryLoader");
    if (loader) {
        g_libraryLoaderClass = (jclass)env->NewGlobalRef(loader);
        g_loadResourceMethod = env->GetStaticMethodID(loader, "loadResource", "(Ljava/lang/String;)[B");
    }

    jclass framework = env->FindClass("dev/eatgrapes/live2d/CubismFramework");
    if (framework) {
        g_cubismFrameworkClass = (jclass)env->NewGlobalRef(framework);
        g_onLogMethod = env->GetStaticMethodID(framework, "onLog", "(Ljava/lang/String;)V");
    }

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

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismFramework_makeGL(JNIEnv* env, jclass clazz) {
    SetRendererBackend(RendererBackend::OpenGL);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismFramework_makeVulkanNative(
    JNIEnv* env,
    jclass clazz,
    jlong device,
    jlong physicalDevice,
    jlong commandPool,
    jlong queue,
    jint swapchainImageCount,
    jint width,
    jint height,
    jlong imageView,
    jint imageFormat,
    jint depthFormat
) {
#if defined(LIVE2D_HAS_VULKAN)
    if (device == 0 || physicalDevice == 0 || commandPool == 0 || queue == 0 || imageView == 0 ||
        swapchainImageCount <= 0 || width <= 0 || height <= 0) {
        ThrowRuntimeException(env, "Invalid Vulkan context arguments");
        return;
    }

    SetRendererBackend(RendererBackend::Vulkan);

    dev::eatgrapes::live2d::VulkanInitContext ctx;
    ctx.device = JLongToVkHandle<VkDevice>(device);
    ctx.physicalDevice = JLongToVkHandle<VkPhysicalDevice>(physicalDevice);
    ctx.commandPool = JLongToVkHandle<VkCommandPool>(commandPool);
    ctx.queue = JLongToVkHandle<VkQueue>(queue);
    ctx.initialized = true;
    dev::eatgrapes::live2d::SetVulkanInitContext(ctx);

    VkExtent2D extent{};
    extent.width = static_cast<uint32_t>(width);
    extent.height = static_cast<uint32_t>(height);
    Live2D::Cubism::Framework::Rendering::CubismRenderer_Vulkan::InitializeConstantSettings(
        ctx.device,
        ctx.physicalDevice,
        ctx.commandPool,
        ctx.queue,
        static_cast<csmUint32>(swapchainImageCount),
        extent,
        JLongToVkHandle<VkImageView>(imageView),
        static_cast<VkFormat>(imageFormat),
        static_cast<VkFormat>(depthFormat)
    );
    Live2D::Cubism::Framework::Rendering::CubismRenderer_Vulkan::EnableChangeRenderTarget();
#else
    ThrowRuntimeException(env, "Vulkan backend is not available in this native build.");
#endif
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
