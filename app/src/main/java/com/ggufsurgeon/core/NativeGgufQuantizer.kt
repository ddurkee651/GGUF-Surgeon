package com.ggufsurgeon.core.native

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

enum class QuantizationType(val value: String) {
    Q4_0("Q4_0"),
    Q4_1("Q4_1"),
    Q5_0("Q5_0"),
    Q5_1("Q5_1"),
    Q8_0("Q8_0"),
    Q2_K("Q2_K"),
    Q3_K("Q3_K"),
    Q4_K("Q4_K"),
    Q5_K("Q5_K"),
    Q6_K("Q6_K");
    
    companion object {
        fun fromString(value: String): QuantizationType = 
            values().find { it.value.equals(value, ignoreCase = true) } ?: Q4_1
    }
}

class NativeGgufQuantizer {
    
    init {
        System.loadLibrary("gguf_native")
    }
    
    fun quantize(
        inputFile: File,
        outputFile: File,
        quantizationType: QuantizationType
    ): Flow<QuantizationProgress> = flow {
        val callback = object : ProgressCallback {
            override fun onProgress(progress: Int, message: String) {
                emit(QuantizationProgress.Progress(progress, message))
            }
            
            override fun onComplete(outputPath: String) {
                emit(QuantizationProgress.Complete(File(outputPath)))
            }
            
            override fun onError(error: String) {
                emit(QuantizationProgress.Error(error))
            }
        }
        
        val result = nativeQuantize(
            inputFile.absolutePath,
            outputFile.absolutePath,
            quantizationType.value,
            callback
        )
        
        if (!result) {
            emit(QuantizationProgress.Error("Quantization operation failed"))
        }
    }
    
    private external fun nativeQuantize(
        inputPath: String,
        outputPath: String,
        quantType: String,
        callback: ProgressCallback
    ): Boolean
    
    interface ProgressCallback {
        fun onProgress(progress: Int, message: String)
        fun onComplete(outputPath: String)
        fun onError(error: String)
    }
}

sealed class QuantizationProgress {
    data class Progress(val percent: Int, val message: String) : QuantizationProgress()
    data class Complete(val outputFile: File) : QuantizationProgress()
    data class Error(val message: String) : QuantizationProgress()
}