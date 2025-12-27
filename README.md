# Live2D-JavaBinding

A Native Java binding for the Live2D Cubism SDK.

> **Status: Work in Progress**  
> This project is currently under development. Basic model display, interaction, and remote control are implemented.

## Features

- **Distributed Separately**: Java logic and native binaries are decoupled. Native JARs are self-contained (statically linked with Cubism Core).
- **Cross-Platform**: Supports Windows (x64), Linux (x64, arm64), and macOS (x64, arm64).
- **Simple API**: Easy-to-use Apis to rendering.

## Quick Usage example

```java
import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;

// 1. Initialize the Framework
CubismFramework.startUp();
CubismFramework.initialize();

// 2. Load Model & Components
CubismUserModel model = new CubismUserModel();
model.loadModel(moc3Bytes);
model.loadPose(poseBytes);
model.loadPhysics(physicsBytes);

// 3. Setup Renderer
model.createRenderer();
model.registerTexture(0, openGLTextureId);

// 4. Interaction & Motion
model.setDragging(nx, ny); // Eye tracking
model.startMotion(motionBytes, priority, loop, name -> {
    System.out.println("Motion finished!");
});

// 5. Update & Draw (in your render loop)
model.update(deltaTime);
model.draw(mvpMatrix);
```

## Project Structure

- `binding/`: Java API and module definitions.
- `native/`: JNI implementation and CMake build scripts.
- `scripts/`: Python build automation for all platforms.
- `example/`: A complete Maven-based example using LWJGL 3, including a Swing control panel.

## Building

Requires Python 3, CMake, a C++14 compiler, and JDK 9+.

```bash
python3 scripts/build.py
```

The artifacts will be generated in the `out/` directory:
- `live2d-shared.jar` (Java API)
- `live2d-native-[platform].jar` (Self-contained Native Library)

## Contributing

Pull Requests (PRs) are welcome! Help us implement more SDK features or improve platform compatibility.

## License

- **Binding Code**: Distributed under the [MIT License](LICENSE).
- **Live2D Cubism SDK**: The use of the underlying SDK is governed by the [Live2D Open Software License Agreement](https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html). You must agree to their terms to use the native components.