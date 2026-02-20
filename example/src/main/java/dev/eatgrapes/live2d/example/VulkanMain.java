package dev.eatgrapes.live2d.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.eatgrapes.live2d.CubismFramework;
import dev.eatgrapes.live2d.CubismUserModel;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_MEMORY_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D24_UNORM_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_TRUE;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFormatProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_RENDERING_INFO;
import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;
import static org.lwjgl.vulkan.VK13.vkCmdEndRendering;

public class VulkanMain {
    private long window;
    private VulkanContext vk;
    private CubismUserModel model;
    private final Map<String, List<byte[]>> motionGroups = new HashMap<>();
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final float[] mvp = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    private float modelScale = 1.0f;

    public void run() throws Exception {
        ensureSupportedPlatform();
        initWindow();
        vk = new VulkanContext(window);
        vk.initialize();
        initCubism();
        loadModel("Hiyori");
        loop();
        cleanup();
    }

    private void ensureSupportedPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean supportedOs = os.contains("linux") || os.contains("win");
        boolean supportedArch = arch.contains("amd64") || arch.contains("x86_64");
        if (!supportedOs || !supportedArch) {
            throw new UnsupportedOperationException("Vulkan example supports linux-x64/windows-x64 only.");
        }
    }

    private void initWindow() {
        if (!glfwInit()) throw new RuntimeException("GLFW init failed");
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        window = glfwCreateWindow(800, 800, "Live2D Vulkan Example", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create window");

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (model == null) return;
            try (MemoryStack stack = stackPush()) {
                IntBuffer wb = stack.mallocInt(1);
                IntBuffer hb = stack.mallocInt(1);
                glfwGetWindowSize(win, wb, hb);
                if (hb.get(0) == 0) return;
                float aspect = (float) wb.get(0) / hb.get(0);
                float nx = (float) (x / (wb.get(0) / 2.0) - 1.0) * aspect;
                float ny = (float) (1.0 - y / (hb.get(0) / 2.0));
                model.setDragging(nx, ny);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE) {
                tasks.add(() -> startRandomMotion("tapbody"));
            }
        });
    }

    private void initCubism() {
        CubismFramework.startUp(msg -> {
            if (!msg.contains("Live2D Cubism SDK Core Version")) {
                System.out.println(msg);
            }
        }, CubismFramework.LogLevel.WARNING);

        CubismFramework.makeVulkan(
                vk.device.address(),
                vk.physicalDevice.address(),
                vk.commandPool,
                vk.graphicsQueue.address(),
                vk.swapchainImages.length,
                vk.swapchainWidth,
                vk.swapchainHeight,
                vk.swapchainImageViews[0],
                vk.swapchainImageFormat,
                vk.depthFormat
        );
        CubismFramework.initialize();
    }

    private void loadModel(String name) throws Exception {
        if (model != null) {
            model.close();
            model = null;
        }
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
        model.loadModel(load(baseDir + refs.get("Moc").getAsString()));
        if (refs.has("Pose")) model.loadPose(load(baseDir + refs.get("Pose").getAsString()));
        if (refs.has("Physics")) model.loadPhysics(load(baseDir + refs.get("Physics").getAsString()));
        model.createRenderer();

        JsonArray textures = refs.getAsJsonArray("Textures");
        for (int i = 0; i < textures.size(); i++) {
            TextureData data = loadTextureRgba(baseDir + textures.get(i).getAsString());
            model.registerTextureVulkan(i, data.width, data.height, data.rgba);
        }

        if (refs.has("Motions")) {
            JsonObject motionsObj = refs.getAsJsonObject("Motions");
            for (String groupName : motionsObj.keySet()) {
                JsonArray groupArr = motionsObj.getAsJsonArray(groupName);
                List<byte[]> loadedGroup = new ArrayList<>();
                for (JsonElement elem : groupArr) {
                    JsonObject m = elem.getAsJsonObject();
                    loadedGroup.add(load(baseDir + m.get("File").getAsString()));
                }
                motionGroups.put(groupName, loadedGroup);
            }
        }
    }

    private void startRandomMotion(String groupName) {
        if (model == null) return;
        List<byte[]> group = null;
        for (Map.Entry<String, List<byte[]>> entry : motionGroups.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(groupName)) {
                group = entry.getValue();
                break;
            }
        }
        if (group == null || group.isEmpty()) return;
        int idx = (int) (Math.random() * group.size());
        model.startMotion(group.get(idx), 3, false, null);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            Runnable task;
            while ((task = tasks.poll()) != null) task.run();

            int imageIndex = vk.acquireNextImage();
            if (imageIndex < 0) continue;

            vk.preModelDraw(imageIndex);
            float aspect = (float) vk.swapchainWidth / Math.max(vk.swapchainHeight, 1);
            for (int i = 0; i < 16; i++) mvp[i] = 0;
            mvp[0] = modelScale / aspect;
            mvp[5] = modelScale;
            mvp[10] = 1.0f;
            mvp[15] = 1.0f;

            if (model != null) {
                model.setVulkanRenderTarget(
                        vk.swapchainImages[imageIndex],
                        vk.swapchainImageViews[imageIndex],
                        vk.swapchainImageFormat,
                        vk.swapchainWidth,
                        vk.swapchainHeight
                );
                model.update(0.016f);
                model.draw(mvp);
            }

            vk.postModelDraw(imageIndex);
            vk.present(imageIndex);
        }
    }

    private void cleanup() {
        if (model != null) {
            model.close();
            model = null;
        }
        CubismFramework.dispose();
        if (vk != null) {
            vk.destroy();
            vk = null;
        }
        if (window != NULL) {
            glfwDestroyWindow(window);
            window = NULL;
        }
        glfwTerminate();
    }

    private byte[] load(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            return is.readAllBytes();
        }
    }

    private TextureData loadTextureRgba(String path) throws Exception {
        byte[] bytes = load(path);
        ByteBuffer encoded = BufferUtils.createByteBuffer(bytes.length);
        encoded.put(bytes).flip();
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            ByteBuffer img = STBImage.stbi_load_from_memory(encoded, w, h, c, 4);
            if (img == null) throw new RuntimeException("Failed to decode texture: " + path);
            byte[] rgba = new byte[w.get(0) * h.get(0) * 4];
            img.get(rgba);
            STBImage.stbi_image_free(img);
            return new TextureData(w.get(0), h.get(0), rgba);
        }
    }

    public static void main(String[] args) throws Exception {
        new VulkanMain().run();
    }

    private static final class TextureData {
        final int width;
        final int height;
        final byte[] rgba;

        TextureData(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }
    }

    private static final class QueueFamilyIndices {
        int graphics = -1;
        int present = -1;

        boolean complete() {
            return graphics >= 0 && present >= 0;
        }
    }

    private static final class ExtensionSupport {
        boolean hasSwapchain;
        boolean hasDynamicRendering;
    }

    private static final class VulkanContext {
        private final long window;

        private VkInstance instance;
        private long surface;
        private VkPhysicalDevice physicalDevice;
        private VkDevice device;
        private VkQueue graphicsQueue;
        private VkQueue presentQueue;
        private int graphicsFamily;
        private int presentFamily;
        private boolean hasDynamicRenderingExt;

        private long commandPool;
        private long swapchain;
        private long[] swapchainImages;
        private long[] swapchainImageViews;
        private int swapchainImageFormat;
        private int swapchainWidth;
        private int swapchainHeight;
        private int depthFormat;
        private long acquireFence;

        VulkanContext(long window) {
            this.window = window;
        }

        void initialize() {
            createInstance();
            createSurface();
            pickPhysicalDevice();
            createDevice();
            createCommandPool();
            createSwapchain();
            createImageViews();
            createSyncObjects();
            depthFormat = findDepthFormat();
        }

        void destroy() {
            if (device != null) vkDeviceWaitIdle(device);
            if (acquireFence != VK_NULL_HANDLE && device != null) vkDestroyFence(device, acquireFence, null);
            if (swapchainImageViews != null && device != null) {
                for (long view : swapchainImageViews) vkDestroyImageView(device, view, null);
            }
            if (swapchain != VK_NULL_HANDLE && device != null) vkDestroySwapchainKHR(device, swapchain, null);
            if (commandPool != VK_NULL_HANDLE && device != null) org.lwjgl.vulkan.VK10.vkDestroyCommandPool(device, commandPool, null);
            if (device != null) vkDestroyDevice(device, null);
            if (surface != VK_NULL_HANDLE && instance != null) vkDestroySurfaceKHR(instance, surface, null);
            if (instance != null) vkDestroyInstance(instance, null);
        }

        int acquireNextImage() {
            try (MemoryStack stack = stackPush()) {
                vkResetFences(device, stack.longs(acquireFence));
                IntBuffer imageIndex = stack.mallocInt(1);
                int result = vkAcquireNextImageKHR(device, swapchain, Long.MAX_VALUE, VK_NULL_HANDLE, acquireFence, imageIndex);
                if (result == VK_ERROR_OUT_OF_DATE_KHR) return -1;
                if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                    throw new RuntimeException("vkAcquireNextImageKHR failed: " + result);
                }
                vkWaitForFences(device, stack.longs(acquireFence), true, Long.MAX_VALUE);
                return imageIndex.get(0);
            }
        }

        void preModelDraw(int imageIndex) {
            VkCommandBuffer commandBuffer = beginSingleTimeCommands();
            try (MemoryStack stack = stackPush()) {
                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
                barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                barrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.image(swapchainImages[imageIndex]);
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                barrier.subresourceRange().baseMipLevel(0);
                barrier.subresourceRange().levelCount(1);
                barrier.subresourceRange().baseArrayLayer(0);
                barrier.subresourceRange().layerCount(1);

                org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        0,
                        null,
                        null,
                        barrier
                );

                VkRenderingAttachmentInfo.Buffer colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack);
                colorAttachment.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
                colorAttachment.imageView(swapchainImageViews[imageIndex]);
                colorAttachment.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                colorAttachment.loadOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR);
                colorAttachment.storeOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE);
                colorAttachment.clearValue().color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1.0f);

                VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack);
                renderingInfo.sType(VK_STRUCTURE_TYPE_RENDERING_INFO);
                renderingInfo.renderArea().offset().set(0, 0);
                renderingInfo.renderArea().extent().set(swapchainWidth, swapchainHeight);
                renderingInfo.layerCount(1);
                renderingInfo.pColorAttachments(colorAttachment);

                vkCmdBeginRendering(commandBuffer, renderingInfo);
                vkCmdEndRendering(commandBuffer);
            }
            endSingleTimeCommands(commandBuffer);
        }

        void postModelDraw(int imageIndex) {
            VkCommandBuffer commandBuffer = beginSingleTimeCommands();
            try (MemoryStack stack = stackPush()) {
                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
                barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                barrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                barrier.oldLayout(VK_IMAGE_LAYOUT_GENERAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.image(swapchainImages[imageIndex]);
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                barrier.subresourceRange().baseMipLevel(0);
                barrier.subresourceRange().levelCount(1);
                barrier.subresourceRange().baseArrayLayer(0);
                barrier.subresourceRange().layerCount(1);

                org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                        0,
                        null,
                        null,
                        barrier
                );
            }
            endSingleTimeCommands(commandBuffer);
        }

        void present(int imageIndex) {
            try (MemoryStack stack = stackPush()) {
                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
                presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
                presentInfo.pSwapchains(stack.longs(swapchain));
                presentInfo.pImageIndices(stack.ints(imageIndex));

                int result = vkQueuePresentKHR(presentQueue, presentInfo);
                if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR && result != VK_ERROR_OUT_OF_DATE_KHR) {
                    throw new RuntimeException("vkQueuePresentKHR failed: " + result);
                }
                vkQueueWaitIdle(presentQueue);
            }
        }

        private void createInstance() {
            if (!GLFWVulkan.glfwVulkanSupported()) throw new RuntimeException("GLFW Vulkan not supported");
            try (MemoryStack stack = stackPush()) {
                VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
                appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
                appInfo.pApplicationName(stack.UTF8("Live2D Vulkan Example"));
                appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
                appInfo.pEngineName(stack.UTF8("No Engine"));
                appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
                appInfo.apiVersion(VK_API_VERSION_1_3);

                PointerBuffer required = GLFWVulkan.glfwGetRequiredInstanceExtensions();
                if (required == null) throw new RuntimeException("Failed to query GLFW Vulkan extensions");

                VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
                createInfo.pApplicationInfo(appInfo);
                createInfo.ppEnabledExtensionNames(required);

                PointerBuffer pInstance = stack.mallocPointer(1);
                int result = vkCreateInstance(createInfo, null, pInstance);
                if (result != VK_SUCCESS) throw new RuntimeException("vkCreateInstance failed: " + result);
                instance = new VkInstance(pInstance.get(0), createInfo);
            }
        }

        private void createSurface() {
            try (MemoryStack stack = stackPush()) {
                LongBuffer pSurface = stack.mallocLong(1);
                int result = GLFWVulkan.glfwCreateWindowSurface(instance, window, null, pSurface);
                if (result != VK_SUCCESS) throw new RuntimeException("glfwCreateWindowSurface failed: " + result);
                surface = pSurface.get(0);
            }
        }

        private void pickPhysicalDevice() {
            try (MemoryStack stack = stackPush()) {
                IntBuffer count = stack.ints(0);
                vkEnumeratePhysicalDevices(instance, count, null);
                if (count.get(0) == 0) throw new RuntimeException("No Vulkan physical device found");
                PointerBuffer devices = stack.mallocPointer(count.get(0));
                vkEnumeratePhysicalDevices(instance, count, devices);

                for (int i = 0; i < devices.capacity(); i++) {
                    VkPhysicalDevice candidate = new VkPhysicalDevice(devices.get(i), instance);
                    ExtensionSupport ext = queryExtensions(candidate);
                    QueueFamilyIndices indices = findQueueFamilies(candidate);
                    if (!ext.hasSwapchain || !indices.complete()) continue;

                    IntBuffer formatCount = stack.ints(0);
                    vkGetPhysicalDeviceSurfaceFormatsKHR(candidate, surface, formatCount, null);
                    IntBuffer presentModeCount = stack.ints(0);
                    vkGetPhysicalDeviceSurfacePresentModesKHR(candidate, surface, presentModeCount, null);
                    if (formatCount.get(0) <= 0 || presentModeCount.get(0) <= 0) continue;

                    physicalDevice = candidate;
                    graphicsFamily = indices.graphics;
                    presentFamily = indices.present;
                    hasDynamicRenderingExt = ext.hasDynamicRendering;
                    return;
                }
            }
            throw new RuntimeException("No suitable Vulkan device");
        }

        private void createDevice() {
            try (MemoryStack stack = stackPush()) {
                int queueInfoCount = graphicsFamily == presentFamily ? 1 : 2;
                VkDeviceQueueCreateInfo.Buffer queueInfos = VkDeviceQueueCreateInfo.calloc(queueInfoCount, stack);
                queueInfos.get(0).sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueInfos.get(0).queueFamilyIndex(graphicsFamily);
                queueInfos.get(0).pQueuePriorities(stack.floats(1.0f));
                if (queueInfoCount == 2) {
                    queueInfos.get(1).sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                    queueInfos.get(1).queueFamilyIndex(presentFamily);
                    queueInfos.get(1).pQueuePriorities(stack.floats(1.0f));
                }

                PointerBuffer extensions = stack.mallocPointer(hasDynamicRenderingExt ? 2 : 1);
                extensions.put(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
                if (hasDynamicRenderingExt) extensions.put(stack.UTF8(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME));
                extensions.flip();

                VkPhysicalDeviceDynamicRenderingFeatures dynamicFeatures = VkPhysicalDeviceDynamicRenderingFeatures.calloc(stack);
                dynamicFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES);
                dynamicFeatures.dynamicRendering(true);

                VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
                createInfo.pQueueCreateInfos(queueInfos);
                createInfo.ppEnabledExtensionNames(extensions);
                createInfo.pNext(dynamicFeatures.address());

                PointerBuffer pDevice = stack.mallocPointer(1);
                int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
                if (result != VK_SUCCESS) throw new RuntimeException("vkCreateDevice failed: " + result);
                device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

                PointerBuffer pQueue = stack.mallocPointer(1);
                vkGetDeviceQueue(device, graphicsFamily, 0, pQueue);
                graphicsQueue = new VkQueue(pQueue.get(0), device);
                vkGetDeviceQueue(device, presentFamily, 0, pQueue);
                presentQueue = new VkQueue(pQueue.get(0), device);
            }
        }

        private void createCommandPool() {
            try (MemoryStack stack = stackPush()) {
                org.lwjgl.vulkan.VkCommandPoolCreateInfo info = org.lwjgl.vulkan.VkCommandPoolCreateInfo.calloc(stack);
                info.sType(org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
                info.queueFamilyIndex(graphicsFamily);
                info.flags(org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
                LongBuffer pPool = stack.mallocLong(1);
                int result = org.lwjgl.vulkan.VK10.vkCreateCommandPool(device, info, null, pPool);
                if (result != VK_SUCCESS) throw new RuntimeException("vkCreateCommandPool failed: " + result);
                commandPool = pPool.get(0);
            }
        }

        private void createSwapchain() {
            try (MemoryStack stack = stackPush()) {
                VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, capabilities);

                IntBuffer formatCount = stack.ints(0);
                vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, null);
                VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(formatCount.get(0), stack);
                vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, formats);

                IntBuffer presentModeCount = stack.ints(0);
                vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentModeCount, null);
                IntBuffer presentModes = stack.mallocInt(presentModeCount.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentModeCount, presentModes);

                int[] formatPair = chooseSurfaceFormat(formats);
                int presentMode = choosePresentMode(presentModes);
                int[] extent = chooseExtent(capabilities);

                int imageCount = capabilities.minImageCount() + 1;
                if (capabilities.maxImageCount() > 0 && imageCount > capabilities.maxImageCount()) {
                    imageCount = capabilities.maxImageCount();
                }

                VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
                createInfo.surface(surface);
                createInfo.minImageCount(imageCount);
                createInfo.imageFormat(formatPair[0]);
                createInfo.imageColorSpace(formatPair[1]);
                createInfo.imageExtent().set(extent[0], extent[1]);
                createInfo.imageArrayLayers(1);
                createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
                if (graphicsFamily != presentFamily) {
                    createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                    createInfo.pQueueFamilyIndices(stack.ints(graphicsFamily, presentFamily));
                } else {
                    createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
                }
                createInfo.preTransform(capabilities.currentTransform());
                createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
                createInfo.presentMode(presentMode);
                createInfo.clipped(true);
                createInfo.oldSwapchain(VK_NULL_HANDLE);

                LongBuffer pSwapchain = stack.mallocLong(1);
                int result = vkCreateSwapchainKHR(device, createInfo, null, pSwapchain);
                if (result != VK_SUCCESS) throw new RuntimeException("vkCreateSwapchainKHR failed: " + result);
                swapchain = pSwapchain.get(0);

                IntBuffer pCount = stack.ints(0);
                vkGetSwapchainImagesKHR(device, swapchain, pCount, null);
                LongBuffer images = stack.mallocLong(pCount.get(0));
                vkGetSwapchainImagesKHR(device, swapchain, pCount, images);
                swapchainImages = new long[images.capacity()];
                for (int i = 0; i < images.capacity(); i++) swapchainImages[i] = images.get(i);

                swapchainImageFormat = formatPair[0];
                swapchainWidth = extent[0];
                swapchainHeight = extent[1];
            }
        }

        private void createImageViews() {
            swapchainImageViews = new long[swapchainImages.length];
            try (MemoryStack stack = stackPush()) {
                for (int i = 0; i < swapchainImages.length; i++) {
                    VkImageViewCreateInfo info = VkImageViewCreateInfo.calloc(stack);
                    info.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                    info.image(swapchainImages[i]);
                    info.viewType(VK_IMAGE_VIEW_TYPE_2D);
                    info.format(swapchainImageFormat);
                    info.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
                    info.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                    info.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                    info.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);
                    info.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    info.subresourceRange().baseMipLevel(0);
                    info.subresourceRange().levelCount(1);
                    info.subresourceRange().baseArrayLayer(0);
                    info.subresourceRange().layerCount(1);
                    LongBuffer pView = stack.mallocLong(1);
                    int result = vkCreateImageView(device, info, null, pView);
                    if (result != VK_SUCCESS) throw new RuntimeException("vkCreateImageView failed: " + result);
                    swapchainImageViews[i] = pView.get(0);
                }
            }
        }

        private void createSyncObjects() {
            try (MemoryStack stack = stackPush()) {
                VkFenceCreateInfo info = VkFenceCreateInfo.calloc(stack);
                info.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
                LongBuffer pFence = stack.mallocLong(1);
                int result = vkCreateFence(device, info, null, pFence);
                if (result != VK_SUCCESS) throw new RuntimeException("vkCreateFence failed: " + result);
                acquireFence = pFence.get(0);
            }
        }

        private int findDepthFormat() {
            int[] candidates = new int[]{VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT};
            try (MemoryStack stack = stackPush()) {
                VkFormatProperties props = VkFormatProperties.calloc(stack);
                for (int format : candidates) {
                    vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);
                    if ((props.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                        return format;
                    }
                }
            }
            throw new RuntimeException("No depth format supported");
        }

        private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice deviceHandle) {
            QueueFamilyIndices out = new QueueFamilyIndices();
            try (MemoryStack stack = stackPush()) {
                IntBuffer count = stack.ints(0);
                vkGetPhysicalDeviceQueueFamilyProperties(deviceHandle, count, null);
                VkQueueFamilyProperties.Buffer props = VkQueueFamilyProperties.calloc(count.get(0), stack);
                vkGetPhysicalDeviceQueueFamilyProperties(deviceHandle, count, props);
                IntBuffer presentSupport = stack.mallocInt(1);
                for (int i = 0; i < props.capacity(); i++) {
                    if ((props.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) out.graphics = i;
                    vkGetPhysicalDeviceSurfaceSupportKHR(deviceHandle, i, surface, presentSupport);
                    if (presentSupport.get(0) == VK_TRUE) out.present = i;
                    if (out.complete()) break;
                }
            }
            return out;
        }

        private ExtensionSupport queryExtensions(VkPhysicalDevice deviceHandle) {
            ExtensionSupport support = new ExtensionSupport();
            try (MemoryStack stack = stackPush()) {
                IntBuffer count = stack.ints(0);
                vkEnumerateDeviceExtensionProperties(deviceHandle, (ByteBuffer) null, count, null);
                VkExtensionProperties.Buffer available = VkExtensionProperties.calloc(count.get(0), stack);
                vkEnumerateDeviceExtensionProperties(deviceHandle, (ByteBuffer) null, count, available);
                for (VkExtensionProperties ext : available) {
                    String name = ext.extensionNameString();
                    if (VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(name)) support.hasSwapchain = true;
                    if (VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME.equals(name)) support.hasDynamicRendering = true;
                }
            }
            return support;
        }

        private VkCommandBuffer beginSingleTimeCommands() {
            try (MemoryStack stack = stackPush()) {
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);
                PointerBuffer pCmd = stack.mallocPointer(1);
                int result = vkAllocateCommandBuffers(device, allocInfo, pCmd);
                if (result != VK_SUCCESS) throw new RuntimeException("vkAllocateCommandBuffers failed: " + result);
                VkCommandBuffer commandBuffer = new VkCommandBuffer(pCmd.get(0), device);

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
                beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                result = org.lwjgl.vulkan.VK10.vkBeginCommandBuffer(commandBuffer, beginInfo);
                if (result != VK_SUCCESS) throw new RuntimeException("vkBeginCommandBuffer failed: " + result);
                return commandBuffer;
            }
        }

        private void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
            try (MemoryStack stack = stackPush()) {
                int result = vkEndCommandBuffer(commandBuffer);
                if (result != VK_SUCCESS) throw new RuntimeException("vkEndCommandBuffer failed: " + result);

                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
                submitInfo.sType(org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO);
                submitInfo.pCommandBuffers(stack.pointers(commandBuffer.address()));
                result = vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
                if (result != VK_SUCCESS) throw new RuntimeException("vkQueueSubmit failed: " + result);
                vkQueueWaitIdle(graphicsQueue);
                vkFreeCommandBuffers(device, commandPool, commandBuffer);
            }
        }

        private int[] chooseSurfaceFormat(VkSurfaceFormatKHR.Buffer formats) {
            for (VkSurfaceFormatKHR format : formats) {
                if (format.format() == VK_FORMAT_B8G8R8A8_SRGB && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    return new int[]{format.format(), format.colorSpace()};
                }
            }
            VkSurfaceFormatKHR fallback = formats.get(0);
            return new int[]{fallback.format(), fallback.colorSpace()};
        }

        private int choosePresentMode(IntBuffer presentModes) {
            for (int i = 0; i < presentModes.capacity(); i++) {
                if (presentModes.get(i) == VK_PRESENT_MODE_FIFO_KHR) return VK_PRESENT_MODE_FIFO_KHR;
            }
            return VK_PRESENT_MODE_FIFO_KHR;
        }

        private int[] chooseExtent(VkSurfaceCapabilitiesKHR capabilities) {
            if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
                return new int[]{capabilities.currentExtent().width(), capabilities.currentExtent().height()};
            }
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                glfwGetFramebufferSize(window, w, h);
                int width = Math.max(capabilities.minImageExtent().width(), Math.min(capabilities.maxImageExtent().width(), w.get(0)));
                int height = Math.max(capabilities.minImageExtent().height(), Math.min(capabilities.maxImageExtent().height(), h.get(0)));
                return new int[]{width, height};
            }
        }
    }
}
