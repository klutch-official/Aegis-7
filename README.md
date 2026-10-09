# Aegis-7

Aegis-7 is an Android-first offline AI assistant.

## Goals

- Run AI models locally without requiring an online AI service.
- Support ARM32 (`armeabi-v7a`) and ARM64 (`arm64-v8a`) Android devices.
- Provide general-assistant and coding model options.
- Load only one AI model at a time to reduce memory usage.
- Store profiles and chat history locally.
- Keep model weights separate from the application source.
- Provide a modern interface built with React, HTML, CSS and JavaScript.
- Use Kotlin for Android integration and C++ for native AI inference.

## Project structure

- `app/` - Android application and native C++ integration.
- `web/` - React interface source code.
- `native/` - Native inference dependency.
- `scripts/` - Build and model verification scripts.
- `.github/workflows/` - Automated checks and Android builds.

## Privacy

The intended design is local-first. Model inference, profiles and chat history
should remain on the device. Network access may be used to obtain the app or
model files when the user chooses to download them.

## Model files

AI model weights are not committed to this repository. Users will need to
obtain compatible model files separately.

## Project status

Early development. The application and inference pipeline are not yet complete.
# Aegis-7
