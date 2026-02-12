package com.ggufsurgeon.core.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.ggufsurgeon.domain.ModelFile
import com.ggufsurgeon.domain.TensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class PythonGgufBridge(private val context: Context) {
    
    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }
    
    suspend fun inspectModel(file: File): ModelFile = withContext(Dispatchers.IO) {
        val py = Python.getInstance()
        val ggufModule = py.getModule("gguf_android_bridge")
        
        val params = py.call {
            it.put("path", file.absolutePath)
        }
        
        val result = ggufModule.callAttr("process_command", "inspect", params)
        val json = JSONObject(result.toString())
        
        ModelFile(
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
            fileSize = json.getLong("file_size"),
            filePath = file.absolutePath
        )
    }
    
    suspend fun editMetadata(input: File, output: File, updates: Map<String, String>): File = 
        withContext(Dispatchers.IO) {
            val py = Python.getInstance()
            val ggufModule = py.getModule("gguf_android_bridge")
            
            val params = py.call {
                it.put("input", input.absolutePath)
                it.put("output", output.absolutePath)
                val updatesPy = py.call { 
                    updates.forEach { (k, v) -> it.put(k, v) }
                }
                it.put("updates", updatesPy)
            }
            
            ggufModule.callAttr("process_command", "edit", params)
            output
        }
    
    suspend fun mergeLora(base: File, lora: File, alpha: Float, output: File): File = 
        withContext(Dispatchers.IO) {
            val py = Python.getInstance()
            val ggufModule = py.getModule("gguf_android_bridge")
            
            val params = py.call {
                it.put("base", base.absolutePath)
                it.put("lora", lora.absolutePath)
                it.put("alpha", alpha)
                it.put("output", output.absolutePath)
            }
            
            ggufModule.callAttr("process_command", "merge", params)
            output
        }
}
