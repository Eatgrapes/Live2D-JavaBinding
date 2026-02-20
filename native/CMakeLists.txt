cmake_minimum_required(VERSION 3.10)
project(live2d_jni)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

add_definitions(-DCSM_TARGET_ANDROID_ES2)
add_definitions(-DLIVE2D_CUSTOM_RENDERER_FACTORY)

string(TOLOWER "${CMAKE_SYSTEM_PROCESSOR}" LIVE2D_SYSTEM_PROCESSOR_LOWER)
set(LIVE2D_ARCH_IS_X64 FALSE)
if(LIVE2D_SYSTEM_PROCESSOR_LOWER STREQUAL "x86_64" OR LIVE2D_SYSTEM_PROCESSOR_LOWER STREQUAL "amd64")
    set(LIVE2D_ARCH_IS_X64 TRUE)
endif()

set(ENABLE_VULKAN FALSE)
if((WIN32 OR (UNIX AND NOT APPLE)) AND CMAKE_SIZEOF_VOID_P EQUAL 8 AND LIVE2D_ARCH_IS_X64)
    find_package(Vulkan QUIET)
    if(Vulkan_FOUND)
        set(ENABLE_VULKAN TRUE)
        add_definitions(-DLIVE2D_HAS_VULKAN)
    else()
        message(WARNING "Vulkan SDK not found. Vulkan backend will be disabled.")
    endif()
elseif((WIN32 OR (UNIX AND NOT APPLE)) AND CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(STATUS "Vulkan backend disabled: only linux-x64/windows-x64 are supported.")
endif()

if(WIN32)
    set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/windows/x86_64/143/Live2DCubismCore_MD.lib")
    set(SHIM_SRC "src/gles2_shim.c")
elseif(APPLE)
    if(CMAKE_OSX_ARCHITECTURES STREQUAL "arm64" OR CMAKE_SYSTEM_PROCESSOR STREQUAL "arm64")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/macos/arm64/libLive2DCubismCore.a")
    else()
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/macos/x86_64/libLive2DCubismCore.a")
    endif()
elseif(ANDROID)
    if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/android/arm64-v8a/libLive2DCubismCore.a")
    elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/android/armeabi-v7a/libLive2DCubismCore.a")
    elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/android/x86/libLive2DCubismCore.a")
    elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/android/x86_64/libLive2DCubismCore.a")
    endif()
else()
    if(CMAKE_SYSTEM_PROCESSOR STREQUAL "aarch64" OR CMAKE_SYSTEM_PROCESSOR STREQUAL "arm64")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/experimental/linux/arm64/libLive2DCubismCore.a")
    else()
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/linux/x86_64/libLive2DCubismCore.a")
    endif()
endif()

get_filename_component(CORE_LIB_PATH "${CORE_LIB_PATH}" ABSOLUTE)

set(FRAMEWORK_SOURCE OpenGL)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Framework framework_build)
target_include_directories(Framework PRIVATE ${JNI_INCLUDE_DIRS} ${CMAKE_CURRENT_SOURCE_DIR}/include)
if(ENABLE_VULKAN)
    target_sources(Framework PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Framework/src/Rendering/Vulkan/CubismClass_Vulkan.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Framework/src/Rendering/Vulkan/CubismOffscreenSurface_Vulkan.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Framework/src/Rendering/Vulkan/CubismRenderer_Vulkan.cpp
    )
    target_include_directories(Framework PRIVATE ${Vulkan_INCLUDE_DIRS})
endif()

include_directories(
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Framework/src
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

file(GLOB_RECURSE JNI_SOURCES "src/*.cpp")
add_library(live2d_jni SHARED ${JNI_SOURCES} ${SHIM_SRC})

target_link_libraries(live2d_jni
    PRIVATE
    Framework
    "${CORE_LIB_PATH}"
    ${JNI_LIBRARIES}
)
if(ENABLE_VULKAN)
    target_link_libraries(live2d_jni PRIVATE Vulkan::Vulkan)
    target_include_directories(live2d_jni PRIVATE ${Vulkan_INCLUDE_DIRS})
endif()

if(APPLE)
    target_link_libraries(live2d_jni PRIVATE "-framework OpenGL")
elseif(WIN32)
    target_link_libraries(live2d_jni PRIVATE opengl32)
elseif(ANDROID)
    target_link_libraries(live2d_jni PRIVATE GLESv2 EGL log android)
else()
    target_link_libraries(live2d_jni PRIVATE GLESv2)
endif()
