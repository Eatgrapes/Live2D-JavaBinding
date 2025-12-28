# Motion & Expressions

Static models are boring! Here is how to make them move.

## Playing Motions

Motions are `.motion3.json` files. You load them as byte arrays and play them.

```java
byte[] motionBytes = loadResource("tap_body.motion3.json");

// Priority: 
// 1 = Idle (can be interrupted)
// 2 = Normal
// 3 = Force (interrupts everything)
int priority = 3;

// Loop: true/false
boolean loop = false;

model.startMotion(motionBytes, priority, loop, name -> {
    System.out.println("Motion " + name + " finished!");
});
```

The model automatically fades between the current motion and the new one. You don't need to manage blending manually.

## Setting Expressions

Expressions are `.exp3.json` files that override specific parameters (like setting eyes to "happy").

1.  **Load the Expression**:
    ```java
    byte[] exprBytes = loadResource("f01.exp3.json");
    model.loadExpression(exprBytes, "Happy"); // Give it a unique name
    ```

2.  **Activate it**:
    ```java
    model.setExpression("Happy");
    ```

Expressions layer on top of motions. So if a motion blinks the eyes, but the expression forces them shut, they will stay shut.
