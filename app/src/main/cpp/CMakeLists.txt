cmake_minimum_required(VERSION 3.22.1)

project(AegisNative LANGUAGES CXX)

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

find_library(
    android_log
    log
)

target_link_libraries(
    aegis_native
    PRIVATE
    ${android_log}
)
