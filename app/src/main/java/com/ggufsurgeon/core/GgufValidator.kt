package com.ggufsurgeon.core

import com.ggufsurgeon.domain.ModelFile
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.ByteBuffer

class GgufValidator {
    
    companion object {
        private const val GGUF_MAGIC = 0x46554747
        private const val GGUF_VERSION = 3
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )
    
    fun validate(file: File): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                
                // Check magic
                channel.read(buffer.clear().limit(4))
                val magic = buffer.getInt(0)
                if (magic != GGUF_MAGIC) {
                    errors += "Invalid GGUF magic number: expected $GGUF_MAGIC, got $magic"
                }
                
                // Check version
                channel.read(buffer.clear().limit(4))
                val version = buffer.getInt(0)
                if (version != GGUF_VERSION) {
                    warnings += "Unusual GGUF version: $version (expected $GGUF_VERSION)"
                }
                
                // Check tensor count
                channel.read(buffer.clear().limit(8))
                val tensorCount = buffer.getLong(0)
                if (tensorCount < 0) {
                    errors += "Invalid tensor count: $tensorCount"
                }
                
                // Check metadata count
                channel.read(buffer.clear().limit(8))
                val metadataCount = buffer.getLong(0)
                if (metadataCount < 0) {
                    errors += "Invalid metadata count: $metadataCount"
                }
                
                // Verify tensor alignment
                var currentPos = channel.position()
                for (i in 0 until metadataCount) {
                    // Read key
                    channel.position(currentPos)
                    val keyLength = readVarint(channel)
                    currentPos += 8 + keyLength
                    
                    // Read value type
                    channel.position(currentPos)
                    val valueType = readByte(channel)
                    currentPos += 1
                    
                    // Skip value based on type
                    currentPos += when (valueType.toInt()) {
                        0, 1, 2, 3, 4, 5, 6, 7 -> 4 // int32/float32/bool
                        8 -> { // string
                            channel.position(currentPos)
                            val strLen = readVarint(channel)
                            8 + strLen
                        }
                        else -> 0
                    }
                }
                
                // Check if we can read tensor info
                channel.position(currentPos)
                if (tensorCount > 0) {
                    val tensorInfoSize = 4 + 8 + 8 // name_len + dims_count + offset
                    for (i in 0 until tensorCount) {
                        val nameLength = readVarint(channel)
                        val dimsCount = readVarint(channel)
                        channel.position(channel.position() + dimsCount * 8 + 4 + 8) // dims + type + offset
                    }
                }
            }
        } catch (e: Exception) {
            errors += "Failed to parse GGUF structure: ${e.message}"
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    fun validateModel(model: ModelFile): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (model.name.isBlank()) {
            errors += "Model name cannot be empty"
        }
        
        if (model.contextLength <= 0) {
            errors += "Context length must be positive, got ${model.contextLength}"
        }
        
        if (model.contextLength > 131072) {
            warnings += "Context length ${model.contextLength} is very large"
        }
        
        if (model.ropeBase <= 0) {
            errors += "RoPE base frequency must be positive, got ${model.ropeBase}"
        }
        
        if (model.ropeScaling <= 0) {
            errors += "RoPE scaling factor must be positive, got ${model.ropeScaling}"
        }
        
        if (model.tensorCount != model.tensors.size) {
            errors += "Tensor count mismatch: metadata=${model.tensorCount}, actual=${model.tensors.size}"
        }
        
        // Validate tensor sizes
        var totalTensorBytes = 0L
        model.tensors.forEach { tensor ->
            val expectedBytes = tensor.shape.fold(1L) { acc, dim -> acc * dim } * 
                               when (tensor.type) {
                                   "F32" -> 4L
                                   "F16" -> 2L
                                   "Q4_0" -> 1L
                                   "Q4_1" -> 2L
                                   "Q5_0" -> 1L
                                   "Q5_1" -> 2L
                                   "Q8_0" -> 1L
                                   else -> 4L
                               }
            
            if (tensor.bytes != expectedBytes) {
                warnings += "Tensor ${tensor.name} size mismatch: reported=${tensor.bytes}, calculated=$expectedBytes"
            }
            totalTensorBytes += tensor.bytes
        }
        
        if (totalTensorBytes > model.fileSize) {
            errors += "Total tensor size exceeds file size: ${totalTensorBytes} > ${model.fileSize}"
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun readVarint(channel: FileChannel): Long {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(buffer.clear().limit(8))
        return buffer.getLong(0)
    }
    
    private fun readByte(channel: FileChannel): Byte {
        val buffer = ByteBuffer.allocate(1)
        channel.read(buffer.clear())
        return buffer.get(0)
    }
}
