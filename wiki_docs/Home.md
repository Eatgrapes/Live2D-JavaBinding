# Welcome to the Live2D-JavaBinding Wiki

This project provides a high-performance, native Java binding for the **Live2D Cubism SDK**. It allows you to load, render, and interact with Live2D models directly in your Java applications using OpenGL.

## Philosophy

The library is designed to be **unopinionated** about your windowing system. Whether you use **LWJGL**, **JOGL**, or even **JavaFX** (with an OpenGL bridge), this binding provides the raw tools to render the model. We handle the JNI complexity; you handle the context.

## Navigation

*   **[Getting Started](Getting-Started)**: Installation and your first model.
*   **[Lifecycle Management](Lifecycle-Management)**: Starting up and shutting down the native core safely.
*   **[Loading Assets](Loading-Assets)**: How to load `.moc3` files, textures, physics, and poses.
*   **[Rendering Loop](Rendering-Loop)**: The essential `update` and `draw` cycle.
*   **[Motion & Expressions](Motion-and-Expressions)**: Bringing the model to life with animations.
*   **[Interaction & Parameters](Interaction-and-Parameters)**: Hit testing, eye tracking, and manual parameter control.

## Status

This project is currently **Work in Progress**. APIs may change slightly as we reach feature parity with the official C++ SDK.
