package com.ggufsurgeon.data

import com.ggufsurgeon.core.GgufBinaryEditor
import com.ggufsurgeon.core.GgufValidator
import com.ggufsurgeon.core.native.NativeGgufMerger
import com.ggufsurgeon.core.native.NativeGgufParser
import com.ggufsurgeon.core.native.NativeGgufQuantizer
import com.ggufsurgeon.core.native.QuantizationType
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
    private val nativeMerger: NativeGgufMerger,
    private val nativeQuantizer: NativeGgufQuantizer
) {
    
    suspend fun inspectModel(file: File): Result<ModelFile> = runCatching {
        NativeGgufParser().use { parser ->
            val model = parser.parse(file)
            
            // Validate structure
            val validation = validator.validate(file)
            require(validation.isValid) { 
                "Invalid GGUF structure: ${validation.errors.joinToString(", ")}" 
            }
            
            // Validate model consistency
            val modelValidation = validator.validateModel(model)
            require(modelValidation.isValid) {
                "Model validation failed: ${modelValidation.errors.joinToString(", ")}"
            }
            
            model.copy(
                validationWarnings = modelValidation.warnings,
                filePath = file.absolutePath
            )
        }
    }
    
    fun editMetadata(
        originalFile: File,
        outputFile: File,
        updates: Map<String, String>
    ): Flow<OperationResult> = flow {
        emit(OperationResult.Progress(10, "Validating input file..."))
        
        val validation = validator.validate(originalFile)
        if (!validation.isValid) {
            emit(OperationResult.Failure("Invalid GGUF file: ${validation.errors.joinToString(", ")}"))
            return@flow
        }
        
        emit(OperationResult.Progress(30, "Editing metadata..."))
        
        val result = binaryEditor.editMetadata(originalFile, outputFile, updates)
        
        if (result.isSuccess) {
            emit(OperationResult.Progress(80, "Validating output..."))
            
            val outputValidation = validator.validate(outputFile)
            if (!outputValidation.isValid) {
                emit(OperationResult.Failure("Output validation failed: ${outputValidation.errors.joinToString(", ")}"))
                outputFile.delete()
                return@flow
            }
            
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
    
    fun mergeLora(
        baseModel: File,
        loraAdapter: File,
        alpha: Float,
        outputFile: File
    ): Flow<OperationResult> = flow {
        emit(OperationResult.Progress(5, "Validating files..."))
        
        if (!baseModel.exists()) {
            emit(OperationResult.Failure("Base model file not found"))
            return@flow
        }
        
        if (!loraAdapter.exists()) {
            emit(OperationResult.Failure("LoRA adapter file not found"))
            return@flow
        }
        
        emit(OperationResult.Progress(10, "Starting LoRA merge..."))
        
        nativeMerger.mergeLora(baseModel, loraAdapter, alpha, outputFile)
            .collect { progress ->
                when (progress) {
                    is NativeGgufMerger.MergeProgress.Progress -> {
                        emit(OperationResult.Progress(
                            progress.percent,
                            "Merging: ${progress.message}"
                        ))
                    }
                    is NativeGgufMerger.MergeProgress.Complete -> {
                        emit(OperationResult.Progress(100, "Validating merged model..."))
                        
                        val validation = validator.validate(progress.outputFile)
                        if (validation.isValid) {
                            emit(OperationResult.Success(
                                outputPath = progress.outputFile.absolutePath,
                                details = "LoRA merge completed successfully"
                            ))
                        } else {
                            emit(OperationResult.Failure(
                                "Merge succeeded but validation failed: ${validation.errors.joinToString(", ")}"
                            ))
                        }
                    }
                    is NativeGgufMerger.MergeProgress.Error -> {
                        emit(OperationResult.Failure(progress.message))
                    }
                }
            }
    }.catch { e ->
        emit(OperationResult.Failure(e.message ?: "Merge operation failed"))
    }
    
    fun quantizeModel(
        inputFile: File,
        outputFile: File,
        quantizationType: QuantizationType
    ): Flow<OperationResult> = flow {
        emit(OperationResult.Progress(5, "Validating input file..."))
        
        if (!inputFile.exists()) {
            emit(OperationResult.Failure("Input file not found"))
            return@flow
        }
        
        val validation = validator.validate(inputFile)
        if (!validation.isValid) {
            emit(OperationResult.Failure("Invalid GGUF file: ${validation.errors.joinToString(", ")}"))
            return@flow
        }
        
        emit(OperationResult.Progress(10, "Starting quantization to ${quantizationType.value}..."))
        
        nativeQuantizer.quantize(inputFile, outputFile, quantizationType)
            .collect { progress ->
                when (progress) {
                    is NativeGgufQuantizer.QuantizationProgress.Progress -> {
                        emit(OperationResult.Progress(
                            progress.percent,
                            "Quantizing: ${progress.message}"
                        ))
                    }
                    is NativeGgufQuantizer.QuantizationProgress.Complete -> {
                        emit(OperationResult.Progress(100, "Validating quantized model..."))
                        
                        val outputValidation = validator.validate(progress.outputFile)
                        if (outputValidation.isValid) {
                            emit(OperationResult.Success(
                                outputPath = progress.outputFile.absolutePath,
                                details = "Quantization completed successfully"
                            ))
                        } else {
                            emit(OperationResult.Failure(
                                "Quantization succeeded but validation failed: ${outputValidation.errors.joinToString(", ")}"
                            ))
                        }
                    }
                    is NativeGgufQuantizer.QuantizationProgress.Error -> {
                        emit(OperationResult.Failure(progress.message))
                    }
                }
            }
    }.catch { e ->
        emit(OperationResult.Failure(e.message ?: "Quantization operation failed"))
    }
    
    fun dropTensors(
        inputFile: File,
        outputFile: File,
        tensorNamesToDrop: List<String>
    ): Flow<OperationResult> = flow {
        emit(OperationResult.Progress(5, "Analyzing tensor structure..."))
        
        // This requires custom implementation to rebuild GGUF without certain tensors
        // For now, we'll implement it by modifying the tensor index table
        
        emit(OperationResult.Failure("Tensor dropping not yet implemented in native code"))
    }
}
