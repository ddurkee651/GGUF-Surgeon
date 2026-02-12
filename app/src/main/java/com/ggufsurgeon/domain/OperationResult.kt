package com.ggufsurgeon.domain

sealed class OperationResult {
    data class Progress(val percent: Int, val message: String) : OperationResult()
    data class Success(val outputPath: String, val details: String) : OperationResult()
    data class Failure(val error: String) : OperationResult()
}
