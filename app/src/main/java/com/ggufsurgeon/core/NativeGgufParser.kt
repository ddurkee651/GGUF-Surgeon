package com.ggufsurgeon.core.native

import com.ggufsurgeon.domain.ModelFile
import com.ggufsurgeon.domain.TensorInfo
import java.io.File

/**
 * Real GGUF parser using native llama.cpp implementation
 */
class NativeGgufParser {
    
    private var nativeHandle: Long = 0
    
    init {
        System.loadLibrary("gguf_native")
    }
    
    @Throws(Exception::class)
    fun parse(file: File): ModelFile {
        require(file.extension.lowercase() == "gguf") { "Only .gguf files are supported" }
        
        nativeHandle = nativeParseFile(file.absolutePath)
        require(nativeHandle != 0L) { "Failed to parse GGUF file" }
        
        val metadataArray = nativeGetMetadata(nativeHandle)
        val metadata = mutableMapOf<String, String>()
        
        for (i in metadataArray.indices step 2) {
            val key = metadataArray[i] as String
            val value = metadataArray[i + 1] as String
            metadata[key] = value
        }
        
        val tensorList = nativeGetTensors(nativeHandle)
        
        return ModelFile(
            name = file.nameWithoutExtension,
            architecture = metadata["general.architecture"] ?: "unknown",
            contextLength = metadata["llama.context_length"]?.toIntOrNull() ?: 4096,
            ropeBase = metadata["rope.freq_base"]?.toFloatOrNull() ?: 10000f,
            ropeScaling = metadata["rope.scaling.linear"]?.toFloatOrNull() ?: 1f,
            quantization = metadata["general.file_type"] ?: "unknown",
            tensorCount = tensorList.size,
            tokenizer = metadata["tokenizer.ggml.model"] ?: "unknown",
            metadata = metadata,
            tensors = tensorList.toList(),
            fileSize = file.length(),
            filePath = file.absolutePath
        )
    }
    
    fun close() {
        if (nativeHandle != 0L) {
            nativeClose(nativeHandle)
            nativeHandle = 0
        }
    }
    
    private external fun nativeParseFile(path: String): Long
    private external fun nativeClose(handle: Long)
    private external fun nativeGetMetadata(handle: Long): Array<String>
    private external fun nativeGetTensors(handle: Long): Array<TensorInfo>
    
    protected fun finalize() {
        close()
    }
}
