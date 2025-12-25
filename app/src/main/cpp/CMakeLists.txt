cmake_minimum_required(VERSION 3.22.1)

project("musimind_audio" LANGUAGES CXX)

# C++ Standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Android libraries
find_library(log-lib log)
find_library(android-lib android)

# Oboe - Fetch from Git
include(FetchContent)
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG        1.9.0
)
FetchContent_MakeAvailable(oboe)

# FluidSynth - will be added as prebuilt library
# For now, we use a simpler approach with TinySoundFont

# TinySoundFont - header-only library for SoundFont synthesis
# https://github.com/schellingb/TinySoundFont
FetchContent_Declare(
    tinysoundfont
    GIT_REPOSITORY https://github.com/schellingb/TinySoundFont.git
    GIT_TAG        main
)
FetchContent_MakeAvailable(tinysoundfont)

# Main native library
add_library(native-audio SHARED
    native-audio.cpp
    SoundFontEngine.cpp
    OboePlayer.cpp
)

# Include directories
target_include_directories(native-audio PRIVATE
    ${tinysoundfont_SOURCE_DIR}
)

# Link libraries
target_link_libraries(native-audio
    ${log-lib}
    ${android-lib}
    oboe
)
