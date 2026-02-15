cmake_minimum_required(VERSION 3.22.1)
project(wiredeye_native LANGUAGES CXX)

add_library(
        wiredeye_native
        SHARED
        native-tun.cpp
        leak/leak_analyzer.cpp
        leak/leak_analyzer_jni.cpp
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(wiredeye_native ${log-lib} ${android-lib})
