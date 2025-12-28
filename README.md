# Live2D-JavaBinding

A native Java binding for the [Live2D Cubism SDK](https://www.live2d.com/en/sdk/about/), enabling seamless integration of Live2D models in Java applications (Suitable for OpenGL).

For detailed documentation, please visit our [Wiki](https://github.com/Eatgrapes/Live2D-JavaBinding/wiki).

> **Status: Work in Progress**  
> This project is currently under active development. Contributions are welcome!

## Features

- **Decoupled Distribution**: Java logic and native binaries are separated for flexibility. Native JARs are fully self-contained.
- **Cross-Platform Support**: Compatible with Windows (x64), Linux (x64, arm64), and macOS (x64, arm64).
- **OpenGL Based**: Designed for OpenGL-accelerated applications (e.g., LWJGL, JOGL).
- **Simple and Intuitive API**: Easy-to-use interfaces for model loading, rendering, and interaction.
- **JNI Integration**: Leverages Java Native Interface (JNI) for high-performance native code execution.

## Implemented Features

- [x] **Model Loading**: Support for `.moc3`, physics, and pose loading.
- [x] **Expression System**: Load and switch model expressions.
- [x] **Motion System**: Playback with priority control and callbacks.
- [x] **Parameter Control**: Manually set or get model parameter values.
- [x] **Interaction**: Dragging support and hit detection.
- [x] **Rendering**: OpenGL-based rendering and texture management.

## About JNI Integration

JNI (Java Native Interface) is a programming framework that enables Java code to interact with native applications and libraries written in languages like C or C++. In this project:

- JNI bridges the Java API (in the `binding/` directory) with the native C++ implementation of the Live2D Cubism SDK (in the `native/` directory).
- The native components are built using CMake and compiled into platform-specific shared libraries, which are bundled into JAR files for easy distribution.

For more details on JNI, refer to the [official Oracle JNI documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/jniTOC.html).

## Quick Start

Here's a simple example to get you up and running:

```java
import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;

// 1. Initialize the Framework
CubismFramework.startUp();
CubismFramework.initialize();

// 2. Load Model & Components
CubismUserModel model = new CubismUserModel();
model.loadModel(moc3Bytes);     // Load model data
model.loadPose(poseBytes);      // Load pose data
model.loadPhysics(physicsBytes); // Load physics data

// 3. Setup Renderer
model.createRenderer();
model.registerTexture(0, openGLTextureId); // Bind textures

// 4. Handle Interactions & Motions
model.setDragging(nx, ny); // Enable eye tracking or dragging
model.startMotion(motionBytes, priority, loop, name -> {
    System.out.println("Motion finished!"); // Callback on completion
});

// 5. Update & Draw (in your main render loop)
model.update(deltaTime); // Update model state
model.draw(mvpMatrix);   // Render the model
```

## Project Structure

- **`binding/`**: Contains the Java API definitions and module configurations.
- **`native/`**: Houses the JNI C++ implementation, along with CMake build scripts for native compilation.
- **`scripts/`**: Python scripts for automating builds across all supported platforms.
- **`example/`**: A complete Maven-based demo project using LWJGL 3 for OpenGL integration.

## Building the Project

### Prerequisites
- Python 3.x
- CMake 3.10+
- A C++14-compatible compiler (e.g., GCC, Clang, or MSVC)
- JDK 9 or higher

### Build Command
Run the following in the project root:

```bash
python3 scripts/build.py
```

This will generate artifacts in the `out/` directory:
- `live2d-shared.jar`: The core Java API.
- `live2d-native-[platform].jar`: Platform-specific native libraries (e.g., `live2d-native-windows-x64.jar`).

## Contributing

We welcome Pull Requests (PRs)! Whether it's implementing additional SDK features, enhancing platform support, or fixing bugs, your help is appreciated. Please follow these steps:
1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a PR with a clear description.

## License

- **Binding Code**: Licensed under the [MIT License](LICENSE).
- **Live2D Cubism SDK**: Usage is subject to the [Live2D Open Software License Agreement](https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html). You must review and agree to their terms before using the native components.
