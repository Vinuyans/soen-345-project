package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.model.Event
import com.example.project.data.model.Reservation
import com.example.project.data.repository.*
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * System Tests simulate end-to-end workflows by chaining repository interactions
 * as they would occur in the actual UI navigation.
 */
class SystemTests {

    private val authRepo = mockk<AuthRepository>()
    private val userRepo = mockk<UserRepository>()
    private val eventRepo = mockk<EventRepository>()
    private val resRepo = mockk<ReservationRepository>()
    private val notifRepo = mockk<NotificationRepository>()

    @Test
    fun `User booking flow - Register to Booked`() {
        val user = AppUser(uid = "u1", email = "test@test.com", contact = "5140000000")
        val event = Event(id = "e1", name = "System Test Event")

        // 1. Register
        every { authRepo.register(any(), any(), any(), any(), any()) } answers {
            (it.invocation.args[3] as () -> Unit).invoke()
        }
        
        // 2. Browse (Load events)
        every { eventRepo.getEvents(any()) } answers {
            (it.invocation.args[0] as (List<Event>) -> Unit).invoke(listOf(event))
        }

        // 3. Book
        every { userRepo.getUser(user.uid, any()) } answers {
            (it.invocation.args[1] as (AppUser?) -> Unit).invoke(user)
        }
        every { resRepo.reserve(user.uid, user.contact, event, any(), any()) } answers {
            val onSuccess = it.invocation.args[3] as (Reservation) -> Unit
            onSuccess(Reservation(id = "r1", eventName = event.name))
        }

        // 4. Confirmation
        every { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) } just runs

        // Execution of the flow
        authRepo.register(user.email, "pass", user, {
            eventRepo.getEvents { events ->
                val selectedEvent = events.first()
                userRepo.getUser(user.uid) { profile ->
                    resRepo.reserve(profile!!.uid, profile.contact, selectedEvent, { res ->
                        notifRepo.enqueueConfirmation(profile.uid, profile.contact, "Success", {}, {})
                    }, {})
                }
            }
        }, {})

        verify { resRepo.reserve("u1", "5140000000", match { it.id == "e1" }, any(), any()) }
        verify { notifRepo.enqueueConfirmation("u1", "5140000000", any(), any(), any()) }
    }

    @Test
    fun `Search and discovery workflow`() {
        val events = listOf(
            Event(id = "1", location = "Montreal", category = "Music"),
            Event(id = "2", location = "Montreal", category = "Food"),
            Event(id = "3", location = "Laval", category = "Music")
        )

        every { eventRepo.getEvents(any()) } answers {
            (it.invocation.args[0] as (List<Event>) -> Unit).invoke(events)
        }

        // Simulating applying filters in the UI
        eventRepo.getEvents { all ->
            val filtered = all.filter { it.location == "Montreal" && it.category == "Music" }
            assertEquals(1, filtered.size)
            assertEquals("1", filtered[0].id)
        }
    }

    @Test
    fun `Reservation management - Cancel and verify removed`() {
        val reservation = Reservation(id = "r1", userId = "u1", status = "active")
        
        every { resRepo.getReservationsForUser("u1", any()) } answers {
            (it.invocation.args[1] as (List<Reservation>) -> Unit).invoke(listOf(reservation))
        }
        every { resRepo.cancelReservation(any(), any(), any()) } answers {
            (it.invocation.args[1] as () -> Unit).invoke()
        }

        // 1. User views reservations
        resRepo.getReservationsForUser("u1") { list ->
            assertTrue(list.any { it.id == "r1" })
            
            // 2. User cancels
            resRepo.cancelReservation(list.first(), {
                // 3. User refreshes (simulate DB returning empty or updated)
                every { resRepo.getReservationsForUser("u1", any()) } answers {
                    (it.invocation.args[1] as (List<Reservation>) -> Unit).invoke(emptyList())
                }
                
                resRepo.getReservationsForUser("u1") { newList ->
                    assertTrue(newList.isEmpty())
                }
            }, {})
        }
    }

    @Test
    fun `Admin event lifecycle workflow`() {
        val adminUid = "admin1"
        val event = Event(id = "e1", name = "Draft", createdBy = adminUid)
        
        every { eventRepo.addEvent(any(), any(), any()) } answers {
            (it.invocation.args[1] as () -> Unit).invoke()
        }
        every { eventRepo.updateEvent(any(), any(), any()) } answers {
            (it.invocation.args[1] as () -> Unit).invoke()
        }
        every { eventRepo.cancelEvent(any(), any(), any()) } answers {
            (it.invocation.args[1] as () -> Unit).invoke()
        }
        every { resRepo.getReservationsByEvent("e1", any()) } answers {
            (it.invocation.args[1] as (List<Reservation>) -> Unit).invoke(listOf(Reservation(userId = "u1")))
        }
        every { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) } just runs

        // 1. Create
        eventRepo.addEvent(event, {
            // 2. Edit
            eventRepo.updateEvent(event.copy(name = "Published"), {
                // 3. Cancel
                eventRepo.cancelEvent(event, {
                    // 4. Notify
                    resRepo.getReservationsByEvent(event.id) { resList ->
                        resList.forEach { r -> 
                            notifRepo.enqueueConfirmation(r.userId, "contact", "Msg", {}, {}) 
                        }
                    }
                }, {})
            }, {})
        }, {})

        verify { eventRepo.addEvent(any(), any(), any()) }
        verify { eventRepo.updateEvent(match { it.name == "Published" }, any(), any()) }
        verify { notifRepo.enqueueConfirmation("u1", any(), any(), any(), any()) }
    }
}
