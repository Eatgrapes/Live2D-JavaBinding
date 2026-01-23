package dev.eatgrapes.live2d;

import java.util.function.Consumer;

/**
 * Represents a Live2D Cubism user model.
 * <p>
 * This class provides a high-level API to interact with a Live2D model, including loading assets
 * (MOC3, physics, pose, expressions), updating the model state, and rendering it.
 */
public class CubismUserModel extends Native {
    private Consumer<String> motionFinishedCallback;

    /**
     * Creates a new empty user model instance.
     */
    public CubismUserModel() {
        super(createNative());
        linkNative();
    }

    private static native long createNative();
    private native void linkNative();

    /**
     * Loads the model data (MOC3) from a byte buffer.
     * @param buffer The byte array containing MOC3 data.
     */
    public void loadModel(byte[] buffer) { loadModelNative(_ptr, buffer); }
    private static native void loadModelNative(long ptr, byte[] buffer);

    /**
     * Loads the physics data from a byte buffer.
     * @param buffer The byte array containing physics3.json data.
     */
    public void loadPhysics(byte[] buffer) { loadPhysicsNative(_ptr, buffer); }
    private static native void loadPhysicsNative(long ptr, byte[] buffer);

    /**
     * Loads the pose data from a byte buffer.
     * @param buffer The byte array containing pose3.json data.
     */
    public void loadPose(byte[] buffer) { loadPoseNative(_ptr, buffer); }
    private static native void loadPoseNative(long ptr, byte[] buffer);

    /**
     * Loads an expression from a byte buffer and assigns it a name.
     * @param buffer The byte array containing exp3.json data.
     * @param name   The unique name to identify this expression.
     */
    public void loadExpression(byte[] buffer, String name) { loadExpressionNative(_ptr, buffer, name); }
    private static native void loadExpressionNative(long ptr, byte[] buffer, String name);

    /**
     * Sets the current active expression by name.
     * @param name The name of the expression to activate.
     */
    public void setExpression(String name) { setExpressionNative(_ptr, name); }
    private static native void setExpressionNative(long ptr, String name);

    /**
     * Creates the renderer for this model.
     * Must be called before drawing.
     */
    public void createRenderer() { createRendererNative(_ptr); }
    private static native void createRendererNative(long ptr);

    /**
     * Registers a texture with the model's renderer.
     * @param index     The texture index (as defined in the model data).
     * @param textureId The OpenGL texture ID.
     */
    public void registerTexture(int index, int textureId) { registerTextureNative(_ptr, index, textureId); }
    private static native void registerTextureNative(long ptr, int index, int textureId);

    /**
     * Sets the dragging coordinates for interaction (e.g., look-at behavior).
     * @param x The x-coordinate in model space.
     * @param y The y-coordinate in model space.
     */
    public void setDragging(float x, float y) { setDraggingNative(_ptr, x, y); }
    private static native void setDraggingNative(long ptr, float x, float y);

    /**
     * Checks if a point hits a specific drawable part (HitArea).
     * @param drawableId The ID of the drawable to check.
     * @param x          The x-coordinate to test.
     * @param y          The y-coordinate to test.
     * @return true if the point hits the drawable, false otherwise.
     */
    public boolean isHit(String drawableId, float x, float y) { return isHitNative(_ptr, drawableId, x, y); }
    private static native boolean isHitNative(long ptr, String drawableId, float x, float y);

    /**
     * Starts playing a motion.
     *
     * @param buffer     The byte array containing motion3.json data.
     * @param priority   The priority of the motion (1=Low, 2=Normal, 3=Force).
     * @param loop       Whether the motion should loop.
     * @param onFinished Callback to be invoked when the motion finishes.
     */
    public void startMotion(byte[] buffer, int priority, boolean loop, Consumer<String> onFinished) {
        this.motionFinishedCallback = onFinished;
        startMotionNative(_ptr, buffer, priority, loop);
    }
    private static native void startMotionNative(long ptr, byte[] buffer, int priority, boolean loop);

    /**
     * Checks if the currently playing motion has finished.
     * @return true if finished, false otherwise.
     */
    public boolean isMotionFinished() { return isMotionFinishedNative(_ptr); }
    private static native boolean isMotionFinishedNative(long ptr);

    // Called from JNI
    private void onMotionFinished(String name) {
        if (motionFinishedCallback != null) {
            motionFinishedCallback.accept(name);
        }
    }

    /**
     * Updates the model state. Should be called every frame.
     * @param deltaTime The time elapsed since the last frame in seconds.
     */
    public void update(float deltaTime) { updateNative(_ptr, deltaTime); }
    private static native void updateNative(long ptr, float deltaTime);

    /**
     * Sets a parameter value.
     * @param id    The parameter ID.
     * @param value The value to set.
     */
    public void setParameterValue(String id, float value) { setParameterValueNative(_ptr, id, value); }
    private static native void setParameterValueNative(long ptr, String id, float value);

    /**
     * Gets the current value of a parameter.
     * @param id The parameter ID.
     * @return The current value.
     */
    public float getParameterValue(String id) { return getParameterValueNative(_ptr, id); }
    private static native float getParameterValueNative(long ptr, String id);

    /**
     * Gets the width of the model's canvas.
     * @return The canvas width.
     */
    public float getCanvasWidth() { return getCanvasWidthNative(_ptr); }
    private static native float getCanvasWidthNative(long ptr);

    /**
     * Gets the height of the model's canvas.
     * @return The canvas height.
     */
    public float getCanvasHeight() { return getCanvasHeightNative(_ptr); }
    private static native float getCanvasHeightNative(long ptr);

    /**
     * Gets the list of drawable IDs in the model.
     * @return An array of drawable ID strings.
     */
    public String[] getDrawableIds() { return getDrawableIdsNative(_ptr); }
    private static native String[] getDrawableIdsNative(long ptr);

    /**
     * Draws the model using the provided MVP matrix.
     * @param mvpMatrix The 4x4 Model-View-Projection matrix.
     */
    public void draw(float[] mvpMatrix) { drawNative(_ptr, mvpMatrix); }
    private static native void drawNative(long ptr, float[] mvpMatrix);

    @Override
    public void close() { deleteNative(_ptr); }
    private static native void deleteNative(long ptr);
}
