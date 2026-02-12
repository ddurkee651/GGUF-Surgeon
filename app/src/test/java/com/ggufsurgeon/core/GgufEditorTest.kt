package com.ggufsurgeon.core

import com.ggufsurgeon.domain.ModelFile
import org.junit.Assert.assertEquals
import org.junit.Test

class GgufEditorTest {
    @Test
    fun `updateMetadata changes safe fields only`() {
        val model = ModelFile(
            name = "base",
            architecture = "llama",
            contextLength = 4096,
            ropeBase = 10000f,
            ropeScaling = 1.0f,
            quantization = "Q4_K",
            tensorCount = 0,
            tokenizer = "llama",
            metadata = emptyMap(),
            tensors = emptyList()
        )

        val updated = GgufEditor().updateMetadata(model, mapOf("general.name" to "patched", "llama.context_length" to "8192"))

        assertEquals("patched", updated.name)
        assertEquals(8192, updated.contextLength)
    }
}
