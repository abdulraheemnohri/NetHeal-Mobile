cmake_minimum_required(VERSION 3.22.1)
project("netheal")

add_library(netheal SHARED IMPORTED)
set_target_properties(netheal PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnetheal.so)
