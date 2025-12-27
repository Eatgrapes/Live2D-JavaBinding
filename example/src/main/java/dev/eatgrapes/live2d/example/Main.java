package dev.eatgrapes.live2d.example;

import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private long window;
    private CubismUserModel model;
    private final float[] mvpMatrix = new float[]{
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    };

    public void run() throws Exception {
        initWindow();
        initLive2D();
        loop();
        cleanup();
    }

    private void initWindow() {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");
        window = glfwCreateWindow(800, 800, "Live2D Example", 0, 0);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);
    }

    private void initLive2D() throws Exception {
        CubismFramework.startUp();
        CubismFramework.initialize();

        model = new CubismUserModel();
        model.loadModel(loadResource("/model/Hiyori/Hiyori.moc3"));
        model.loadPose(loadResource("/model/Hiyori/Hiyori.pose3.json"));
        model.loadPhysics(loadResource("/model/Hiyori/Hiyori.physics3.json"));
        model.createRenderer();

        model.registerTexture(0, loadTexture("/model/Hiyori/Hiyori.2048/texture_00.png"));
        model.registerTexture(1, loadTexture("/model/Hiyori/Hiyori.2048/texture_01.png"));
    }

    private void loop() {
        long startTime = System.currentTimeMillis();
        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            float time = (System.currentTimeMillis() - startTime) / 1000.0f;
            model.setParameterValue("ParamAngleX", (float) Math.sin(time) * 30.0f);

            model.update(0.016f);
            model.draw(mvpMatrix);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        model.close();
        CubismFramework.dispose();
        glfwTerminate();
    }

    private byte[] loadResource(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return is.readAllBytes();
        }
    }

    private int loadTexture(String path) throws Exception {
        Path temp = Files.createTempFile("l2d", ".png");
        try (InputStream is = getClass().getResourceAsStream(path)) {
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        int w, h, tex;
        ByteBuffer img;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer wb = stack.mallocInt(1), hb = stack.mallocInt(1), cb = stack.mallocInt(1);
            img = STBImage.stbi_load(temp.toString(), wb, hb, cb, 4);
            w = wb.get(); h = hb.get();
        }
        tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img);
        STBImage.stbi_image_free(img);
        Files.delete(temp);
        return tex;
    }

    public static void main(String[] args) throws Exception {
        new Main().run();
    }
}