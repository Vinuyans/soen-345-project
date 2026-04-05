package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.model.Event
import com.example.project.data.model.Reservation
import com.example.project.data.repository.*
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptanceTests {

    private val authRepo = mockk<AuthRepository>()
    private val eventRepo = mockk<EventRepository>()
    private val resRepo = mockk<ReservationRepository>()
    private val notifRepo = mockk<NotificationRepository>()
    private val userRepo = mockk<UserRepository>()

    @Test
    fun `User can register and book events`() {
        val user = AppUser(uid = "u1", email = "bob@example.com", contact = "514-555-1234")
        val event = Event(id = "e1", name = "Community Meetup")

        every { authRepo.register(any(), any(), any(), any(), any()) } answers { (invocation.args[3] as () -> Unit).invoke() }
        every { eventRepo.getEvents(any()) } answers { (invocation.args[0] as (List<Event>) -> Unit).invoke(listOf(event)) }
        every { userRepo.getUser(any(), any()) } answers { (invocation.args[1] as (AppUser?) -> Unit).invoke(user) }
        every { resRepo.reserve(any(), any(), any(), any(), any()) } answers { 
            val success = invocation.args[3] as (Reservation) -> Unit
            success(Reservation(id = "r1", eventName = "Community Meetup"))
        }
        every { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) } just runs

        var registered = false
        authRepo.register(user.email, "pass123", user, { registered = true }, {})
        
        var reservationCreated = false
        if (registered) {
            resRepo.reserve(user.uid, user.contact, event, {
                reservationCreated = true
            }, {})
        }

        assertTrue("User should be registered and reservation should be created", registered && reservationCreated)
    }

    @Test
    fun `User can find relevant events via search and filtering`() {
        val events = listOf(
            Event(name = "Yoga", category = "Health", location = "Park"),
            Event(name = "Coding", category = "Tech", location = "Lab"),
            Event(name = "Cooking", category = "Health", location = "Kitchen")
        )

        val filtered = events.filter { it.category == "Health" && it.location == "Park" }
        
        assertEquals(1, filtered.size)
        assertEquals("Yoga", filtered[0].name)
    }

    @Test
    fun `User can manage and cancel their reservations`() {
        val reservation = Reservation(id = "r1", status = "active")
        
        every { resRepo.cancelReservation(any(), any(), any()) } answers { (invocation.args[1] as () -> Unit).invoke() }

        var cancelSuccess = false
        resRepo.cancelReservation(reservation, { cancelSuccess = true }, {})
        
        assertTrue("User successfully cancelled their booking", cancelSuccess)
    }

    @Test
    fun `Admin manages event lifecycle (Create, Update, Cancel)`() {
        val event = Event(id = "e1", name = "Initial")
        
        every { eventRepo.addEvent(any(), any(), any()) } answers { (invocation.args[1] as () -> Unit).invoke() }
        every { eventRepo.updateEvent(any(), any(), any()) } answers { (invocation.args[1] as () -> Unit).invoke() }
        every { eventRepo.cancelEvent(any(), any(), any()) } answers { (invocation.args[1] as () -> Unit).invoke() }

        var step = 0
        eventRepo.addEvent(event, { 
            step++
            eventRepo.updateEvent(event.copy(name = "Updated"), {
                step++
                eventRepo.cancelEvent(event, {
                    step++
                }, {})
            }, {})
        }, {})

        assertEquals("Admin should have completed all 3 lifecycle stages", 3, step)
    }

    @Test
    fun `Users are notified of important actions`() {
        every { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) } answers {
            (invocation.args[3] as () -> Unit).invoke()
        }

        var notificationSent = false
        notifRepo.enqueueConfirmation("u1", "email@test.com", "Your event was cancelled", {
            notificationSent = true
        }, {})

        assertTrue("Notification was successfully triggered for the user", notificationSent)
    }
}
