# Interaction & Parameters

Interactive apps require the model to react to the user.

## Mouse Dragging (Look At)

You can make the character look at the mouse cursor.

```java
// Coordinates should be normalized:
// X: -1.0 (Left) to 1.0 (Right)
// Y: -1.0 (Bottom) to 1.0 (Top)
model.setDragging(mouseX, mouseY);
```

Call this every time the mouse moves. The model uses this to update standard parameters like `ParamAngleX`, `ParamAngleY`, `ParamEyeBallX`, etc.

## Hit Testing

Detect if the user clicked a specific part of the model (like the head or chest).

```java
// The ID usually comes from the .model3.json "HitAreas" section
String hitAreaId = "Head"; 

if (model.isHit(hitAreaId, mouseX, mouseY)) {
    System.out.println("Headpats received!");
    model.startExpression("Blush");
}
```

## Manual Parameter Control

Sometimes you want direct control (e.g., syncing mouth open with microphone volume).

```java
// 1. Set a value (0.0 to 1.0, or -30 to 30 depending on the parameter)
model.setParameterValue("ParamMouthOpenY", 1.0f);

// 2. Get the current value
float currentMouth = model.getParameterValue("ParamMouthOpenY");
```

> **Note**: If a motion is playing that *also* controls this parameter, the motion might overwrite your manual value in the next `update()` call. To fix this, set the parameter *after* calling `model.update()`.
