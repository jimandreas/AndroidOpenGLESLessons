# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Project Overview

Android OpenGL ES 2.0 tutorial application with 8 progressive lessons. Originally ported from [Learn-OpenGLES-Tutorials](https://github.com/learnopengles/Learn-OpenGLES-Tutorials) to Android Studio with Kotlin conversion.

**Target:** Android 5.0+ (API 21), requires OpenGL ES 2.0

## Architecture

### Lesson Structure Pattern

Each lesson follows a consistent architecture:

**Simple lessons (1-4):**
- `LessonXActivity` - Creates GLSurfaceView, handles lifecycle
- `LessonXRenderer` - Implements `GLSurfaceView.Renderer` with `onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`

**Advanced lessons (5-8):**
- `LessonXActivity` - Creates custom GLSurfaceView subclass
- `LessonXGLSurfaceView` - Custom view with extended capabilities
- `LessonXRenderer` - Advanced rendering with VBOs/IBOs

### Key Directories

- `app/src/main/java/com/learnopengles/android/` - All lesson and utility code
- `app/src/main/java/com/learnopengles/android/common/` - Shared utilities (shader loading, texture handling, geometry generation)
- `app/src/main/res/raw/` - GLSL shader files (vertex and fragment shaders)

### Common Utilities (`android/common/`)

- **ShaderHelper.kt** - Shader compilation and program linking
- **RawResourceReader.kt** - Load GLSL source from `/res/raw/`
- **TextureHelper.kt** - Texture loading and OpenGL texture management
- **ShapeBuilder.kt** - Generate vertex/index data for geometric shapes

### Entry Points

- **TableOfContents.kt** - Main launcher activity (lesson selector)
- **LearnOpenglesApplication.kt** - Application class, initializes Timber logging

### Live Wallpapers

Three wallpaper implementations exist in separate packages (`livewallpaper/`, `rbgrnlivewallpaper/`, `switchinglivewallpaper/`). These remain in Java.

## Lesson Progression

1. Basic triangle rendering
2. Ambient and diffuse lighting
3. Per-pixel lighting
4. Texture mapping
5. Transparency and blending
6. Texture filtering (nearest, linear, mipmap)
7. Vertex Buffer Objects (VBOs)
8. Index Buffer Objects (IBOs)

## Build Configuration

- **Gradle:** 8.5
- **Android Gradle Plugin:** 8.2.2
- **Kotlin:** 1.9.22
- **Compile/Target SDK:** 34
- **Min SDK:** 21
- **Java/Kotlin JVM Target:** 17
