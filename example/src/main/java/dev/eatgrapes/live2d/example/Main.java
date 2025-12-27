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
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

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

        // Required for Live2D transparency and masking
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer wb = stack.mallocInt(1), hb = stack.mallocInt(1);
                glfwGetWindowSize(window, wb, hb);
                float nx = (float) (x / (wb.get(0) / 2.0) - 1.0);
                float ny = (float) (1.0 - y / (hb.get(0) / 2.0));
                if (model != null) model.setDragging(nx, ny);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    DoubleBuffer xb = stack.mallocDouble(1), yb = stack.mallocDouble(1);
                    IntBuffer wb = stack.mallocInt(1), hb = stack.mallocInt(1);
                    glfwGetCursorPos(window, xb, yb);
                    glfwGetWindowSize(window, wb, hb);
                    
                    float nx = (float) (xb.get(0) / (wb.get(0) / 2.0) - 1.0);
                    float ny = (float) (1.0 - yb.get(0) / (hb.get(0) / 2.0));
                    
                    if (model.isHit("HitArea", nx, ny)) {
                        System.out.println("Hit Body!");
                        model.startMotion(load("/model/Hiyori/motions/Hiyori_m04.motion3.json"), 2, null);
                    } else if (model.isHit("ArtMesh12", nx, ny) || model.isHit("ArtMesh01", nx, ny)) {
                        System.out.println("Hit Head/Face!");
                        model.startMotion(load("/model/Hiyori/motions/Hiyori_m01.motion3.json"), 2, null);
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

        System.out.println("Drawable IDs:");
        for (String id : model.getDrawableIds()) {
            System.out.println("  " + id);
        }

        model.registerTexture(0, loadTex("/model/Hiyori/Hiyori.2048/texture_00.png"));
        model.registerTexture(1, loadTex("/model/Hiyori/Hiyori.2048/texture_01.png"));
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
                glfwGetWindowSize(window, w, h);
                glViewport(0, 0, w.get(0), h.get(0));
            }

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
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img);
        STBImage.stbi_image_free(img);
        Files.delete(tmp);
        return tex;
    }

    public static void main(String[] args) throws Exception { new Main().run(); }
}
