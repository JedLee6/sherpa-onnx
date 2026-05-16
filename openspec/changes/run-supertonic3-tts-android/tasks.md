## 1. Copy native SO libraries

- [ ] 1.1 Copy SO files (libonnxruntime.so, libsherpa-onnx-jni.so) for arm64-v8a from SherpaOnnxTts to SherpaOnnxTtsEngine jniLibs
- [ ] 1.2 Copy SO files for armeabi-v7a from SherpaOnnxTts to SherpaOnnxTtsEngine jniLibs
- [ ] 1.3 Copy SO files for x86 from SherpaOnnxTts to SherpaOnnxTtsEngine jniLibs
- [ ] 1.4 Copy SO files for x86_64 from SherpaOnnxTts to SherpaOnnxTtsEngine jniLibs

## 2. Build and run

- [ ] 2.1 Build debug APK with ./gradlew assembleDebug
- [ ] 2.2 Install APK on connected Android device with ./gradlew installDebug
- [ ] 2.3 Verify app launches and TTS synthesizes speech
