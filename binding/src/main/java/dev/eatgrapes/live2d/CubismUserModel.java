package dev.eatgrapes.live2d;

import java.util.function.Consumer;

public class CubismUserModel extends Native {
    private Consumer<String> motionFinishedCallback;

    public CubismUserModel() {
        super(createNative());
    }

    private static native long createNative();

    public void loadModel(byte[] buffer) { loadModelNative(_ptr, buffer); }
    private static native void loadModelNative(long ptr, byte[] buffer);

    public void loadPhysics(byte[] buffer) { loadPhysicsNative(_ptr, buffer); }
    private static native void loadPhysicsNative(long ptr, byte[] buffer);

    public void loadPose(byte[] buffer) { loadPoseNative(_ptr, buffer); }
    private static native void loadPoseNative(long ptr, byte[] buffer);

    public void loadExpression(byte[] buffer, String name) { loadExpressionNative(_ptr, buffer, name); }
    private static native void loadExpressionNative(long ptr, byte[] buffer, String name);

    public void createRenderer() { createRendererNative(_ptr); }
    private static native void createRendererNative(long ptr);

    public void registerTexture(int index, int textureId) { registerTextureNative(_ptr, index, textureId); }
    private static native void registerTextureNative(long ptr, int index, int textureId);

    // Eye Tracking / Dragging
    public void setDragging(float x, float y) { setDraggingNative(_ptr, x, y); }
    private static native void setDraggingNative(long ptr, float x, float y);

    // Hit Detection
    public boolean isHit(String drawableId, float x, float y) { return isHitNative(_ptr, drawableId, x, y); }
    private static native boolean isHitNative(long ptr, String drawableId, float x, float y);

    // Motion Playback
    public void startMotion(byte[] buffer, int priority, Consumer<String> onFinished) {
        this.motionFinishedCallback = onFinished;
        startMotionNative(_ptr, buffer, priority);
    }
    private static native void startMotionNative(long ptr, byte[] buffer, int priority);

    // Called from JNI
    private void onMotionFinished(String name) {
        if (motionFinishedCallback != null) {
            motionFinishedCallback.accept(name);
        }
    }

    public void update(float deltaTime) { updateNative(_ptr, deltaTime); }
    private static native void updateNative(long ptr, float deltaTime);

    public void setParameterValue(String id, float value) { setParameterValueNative(_ptr, id, value); }
    private static native void setParameterValueNative(long ptr, String id, float value);

    public float getCanvasWidth() { return getCanvasWidthNative(_ptr); }
    private static native float getCanvasWidthNative(long ptr);

    public float getCanvasHeight() { return getCanvasHeightNative(_ptr); }
    private static native float getCanvasHeightNative(long ptr);

    public String[] getDrawableIds() { return getDrawableIdsNative(_ptr); }
    private static native String[] getDrawableIdsNative(long ptr);

    public void draw(float[] mvpMatrix) { drawNative(_ptr, mvpMatrix); }
    private static native void drawNative(long ptr, float[] mvpMatrix);

    @Override
    public void close() { deleteNative(_ptr); }
    private static native void deleteNative(long ptr);
}
