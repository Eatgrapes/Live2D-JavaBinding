# Rendering Loop

To verify the model is working, you need to update its state and draw it every frame. This typically happens inside your main game loop (e.g., GLFW loop).

## The Update Step

The `update` method advances the physics, fades motions, and calculates the new positions of all vertices.

```java
// deltaTime: Time in seconds since the last frame (e.g., 0.016 for 60fps)
model.update(deltaTime);
```

> **Warning**: Do not pass `0` as delta time, as physics calculations may behave unpredictably.

## The Draw Step

The `draw` method issues the OpenGL commands to render the mesh. It requires a Model-View-Projection (MVP) matrix to position the model on your screen.

```java
// A simple 4x4 Identity Matrix (Float array of size 16)
float[] mvpMatrix = new float[] {
    1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1
};

// Adjust for aspect ratio so the model doesn't look stretched
float aspect = (float) windowWidth / windowHeight;
mvpMatrix[0] = 1.0f / aspect; // Scale X by 1/aspect

model.draw(mvpMatrix);
```

## Context Awareness

`model.draw()` calls OpenGL functions (`glDrawElements`, `glBindTexture`, etc.). Therefore, **it must be called from the thread that holds the OpenGL context**.
