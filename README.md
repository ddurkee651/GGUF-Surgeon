# GGUF Model Surgeon (Android)

GGUF Model Surgeon is an Android toolkit for **direct GGUF file-level control** on-device.  
It is a model engineering utility, not a chatbot UI.

## What this project provides

- **Model inspection**: parse a `.gguf` file and display architecture, context, RoPE values, tokenizer, metadata, and tensors.
- **Safe metadata editing**: change non-destructive fields and save a new GGUF output file.
- **LoRA merge workflow**: choose base + adapter and run merge parameters (currently scaffolded).
- **Optimization workflow**: simulate quantization targeting and tensor slimming metadata notes.
- **Validation checks**: blocks invalid saves when basic structural constraints fail.

## Architecture

- `core/` GGUF parser/editor/validator/operations engine.
- `data/` repository orchestration.
- `ui/` Compose screens: Home, Viewer, Edit, Merge, Optimize.
- `domain/` model and operation result types.

## Current status

This is a functional scaffold meant to be extended with:

1. Real binary GGUF section parsing.
2. Native integration with llama.cpp tools for true merge/quantize operations.
3. SAF (Storage Access Framework) file picker integration.
4. Background workers and progress tracking for long-running tasks.

## Run

```bash
./gradlew assembleDebug
```

