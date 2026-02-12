package com.ggufsurgeon.core

import com.ggufsurgeon.domain.ModelFile
import java.io.File

class GgufEditor {
    fun updateMetadata(model: ModelFile, updates: Map<String, String>): ModelFile {
        val newMetadata = model.metadata.toMutableMap().apply { putAll(updates) }
        return model.copy(
            name = updates["general.name"] ?: model.name,
            contextLength = updates["llama.context_length"]?.toIntOrNull() ?: model.contextLength,
            ropeScaling = updates["rope.scaling"]?.toFloatOrNull() ?: model.ropeScaling,
            metadata = newMetadata
        )
    }

    fun saveAsNewFile(original: File, model: ModelFile, output: File): File {
        require(original.exists()) { "Original GGUF file does not exist" }
        original.copyTo(output, overwrite = true)
        output.appendText("\n# GGUF-Surgeon metadata patch\n")
        model.metadata.forEach { (k, v) -> output.appendText("# $k=$v\n") }
        return output
    }
}
