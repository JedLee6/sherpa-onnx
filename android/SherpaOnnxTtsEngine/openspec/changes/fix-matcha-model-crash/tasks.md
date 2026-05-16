## 1. Infrastructure and Model Config

- [x] 1.1 Add `dictDir` property to `ModelConfig` in `Models.kt`.
- [x] 1.2 Update the `matcha-icefall-zh-baker` configuration in `Models.kt` to include `dictDir = "matcha-icefall-zh-baker/dict"`.

## 2. TtsEngine Updates

- [x] 2.1 Update `TtsEngine.kt` to handle `dictDir` copying in `realInitTts`.
- [x] 2.2 Update `getOfflineTtsConfig` call in `TtsEngine.kt` to use the resolved `dictDir`.

## 3. Testing

- [ ] 3.1 Verify that the Matcha model now loads correctly without crashing.
- [ ] 3.2 Confirm that Chinese text is correctly synthesized using the Matcha model.
