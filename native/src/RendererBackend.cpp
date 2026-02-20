#include "RendererBackend.hpp"

#include <Rendering/CubismRenderer.hpp>
#include <Rendering/OpenGL/CubismRenderer_OpenGLES2.hpp>

#if defined(LIVE2D_HAS_VULKAN)
#include <Rendering/Vulkan/CubismRenderer_Vulkan.hpp>
#endif

using namespace Live2D::Cubism::Framework::Rendering;

namespace
{
RendererBackend g_backend = RendererBackend::OpenGL;

#if defined(LIVE2D_HAS_VULKAN)
dev::eatgrapes::live2d::VulkanInitContext g_vulkanContext;
#endif
}

namespace Live2D { namespace Cubism { namespace Framework { namespace Rendering {

void SetRendererBackend(RendererBackend backend)
{
    g_backend = backend;
}

RendererBackend GetRendererBackend()
{
    return g_backend;
}

CubismRenderer* CubismRenderer::Create()
{
#if defined(LIVE2D_HAS_VULKAN)
    if (g_backend == RendererBackend::Vulkan)
    {
        return CSM_NEW CubismRenderer_Vulkan();
    }
#endif
    return CSM_NEW CubismRenderer_OpenGLES2();
}

void CubismRenderer::StaticRelease()
{
    CubismRenderer_OpenGLES2::DoStaticRelease();
#if defined(LIVE2D_HAS_VULKAN)
    CubismRenderer_Vulkan::DoStaticRelease();
#endif
}

}}}}

#if defined(LIVE2D_HAS_VULKAN)
namespace dev { namespace eatgrapes { namespace live2d {

void SetVulkanInitContext(const VulkanInitContext& context)
{
    g_vulkanContext = context;
}

const VulkanInitContext& GetVulkanInitContext()
{
    return g_vulkanContext;
}

bool HasVulkanInitContext()
{
    return g_vulkanContext.initialized;
}

}}}
#endif
