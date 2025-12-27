package dev.eatgrapes.live2d.example;

import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private long window;
    private CubismUserModel model;
    private final float[] mvp = new float[]{
        1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1
    };

    public void run() throws Exception {
        init();
        setup();
        loop();
        cleanup();
    }

    private void init() {
        if (!glfwInit()) throw new RuntimeException("GLFW failed");
        window = glfwCreateWindow(800, 800, "Live2D Interaction", 0, 0);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            float nx = (float) (x / 400.0 - 1.0);
            float ny = (float) (1.0 - y / 400.0);
            if (model != null) model.setDragging(nx, ny);
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    DoubleBuffer x = stack.mallocDouble(1), y = stack.mallocDouble(1);
                    glfwGetCursorPos(window, x, y);
                    float nx = (float) (x.get(0) / 400.0 - 1.0);
                    float ny = (float) (1.0 - y.get(0) / 400.0);
                    
                    if (model.isHit("HitArea", nx, ny)) {
                        System.out.println("Hit!");
                        byte[] motion = load("/model/Hiyori/motions/Hiyori_m01.motion3.json");
                        model.startMotion(motion, 2, name -> System.out.println("Motion finished: " + name));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void setup() throws Exception {
        CubismFramework.startUp();
        CubismFramework.initialize();

        model = new CubismUserModel();
        model.loadModel(load("/model/Hiyori/Hiyori.moc3"));
        model.loadPose(load("/model/Hiyori/Hiyori.pose3.json"));
        model.loadPhysics(load("/model/Hiyori/Hiyori.physics3.json"));
        model.createRenderer();

        model.registerTexture(0, loadTex("/model/Hiyori/Hiyori.2048/texture_00.png"));
        model.registerTexture(1, loadTex("/model/Hiyori/Hiyori.2048/texture_01.png"));
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            model.update(0.016f);
            model.draw(mvp);
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        model.close();
        CubismFramework.dispose();
        glfwTerminate();
    }

    private byte[] load(String p) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(p)) { return is.readAllBytes(); }
    }

    private int loadTex(String p) throws Exception {
        Path tmp = Files.createTempFile("l2d", ".png");
        try (InputStream is = getClass().getResourceAsStream(p)) { Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING); }
        int w, h, tex;
        ByteBuffer img;
        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer wb = s.mallocInt(1), hb = s.mallocInt(1), cb = s.mallocInt(1);
            img = STBImage.stbi_load(tmp.toString(), wb, hb, cb, 4);
            w = wb.get(); h = hb.get();
        }
        tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img);
        STBImage.stbi_image_free(img);
        Files.delete(tmp);
        return tex;
    }

    public static void main(String[] args) throws Exception { new Main().run(); }
}
