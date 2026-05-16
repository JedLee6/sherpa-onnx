## Context

The Matcha TTS architecture requires two components: an acoustic model (`model-steps-3.onnx`) and a vocoder (`vocos-22khz-univ.onnx`). Unlike other model files that are stored inside their respective model directories, the vocoder is a **shared universal file** stored at the assets root level. This is by design in the sherpa-onnx library (see `Tts.kt` line 346 where `vocoder = vocoder` without modelDir prefix).

The vocoder must be downloaded from:
`https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos-22khz-univ.onnx`

## Goals / Non-Goals

**Goals:**
- Download and add the missing vocoder file to the app assets.
- Restore the Matcha model in `Models.kt` with correct configuration.
- Ensure the Matcha model loads and synthesizes Chinese text correctly.

**Non-Goals:**
- Changing the vocoder path convention in `Tts.kt` (shared library code).

## Decisions

- **Vocoder Placement**: Place at `app/src/main/assets/vocos-22khz-univ.onnx` (root level, as expected by `getOfflineTtsConfig`).
- **Rule FSTs**: Include `date.fst`, `number.fst`, `phone.fst` as `ruleFsts` in the model config for proper Chinese text normalization.
- **Required Files Validation**: The `requiredFiles` list must include the root-level vocoder path.

## Risks / Trade-offs

- The vocoder file adds ~23MB to the APK size.
