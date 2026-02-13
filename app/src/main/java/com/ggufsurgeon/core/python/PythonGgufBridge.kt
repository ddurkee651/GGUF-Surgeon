package com.ggufsurgeon.core.python

import android.content.Context
import com.ggufsurgeon.domain.ModelFile
import com.ggufsurgeon.domain.TensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Bridge to Python GGUF tools using ProcessBuilder to call Python directly
 * This assumes Python is available on the system (Termux) or you're using the Python binary from assets
 */
class PythonGgufBridge(private val context: Context) {
    
    private val pythonScriptPath: String by lazy {
        // Copy script from assets to files directory
        val scriptFile = File(context.filesDir, "gguf_android_bridge.py")
        if (!scriptFile.exists()) {
            context.assets.open("python/gguf_android_bridge.py").use { input ->
                scriptFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            scriptFile.setExecutable(true)
        }
        scriptFile.absolutePath
    }
    
    suspend fun inspectModel(file: File): Result<ModelFile> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(
                "python3",
                pythonScriptPath,
                "inspect",
                file.absolutePath
            ).start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                val json = JSONObject(output)
                Result.success(parseModelJson(json, file))
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun mergeLora(
        baseModel: File,
        loraAdapter: File,
        alpha: Float,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(
                "python3",
                pythonScriptPath,
                "merge",
                baseModel.absolutePath,
                loraAdapter.absolutePath,
                alpha.toString(),
                outputFile.absolutePath
            ).start()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Result.success(outputFile)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseModelJson(json: JSONObject, file: File): ModelFile {
        return ModelFile(
            name = json.getString("name"),
            architecture = json.getString("architecture"),
            contextLength = json.getInt("context_length"),
            ropeBase = json.getDouble("rope_base").toFloat(),
            ropeScaling = json.getDouble("rope_scaling").toFloat(),
            quantization = json.getString("quantization"),
            tensorCount = json.getInt("tensor_count"),
            tokenizer = json.getString("tokenizer"),
            metadata = json.getJSONObject("metadata").let { meta ->
                meta.keys().asSequence().associateWith { meta.getString(it) }
            },
            tensors = json.getJSONArray("tensors").let { array ->
                (0 until array.length()).map { i ->
                    val t = array.getJSONObject(i)
                    TensorInfo(
                        name = t.getString("name"),
                        shape = t.getJSONArray("shape").let { shape ->
                            (0 until shape.length()).map { shape.getInt(it) }
                        },
                        type = t.getString("type"),
                        bytes = t.getLong("bytes")
                    )
                }
            },
            fileSize = file.length(),
            filePath = file.absolutePath,
            validationWarnings = emptyList()
        )
    }
}
