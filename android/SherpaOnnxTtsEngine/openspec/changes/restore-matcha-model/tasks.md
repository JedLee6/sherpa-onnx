## 1. Download Missing Vocoder

- [x] 1.1 Download `vocos-22khz-univ.onnx` from `https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos-22khz-univ.onnx` to `app/src/main/assets/vocos-22khz-univ.onnx`.

## 2. Restore Matcha Model Config

- [x] 2.1 Uncomment and update the `matcha-icefall-zh-baker` entry in `Models.kt` with correct configuration including `ruleFsts`, `dictDir`, and `requiredFiles`.

## 3. Add ruleFsts Support

- [x] 3.1 Add a `ruleFsts` property to `ModelConfig` in `Models.kt`.
- [x] 3.2 Update `TtsEngine.realInitTts` to use `config.ruleFsts` instead of the instance variable when available.
