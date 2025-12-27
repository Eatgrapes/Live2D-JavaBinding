#include <jni.h>
#include <Model/CubismUserModel.hpp>
#include <Id/CubismIdManager.hpp>
#include <Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp>
#include <vector>

using namespace Live2D::Cubism::Framework;
using namespace Live2D::Cubism::Framework::Rendering;

class JniUserModel : public CubismUserModel {
public:
    void LoadModelWithCopy(const csmByte* buffer, csmSizeInt size) {
        _mocData.assign(buffer, buffer + size);
        this->LoadModel(_mocData.data(), static_cast<csmSizeInt>(_mocData.size()));
    }

    void UpdateFramework(float deltaTime) {
        if (!_model) return;
        if (_pose) _pose->UpdateParameters(_model, deltaTime);
        if (_physics) _physics->Evaluate(_model, deltaTime);
        _model->Update();
    }

private:
    std::vector<csmByte> _mocData;
};

extern "C" {

JNIEXPORT jlong JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createNative(JNIEnv* env, jclass clazz) {
    return (jlong) new JniUserModel();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_deleteNative(JNIEnv* env, jclass clazz, jlong ptr) {
    delete (JniUserModel*) ptr;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadModelNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer) {
    auto* model = (JniUserModel*) ptr;
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    model->LoadModelWithCopy((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPhysicsNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer) {
    auto* model = (JniUserModel*) ptr;
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    model->LoadPhysics((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPoseNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer) {
    auto* model = (JniUserModel*) ptr;
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    model->LoadPose((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadExpressionNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer, jstring name) {
    auto* model = (JniUserModel*) ptr;
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    const char* n = env->GetStringUTFChars(name, nullptr);
    model->LoadExpression((const csmByte*)data, len, n);
    env->ReleaseStringUTFChars(name, n);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createRendererNative(JNIEnv* env, jclass clazz, jlong ptr) {
    ((JniUserModel*)ptr)->CreateRenderer();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_updateNative(JNIEnv* env, jclass clazz, jlong ptr, jfloat deltaTime) {
    ((JniUserModel*)ptr)->UpdateFramework(deltaTime);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_registerTextureNative(JNIEnv* env, jclass clazz, jlong ptr, jint index, jint textureId) {
    auto* renderer = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (renderer) renderer->BindTexture(index, static_cast<GLuint>(textureId));
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setParameterValueNative(JNIEnv* env, jclass clazz, jlong ptr, jstring id, jfloat value) {
    const char* idStr = env->GetStringUTFChars(id, nullptr);
    ((JniUserModel*)ptr)->GetModel()->SetParameterValue(CubismFramework::GetIdManager()->GetId(idStr), value);
    env->ReleaseStringUTFChars(id, idStr);
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getCanvasWidthNative(JNIEnv* env, jclass clazz, jlong ptr) {
    return ((JniUserModel*)ptr)->GetModel()->GetCanvasWidth();
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getCanvasHeightNative(JNIEnv* env, jclass clazz, jlong ptr) {
    return ((JniUserModel*)ptr)->GetModel()->GetCanvasHeight();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_drawNative(JNIEnv* env, jclass clazz, jlong ptr, jfloatArray mvpMatrix) {
    auto* model = (JniUserModel*) ptr;
    auto* renderer = model->GetRenderer<CubismRenderer_OpenGLES2>();
    if (renderer) {
        jfloat* matrix = env->GetFloatArrayElements(mvpMatrix, nullptr);
        CubismMatrix44 m;
        m.SetMatrix(matrix);
        renderer->SetMvpMatrix(&m);
        renderer->DrawModel();
        env->ReleaseFloatArrayElements(mvpMatrix, matrix, JNI_ABORT);
    }
}

}