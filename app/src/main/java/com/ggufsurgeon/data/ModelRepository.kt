package com.ggufsurgeon.data

import com.ggufsurgeon.core.GgufBinaryEditor
import com.ggufsurgeon.core.GgufValidator
import com.ggufsurgeon.core.python.PythonGgufBridge
import com.ggufsurgeon.domain.ModelFile
import com.ggufsurgeon.domain.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val validator: GgufValidator,
    private val binaryEditor: GgufBinaryEditor,
    private val pythonBridge: PythonGgufBridge
) {
    
    suspend fun inspectModel(file: File): Result<ModelFile> = 
        pythonBridge.inspectModel(file)
    
    fun editMetadata(
        originalFile: File,
        outputFile: File,
        updates: Map<String, String>
    ): Flow<OperationResult> = flow {
        emit(OperationResult.Progress(10, "Editing metadata..."))
        val result = binaryEditor.editMetadata(originalFile, outputFile, updates)
        if (result.isSuccess) {
            emit(OperationResult.Progress(100, "Complete"))
            emit(OperationResult.Success(
                outputPath = outputFile.absolutePath,
                details = "Metadata updated successfully"
            ))
        } else {
            emit(OperationResult.Failure(result.exceptionOrNull()?.message ?: "Unknown error"))
        }
    }.catch { e ->
        emit(OperationResult.Failure(e.message ?: "Operation failed"))
    }
    
    suspend fun mergeLora(
        baseModel: File,
        loraAdapter: File,
        alpha: Float,
        outputFile: File
    ): Result<File> = pythonBridge.mergeLora(baseModel, loraAdapter, alpha, outputFile)
    
    fun quantizeModel(
        inputFile: File,
        outputFile: File,
        quantizationType: String
    ): Flow<OperationResult> = flow {
        emit(OperationResult.Progress(0, "Quantization coming soon!"))
        emit(OperationResult.Failure(
            "Quantization requires llama.cpp binary - coming in next update!"
        ))
    }
}
