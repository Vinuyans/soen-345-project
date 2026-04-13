package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.model.Event
import com.example.project.data.model.NotificationJob
import com.example.project.data.model.UserRole
import com.example.project.data.repository.EventRepository
import com.example.project.data.repository.ReservationRepository
import com.example.project.data.repository.UserRepository
import com.google.firebase.database.DatabaseReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataCoverageTests {

    @Test
    fun `event repository addEvent returns error when push key is missing`() {
        val eventsRef = mockk<DatabaseReference>()
        val pushedRef = mockk<DatabaseReference>()
        val repository = EventRepository(eventsRef)

        every { eventsRef.push() } returns pushedRef
        every { pushedRef.key } returns null

        var successCalled = false
        var errorMessage: String? = null

        repository.addEvent(
            event = Event(name = "Board Games Night"),
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )

        assertTrue(!successCalled)
        assertEquals("Could not create id", errorMessage)
    }

    @Test
    fun `event repository cancelEvent delegates to updateEvent with cancelled flag`() {
        val repository = spyk(EventRepository(mockk(relaxed = true)))
        val event = Event(id = "evt-1", cancelled = false)
        var successCalled = false

        every { repository.updateEvent(any(), any(), any()) } answers {
            val onSuccess = invocation.args[1] as () -> Unit
            onSuccess()
        }

        repository.cancelEvent(event, onSuccess = { successCalled = true }, onError = {})

        verify {
            repository.updateEvent(
                match { it.id == "evt-1" && it.cancelled },
                any(),
                any()
            )
        }
        assertTrue(successCalled)
    }

    @Test
    fun `event repository getEventsByAdmin filters events by creator uid`() {
        val repository = spyk(EventRepository(mockk(relaxed = true)))
        val allEvents = listOf(
            Event(id = "1", name = "A", createdBy = "admin-1"),
            Event(id = "2", name = "B", createdBy = "admin-2"),
            Event(id = "3", name = "C", createdBy = "admin-1")
        )

        every { repository.getEvents(any()) } answers {
            val onResult = invocation.args[0] as (List<Event>) -> Unit
            onResult(allEvents)
        }

        var filtered: List<Event> = emptyList()
        repository.getEventsByAdmin("admin-1") { filtered = it }

        assertEquals(2, filtered.size)
        assertEquals(listOf("1", "3"), filtered.map { it.id })
    }

    @Test
    fun `user repository getUserRole returns ADMIN when role is admin`() {
        val repository = spyk(UserRepository(mockk(relaxed = true)))

        every { repository.getUser("u-1", any()) } answers {
            val onResult = invocation.args[1] as (AppUser?) -> Unit
            onResult(AppUser(uid = "u-1", role = "admin"))
        }

        var role: UserRole? = null
        repository.getUserRole("u-1") { role = it }

        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun `user repository getUserRole falls back to USER for unknown role value`() {
        val repository = spyk(UserRepository(mockk(relaxed = true)))

        every { repository.getUser("u-2", any()) } answers {
            val onResult = invocation.args[1] as (AppUser?) -> Unit
            onResult(AppUser(uid = "u-2", role = "superuser"))
        }

        var role: UserRole? = null
        repository.getUserRole("u-2") { role = it }

        assertEquals(UserRole.USER, role)
    }

    @Test
    fun `user repository getUserRole returns null when user is missing`() {
        val repository = spyk(UserRepository(mockk(relaxed = true)))

        every { repository.getUser("missing", any()) } answers {
            val onResult = invocation.args[1] as (AppUser?) -> Unit
            onResult(null)
        }

        var role: UserRole? = UserRole.ADMIN
        repository.getUserRole("missing") { role = it }

        assertNull(role)
    }

    @Test
    fun `reservation repository reserve returns error when push key is missing`() {
        val reservationRef = mockk<DatabaseReference>()
        val pushedRef = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.push() } returns pushedRef
        every { pushedRef.key } returns null

        var successReservationId: String? = null
        var errorMessage: String? = null

        repository.reserve(
            userId = "u-1",
            contact = "u1@example.com",
            event = Event(id = "e-1", name = "Chess", location = "Montreal", date = "2026-05-10", category = "Games"),
            onSuccess = { successReservationId = it.id },
            onError = { errorMessage = it }
        )

        assertNull(successReservationId)
        assertEquals("Could not create reservation", errorMessage)
    }

    @Test
    fun `notification job defaults to pending status`() {
        val job = NotificationJob(
            id = "n-1",
            userId = "u-1",
            channel = "email",
            destination = "user@example.com",
            message = "Confirmed"
        )

        assertEquals("pending", job.status)
    }

    @Test
    fun `notification job copy can update status to sent`() {
        val pending = NotificationJob(
            id = "n-1",
            userId = "u-1",
            channel = "sms",
            destination = "5145550199",
            message = "Confirmed"
        )

        val sent = pending.copy(status = "sent")

        assertEquals("pending", pending.status)
        assertEquals("sent", sent.status)
    }
}
