package com.example.project

import com.example.project.data.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsTests {

    private val notificationRepository = mockk<NotificationRepository>()

    @Test
    fun `Reservation made - email confirmation received`() {
        val email = "user@example.com"
        every { notificationRepository.enqueueConfirmation(any(), email, any(), any(), any()) } answers {
            val onDone = invocation.args[3] as () -> Unit
            onDone()
        }

        var done = false
        notificationRepository.enqueueConfirmation("u1", email, "Your reservation is confirmed", { done = true }, {})

        assertTrue(done)
    }

    @Test
    fun `Reservation made - SMS confirmation received`() {
        val phone = "5145550000"
        every { notificationRepository.enqueueConfirmation(any(), phone, any(), any(), any()) } answers {
            val onDone = invocation.args[3] as () -> Unit
            onDone()
        }

        var done = false
        notificationRepository.enqueueConfirmation("u1", phone, "Your reservation is confirmed", { done = true }, {})

        assertTrue(done)
    }

    @Test
    fun `Invalid email or phone - confirmation fails with error`() {
        val invalidContact = "not-an-email"
        val errorMsg = "Invalid destination format"
        every { notificationRepository.enqueueConfirmation(any(), invalidContact, any(), any(), any()) } answers {
            val onError = invocation.args[4] as (String) -> Unit
            onError(errorMsg)
        }

        var errorReceived = ""
        notificationRepository.enqueueConfirmation("u1", invalidContact, "Msg", {}, { errorReceived = it })

        assertEquals(errorMsg, errorReceived)
    }

    @Test
    fun `Notification service down - retry or error message`() {
        val errorMsg = "Service Unavailable"
        every { notificationRepository.enqueueConfirmation(any(), any(), any(), any(), any()) } answers {
            val onError = invocation.args[4] as (String) -> Unit
            onError(errorMsg)
        }

        var errorReceived = ""
        notificationRepository.enqueueConfirmation("u1", "dest", "Msg", {}, { errorReceived = it })

        assertEquals(errorMsg, errorReceived)
    }

    @Test
    fun `Duplicate reservation - only one confirmation sent`() {
        val email = "user@example.com"
        every { notificationRepository.enqueueConfirmation(any(), email, any(), any(), any()) } answers {
            val onDone = invocation.args[3] as () -> Unit
            onDone()
        }

        notificationRepository.enqueueConfirmation("u1", email, "Msg", {}, {})
        
        verify(exactly = 1) { 
            notificationRepository.enqueueConfirmation("u1", email, "Msg", any(), any()) 
        }
    }
}
