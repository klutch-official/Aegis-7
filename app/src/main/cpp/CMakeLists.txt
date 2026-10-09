cmake_minimum_required(VERSION 3.22.1)

project(AegisNative LANGUAGES C CXX)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

add_library(
    aegis_native
    SHARED
    native_ai.cpp
)

target_compile_features(aegis_native PRIVATE cxx_std_17)

target_compile_options(
    aegis_native
    PRIVATE
    -Wall
    -Wextra
)

target_include_directories(
    aegis_native
    PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include"
)

find_library(android_log log)

target_link_libraries(
    aegis_native
    PRIVATE
    llama
    ${android_log}
)
