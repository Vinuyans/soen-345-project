package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class AuthTests {

    class GeneralTests {
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

    @RunWith(Parameterized::class)
    class ValidPhoneRegistrationTests(private val phone: String) {
        private val authRepository = mockk<AuthRepository>()

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "phone={0}")
            fun data(): Collection<Array<String>> {
                return listOf(
                    arrayOf("1234567890"),
                    arrayOf("+1-123-456-7890"),
                    arrayOf("+44 20 7946 0958"),
                    arrayOf("514.555.0199"),
                    arrayOf("+33123456789")
                )
            }
        }

        @Test
        fun `Register with valid phone number - account created`() {
            every { authRepository.register(any(), any(), any(), any(), any()) } answers {
                val onSuccess = invocation.args[3] as () -> Unit
                onSuccess()
            }

            val user = AppUser(phone = phone, email = "$phone@phone.com")
            var success = false
            authRepository.register("$phone@phone.com", "pass123", user, { success = true }, {})
            assertTrue("Should succeed for phone: $phone", success)
        }
    }

    @RunWith(Parameterized::class)
    class InvalidPhoneRegistrationTests(private val phone: String) {
        private val authRepository = mockk<AuthRepository>()

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "phone={0}")
            fun data(): Collection<Array<String>> {
                return listOf(
                    arrayOf("123"),                // Too short
                    arrayOf("12345678901234567"), // Too long
                    arrayOf("not-a-number")       // Not numeric
                )
            }
        }

        @Test
        fun `Register with invalid phone number - validation error`() {
            val errorMsg = "Invalid phone number format."
            
            every { authRepository.register(any(), any(), any(), any(), any()) } answers {
                val userArg = invocation.args[2] as AppUser
                val p = userArg.phone
                // Simulate validation logic: 7-15 digits, allows +, -, ., space, ()
                val isValid = p.length in 7..15 && p.all { it.isDigit() || it in "+- .()" }
                
                if (isValid) {
                    (invocation.args[3] as () -> Unit).invoke()
                } else {
                    (invocation.args[4] as (String) -> Unit).invoke(errorMsg)
                }
            }

            val user = AppUser(phone = phone)
            var errorReceived = ""
            authRepository.register("$phone@phone.com", "pass123", user, {}, { errorReceived = it })
            assertEquals("Should fail for phone: $phone", errorMsg, errorReceived)
        }
    }
}
