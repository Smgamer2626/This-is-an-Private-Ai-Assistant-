package com.example

import com.example.data.network.ChatMessagePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun chatMessagePayload_isCorrect() {
        val payload = ChatMessagePayload(role = "user", content = "Explain Ohm's Law")
        assertEquals("user", payload.role)
        assertEquals("Explain Ohm's Law", payload.content)
    }

    @Test
    fun defaultHost_isValid() {
        val host = "http://127.0.0.1:8080"
        assertTrue(host.startsWith("http://"))
        assertTrue(host.contains("8080"))
    }
}
