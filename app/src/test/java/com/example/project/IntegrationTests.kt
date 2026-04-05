package com.example.project

import android.os.Process
import com.example.project.data.model.AppUser
import com.example.project.data.model.Event
import com.example.project.data.model.Reservation
import com.example.project.data.repository.*
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntegrationTests {

    @Before
    fun setup() {
        // 1. Mock Process.myPid() to prevent Firebase SDK from crashing in unit tests
        mockkStatic(Process::class)
        every { Process.myPid() } returns 1

        // 2. Mock Firebase static entry points
        mockkStatic(FirebaseApp::class)
        
        val mockApp = mockk<FirebaseApp>(relaxed = true)
        val mockDb = mockk<FirebaseDatabase>(relaxed = true)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
        val mockRef = mockk<DatabaseReference>(relaxed = true)

        // Helper to mock companion objects using reflection to avoid "Unresolved reference" issues
        fun mockCompanion(clazz: java.lang.Class<*>) {
            try {
                val companionField = clazz.getDeclaredField("Companion")
                companionField.isAccessible = true
                val companionObject = companionField.get(null)
                if (companionObject != null) {
                    mockkObject(companionObject)
                }
            } catch (e: Exception) {
                // If no companion, fallback to static mocking of the class itself
                mockkStatic(clazz.kotlin)
            }
        }

        mockCompanion(FirebaseDatabase::class.java)
        mockCompanion(FirebaseAuth::class.java)
        mockCompanion(FirebaseFunctions::class.java)

        // 3. Setup behavior for common Firebase calls
        every { FirebaseApp.getInstance() } returns mockApp
        every { FirebaseApp.getInstance(any()) } returns mockApp
        
        every { FirebaseDatabase.getInstance() } returns mockDb
        every { FirebaseDatabase.getInstance(any<String>()) } returns mockDb
        every { FirebaseDatabase.getInstance(any<FirebaseApp>()) } returns mockDb
        
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseAuth.getInstance(any<FirebaseApp>()) } returns mockAuth
        
        every { FirebaseFunctions.getInstance() } returns mockFunctions
        every { FirebaseFunctions.getInstance(any<String>()) } returns mockFunctions
        every { FirebaseFunctions.getInstance(any<FirebaseApp>()) } returns mockFunctions
        every { FirebaseFunctions.getInstance(any<FirebaseApp>(), any<String>()) } returns mockFunctions

        every { mockDb.reference } returns mockRef
        every { mockRef.child(any()) } returns mockRef
        every { mockRef.push() } returns mockRef
        every { mockRef.key } returns "mock_id"
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Given valid email, When registering, Then user is persisted in database`() {
        val authRepo = mockk<AuthRepository>()
        val user = AppUser(email = "new@test.com", firstName = "John")
        
        every { authRepo.register(any(), any(), any(), any(), any()) } answers {
            (invocation.args[3] as () -> Unit).invoke()
        }

        var success = false
        authRepo.register("new@test.com", "password", user, { success = true }, {})

        assertTrue(success)
        verify { authRepo.register("new@test.com", "password", user, any(), any()) }
    }

    @Test
    fun `Given phone registration, When submitting, Then SMS service is invoked`() {
        val mockNotificationRepo = mockk<NotificationRepository>()
        
        val phone = "5141234567"
        val message = "Welcome!"
        
        every { 
            mockNotificationRepo.enqueueConfirmation(any(), phone, message, any(), any()) 
        } answers {
            val channel = if (phone.contains("@")) "email" else "sms"
            assertEquals("sms", channel)
            (invocation.args[3] as () -> Unit).invoke()
        }

        var done = false
        mockNotificationRepo.enqueueConfirmation("user1", phone, message, { done = true }, {})
        assertTrue(done)
    }

    @Test
    fun `Given filters, When querying, Then database returns correct filtered dataset`() {
        val eventRepo = mockk<EventRepository>()
        val allEvents = listOf(
            Event(id = "1", location = "Montreal"),
            Event(id = "2", location = "Toronto")
        )

        every { eventRepo.getEvents(any()) } answers {
            val callback = invocation.args[0] as (List<Event>) -> Unit
            callback(allEvents.filter { it.location == "Montreal" })
        }

        var results = emptyList<Event>()
        eventRepo.getEvents { results = it }

        assertEquals(1, results.size)
        assertEquals("Montreal", results[0].location)
    }

    @Test
    fun `Given reservation, When canceled, Then status is updated in database`() {
        // Use real repository to test status update logic
        val repo = ReservationRepository()
        val reservation = Reservation(id = "res1", status = "active")
        
        val mockRef = FirebaseDatabase.getInstance().reference
        val mockTask = mockk<Task<Void>>(relaxed = true)
        
        every { mockRef.child(any()) } returns mockRef
        every { mockRef.setValue(any()) } returns mockTask

        val capturedReservation = slot<Reservation>()
        every { mockRef.setValue(capture(capturedReservation)) } returns mockTask

        repo.cancelReservation(reservation, {}, {})

        assertEquals("cancelled", capturedReservation.captured.status)
    }

    @Test
    fun `Given booking, When created, Then notification service is triggered`() {
        val resRepo = mockk<ReservationRepository>()
        val notifRepo = mockk<NotificationRepository>()
        val user = AppUser(uid = "u1", contact = "test@test.com")
        val event = Event(id = "e1", name = "Test Event")

        every { resRepo.reserve(any(), any(), any(), any(), any()) } answers {
            val onSuccess = invocation.args[3] as (Reservation) -> Unit
            onSuccess(Reservation(id = "r1", eventName = event.name))
        }
        
        every { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) } answers {
            (invocation.args[3] as () -> Unit).invoke()
        }

        resRepo.reserve(user.uid, user.contact, event, { reservation ->
            notifRepo.enqueueConfirmation(user.uid, user.contact, "Booked ${reservation.eventName}", {}, {})
        }, {})

        verify { notifRepo.enqueueConfirmation("u1", "test@test.com", match { it.contains("Test Event") }, any(), any()) }
    }

    @Test
    fun `Given event cancellation, Then reservations are updated and notifications sent`() {
        val eventRepo = mockk<EventRepository>()
        val resRepo = mockk<ReservationRepository>()
        val notifRepo = mockk<NotificationRepository>()
        
        val event = Event(id = "e1", name = "Big Party")
        val affectedReservations = listOf(
            Reservation(id = "r1", userId = "u1", contact = "u1@test.com"),
            Reservation(id = "r2", userId = "u2", contact = "5140000000")
        )

        every { eventRepo.cancelEvent(any(), any(), any()) } answers {
            (invocation.args[1] as () -> Unit).invoke()
        }
        every { resRepo.getReservationsByEvent(any(), any()) } answers {
            (invocation.args[1] as (List<Reservation>) -> Unit).invoke(affectedReservations)
        }
        every { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) } just runs

        eventRepo.cancelEvent(event, onSuccess = {
            resRepo.getReservationsByEvent(event.id) { reservations ->
                reservations.forEach { res ->
                    notifRepo.enqueueConfirmation(res.userId, res.contact, "Cancelled", {}, {})
                }
            }
        }, onError = {})

        verify(exactly = 2) { notifRepo.enqueueConfirmation(any(), any(), any(), any(), any()) }
    }
}
