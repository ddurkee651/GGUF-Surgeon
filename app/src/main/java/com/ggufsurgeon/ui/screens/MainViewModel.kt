package com.ggufsurgeon.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ggufsurgeon.core.native.QuantizationType
import com.ggufsurgeon.data.ModelRepository
import com.ggufsurgeon.domain.ModelFile
import com.ggufsurgeon.domain.OperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// Import for mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ModelRepository
) : ViewModel() {
    
    // UI State
    var selectedPath by mutableStateOf("")
        private set
        
    var currentModel by mutableStateOf<ModelFile?>(null)
        private set
        
    var statusMessage by mutableStateOf("Ready")
        private set
        
    var isOperationInProgress by mutableStateOf(false)
        private set
        
    var operationProgress by mutableStateOf(0)
        private set
        
    var operationDetails by mutableStateOf("")
        private set
        
    var lastOperationResult by mutableStateOf<OperationResult?>(null)
        private set
    
    private var currentJob: Job? = null
    
    /**
     * Inspect and parse a GGUF model file
     */
    fun inspectModel(file: File) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            isOperationInProgress = true
            operationProgress = 10
            statusMessage = "Parsing model: ${file.name}..."
            operationDetails = "Reading GGUF structure"
            
            repository.inspectModel(file)
                .onSuccess { model ->
                    currentModel = model
                    selectedPath = file.absolutePath
                    operationProgress = 100
                    statusMessage = "✓ Model loaded: ${model.name} (${model.architecture})"
                    operationDetails = "Complete"
                    lastOperationResult = OperationResult.Success(
                        outputPath = file.absolutePath,
                        details = "Model parsed successfully"
                    )
                }
                .onFailure { exception ->
                    operationProgress = 0
                    statusMessage = "✗ Failed to parse model: ${exception.message}"
                    operationDetails = "Error"
                    lastOperationResult = OperationResult.Failure(
                        error = exception.message ?: "Unknown error"
                    )
                }
            
            isOperationInProgress = false
        }
    }
    
    /**
     * Edit model metadata and save as new file
     */
    fun editMetadata(
        originalFile: File,
        outputFile: File,
        updates: Map<String, String>
    ) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            isOperationInProgress = true
            operationProgress = 0
            statusMessage = "Editing metadata..."
            
            repository.editMetadata(originalFile, outputFile, updates)
                .collectLatest { result ->
                    when (result) {
                        is OperationResult.Progress -> {
                            operationProgress = result.percent
                            operationDetails = result.message
                            statusMessage = result.message
                        }
                        is OperationResult.Success -> {
                            operationProgress = 100
                            statusMessage = "✓ Model saved to ${File(result.outputPath).name}"
                            operationDetails = "Complete"
                            lastOperationResult = result
                            
                            // Reload the edited model
                            inspectModel(File(result.outputPath))
                        }
                        is OperationResult.Failure -> {
                            operationProgress = 0
                            statusMessage = "✗ Edit failed: ${result.error}"
                            operationDetails = "Error"
                            lastOperationResult = result
                        }
                    }
                }
            
            isOperationInProgress = false
        }
    }
    
    /**
     * Merge LoRA adapter with base model
     */
    fun mergeLora(
        baseModel: File,
        loraAdapter: File,
        alpha: Float,
        outputFile: File
    ) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            isOperationInProgress = true
            operationProgress = 0
            statusMessage = "Starting LoRA merge..."
            
            repository.mergeLora(baseModel, loraAdapter, alpha, outputFile)
                .collectLatest { result ->
                    when (result) {
                        is OperationResult.Progress -> {
                            operationProgress = result.percent
                            operationDetails = result.message
                            statusMessage = result.message
                        }
                        is OperationResult.Success -> {
                            operationProgress = 100
                            statusMessage = "✓ Merge complete: ${File(result.outputPath).name}"
                            operationDetails = "Complete"
                            lastOperationResult = result
                            
                            // Calculate size
                            val outputSize = File(result.outputPath).length() / (1024 * 1024)
                            statusMessage = "✓ Merge complete: ${outputSize}MB"
                            
                            // Reload the merged model
                            inspectModel(File(result.outputPath))
                        }
                        is OperationResult.Failure -> {
                            operationProgress = 0
                            statusMessage = "✗ Merge failed: ${result.error}"
                            operationDetails = "Error"
                            lastOperationResult = result
                        }
                    }
                }
            
            isOperationInProgress = false
        }
    }
    
    /**
     * Quantize model to reduce size
     */
    fun quantizeModel(
        inputFile: File,
        outputFile: File,
        quantizationType: QuantizationType
    ) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            isOperationInProgress = true
            operationProgress = 0
            statusMessage = "Starting quantization to ${quantizationType.value}..."
            
            repository.quantizeModel(inputFile, outputFile, quantizationType)
                .collectLatest { result ->
                    when (result) {
                        is OperationResult.Progress -> {
                            operationProgress = result.percent
                            operationDetails = result.message
                            statusMessage = result.message
                        }
                        is OperationResult.Success -> {
                            operationProgress = 100
                            statusMessage = "✓ Quantization complete: ${File(result.outputPath).name}"
                            operationDetails = "Complete"
                            lastOperationResult = result
                            
                            // Calculate size reduction
                            val inputSize = inputFile.length() / (1024 * 1024)
                            val outputSize = File(result.outputPath).length() / (1024 * 1024)
                            val reduction = ((1.0 - outputSize.toDouble() / inputSize.toDouble()) * 100).toInt()
                            
                            statusMessage = "✓ Quantized: ${inputSize}MB → ${outputSize}MB (${reduction}% reduction)"
                            
                            // Reload the quantized model
                            inspectModel(File(result.outputPath))
                        }
                        is OperationResult.Failure -> {
                            operationProgress = 0
                            statusMessage = "✗ Quantization failed: ${result.error}"
                            operationDetails = "Error"
                            lastOperationResult = result
                        }
                    }
                }
            
            isOperationInProgress = false
        }
    }
    
    /**
     * Cancel ongoing operation
     */
    fun cancelOperation() {
        currentJob?.cancel()
        isOperationInProgress = false
        operationProgress = 0
        statusMessage = "Operation cancelled"
        operationDetails = "Cancelled"
    }
    
    /**
     * Clear current model
     */
    fun clearModel() {
        currentModel = null
        selectedPath = ""
        statusMessage = "Ready"
        lastOperationResult = null
    }
    
    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}
