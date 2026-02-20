#pragma once

#include <cstdint>

namespace Live2D { namespace Cubism { namespace Framework { namespace Rendering {

enum class RendererBackend : uint8_t
{
    OpenGL = 0,
    Vulkan = 1
};

void SetRendererBackend(RendererBackend backend);
RendererBackend GetRendererBackend();

}}}}

#if defined(LIVE2D_HAS_VULKAN)
#include <vulkan/vulkan.h>

namespace dev { namespace eatgrapes { namespace live2d {

struct VulkanInitContext
{
    VkDevice device = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkCommandPool commandPool = VK_NULL_HANDLE;
    VkQueue queue = VK_NULL_HANDLE;
    bool initialized = false;
};

void SetVulkanInitContext(const VulkanInitContext& context);
const VulkanInitContext& GetVulkanInitContext();
bool HasVulkanInitContext();

}}}
#endif
