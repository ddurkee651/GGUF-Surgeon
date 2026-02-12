package com.ggufsurgeon.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize  // ✅ ADD THIS IMPORT

@Parcelize
data class ModelFile(
    val name: String,
    val architecture: String,
    val contextLength: Int,
    val ropeBase: Float,
    val ropeScaling: Float,
    val quantization: String,
    val tensorCount: Int,
    val tokenizer: String,
    val metadata: Map<String, String>,
    val tensors: List<TensorInfo>,
    val fileSize: Long = 0,
    val filePath: String = "",
    val validationWarnings: List<String> = emptyList()
) : Parcelable
