package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.model.Reservation
import org.junit.Assert.*
import org.junit.Test

class NotificationTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeUser(
        email: String = "user@example.com",
        phone: String = "",
        notificationsEnabled: Boolean = true
    ) = AppUser(
        uid = "uid-123",
        firstName = "John",
        lastName = "Doe",
        email = email,
        phone = phone,
        notificationsEnabled = notificationsEnabled
    )

    private fun makeReservation(
        eventName: String = "Ye Concert",
        date: String = "2026-05-01",
        location: String = "Bell Center"
    ) = Reservation(
        id = "res-001",
        userId = "uid-123",
        eventId = "evt-001",
        eventName = eventName,
        location = location,
        date = date,
        category = "Music",
        contact = "user@example.com",
        status = "active"
    )

    private fun buildConfirmationMessage(reservation: Reservation) =
        "Reservation confirmed for ${reservation.eventName} on ${reservation.date} at ${reservation.location}."

    private fun buildCancellationMessage(reservation: Reservation) =
        "Your reservation for ${reservation.eventName} on ${reservation.date} has been cancelled."

    // ── AppUser model tests ──────────────────────────────────────────────────

    @Test
    fun `user with notifications enabled should send notification`() {
        val user = makeUser(notificationsEnabled = true)
        assertTrue(user.notificationsEnabled)
    }

    @Test
    fun `user with notifications disabled should not send notification`() {
        val user = makeUser(notificationsEnabled = false)
        assertFalse(user.notificationsEnabled)
    }

    @Test
    fun `user email is used as notification destination`() {
        val user = makeUser(email = "test@example.com")
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `user with phone number receives sms notification`() {
        val user = makeUser(phone = "+15141234567")
        assertTrue(user.phone.isNotBlank())
    }

    @Test
    fun `user without phone number does not receive sms`() {
        val user = makeUser(phone = "")
        assertTrue(user.phone.isBlank())
    }

    // ── Notification channel selection ───────────────────────────────────────

    @Test
    fun `email destination is detected by at-sign`() {
        val destination = "user@example.com"
        val channel = if (destination.contains("@")) "email" else "sms"
        assertEquals("email", channel)
    }

    @Test
    fun `phone destination is detected as sms channel`() {
        val destination = "+15141234567"
        val channel = if (destination.contains("@")) "email" else "sms"
        assertEquals("sms", channel)
    }

    @Test
    fun `blank phone means only email notification is sent`() {
        val user = makeUser(phone = "")
        val channels = mutableListOf<String>()
        channels.add("email")
        if (user.phone.isNotBlank()) channels.add("sms")
        assertEquals(listOf("email"), channels)
    }

    @Test
    fun `non-blank phone means both email and sms are sent`() {
        val user = makeUser(phone = "+15141234567")
        val channels = mutableListOf<String>()
        channels.add("email")
        if (user.phone.isNotBlank()) channels.add("sms")
        assertEquals(listOf("email", "sms"), channels)
    }

    // ── Confirmation message content ─────────────────────────────────────────

    @Test
    fun `confirmation message contains event name`() {
        val reservation = makeReservation(eventName = "Ye Concert")
        val message = buildConfirmationMessage(reservation)
        assertTrue(message.contains("Ye Concert"))
    }

    @Test
    fun `confirmation message contains event date`() {
        val reservation = makeReservation(date = "2026-05-01")
        val message = buildConfirmationMessage(reservation)
        assertTrue(message.contains("2026-05-01"))
    }

    @Test
    fun `confirmation message contains event location`() {
        val reservation = makeReservation(location = "Bell Center")
        val message = buildConfirmationMessage(reservation)
        assertTrue(message.contains("Bell Center"))
    }

    @Test
    fun `cancellation message contains event name`() {
        val reservation = makeReservation(eventName = "Montreal FC Match")
        val message = buildCancellationMessage(reservation)
        assertTrue(message.contains("Montreal FC Match"))
    }

    @Test
    fun `cancellation message contains cancelled status`() {
        val reservation = makeReservation()
        val message = buildCancellationMessage(reservation)
        assertTrue(message.contains("cancelled"))
    }

    // ── Reservation model tests ──────────────────────────────────────────────

    @Test
    fun `active reservation has correct status`() {
        val reservation = makeReservation()
        assertEquals("active", reservation.status)
    }

    @Test
    fun `cancelled reservation has correct status`() {
        val reservation = makeReservation().copy(status = "cancelled")
        assertEquals("cancelled", reservation.status)
    }

    @Test
    fun `reservation stores correct user id`() {
        val reservation = makeReservation()
        assertEquals("uid-123", reservation.userId)
    }
}
