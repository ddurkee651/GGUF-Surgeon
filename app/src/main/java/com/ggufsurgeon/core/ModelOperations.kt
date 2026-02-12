package com.ggufsurgeon.core

import com.ggufsurgeon.domain.OperationResult
import java.io.File

class ModelOperations {
    fun mergeLora(baseModel: File, loraAdapter: File, alpha: Float, rank: Int, scaling: Float): OperationResult {
        if (!baseModel.exists() || !loraAdapter.exists()) {
            return OperationResult.Failure("Base model or LoRA adapter not found")
        }
        if (alpha <= 0 || rank <= 0 || scaling <= 0) {
            return OperationResult.Failure("Merge parameters must be positive")
        }

        val output = File(baseModel.parentFile, "${baseModel.nameWithoutExtension}-merged.gguf")
        baseModel.copyTo(output, overwrite = true)
        output.appendText("\n# merged_with=${loraAdapter.name},alpha=$alpha,rank=$rank,scaling=$scaling\n")
        return OperationResult.Success(output.absolutePath, "LoRA merge simulated successfully")
    }

    fun optimizeModel(model: File, targetQuantization: String, dropTensorPrefixes: List<String>): OperationResult {
        if (!model.exists()) return OperationResult.Failure("Model file not found")
        if (targetQuantization.isBlank()) return OperationResult.Failure("Target quantization cannot be blank")

        val output = File(model.parentFile, "${model.nameWithoutExtension}-${targetQuantization.lowercase()}.gguf")
        model.copyTo(output, overwrite = true)
        output.appendText("\n# optimized_quant=$targetQuantization\n")
        if (dropTensorPrefixes.isNotEmpty()) {
            output.appendText("# dropped_tensors=${dropTensorPrefixes.joinToString(",")}\n")
        }
        return OperationResult.Success(output.absolutePath, "Optimization simulated successfully")
    }
}
