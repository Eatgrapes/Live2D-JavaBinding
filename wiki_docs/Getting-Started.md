# Getting Started

This guide will help you integrate Live2D-JavaBinding into your project.

## Prerequisites

1.  **Java 9 or higher**: The library uses JPMS (Java Platform Module System) and APIs introduced in Java 9.
2.  **OpenGL Context**: You need a library to create a window and an OpenGL context. We recommend **LWJGL 3**, but any library that provides access to OpenGL bindings will work.

## Installation

Since this library is not hosted on a public Maven repository, you must download the latest release and install it manually.

### 1. Download Release
Go to the [GitHub Releases](../../releases) page and download:
*   `live2d-shared.jar`
*   `live2d-native-[platform].jar` (e.g., `live2d-native-windows-x64.jar`)

### 2. Manual Installation
You can install them into your local Maven repository:

```bash
mvn install:install-file -Dfile=live2d-shared.jar -DgroupId=com.example -DartifactId=live2d-shared -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=live2d-native-windows-x64.jar -DgroupId=com.example -DartifactId=live2d-native -Dversion=1.0.0 -Dpackaging=jar -Dclassifier=windows-x64
```

### 3. Build Configuration

#### Maven
```xml
<dependencies>
    <!-- The Core Java API -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>live2d-shared</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Native Implementation -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>live2d-native</artifactId>
        <version>1.0.0</version>
        <classifier>windows-x64</classifier> <!-- Change classifier as needed -->
    </dependency>
</dependencies>
```

#### Gradle
For Gradle, the simplest way is to put the JARs directly into your project:

1.  Create a `libs` folder in your project root.
2.  Copy the downloaded JARs into it.
3.  Add dependencies in `build.gradle`:

```groovy
dependencies {
    implementation files('libs/live2d-shared.jar')
    implementation files('libs/live2d-native-windows-x64.jar') // Change for your platform
}
```

## Your First Steps

The general flow of a Live2D application is:

1.  **Initialize**: Call `CubismFramework.startUp()`.
2.  **Load**: Load your model bytes and textures.
3.  **Loop**: In your render loop, call `model.update()` and `model.draw()`.
4.  **Dispose**: Clean up when done.

Check out the specific sections in the sidebar to learn how to implement each step!
