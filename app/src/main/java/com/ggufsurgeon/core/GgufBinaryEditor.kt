package com.ggufsurgeon.core

import com.ggufsurgeon.domain.ModelFile
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Real GGUF binary editor that directly modifies the file structure
 */
class GgufBinaryEditor {
    
    companion object {
        private const val GGUF_MAGIC = 0x46554747 // "GGUF"
        private const val GGUF_VERSION = 3
    }
    
    data class GgufHeader(
        val magic: Int,
        val version: Int,
        val tensorCount: Long,
        val metadataKvCount: Long
    )
    
    /**
     * Edit metadata and save as new file with proper GGUF structure
     */
    fun editMetadata(
        originalFile: File,
        outputFile: File,
        updates: Map<String, String>
    ): Result<File> = runCatching {
        // Copy original file
        originalFile.copyTo(outputFile, overwrite = true)
        
        RandomAccessFile(outputFile, "rw").use { raf ->
            val channel = raf.channel
            val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            
            // Read and verify header
            channel.position(0)
            channel.read(buffer.clear().limit(4))
            val magic = buffer.getInt(0)
            require(magic == GGUF_MAGIC) { "Invalid GGUF magic number" }
            
            channel.read(buffer.clear().limit(4))
            val version = buffer.getInt(0)
            require(version == GGUF_VERSION) { "Unsupported GGUF version: $version" }
            
            // Read tensor count and metadata count
            channel.read(buffer.clear().limit(8))
            val tensorCount = buffer.getLong(0)
            
            channel.read(buffer.clear().limit(8))
            val metadataCount = buffer.getLong(0)
            
            // Find and update metadata KV pairs
            var currentPos = channel.position()
            
            for (i in 0 until metadataCount) {
                // Read key
                channel.position(currentPos)
                val key = readString(channel)
                currentPos = channel.position()
                
                // Read value type and data
                channel.position(currentPos)
                val valueType = readInt(channel)
                currentPos = channel.position()
                
                val value = when (valueType) {
                    8 -> { // String type
                        channel.position(currentPos)
                        readString(channel)
                    }
                    4 -> { // Int32
                        channel.position(currentPos)
                        readInt(channel).toString()
                    }
                    5 -> { // Float32
                        channel.position(currentPos)
                        readFloat(channel).toString()
                    }
                    6 -> { // Bool
                        channel.position(currentPos)
                        (readByte(channel) != 0.toByte()).toString()
                    }
                    else -> {
                        // Skip unknown type
                        channel.position(currentPos + 8)
                        ""
                    }
                }
                currentPos = channel.position()
                
                // Update if key matches
                if (updates.containsKey(key)) {
                    val newValue = updates[key]!!
                    writeUpdatedMetadata(channel, currentPos - value.length.toLong(), key, newValue)
                }
            }
        }
        
        outputFile
    }
    
    private fun readString(channel: FileChannel): String {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(buffer.clear().limit(8))
        val length = buffer.getLong(0)
        
        val strBuffer = ByteBuffer.allocate(length.toInt())
        channel.read(strBuffer.clear())
        return String(strBuffer.array(), Charsets.UTF_8)
    }
    
    private fun readInt(channel: FileChannel): Int {
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(buffer.clear())
        return buffer.getInt(0)
    }
    
    private fun readFloat(channel: FileChannel): Float {
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(buffer.clear())
        return buffer.getFloat(0)
    }
    
    private fun readByte(channel: FileChannel): Byte {
        val buffer = ByteBuffer.allocate(1)
        channel.read(buffer.clear())
        return buffer.get(0)
    }
    
    private fun writeUpdatedMetadata(channel: FileChannel, position: Long, key: String, value: String) {
        channel.position(position)
        
        // Write new value as string (type 8)
        val buffer = ByteBuffer.allocate(1 + 8 + value.toByteArray().size)
            .order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.put(8.toByte()) // String type
        buffer.putLong(value.toByteArray().size.toLong())
        buffer.put(value.toByteArray())
        buffer.flip()
        
        channel.write(buffer)
    }
    
    fun validateGgufStructure(file: File): List<String> {
        val errors = mutableListOf<String>()
        
        try {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                
                // Check magic
                channel.read(buffer.clear().limit(4))
                if (buffer.getInt(0) != GGUF_MAGIC) {
                    errors += "Invalid GGUF magic number"
                }
                
                // Check version
                channel.read(buffer.clear().limit(4))
                val version = buffer.getInt(0)
                if (version != GGUF_VERSION) {
                    errors += "Unsupported version: $version"
                }
                
                // Check file size consistency
                val fileSize = file.length()
                val headerSize = channel.position()
                
                if (fileSize < headerSize) {
                    errors += "File size smaller than header"
                }
            }
        } catch (e: Exception) {
            errors += "Failed to read GGUF structure: ${e.message}"
        }
        
        return errors
    }
}
