#include <jni.h>
#include <Model/CubismUserModel.hpp>
#include <Id/CubismIdManager.hpp>
#include <Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp>
#include <Motion/CubismMotion.hpp>
#include <Motion/CubismExpressionMotion.hpp>
#include "RendererBackend.hpp"
#include <vector>
#include <string>
#include <map>
#include <mutex>
#include <algorithm>
#include <cstdint>

#if defined(LIVE2D_HAS_VULKAN)
#include <Rendering/Vulkan/CubismRenderer_Vulkan.hpp>
#include <Rendering/Vulkan/CubismClass_Vulkan.hpp>
#endif

#ifdef _WIN32
extern "C" void init_gles2_shim();
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
namespace
{
template<typename T>
T JLongToVkHandle(jlong value)
{
    return (T)(uintptr_t)value;
}

VkCommandBuffer BeginSingleTimeCommands(const dev::eatgrapes::live2d::VulkanInitContext& ctx)
{
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandPool = ctx.commandPool;
    allocInfo.commandBufferCount = 1;

    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    vkAllocateCommandBuffers(ctx.device, &allocInfo, &commandBuffer);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    vkBeginCommandBuffer(commandBuffer, &beginInfo);

    return commandBuffer;
}

void EndSingleTimeCommands(const dev::eatgrapes::live2d::VulkanInitContext& ctx, VkCommandBuffer commandBuffer)
{
    vkEndCommandBuffer(commandBuffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    vkQueueSubmit(ctx.queue, 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(ctx.queue);
    vkFreeCommandBuffers(ctx.device, ctx.commandPool, 1, &commandBuffer);
}

void CopyBufferToImage(VkCommandBuffer commandBuffer, VkBuffer buffer, VkImage image, uint32_t width, uint32_t height)
{
    VkBufferImageCopy region{};
    region.bufferOffset = 0;
    region.bufferRowLength = 0;
    region.bufferImageHeight = 0;
    region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    region.imageSubresource.mipLevel = 0;
    region.imageSubresource.baseArrayLayer = 0;
    region.imageSubresource.layerCount = 1;
    region.imageOffset = {0, 0, 0};
    region.imageExtent = {width, height, 1};

    vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
}
}
#endif

class JniUserModel : public CubismUserModel {
public:
    JniUserModel(JNIEnv* env) {
        env->GetJavaVM(&_jvm);
    }

    void link(JNIEnv* env, jobject obj) {
        _javaObj = env->NewGlobalRef(obj);
    }

    ~JniUserModel() {
        JNIEnv* env = getEnv();
        if (env && _javaObj) env->DeleteGlobalRef(_javaObj);
        for (auto& it : _motionBuffers) CubismMotion::Delete(it.first);
        for (auto& it : _expressions) ACubismMotion::Delete(it.second);
#if defined(LIVE2D_HAS_VULKAN)
        releaseVulkanTextures();
#endif
    }

    void loadModelCopy(const csmByte* buffer, csmSizeInt size) {
        _mocBuffer.assign(buffer, buffer + size);
        LoadModel(_mocBuffer.data(), (csmSizeInt)_mocBuffer.size());
        if (_model) _model->SaveParameters();
    }

    void loadPhysicsCopy(const csmByte* buffer, csmSizeInt size) {
        _physicsBuffer.assign(buffer, buffer + size);
        LoadPhysics(_physicsBuffer.data(), (csmSizeInt)_physicsBuffer.size());
    }

    void loadPoseCopy(const csmByte* buffer, csmSizeInt size) {
        _poseBuffer.assign(buffer, buffer + size);
        LoadPose(_poseBuffer.data(), (csmSizeInt)_poseBuffer.size());
    }

    void loadExpressionCopy(const csmByte* buffer, csmSizeInt size, const std::string& name) {
        std::vector<csmByte> exprBuf(buffer, buffer + size);
        auto* expr = LoadExpression(exprBuf.data(), (csmSizeInt)exprBuf.size(), name.c_str());
        if (expr) {
            _expressions[name] = expr;
            _expressionBuffers[name] = std::move(exprBuf);
        }
    }

    void setExpression(const std::string& name) {
        if (_expressions.count(name)) {
            _expressionManager->StartMotionPriority(_expressions[name], false, 3);
        }
    }

    void startMotionCopy(const csmByte* buffer, csmSizeInt size, int priority, bool loop) {
        std::vector<csmByte> motionBuf(buffer, buffer + size);
        auto* motion = CubismMotion::Create(motionBuf.data(), (csmSizeInt)motionBuf.size());
        if (!motion) return;

        motion->SetLoop(loop);
        _motionBuffers[motion] = std::move(motionBuf);
        
        motion->SetFinishedMotionHandlerAndMotionCustomData([](ACubismMotion* self) {
            auto* m = static_cast<CubismMotion*>(self);
            auto* model = static_cast<JniUserModel*>(m->GetFinishedMotionCustomData());
            model->queueFinishedMotion(m);
        }, this);

        _motionManager->StartMotionPriority(motion, false, priority);
    }

    void queueFinishedMotion(CubismMotion* motion) {
        std::lock_guard<std::mutex> lock(_pendingMutex);
        if (std::find(_pendingDeletion.begin(), _pendingDeletion.end(), motion) == _pendingDeletion.end()) {
            _pendingDeletion.push_back(motion);
        }
    }

    void update(float dt) {
        if (!_model) return;

        {
            std::lock_guard<std::mutex> lock(_pendingMutex);
            for (auto* m : _pendingDeletion) {
                if (_motionBuffers.count(m)) {
                    _motionBuffers.erase(m);
                    CubismMotion::Delete(m);
                    notifyFinished();
                }
            }
            _pendingDeletion.clear();
        }

        _model->LoadParameters();
        _motionManager->UpdateMotion(_model, dt);
        _model->SaveParameters();

        if (_expressionManager) {
            _expressionManager->UpdateMotion(_model, dt);
        }

        if (_pose) _pose->UpdateParameters(_model, dt);
        
        if (_dragManager) {
            _dragManager->Update(dt);
            auto* idm = CubismFramework::GetIdManager();
            _model->AddParameterValue(idm->GetId("ParamAngleX"), _dragManager->GetX() * 30.0f);
            _model->AddParameterValue(idm->GetId("ParamAngleY"), _dragManager->GetY() * 30.0f);
            _model->AddParameterValue(idm->GetId("ParamEyeBallX"), _dragManager->GetX());
            _model->AddParameterValue(idm->GetId("ParamEyeBallY"), _dragManager->GetY());
        }

        if (_physics) _physics->Evaluate(_model, dt);
        _model->Update();
    }

    bool isMotionFinished() { return _motionManager->IsFinished(); }

    void notifyFinished() {
        JNIEnv* env = getEnv();
        if (!env || !_javaObj) return;
        jclass cls = env->GetObjectClass(_javaObj);
        jmethodID mid = env->GetMethodID(cls, "onMotionFinished", "(Ljava/lang/String;)V");
        jstring name = env->NewStringUTF("motion");
        env->CallVoidMethod(_javaObj, mid, name);
        env->DeleteLocalRef(name);
    }

    bool isHitTransformed(const char* id, float x, float y) {
        if (!_model || !_modelMatrix) return false;
        return IsHit(CubismFramework::GetIdManager()->GetId(id), _modelMatrix->InvertTransformX(x), _modelMatrix->InvertTransformY(y));
    }

#if defined(LIVE2D_HAS_VULKAN)
    bool registerTextureVulkanCopy(int index, int width, int height, const csmByte* rgba, csmSizeInt size) {
        if (!rgba || width <= 0 || height <= 0 || size < static_cast<csmSizeInt>(width * height * 4)) {
            return false;
        }

        if (!dev::eatgrapes::live2d::HasVulkanInitContext()) {
            return false;
        }
        const auto& ctx = dev::eatgrapes::live2d::GetVulkanInitContext();
        if (!ctx.initialized || ctx.device == VK_NULL_HANDLE || ctx.physicalDevice == VK_NULL_HANDLE ||
            ctx.commandPool == VK_NULL_HANDLE || ctx.queue == VK_NULL_HANDLE) {
            return false;
        }

        CubismBufferVulkan stagingBuffer;
        const VkDeviceSize imageSize = static_cast<VkDeviceSize>(width) * static_cast<VkDeviceSize>(height) * 4;
        stagingBuffer.CreateBuffer(
            ctx.device,
            ctx.physicalDevice,
            imageSize,
            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );
        stagingBuffer.Map(ctx.device, imageSize);
        stagingBuffer.MemCpy(rgba, imageSize);
        stagingBuffer.UnMap(ctx.device);

        CubismImageVulkan image;
        image.CreateImage(
            ctx.device,
            ctx.physicalDevice,
            width,
            height,
            1,
            VK_FORMAT_R8G8B8A8_UNORM,
            VK_IMAGE_TILING_OPTIMAL,
            VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT
        );

        VkCommandBuffer commandBuffer = BeginSingleTimeCommands(ctx);
        image.SetImageLayout(commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, VK_IMAGE_ASPECT_COLOR_BIT);
        CopyBufferToImage(commandBuffer, stagingBuffer.GetBuffer(), image.GetImage(), static_cast<uint32_t>(width), static_cast<uint32_t>(height));
        image.SetImageLayout(commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, 1, VK_IMAGE_ASPECT_COLOR_BIT);
        EndSingleTimeCommands(ctx, commandBuffer);

        image.CreateView(ctx.device, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_ASPECT_COLOR_BIT, 1);

        VkPhysicalDeviceProperties properties{};
        vkGetPhysicalDeviceProperties(ctx.physicalDevice, &properties);
        image.CreateSampler(ctx.device, properties.limits.maxSamplerAnisotropy, 1);
        stagingBuffer.Destroy(ctx.device);

        auto* renderer = GetRenderer<CubismRenderer_Vulkan>();
        if (!renderer) {
            image.Destroy(ctx.device);
            return false;
        }

        (void)index;
        renderer->BindTexture(image);
        _vulkanTextures.push_back(image);
        return true;
    }

    void setVulkanRenderTarget(VkImage image, VkImageView imageView, VkFormat format, uint32_t width, uint32_t height) {
        VkExtent2D extent{};
        extent.width = width;
        extent.height = height;
        CubismRenderer_Vulkan::SetRenderTarget(image, imageView, format, extent);
    }

    void releaseVulkanTextures() {
        if (!dev::eatgrapes::live2d::HasVulkanInitContext()) {
            return;
        }
        const auto& ctx = dev::eatgrapes::live2d::GetVulkanInitContext();
        if (!ctx.initialized || ctx.device == VK_NULL_HANDLE) {
            return;
        }
        for (auto& texture : _vulkanTextures) {
            texture.Destroy(ctx.device);
        }
        _vulkanTextures.clear();
    }
#endif

private:
    JNIEnv* getEnv() {
        JNIEnv* env;
        if (_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_EDETACHED) {
#ifdef __ANDROID__
            _jvm->AttachCurrentThread(&env, nullptr);
#else
            _jvm->AttachCurrentThread((void**)&env, nullptr);
#endif
        }
        return env;
    }

    JavaVM* _jvm;
    jobject _javaObj = nullptr;
    std::vector<csmByte> _mocBuffer, _physicsBuffer, _poseBuffer;
    std::map<CubismMotion*, std::vector<csmByte>> _motionBuffers;
    std::map<std::string, std::vector<csmByte>> _expressionBuffers;
    std::map<std::string, ACubismMotion*> _expressions;
    std::vector<CubismMotion*> _pendingDeletion;
    std::mutex _pendingMutex;
#if defined(LIVE2D_HAS_VULKAN)
    std::vector<CubismImageVulkan> _vulkanTextures;
#endif
};

extern "C" {

JNIEXPORT jlong JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createNative(JNIEnv* env, jclass) {
    return (jlong) new JniUserModel(env);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_linkNative(JNIEnv* env, jobject thiz) {
    jclass cls = env->GetObjectClass(thiz);
    jfieldID fid = env->GetFieldID(cls, "_ptr", "J");
    jlong ptr = env->GetLongField(thiz, fid);
    ((JniUserModel*)ptr)->link(env, thiz);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_deleteNative(JNIEnv*, jclass, jlong ptr) {
    delete (JniUserModel*)ptr;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadModelNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->loadModelCopy((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPhysicsNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->loadPhysicsCopy((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPoseNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->loadPoseCopy((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadExpressionNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer, jstring name) {
    const char* n = env->GetStringUTFChars(name, nullptr);
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->loadExpressionCopy((const csmByte*)data, len, n);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
    env->ReleaseStringUTFChars(name, n);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setExpressionNative(JNIEnv* env, jclass, jlong ptr, jstring name) {
    const char* n = env->GetStringUTFChars(name, nullptr);
    ((JniUserModel*)ptr)->setExpression(n);
    env->ReleaseStringUTFChars(name, n);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createRendererNative(JNIEnv*, jclass, jlong ptr) {
#ifdef _WIN32
    if (GetRendererBackend() == RendererBackend::OpenGL) {
        init_gles2_shim();
    }
#endif
    ((JniUserModel*)ptr)->CreateRenderer();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_registerTextureNative(JNIEnv*, jclass, jlong ptr, jint index, jint textureId) {
    auto* r = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (r) r->BindTexture(index, (GLuint)textureId);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_registerTextureVulkanNative(
    JNIEnv* env, jclass, jlong ptr, jint index, jint width, jint height, jbyteArray rgbaPixels
) {
#if defined(LIVE2D_HAS_VULKAN)
    if (!rgbaPixels) {
        ThrowRuntimeException(env, "rgbaPixels is null");
        return;
    }
    const jsize len = env->GetArrayLength(rgbaPixels);
    jbyte* data = env->GetByteArrayElements(rgbaPixels, nullptr);
    const bool ok = ((JniUserModel*)ptr)->registerTextureVulkanCopy(
        index,
        width,
        height,
        reinterpret_cast<const csmByte*>(data),
        static_cast<csmSizeInt>(len)
    );
    env->ReleaseByteArrayElements(rgbaPixels, data, JNI_ABORT);
    if (!ok) {
        ThrowRuntimeException(env, "Failed to register Vulkan texture.");
    }
#else
    ThrowRuntimeException(env, "Vulkan backend is not available in this native build.");
#endif
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setVulkanRenderTargetNative(
    JNIEnv* env, jclass, jlong ptr, jlong image, jlong imageView, jint imageFormat, jint width, jint height
) {
#if defined(LIVE2D_HAS_VULKAN)
    if (image == 0 || imageView == 0 || width <= 0 || height <= 0) {
        ThrowRuntimeException(env, "Invalid Vulkan render target arguments");
        return;
    }
    ((JniUserModel*)ptr)->setVulkanRenderTarget(
        JLongToVkHandle<VkImage>(image),
        JLongToVkHandle<VkImageView>(imageView),
        static_cast<VkFormat>(imageFormat),
        static_cast<uint32_t>(width),
        static_cast<uint32_t>(height)
    );
#else
    ThrowRuntimeException(env, "Vulkan backend is not available in this native build.");
#endif
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setDraggingNative(JNIEnv*, jclass, jlong ptr, jfloat x, jfloat y) {
    ((JniUserModel*)ptr)->SetDragging(x, y);
}

JNIEXPORT jboolean JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_isHitNative(JNIEnv* env, jclass, jlong ptr, jstring id, jfloat x, jfloat y) {
    const char* s = env->GetStringUTFChars(id, nullptr);
    bool hit = ((JniUserModel*)ptr)->isHitTransformed(s, x, y);
    env->ReleaseStringUTFChars(id, s);
    return hit;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_startMotionNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer, jint priority, jboolean loop) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->startMotionCopy((const csmByte*)data, len, priority, loop);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT jboolean JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_isMotionFinishedNative(JNIEnv*, jclass, jlong ptr) {
    return ((JniUserModel*)ptr)->isMotionFinished();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_updateNative(JNIEnv*, jclass, jlong ptr, jfloat dt) {
    ((JniUserModel*)ptr)->update(dt);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setParameterValueNative(JNIEnv* env, jclass, jlong ptr, jstring id, jfloat value) {
    const char* s = env->GetStringUTFChars(id, nullptr);
    auto* model = ((JniUserModel*)ptr)->GetModel();
    model->SetParameterValue(CubismFramework::GetIdManager()->GetId(s), value);
    model->SaveParameters();
    env->ReleaseStringUTFChars(id, s);
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getParameterValueNative(JNIEnv* env, jclass, jlong ptr, jstring id) {
    const char* s = env->GetStringUTFChars(id, nullptr);
    float value = ((JniUserModel*)ptr)->GetModel()->GetParameterValue(CubismFramework::GetIdManager()->GetId(s));
    env->ReleaseStringUTFChars(id, s);
    return value;
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getCanvasWidthNative(JNIEnv*, jclass, jlong ptr) {
    return ((JniUserModel*)ptr)->GetModel()->GetCanvasWidth();
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getCanvasHeightNative(JNIEnv*, jclass, jlong ptr) {
    return ((JniUserModel*)ptr)->GetModel()->GetCanvasHeight();
}

JNIEXPORT jobjectArray JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getDrawableIdsNative(JNIEnv* env, jclass, jlong ptr) {
    auto* model = ((JniUserModel*)ptr)->GetModel();
    int count = model->GetDrawableCount();
    jobjectArray res = env->NewObjectArray(count, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < count; i++) {
        jstring s = env->NewStringUTF(model->GetDrawableId(i)->GetString().GetRawString());
        env->SetObjectArrayElement(res, i, s);
        env->DeleteLocalRef(s);
    }
    return res;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_drawNative(JNIEnv* env, jclass, jlong ptr, jfloatArray matrix) {
    jfloat* m_ptr = env->GetFloatArrayElements(matrix, nullptr);
    CubismMatrix44 m; m.SetMatrix(m_ptr);
    auto* glRenderer = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (glRenderer) {
        glRenderer->SetMvpMatrix(&m);
        glRenderer->DrawModel();
    }
#if defined(LIVE2D_HAS_VULKAN)
    else {
        auto* vkRenderer = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_Vulkan>();
        if (vkRenderer) {
            vkRenderer->SetMvpMatrix(&m);
            vkRenderer->DrawModel();
        }
    }
#endif
    env->ReleaseFloatArrayElements(matrix, m_ptr, JNI_ABORT);
}

}
