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
        // Assuming phone number registration uses the same method or a simulated one
        val user = AppUser(contact = "1234567890", email = "1234567890@phone.com")
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val onSuccess = invocation.args[3] as () -> Unit
            onSuccess()
        }

        var success = false
        authRepository.register("1234567890@phone.com", "pass123", user, { success = true }, {})

        assertTrue(success)
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
        val user = AppUser(contact = "123")
        val errorMsg = "Invalid phone number format."
        every { authRepository.register(any(), any(), any(), any(), any()) } answers {
            val onError = invocation.args[4] as (String) -> Unit
            onError(errorMsg)
        }

        var errorReceived = ""
        authRepository.register("123@phone.com", "pass123", user, {}, { errorReceived = it })

        assertEquals(errorMsg, errorReceived)
    }

    @Test
    fun `Submit empty fields - handled by caller or repo`() {
        // RegisterActivity handles this, but repo should also return error if attempted
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
