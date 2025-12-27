package dev.eatgrapes.live2d;

public class CubismUserModel extends Native {
    
    public CubismUserModel() {
        super(createNative());
    }

    private static native long createNative();

    public void loadModel(byte[] buffer) {
        loadModelNative(_ptr, buffer);
    }

    private static native void loadModelNative(long ptr, byte[] buffer);

    public void loadPhysics(byte[] buffer) {
        loadPhysicsNative(_ptr, buffer);
    }

    private static native void loadPhysicsNative(long ptr, byte[] buffer);

    public void loadPose(byte[] buffer) {
        loadPoseNative(_ptr, buffer);
    }

    private static native void loadPoseNative(long ptr, byte[] buffer);

    public void loadExpression(byte[] buffer, String name) {
        loadExpressionNative(_ptr, buffer, name);
    }

    private static native void loadExpressionNative(long ptr, byte[] buffer, String name);

    public void createRenderer() {
        createRendererNative(_ptr);
    }

    private static native void createRendererNative(long ptr);

    public void update(float deltaTime) {
        updateNative(_ptr, deltaTime);
    }

    private static native void updateNative(long ptr, float deltaTime);

    public void registerTexture(int index, int textureId) {
        registerTextureNative(_ptr, index, textureId);
    }

    private static native void registerTextureNative(long ptr, int index, int textureId);

    public void setParameterValue(String id, float value) {
        setParameterValueNative(_ptr, id, value);
    }

    private static native void setParameterValueNative(long ptr, String id, float value);

    public float getCanvasWidth() {
        return getCanvasWidthNative(_ptr);
    }

    private static native float getCanvasWidthNative(long ptr);

    public float getCanvasHeight() {
        return getCanvasHeightNative(_ptr);
    }

    private static native float getCanvasHeightNative(long ptr);

    public void draw(float[] mvpMatrix) {
        drawNative(_ptr, mvpMatrix);
    }

    private static native void drawNative(long ptr, float[] mvpMatrix);

    @Override
    public void close() {
        deleteNative(_ptr);
    }

    private static native void deleteNative(long ptr);
}