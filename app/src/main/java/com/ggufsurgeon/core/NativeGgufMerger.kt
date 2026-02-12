package com.ggufsurgeon.core.native

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class NativeGgufMerger {
    
    init {
        System.loadLibrary("gguf_native")
    }
    
    fun mergeLora(
        baseModel: File,
        loraAdapter: File,
        alpha: Float,
        outputFile: File
    ): Flow<MergeProgress> = flow {
        val callback = object : ProgressCallback {
            override fun onProgress(progress: Int, message: String) {
                emit(MergeProgress.Progress(progress, message))
            }
            
            override fun onComplete(outputPath: String) {
                emit(MergeProgress.Complete(File(outputPath)))
            }
            
            override fun onError(error: String) {
                emit(MergeProgress.Error(error))
            }
        }
        
        val result = nativeMergeLora(
            baseModel.absolutePath,
            loraAdapter.absolutePath,
            alpha,
            outputFile.absolutePath,
            callback
        )
        
        if (!result) {
            emit(MergeProgress.Error("Merge operation failed"))
        }
    }
    
    private external fun nativeMergeLora(
        basePath: String,
        loraPath: String,
        alpha: Float,
        outputPath: String,
        callback: ProgressCallback
    ): Boolean
    
    interface ProgressCallback {
        fun onProgress(progress: Int, message: String)
        fun onComplete(outputPath: String)
        fun onError(error: String)
    }
}

sealed class MergeProgress {
    data class Progress(val percent: Int, val message: String) : MergeProgress()
    data class Complete(val outputFile: File) : MergeProgress()
    data class Error(val message: String) : MergeProgress()
}