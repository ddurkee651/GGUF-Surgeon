package com.ggufsurgeon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GgufParserTest {
    @Test
    fun `parse reads gguf and exposes defaults`() {
        val tmp = File.createTempFile("sample", ".gguf")
        tmp.writeText("GGUF")

        val parsed = GgufParser().parse(tmp)

        assertEquals("sample", parsed.name)
        assertEquals(4096, parsed.contextLength)
        assertTrue(parsed.metadata.containsKey("file_size"))
    }
}
