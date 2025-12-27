cmake_minimum_required(VERSION 3.10)
project(live2d_jni)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

add_definitions(-DCSM_TARGET_ANDROID_ES2)

if(WIN32)
    set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/windows/x86_64/143/Live2DCubismCore_MD.lib")
    set(SHIM_SRC "src/gles2_shim.c")
elseif(APPLE)
    if(CMAKE_OSX_ARCHITECTURES STREQUAL "arm64" OR CMAKE_SYSTEM_PROCESSOR STREQUAL "arm64")
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/macos/arm64/libLive2DCubismCore.a")
    else()
        set(CORE_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../sdk/Core/lib/macos/x86_64/libLive2DCubismCore.a")
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

if(APPLE)
    target_link_libraries(live2d_jni PRIVATE "-framework OpenGL")
elseif(WIN32)
    target_link_libraries(live2d_jni PRIVATE opengl32)
else()
    target_link_libraries(live2d_jni PRIVATE GLESv2)
endif()