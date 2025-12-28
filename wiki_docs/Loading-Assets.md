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

Textures are not loaded directly by the Live2D engine. Instead, **you** must load the image into OpenGL (using `STBImage` or similar) and give the Texture ID to the model.

```java
// 1. Load image to OpenGL and get an ID (e.g., using LWJGL)
int glTextureId = loadTexture("texture_00.png"); 

// 2. Register it with the model. 
// The index corresponds to the texture order in the .model3.json file.
model.registerTexture(0, glTextureId);
```

## 5. Renderer Creation

Once the model is loaded, you must initialize its internal renderer. This requires an active OpenGL context.

```java
// Must be called on the render thread!
model.createRenderer();
```
