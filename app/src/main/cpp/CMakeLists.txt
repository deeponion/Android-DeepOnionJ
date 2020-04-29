# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
             x13

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             aes_helper.c
             blake.c
             echo.c
             hamsi.c
             jh.c
             shavite.c
             sph_blake.h
             sph_echo.h
             sph_hamsi.h
             sph_luffa.h
             sph_skein.h
             bmw.c
             fugue.c
             hashblock.cpp
             keccak.c
             simd.c
             sph_bmw.h
             sph_fugue.h
             sph_jh.h
             sph_shavite.h
             sph_types.h
             cubehash.c
             groestl.c
             hashblock.h
             hamsi_helper.c
             luffa.c
             skein.c
             sph_cubehash.h
             sph_groestl.h
             sph_keccak.h
             sph_simd.h
             uint256.h
             )