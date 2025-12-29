package dev.eatgrapes.live2d.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class Main {
    private long window;
    private CubismUserModel model;
    private final Map<String, List<byte[]>> motionGroups = new HashMap<>();
    private final List<Integer> loadedTextures = new ArrayList<>();
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
                String key = id.toLowerCase();
                if (key.equals("tap_body")) key = "tapbody";
                
                List<byte[]> group = null;
                for (String k : motionGroups.keySet()) {
                    if (k.equalsIgnoreCase(key)) {
                        group = motionGroups.get(k);
                        break;
                    }
                }
                
                if (group != null && !group.isEmpty()) {
                    int idx = (int) (Math.random() * group.size());
                    model.startMotion(group.get(idx), 3, false, null);
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
        server.createContext("/model", t -> {
            String query = t.getRequestURI().getQuery();
            String name = query.split("=")[1];
            taskQueue.add(() -> {
                try {
                    loadModel(name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
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
        
        CubismFramework.startUp(msg -> {
            if (!msg.contains("Live2D Cubism SDK Core Version")) {
                System.out.println(msg);
            }
        }, CubismFramework.LogLevel.WARNING);
        CubismFramework.initialize();
    }

    private void setup() throws Exception {
        loadModel("Hiyori");
    }
    
    private void loadModel(String name) throws Exception {
        if (model != null) {
            model.close();
            model = null;
        }
        for (int tex : loadedTextures) {
            glDeleteTextures(tex);
        }
        loadedTextures.clear();
        motionGroups.clear();
        
        String baseDir = "/model/" + name + "/";
        String model3Path = baseDir + name + ".model3.json";
        
        Gson gson = new Gson();
        JsonObject settings;
        try (InputStream is = getClass().getResourceAsStream(model3Path)) {
            if (is == null) throw new RuntimeException("Model config not found: " + model3Path);
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                settings = gson.fromJson(reader, JsonObject.class);
            }
        }
        
        model = new CubismUserModel();
        
        JsonObject refs = settings.getAsJsonObject("FileReferences");
        
        String mocFile = refs.get("Moc").getAsString();
        model.loadModel(load(baseDir + mocFile));
        
        if (refs.has("Pose")) {
            model.loadPose(load(baseDir + refs.get("Pose").getAsString()));
        }
        
        if (refs.has("Physics")) {
            model.loadPhysics(load(baseDir + refs.get("Physics").getAsString()));
        }
        
        model.createRenderer();
        
        JsonArray textures = refs.getAsJsonArray("Textures");
        for (int i = 0; i < textures.size(); i++) {
            String texFile = textures.get(i).getAsString();
            model.registerTexture(i, loadTex(baseDir + texFile));
        }
        
        if (refs.has("Motions")) {
            JsonObject motionsObj = refs.getAsJsonObject("Motions");
            for (String groupName : motionsObj.keySet()) {
                JsonArray groupArr = motionsObj.getAsJsonArray(groupName);
                List<byte[]> loadedGroup = new ArrayList<>();
                for (JsonElement elem : groupArr) {
                    JsonObject m = elem.getAsJsonObject();
                    String mFile = m.get("File").getAsString();
                    try {
                        loadedGroup.add(load(baseDir + mFile));
                    } catch (Exception e) {
                    }
                }
                motionGroups.put(groupName, loadedGroup);
            }
        }
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
            if (model != null) {
                model.update(0.016f);
                model.draw(mvp);
            }
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        if (model != null) model.close();
        CubismFramework.dispose();
        glfwTerminate();
    }

    private byte[] load(String p) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(p)) {
            if (is == null) throw new RuntimeException("Resource not found: " + p);
            return is.readAllBytes(); 
        }
    }

    private int loadTex(String p) throws Exception {
        Path tmp = Files.createTempFile("l2d", ".png");
        try (InputStream is = getClass().getResourceAsStream(p)) {
            if (is == null) throw new RuntimeException("Texture not found: " + p);
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING); 
        }
        int w, h, tex;
        ByteBuffer img;
        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer wb = s.mallocInt(1), hb = s.mallocInt(1), cb = s.mallocInt(1);
            img = STBImage.stbi_load(tmp.toString(), wb, hb, cb, 4);
            if (img == null) throw new RuntimeException("Failed to load texture image: " + p);
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
        loadedTextures.add(tex);
        return tex;
    }

    public static void main(String[] args) throws Exception { new Main().run(); }
}