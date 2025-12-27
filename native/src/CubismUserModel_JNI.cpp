#include <jni.h>
#include <Model/CubismUserModel.hpp>
#include <Id/CubismIdManager.hpp>
#include <Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp>
#include <Motion/CubismMotion.hpp>
#include <vector>
#include <string>

using namespace Live2D::Cubism::Framework;
using namespace Live2D::Cubism::Framework::Rendering;

class JniUserModel : public CubismUserModel {
public:
    JniUserModel(JNIEnv* env, jobject javaObj) {
        env->GetJavaVM(&_jvm);
        _javaObj = env->NewGlobalRef(javaObj);
    }

    ~JniUserModel() {
        JNIEnv* env = GetEnv();
        if (env) env->DeleteGlobalRef(_javaObj);
    }

    void LoadModelWithCopy(const csmByte* buffer, csmSizeInt size) {
        _mocData.assign(buffer, buffer + size);
        this->LoadModel(_mocData.data(), static_cast<csmSizeInt>(_mocData.size()));
    }

    void StartMotionWithCallback(const csmByte* buffer, csmSizeInt size, int priority) {
        auto* motion = CubismMotion::Create(buffer, size);
        if (!motion) return;

        motion->SetFinishedMotionHandler([](ACubismMotion* self, void* customData) {
            auto* model = static_cast<JniUserModel*>(customData);
            model->NotifyMotionFinished("motion");
            CubismMotion::Delete(static_cast<CubismMotion*>(self));
        }, this);

        _motionManager->StartMotionPriority(motion, true, priority);
    }

    void NotifyMotionFinished(const std::string& name) {
        JNIEnv* env = GetEnv();
        if (!env) return;
        jclass clazz = env->GetObjectClass(_javaObj);
        jmethodID mid = env->GetMethodID(clazz, "onMotionFinished", "(Ljava/lang/String;)V");
        jstring jname = env->NewStringUTF(name.c_str());
        env->CallVoidMethod(_javaObj, mid, jname);
        env->DeleteLocalRef(jname);
    }

    void UpdateFramework(float deltaTime) {
        if (!_model) return;

        _model->LoadParameters();
        if (_motionManager->IsFinished()) {
            // Idle logic could go here
        } else {
            _motionManager->UpdateMotion(_model, deltaTime);
        }
        _model->SaveParameters();

        if (_dragManager) {
            _dragManager->Update(deltaTime);
            _model->AddParameterValue(CubismFramework::GetIdManager()->GetId("ParamAngleX"), _dragManager->GetX() * 30.0f);
            _model->AddParameterValue(CubismFramework::GetIdManager()->GetId("ParamAngleY"), _dragManager->GetY() * 30.0f);
            _model->AddParameterValue(CubismFramework::GetIdManager()->GetId("ParamBodyAngleX"), _dragManager->GetX() * 10.0f);
            _model->AddParameterValue(CubismFramework::GetIdManager()->GetId("ParamEyeBallX"), _dragManager->GetX());
            _model->AddParameterValue(CubismFramework::GetIdManager()->GetId("ParamEyeBallY"), _dragManager->GetY());
        }

        if (_pose) _pose->UpdateParameters(_model, deltaTime);
        if (_physics) _physics->Evaluate(_model, deltaTime);
        _model->Update();
    }

private:
    JNIEnv* GetEnv() {
        JNIEnv* env;
        if (_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_EDETACHED) {
            _jvm->AttachCurrentThread((void**)&env, nullptr);
        }
        return env;
    }

    JavaVM* _jvm;
    jobject _javaObj;
    std::vector<csmByte> _mocData;
};

extern "C" {

JNIEXPORT jlong JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createNative(JNIEnv* env, jobject thiz) {
    return (jlong) new JniUserModel(env, thiz);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_deleteNative(JNIEnv* env, jclass clazz, jlong ptr) {
    delete (JniUserModel*) ptr;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadModelNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->LoadModelWithCopy((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPhysicsNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->LoadPhysics((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPoseNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->LoadPose((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadExpressionNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer, jstring name) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    const char* n = env->GetStringUTFChars(name, nullptr);
    ((JniUserModel*)ptr)->LoadExpression((const csmByte*)data, len, n);
    env->ReleaseStringUTFChars(name, n);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createRendererNative(JNIEnv* env, jclass clazz, jlong ptr) {
    ((JniUserModel*)ptr)->CreateRenderer();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_registerTextureNative(JNIEnv* env, jclass clazz, jlong ptr, jint index, jint textureId) {
    auto* renderer = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (renderer) renderer->BindTexture(index, (GLuint)textureId);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setDraggingNative(JNIEnv* env, jclass clazz, jlong ptr, jfloat x, jfloat y) {
    ((JniUserModel*)ptr)->SetDragging(x, y);
}

JNIEXPORT jboolean JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_isHitNative(JNIEnv* env, jclass clazz, jlong ptr, jstring drawableId, jfloat x, jfloat y) {
    const char* idStr = env->GetStringUTFChars(drawableId, nullptr);
    bool hit = ((JniUserModel*)ptr)->IsHit(CubismFramework::GetIdManager()->GetId(idStr), x, y);
    env->ReleaseStringUTFChars(drawableId, idStr);
    return hit;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_startMotionNative(JNIEnv* env, jclass clazz, jlong ptr, jbyteArray buffer, jint priority) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->StartMotionWithCallback((const csmByte*)data, len, priority);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_updateNative(JNIEnv* env, jclass clazz, jlong ptr, jfloat deltaTime) {
    ((JniUserModel*)ptr)->UpdateFramework(deltaTime);
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
