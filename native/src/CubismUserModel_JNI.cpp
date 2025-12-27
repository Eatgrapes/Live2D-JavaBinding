#include <jni.h>
#include <Model/CubismUserModel.hpp>
#include <Id/CubismIdManager.hpp>
#include <Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp>
#include <Motion/CubismMotion.hpp>
#include <vector>
#include <string>
#include <map>
#include <mutex>

using namespace Live2D::Cubism::Framework;
using namespace Live2D::Cubism::Framework::Rendering;

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
    }

    void loadModelCopy(const csmByte* buffer, csmSizeInt size) {
        _mocBuffer.assign(buffer, buffer + size);
        LoadModel(_mocBuffer.data(), (csmSizeInt)_mocBuffer.size());
    }

    void loadPhysicsCopy(const csmByte* buffer, csmSizeInt size) {
        _physicsBuffer.assign(buffer, buffer + size);
        LoadPhysics(_physicsBuffer.data(), (csmSizeInt)_physicsBuffer.size());
    }

    void loadPoseCopy(const csmByte* buffer, csmSizeInt size) {
        _poseBuffer.assign(buffer, buffer + size);
        LoadPose(_poseBuffer.data(), (csmSizeInt)_poseBuffer.size());
    }

    void startMotionCopy(const csmByte* buffer, csmSizeInt size, int priority) {
        std::vector<csmByte> motionBuf(buffer, buffer + size);
        auto* motion = CubismMotion::Create(motionBuf.data(), (csmSizeInt)motionBuf.size());
        if (!motion) return;
        _motionBuffers[motion] = std::move(motionBuf);
        motion->SetFinishedMotionHandlerAndMotionCustomData([](ACubismMotion* self) {
            auto* m = static_cast<CubismMotion*>(self);
            auto* model = static_cast<JniUserModel*>(m->GetFinishedMotionCustomData());
            model->queueFinishedMotion(m);
        }, this);
        _motionManager->StartMotionPriority(motion, true, priority);
    }

    void queueFinishedMotion(CubismMotion* motion) {
        std::lock_guard<std::mutex> lock(_pendingMutex);
        _pendingDeletion.push_back(motion);
    }

    void update(float dt) {
        if (!_model) return;
        {
            std::lock_guard<std::mutex> lock(_pendingMutex);
            for (auto* m : _pendingDeletion) {
                _motionBuffers.erase(m);
                CubismMotion::Delete(m);
                notifyFinished();
            }
            _pendingDeletion.clear();
        }
        _model->LoadParameters();
        _motionManager->UpdateMotion(_model, dt);
        _model->SaveParameters();
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

private:
    JNIEnv* getEnv() {
        JNIEnv* env;
        if (_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_EDETACHED) _jvm->AttachCurrentThread((void**)&env, nullptr);
        return env;
    }
    JavaVM* _jvm;
    jobject _javaObj = nullptr;
    std::vector<csmByte> _mocBuffer, _physicsBuffer, _poseBuffer;
    std::map<CubismMotion*, std::vector<csmByte>> _motionBuffers;
    std::vector<CubismMotion*> _pendingDeletion;
    std::mutex _pendingMutex;
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
    ((JniUserModel*)ptr)->LoadExpression((const csmByte*)data, len, n);
    env->ReleaseByteArrayElements(buffer, data, JNI_ABORT);
    env->ReleaseStringUTFChars(name, n);
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_createRendererNative(JNIEnv*, jclass, jlong ptr) {
    ((JniUserModel*)ptr)->CreateRenderer();
}

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_registerTextureNative(JNIEnv*, jclass, jlong ptr, jint index, jint textureId) {
    auto* r = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (r) r->BindTexture(index, (GLuint)textureId);
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

JNIEXPORT void JNICALL Java_dev_eatgrapes_live2d_CubismUserModel_startMotionNative(JNIEnv* env, jclass, jlong ptr, jbyteArray buffer, jint priority) {
    jsize len = env->GetArrayLength(buffer);
    jbyte* data = env->GetByteArrayElements(buffer, nullptr);
    ((JniUserModel*)ptr)->startMotionCopy((const csmByte*)data, len, priority);
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
    auto* r = ((JniUserModel*)ptr)->GetRenderer<CubismRenderer_OpenGLES2>();
    if (r) { r->SetMvpMatrix(&m); r->DrawModel(); }
    env->ReleaseFloatArrayElements(matrix, m_ptr, JNI_ABORT);
}

}