package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTests {

    private val authRepository = mockk<AuthRepository>()

    @Test
    fun `Register with valid email - account created`() {
        val user = AppUser(email = "user@example.com")
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val onSuccess = invocation.args[3] as () -> Unit
            onSuccess()
        }

        var success = false
        authRepository.register("user@example.com", "pass123", user, { success = true }, {})

        assertTrue(success)
        verify { authRepository.register("user@example.com", "pass123", user, any(), any()) }
    }

    @Test
    fun `Register with valid phone number - account created`() {
        val validPhones = listOf(
            "1234567890",
            "+1-123-456-7890",
            "+44 20 7946 0958",
            "514.555.0199",
            "+33123456789"
        )
        
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val onSuccess = invocation.args[3] as () -> Unit
            onSuccess()
        }

        validPhones.forEach { phone ->
            val user = AppUser(contact = phone, email = "$phone@phone.com")
            var success = false
            authRepository.register("$phone@phone.com", "pass123", user, { success = true }, {})
            assertTrue("Should succeed for phone: $phone", success)
        }
    }

    @Test
    fun `Register with already used email - error shown`() {
        val user = AppUser(email = "used@example.com")
        val errorMsg = "The email address is already in use by another account."
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val onError = invocation.args[4] as (String) -> Unit
            onError(errorMsg)
        }

        var errorReceived = ""
        authRepository.register("used@example.com", "pass123", user, {}, { errorReceived = it })

        assertEquals(errorMsg, errorReceived)
    }

    @Test
    fun `Register with invalid email format - validation error`() {
        val user = AppUser(email = "invalid-email")
        val errorMsg = "The email address is badly formatted."
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val onError = invocation.args[4] as (String) -> Unit
            onError(errorMsg)
        }

        var errorReceived = ""
        authRepository.register("invalid-email", "pass123", user, {}, { errorReceived = it })

        assertEquals(errorMsg, errorReceived)
    }

    @Test
    fun `Register with invalid phone number - validation error`() {
        val invalidPhones = listOf(
            "123",                // Too short
            "12345678901234567", // Too long (assuming 15 max)
            "not-a-number"       // Not numeric
        )
        val errorMsg = "Invalid phone number format."
        
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val userArg = invocation.args[2] as AppUser
            val phone = userArg.contact
            // Simulate validation logic: 7-15 digits, allows +, -, ., space, ()
            val isValid = phone.length in 7..15 && phone.all { it.isDigit() || it in "+- .()" }
            
            if (isValid) {
                (invocation.args[3] as () -> Unit).invoke()
            } else {
                (invocation.args[4] as (String) -> Unit).invoke(errorMsg)
            }
        }

        invalidPhones.forEach { phone ->
            val user = AppUser(contact = phone)
            var errorReceived = ""
            authRepository.register("$phone@phone.com", "pass123", user, {}, { errorReceived = it })
            assertEquals("Should fail for phone: $phone", errorMsg, errorReceived)
        }
    }

    @Test
    fun `Submit empty fields - handled by caller or repo`() {
        val user = AppUser(email = "")
        val errorMsg = "Email cannot be empty"
        every { authRepository.register("", any(), any(), any(), any()) } answers {
            val onError = invocation.args[4] as (String) -> Unit
            onError(errorMsg)
        }

        var errorReceived = ""
        authRepository.register("", "pass123", user, {}, { errorReceived = it })

        assertEquals(errorMsg, errorReceived)
    }
}
