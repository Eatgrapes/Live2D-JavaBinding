# Loading Assets

Live2D models are composed of several files. You need to load them in a specific order. The API expects `byte[]` arrays, so you can load these files from the disk, a JAR resource, or a network stream.

## 1. The Core Model (.moc3)

This is the geometry and structure of the character. It is **mandatory**.

```java
byte[] mocBytes = Files.readAllBytes(Path.of("Hiyori.moc3"));
CubismUserModel model = new CubismUserModel();
model.loadModel(mocBytes);
```

## 2. Poses (.pose3.json)

Poses handle "Part Sorting" and visibility. For example, hiding the arms when they go behind the back. It is **highly recommended**.

```java
byte[] poseBytes = Files.readAllBytes(Path.of("Hiyori.pose3.json"));
model.loadPose(poseBytes);
```

## 3. Physics (.physics3.json)

This file controls the automatic movement of hair, clothes, and accessories based on the model's movement. Without this, the model will look very stiff.

```java
byte[] physicsBytes = Files.readAllBytes(Path.of("Hiyori.physics3.json"));
model.loadPhysics(physicsBytes);
```

## 4. Textures

Textures are not loaded directly by the Live2D engine. Registration differs by backend:

```java
// OpenGL: load image to OpenGL and get an ID
int glTextureId = loadTexture("texture_00.png");
model.registerTexture(0, glTextureId);

// Vulkan: decode to RGBA8 bytes and register
byte[] rgba = decodeToRgba8("texture_00.png");
model.registerTextureVulkan(0, width, height, rgba);
```

The texture index corresponds to the texture order in the `.model3.json` file.

## 5. Renderer Creation

Once the model is loaded, you must initialize its internal renderer.  
Before this step, select backend globally using `CubismFramework.makeGL()` or `CubismFramework.makeVulkan(...)`.

```java
// Must be called on the render thread/context owner
model.createRenderer();
```
