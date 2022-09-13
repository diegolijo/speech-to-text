cmake_minimum_required(VERSION 3.10.2)

project("native_c")

add_library(
        native_c
        SHARED
        native-lib.cpp
        AudioEngine.cpp)

find_library(
        log-lib
        log)

find_package (oboe REQUIRED CONFIG)

target_link_libraries(
        native_c
        oboe::oboe
        ${log-lib})
