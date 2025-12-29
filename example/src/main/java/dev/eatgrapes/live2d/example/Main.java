package dev.eatgrapes.live2d.example;

import com.sun.net.httpserver.HttpServer;
import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class Main {
    private long window;
    private CubismUserModel model;
    private final Map<String, byte[]> motions = new HashMap<>();
    private final float[] mvp = new float[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private float modelScale = 1.0f;

    public void run() throws Exception {
        init();
        setup();
        startServer();
        ControlPanel.show();
        loop();
        cleanup();
    }

    private void startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/motion", t -> {
            String query = t.getRequestURI().getQuery();
            String id = query.split("=")[1];
            taskQueue.add(() -> {
                if (motions.containsKey(id)) {
                    model.startMotion(motions.get(id), 3, false, null);
                }
            });
            t.sendResponseHeaders(200, 0);
            t.close();
        });
        server.createContext("/expression", t -> {
            String query = t.getRequestURI().getQuery();
            String name = query.split("=")[1];
            taskQueue.add(() -> model.setExpression(name));
            t.sendResponseHeaders(200, 0);
            t.close();
        });
        server.createContext("/parameter", t -> {
            String query = t.getRequestURI().getQuery();
            String[] parts = query.split("&");
            String id = "";
            float value = 0;
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv[0].equals("id")) id = kv[1];
                else if (kv[0].equals("value")) value = Float.parseFloat(kv[1]);
            }
            String finalId = id;
            float finalValue = value;
            taskQueue.add(() -> model.setParameterValue(finalId, finalValue));
            t.sendResponseHeaders(200, 0);
            t.close();
        });
        server.createContext("/scale", t -> {
            String query = t.getRequestURI().getQuery();
            String val = query.split("=")[1];
            float s = Float.parseFloat(val);
            taskQueue.add(() -> this.modelScale = s);
            t.sendResponseHeaders(200, 0);
            t.close();
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Control server started on port 8080");
    }

    private void init() {
        if (!glfwInit()) throw new RuntimeException("GLFW failed");
        window = glfwCreateWindow(800, 800, "Live2D Example", 0, 0);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            try (MemoryStack s = MemoryStack.stackPush()) {
                IntBuffer wb = s.mallocInt(1), hb = s.mallocInt(1);
                glfwGetWindowSize(win, wb, hb);
                float aspect = (float) wb.get(0) / hb.get(0);
                float nx = (float) (x / (wb.get(0) / 2.0) - 1.0) * aspect;
                float ny = (float) (1.0 - y / (hb.get(0) / 2.0));
                if (model != null) model.setDragging(nx, ny);
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
        motions.put("idle", load("/model/Hiyori/motions/Hiyori_m01.motion3.json"));
        motions.put("tap_body", load("/model/Hiyori/motions/Hiyori_m04.motion3.json"));
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            Runnable task;
            while ((task = taskQueue.poll()) != null) task.run();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            try (MemoryStack s = MemoryStack.stackPush()) {
                IntBuffer w = s.mallocInt(1), h = s.mallocInt(1);
                glfwGetWindowSize(window, w, h);
                glViewport(0, 0, w.get(0), h.get(0));
                float aspect = (float) w.get(0) / h.get(0);
                for (int i = 0; i < 16; i++) mvp[i] = 0;
                mvp[0] = modelScale / aspect; mvp[5] = modelScale; mvp[10] = 1.0f; mvp[15] = 1.0f;
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