cmake_minimum_required(VERSION 3.22.1)
project("ggufsurgeon")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Set output directories
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Download and extract llama.cpp
include(FetchContent)
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG b3623  # Stable release
    GIT_SHALLOW TRUE
    GIT_PROGRESS TRUE
)

FetchContent_MakeAvailable(llama_cpp)

# Set llama.cpp build options
set(LLAMA_BUILD_TESTS OFF)
set(LLAMA_BUILD_EXAMPLES OFF)
set(LLAMA_BUILD_SERVER OFF)
set(LLAMA_METAL OFF)
set(LLAMA_CUBLAS OFF)
set(LLAMA_VULKAN ON)

# Add GGUF reader/writer library
add_library(gguf_reader STATIC
    ${llama_cpp_SOURCE_DIR}/ggml.c
    ${llama_cpp_SOURCE_DIR}/ggml-alloc.c
    ${llama_cpp_SOURCE_DIR}/ggml-backend.c
    ${llama_cpp_SOURCE_DIR}/ggml-quants.c
    ${llama_cpp_SOURCE_DIR}/llama.cpp
)

target_include_directories(gguf_reader PRIVATE ${llama_cpp_SOURCE_DIR})
target_compile_definitions(gguf_reader PRIVATE GGML_USE_ACCELERATE)

# Native interface
add_library(gguf_native SHARED
    native/gguf_bridge.cpp
    native/gguf_parser.cpp
    native/gguf_merger.cpp
    native/gguf_quantizer.cpp
)

target_include_directories(gguf_native PRIVATE
    ${llama_cpp_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/native
)

target_link_libraries(gguf_native
    gguf_reader
    jnigraphics
    log
    z
)

# Architecture-specific optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(gguf_reader PRIVATE -O3 -march=armv8.2a+fp16+dotprod)
    target_compile_options(gguf_native PRIVATE -O3 -march=armv8.2a+fp16+dotprod)
endif()