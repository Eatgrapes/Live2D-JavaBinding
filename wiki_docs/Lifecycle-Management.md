# Lifecycle Management

Because this library relies on native C++ code, managing the lifecycle of the memory and the framework is crucial. If you don't do this correctly, your app might crash or leak memory.

## 1. Global Initialization

Before you do *anything* else (even before creating a model), you must initialize the Cubism Framework. This sets up the internal memory allocators and logging systems.

```java
import dev.eatgrapes.live2d.CubismFramework;

// Call this ONCE at the start of your application
CubismFramework.startUp();
CubismFramework.initialize();
```

> **Note**: This automatically extracts and loads the native `.dll`, `.so`, or `.dylib` from the jar file. You don't need to manually configure `java.library.path`.

## 2. Global Disposal

When your application is closing (e.g., in your window's close callback), you must tear down the framework to free native resources.

```java
// Call this ONCE when your app is exiting
CubismFramework.dispose();
```

## 3. Model Lifecycle

Individual models (`CubismUserModel`) also have native resources. Always close them when you are done with a specific character, even if the app is still running.

```java
CubismUserModel model = new CubismUserModel();

// ... use the model ...

// When switching characters or closing the level:
model.close(); // IMPORTANT! Frees the C++ model instance
```

**Pro Tip**: `CubismUserModel` implements `AutoCloseable`, so you can use it in try-with-resources blocks for short-lived tests, though usually, you'll keep it alive as a field in your renderer class.
