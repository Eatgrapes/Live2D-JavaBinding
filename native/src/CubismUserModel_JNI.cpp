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
        JNIEnv* env = getEnv();
        if (env) env->DeleteGlobalRef(_javaObj);
    }

    void loadModelCopy(const csmByte* buffer, csmSizeInt size) {
        _mocData.assign(buffer, buffer + size);
        LoadModel(_mocData.data(), (csmSizeInt)_mocData.size());
    }

    void startMotion(const csmByte* buffer, csmSizeInt size, int priority) {
        auto* motion = CubismMotion::Create(buffer, size);
        if (!motion) return;

        motion->SetFinishedMotionHandlerAndMotionCustomData([](ACubismMotion* self, void* data) {
            auto* model = static_cast<JniUserModel*>(data);
            model->notifyFinished();
            CubismMotion::Delete(static_cast<CubismMotion*>(self));
        }, this);

        _motionManager->StartMotionPriority(motion, true, priority);
    }

    void notifyFinished() {
        JNIEnv* env = getEnv();
        if (!env) return;
        jclass cls = env->GetObjectClass(_javaObj);
        jmethodID mid = env->GetMethodID(cls, "onMotionFinished", "(Ljava/lang/String;)V");
        jstring name = env->NewStringUTF("motion");
        env->CallVoidMethod(_javaObj, mid, name);
        env->DeleteLocalRef(name);
    }

    void update(float dt) {
        if (!_model) return;

        _model->LoadParameters();
        if (!_motionManager->IsFinished()) {
            _motionManager->UpdateMotion(_model, dt);
        }
        _model->SaveParameters();

        if (_dragManager) {
            _dragManager->Update(dt);
            auto* idm = CubismFramework::GetIdManager();
            _model->AddParameterValue(idm->GetId("ParamAngleX"), _dragManager->GetX() * 30.0f);
            _model->AddParameterValue(idm->GetId("ParamAngleY"), _dragManager->GetY() * 30.0f);
            _model->AddParameterValue(idm->GetId("ParamBodyAngleX"), _dragManager->GetX() * 10.0f);
            _model->AddParameterValue(idm->GetId("ParamEyeBallX"), _dragManager->GetX());
            _model->AddParameterValue(idm->GetId("ParamEyeBallY"), _dragManager->GetY());
        }

        if (_pose) _pose->UpdateParameters(_model, dt);
        if (_physics) _physics->Evaluate(_model, dt);
        _model->Update();
    }

private:
    JNIEnv* getEnv() {
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

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_deleteNative(JNIEnv* env, jclass, jlong ptr) {
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
    ((JniUserModel*)ptr)->LoadPhysics((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadPoseNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->LoadPose((const csmByte*)data, len);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_loadExpressionNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer, jstring name) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    const char* n = env->GetStringUTFChars(name, nullptr);
    ((JniUserModel*)ptr)->LoadExpression((const csmByte*)data, len, n);
    env->ReleaseStringUTFChars(name, n);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createRendererNative(JNIEnv*, jclass, jlong ptr) {
    ((JniUserModel*)ptr)->CreateRenderer();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_registerTextureNative(JNIEnv*, jclass, jlong ptr, jint index, jint textureId) {
    auto* renderer = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (renderer) renderer->BindTexture(index, (GLuint)textureId);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setDraggingNative(JNIEnv*, jclass, jlong ptr, jfloat x, jfloat y) {
    ((JniUserModel*)ptr)->SetDragging(x, y);
}

JNIEXPORT jboolean JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_isHitNative(JNIEnv* env, jclass, jlong ptr, jstring id, jfloat x, jfloat y) {
    const char* s = env->GetStringUTFChars(id, nullptr);
    bool hit = ((JniUserModel*)ptr)->IsHit(CubismFramework::GetIdManager()->GetId(s), x, y);
    env->ReleaseStringUTFChars(id, s);
    return hit;
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_startMotionNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer, jint priority) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->startMotion((const csmByte*)data, len, priority);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_updateNative(JNIEnv*, jclass, jlong ptr, jfloat dt) {
    ((JniUserModel*)ptr)->update(dt);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_setParameterValueNative(JNIEnv* env, jclass, jlong ptr, jstring id, jfloat value) {
    const char* s = env->GetStringUTFChars(id, nullptr);
    ((JniUserModel*)ptr)->GetModel()->SetParameterValue(CubismFramework::GetIdManager()->GetId(s), value);
    env->ReleaseStringUTFChars(id, s);
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getCanvasWidthNative(JNIEnv*, jclass, jlong ptr) {
    return ((JniUserModel*)ptr)->GetModel()->GetCanvasWidth();
}

JNIEXPORT jfloat JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_getCanvasHeightNative(JNIEnv*, jclass, jlong ptr) {
    return ((JniUserModel*)ptr)->GetModel()->GetCanvasHeight();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_drawNative(JNIEnv* env, jclass, jlong ptr, jfloatArray matrix) {
    auto* model = (JniUserModel*)ptr;
    auto* renderer = model->GetRenderer<CubismRenderer_OpenGLES2>();
    if (renderer) {
        jfloat* m_ptr = env->GetFloatArrayElements(matrix, nullptr);
        CubismMatrix44 m;
        m.SetMatrix(m_ptr);
        renderer->SetMvpMatrix(&m);
        renderer->DrawModel();
        env->ReleaseFloatArrayElements(matrix, m_ptr, JNI_ABORT);
    }
}

}
