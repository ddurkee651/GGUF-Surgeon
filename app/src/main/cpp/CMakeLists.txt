cmake_minimum_required(VERSION 3.22.1)
project("ggufsurgeon")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# ===== LLAMA.CPP =====
add_subdirectory(llama.cpp EXCLUDE_FROM_ALL)

# Define llama.cpp sources
set(LLAMA_COMMON_SOURCES
    llama.cpp/ggml.c
    llama.cpp/ggml-alloc.c
    llama.cpp/ggml-backend.c
    llama.cpp/ggml-quants.c
    llama.cpp/llama.cpp
)

set(LLAMA_COMMON_INCLUDES
    llama.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/llama.cpp
)

# Build static library from llama.cpp
add_library(llama_common STATIC ${LLAMA_COMMON_SOURCES})
target_include_directories(llama_common PRIVATE ${LLAMA_COMMON_INCLUDES})
target_compile_features(llama_common PUBLIC cxx_std_11)

# Android specific flags
if(ANDROID)
    target_compile_definitions(llama_common PRIVATE GGML_USE_ACCELERATE)
endif()

# Architecture optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(llama_common PRIVATE 
        -O3 
        -march=armv8.2a+fp16+dotprod
        -DGGML_USE_ACCELERATE
    )
endif()

# ===== OUR NATIVE BRIDGE =====
add_library(gguf_native SHARED
    native/gguf_bridge.cpp
)

target_include_directories(gguf_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/native
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp
)

target_link_libraries(gguf_native
    llama_common
    log
    z
    dl
    c++_static
)

# OpenMP support (optional, improves performance)
find_package(OpenMP)
if(OpenMP_CXX_FOUND)
    target_link_libraries(gguf_native OpenMP::OpenMP_CXX)
endif()

# ===== OUTPUT =====
set_target_properties(gguf_native PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
    PREFIX "lib"
)

# ===== COMPILER OPTIMIZATIONS =====
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(gguf_native PRIVATE -O3 -flto)
    target_compile_options(llama_common PRIVATE -O3 -flto)
else()
    target_compile_options(gguf_native PRIVATE -O2 -g)
    target_compile_options(llama_common PRIVATE -O2 -g)
endif()
