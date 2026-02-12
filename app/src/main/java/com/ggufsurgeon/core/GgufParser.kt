package com.ggufsurgeon.core

import com.ggufsurgeon.domain.ModelFile
import com.ggufsurgeon.domain.TensorInfo
import java.io.File

/**
 * Lightweight parser scaffold for GGUF. In production, replace with a full binary parser.
 */
class GgufParser {
    fun parse(file: File): ModelFile {
        require(file.extension.lowercase() == "gguf") { "Only .gguf files are supported" }

        val metadata = mutableMapOf<String, String>()
        val headerPreview = file.inputStream().use { input ->
            val bytes = ByteArray(64)
            val count = input.read(bytes)
            if (count > 0) bytes.copyOf(count).decodeToString() else ""
        }

        metadata["source_file"] = file.absolutePath
        metadata["file_size"] = file.length().toString()
        metadata["header_preview"] = headerPreview.filter { it.code in 32..126 }

        return ModelFile(
            name = file.nameWithoutExtension,
            architecture = metadata["general.architecture"] ?: "unknown",
            contextLength = (metadata["llama.context_length"] ?: "4096").toInt(),
            ropeBase = (metadata["rope.freq_base"] ?: "10000.0").toFloat(),
            ropeScaling = (metadata["rope.scaling"] ?: "1.0").toFloat(),
            quantization = metadata["general.file_type"] ?: "unknown",
            tensorCount = 0,
            tokenizer = metadata["tokenizer.ggml.model"] ?: "unknown",
            metadata = metadata,
            tensors = emptyList()
        )
    }

    fun parseTensors(file: File): List<TensorInfo> {
        if (file.extension.lowercase() != "gguf") return emptyList()
        return listOf(
            TensorInfo("tok_embeddings.weight", listOf(32000, 4096), "Q4_K", 52_428_800),
            TensorInfo("layers.0.attention.wq.weight", listOf(4096, 4096), "Q4_K", 8_388_608)
        )
    }
}
