package com.example.project

import com.example.project.data.model.AppUser
import com.example.project.data.model.Event
import com.example.project.data.model.NotificationJob
import com.example.project.data.model.Reservation
import com.example.project.data.model.UserRole
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.EventRepository
import com.example.project.data.repository.NotificationRepository
import com.example.project.data.repository.ReservationRepository
import com.example.project.data.repository.UserRepository
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryCoverageTests {

    @Test
    fun `event repository getEvents returns mapped events on success`() {
        val eventsRef = mockk<DatabaseReference>()
        val task = mockSuccessTask(mockEventsSnapshot())
        val repository = EventRepository(eventsRef)

        every { eventsRef.get() } returns task

        var result: List<Event> = emptyList()
        repository.getEvents { result = it }

        assertEquals(2, result.size)
        assertEquals(listOf("event-1", "event-2"), result.map { it.id })
    }

    @Test
    fun `event repository getEvents returns empty list on failure`() {
        val eventsRef = mockk<DatabaseReference>()
        val task = mockFailureTask<DataSnapshot>(RuntimeException("db down"))
        val repository = EventRepository(eventsRef)

        every { eventsRef.get() } returns task

        var result: List<Event>? = null
        repository.getEvents { result = it }

        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun `event repository addEvent stores generated id and invokes success`() {
        val eventsRef = mockk<DatabaseReference>()
        val pushRef = mockk<DatabaseReference>()
        val eventNode = mockk<DatabaseReference>()
        val repository = EventRepository(eventsRef)

        every { eventsRef.push() } returns pushRef
        every { pushRef.key } returns "evt-1"
        every { eventsRef.child("evt-1") } returns eventNode
        every { eventNode.setValue(any()) } returns mockSuccessTask(null)

        var called = false
        repository.addEvent(Event(name = "Karaoke"), onSuccess = { called = true }, onError = {})

        val saved = slot<Any>()
        verify { eventNode.setValue(capture(saved)) }
        val savedEvent = saved.captured as Event
        assertEquals("evt-1", savedEvent.id)
        assertTrue(called)
    }

    @Test
    fun `event repository updateEvent invokes error when save fails`() {
        val eventsRef = mockk<DatabaseReference>()
        val eventNode = mockk<DatabaseReference>()
        val repository = EventRepository(eventsRef)

        every { eventsRef.child("evt-1") } returns eventNode
        every { eventNode.setValue(any()) } returns mockFailureTask(RuntimeException("cannot update"))

        var error: String? = null
        repository.updateEvent(Event(id = "evt-1", name = "Updated"), onSuccess = {}, onError = { error = it })

        assertEquals("cannot update", error)
    }

    @Test
    fun `reservation repository getReservationsForUser filters by user id`() {
        val reservationRef = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.get() } returns mockSuccessTask(mockReservationSnapshot())

        var result: List<Reservation> = emptyList()
        repository.getReservationsForUser("u-1") { result = it }

        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == "u-1" })
    }

    @Test
    fun `reservation repository getReservationsByEvent filters by event id`() {
        val reservationRef = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.get() } returns mockSuccessTask(mockReservationSnapshot())

        var result: List<Reservation> = emptyList()
        repository.getReservationsByEvent("e1") { result = it }

        assertEquals(2, result.size)
        assertTrue(result.all { it.eventId == "e1" })
    }

    @Test
    fun `reservation repository getReservationsByEvent returns empty list on failure`() {
        val reservationRef = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.get() } returns mockFailureTask(RuntimeException("read failed"))

        var result: List<Reservation>? = null
        repository.getReservationsByEvent("e1") { result = it }

        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun `reservation repository reserve returns saved reservation on success`() {
        val reservationRef = mockk<DatabaseReference>()
        val pushRef = mockk<DatabaseReference>()
        val node = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.push() } returns pushRef
        every { pushRef.key } returns "res-1"
        every { reservationRef.child("res-1") } returns node
        every { node.setValue(any()) } returns mockSuccessTask(null)

        var saved: Reservation? = null
        repository.reserve(
            userId = "u-1",
            contact = "u1@example.com",
            event = Event(id = "evt-1", name = "Expo", location = "MTL", date = "2026-06-01", category = "Art"),
            onSuccess = { saved = it },
            onError = {}
        )

        assertNotNull(saved)
        assertEquals("res-1", saved!!.id)
        assertEquals("evt-1", saved!!.eventId)
    }

    @Test
    fun `reservation repository cancelReservation sets cancelled status`() {
        val reservationRef = mockk<DatabaseReference>()
        val node = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.child("res-1") } returns node
        every { node.setValue(any()) } returns mockSuccessTask(null)

        var success = false
        repository.cancelReservation(Reservation(id = "res-1", status = "active"), onSuccess = { success = true }, onError = {})

        val saved = slot<Any>()
        verify { node.setValue(capture(saved)) }
        assertEquals("cancelled", (saved.captured as Reservation).status)
        assertTrue(success)
    }

    @Test
    fun `reservation repository reserve returns fallback error when save fails`() {
        val reservationRef = mockk<DatabaseReference>()
        val pushRef = mockk<DatabaseReference>()
        val node = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.push() } returns pushRef
        every { pushRef.key } returns "res-2"
        every { reservationRef.child("res-2") } returns node
        every { node.setValue(any()) } returns mockFailureTask(RuntimeException())

        var error: String? = null
        repository.reserve(
            userId = "u-1",
            contact = "u1@example.com",
            event = Event(id = "evt-1"),
            onSuccess = {},
            onError = { error = it }
        )

        assertEquals("Could not reserve event", error)
    }

    @Test
    fun `reservation repository cancelReservation returns fallback error on failure`() {
        val reservationRef = mockk<DatabaseReference>()
        val node = mockk<DatabaseReference>()
        val repository = ReservationRepository(reservationRef)

        every { reservationRef.child("res-1") } returns node
        every { node.setValue(any()) } returns mockFailureTask(RuntimeException())

        var error: String? = null
        repository.cancelReservation(Reservation(id = "res-1"), onSuccess = {}, onError = { error = it })

        assertEquals("Could not cancel reservation", error)
    }

    @Test
    fun `user repository getUser maps snapshot object on success`() {
        val usersRef = mockk<DatabaseReference>()
        val userNode = mockk<DatabaseReference>()
        val snapshot = mockk<DataSnapshot>()
        val repository = UserRepository(usersRef)

        every { usersRef.child("u-1") } returns userNode
        every { userNode.get() } returns mockSuccessTask(snapshot)
        every { snapshot.getValue(AppUser::class.java) } returns AppUser(uid = "u-1", role = "user")

        var result: AppUser? = null
        repository.getUser("u-1") { result = it }

        assertNotNull(result)
        assertEquals("u-1", result!!.uid)
    }

    @Test
    fun `user repository getUser returns null on failure`() {
        val usersRef = mockk<DatabaseReference>()
        val userNode = mockk<DatabaseReference>()
        val repository = UserRepository(usersRef)

        every { usersRef.child("u-1") } returns userNode
        every { userNode.get() } returns mockFailureTask(RuntimeException("read failed"))

        var result: AppUser? = AppUser(uid = "temp")
        repository.getUser("u-1") { result = it }

        assertNull(result)
    }

    @Test
    fun `user role from value unknown falls back to USER`() {
        assertEquals(UserRole.USER, UserRole.fromValue("random"))
    }

    @Test
    fun `auth repository login triggers success callback`() {
        val auth = mockk<FirebaseAuth>()
        val repo = AuthRepository(auth, mockk(relaxed = true))

        every { auth.signInWithEmailAndPassword("user@example.com", "123456") } returns mockSuccessTask(mockk<AuthResult>())

        var ok = false
        repo.login("user@example.com", "123456", onSuccess = { ok = true }, onError = {})

        assertTrue(ok)
    }

    @Test
    fun `auth repository login failure returns fallback message`() {
        val auth = mockk<FirebaseAuth>()
        val repo = AuthRepository(auth, mockk(relaxed = true))

        every { auth.signInWithEmailAndPassword(any(), any()) } returns mockFailureTask(RuntimeException())

        var error: String? = null
        repo.login("user@example.com", "bad", onSuccess = {}, onError = { error = it })

        assertEquals("Login failed", error)
    }

    @Test
    fun `auth repository register stores profile and invokes success`() {
        val auth = mockk<FirebaseAuth>()
        val authResult = mockk<AuthResult>()
        val firebaseUser = mockk<FirebaseUser>()

        val databaseRef = mockk<DatabaseReference>()
        val usersNode = mockk<DatabaseReference>()
        val uidNode = mockk<DatabaseReference>()
        val repo = AuthRepository(auth, databaseRef)

        every { auth.createUserWithEmailAndPassword("new@example.com", "123456") } returns mockSuccessTask(authResult)
        every { authResult.user } returns firebaseUser
        every { firebaseUser.uid } returns "uid-1"
        every { databaseRef.child("users") } returns usersNode
        every { usersNode.child("uid-1") } returns uidNode
        every { uidNode.setValue(any()) } returns mockSuccessTask(null)

        var success = false
        repo.register(
            email = "new@example.com",
            password = "123456",
            user = AppUser(email = "new@example.com"),
            onSuccess = { success = true },
            onError = {}
        )

        assertTrue(success)
    }

    @Test
    fun `auth repository register returns invalid user id when auth user uid is null`() {
        val auth = mockk<FirebaseAuth>()
        val authResult = mockk<AuthResult>()
        val repo = AuthRepository(auth, mockk(relaxed = true))

        every { auth.createUserWithEmailAndPassword(any(), any()) } returns mockSuccessTask(authResult)
        every { authResult.user } returns null

        var error: String? = null
        repo.register("new@example.com", "123456", AppUser(), onSuccess = {}, onError = { error = it })

        assertEquals("Invalid user id", error)
    }

    @Test
    fun `auth repository register returns fallback on auth failure`() {
        val auth = mockk<FirebaseAuth>()
        val repo = AuthRepository(auth, mockk(relaxed = true))

        every { auth.createUserWithEmailAndPassword(any(), any()) } returns mockFailureTask(RuntimeException())

        var error: String? = null
        repo.register("new@example.com", "123456", AppUser(), onSuccess = {}, onError = { error = it })

        assertEquals("Registration failed", error)
    }

    @Test
    fun `auth repository currentUserId and logout use FirebaseAuth`() {
        val auth = mockk<FirebaseAuth>()
        val user = mockk<FirebaseUser>()
        val repo = AuthRepository(auth, mockk(relaxed = true))

        every { auth.currentUser } returns user
        every { user.uid } returns "uid-9"
        every { auth.signOut() } returns Unit

        assertEquals("uid-9", repo.currentUserId())
        repo.logout()
        verify { auth.signOut() }
    }

    @Test
    fun `notification enqueueConfirmation routes email channel`() {
        val deps = notificationDeps()
        val repo = NotificationRepository(deps.ref, deps.functions)

        var done = false
        repo.enqueueConfirmation("u-1", "u1@example.com", "hello", onDone = { done = true }, onError = {})

        assertTrue(done)
        assertEquals("email", (deps.jobSlot.captured as NotificationJob).channel)
        assertEquals("sent", deps.lastStatusSet)
    }

    @Test
    fun `notification enqueueConfirmation callable failure marks failed and returns error`() {
        val deps = notificationDeps(callShouldFail = true)
        val repo = NotificationRepository(deps.ref, deps.functions)

        var error: String? = null
        repo.enqueueConfirmation("u-1", "5145550000", "hello", onDone = {}, onError = { error = it })

        assertEquals("sms", (deps.jobSlot.captured as NotificationJob).channel)
        assertEquals("failed", deps.lastStatusSet)
        assertEquals("Dispatch failed", error)
    }

    @Test
    fun `notification sendNotification sends sms when phone is present`() {
        val deps = notificationDeps(doubleDispatch = true)
        val repo = NotificationRepository(deps.ref, deps.functions)

        var done = false
        repo.sendNotification("u-1", "u1@example.com", "5145550000", "message", onDone = { done = true }, onError = {})

        assertTrue(done)
        assertEquals(2, deps.channelsCaptured.size)
        assertTrue(deps.channelsCaptured.contains("email"))
        assertTrue(deps.channelsCaptured.contains("sms"))
    }

    private fun mockEventsSnapshot(): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        val child1 = mockk<DataSnapshot>()
        val child2 = mockk<DataSnapshot>()
        val child3 = mockk<DataSnapshot>()

        every { snapshot.children } returns listOf(child1, child2, child3)
        every { child1.getValue(Event::class.java) } returns Event(id = "event-1", createdBy = "admin-1")
        every { child2.getValue(Event::class.java) } returns null
        every { child3.getValue(Event::class.java) } returns Event(id = "event-2", createdBy = "admin-2")
        return snapshot
    }

    private fun mockReservationSnapshot(): DataSnapshot {
        val snapshot = mockk<DataSnapshot>()
        val child1 = mockk<DataSnapshot>()
        val child2 = mockk<DataSnapshot>()
        val child3 = mockk<DataSnapshot>()

        every { snapshot.children } returns listOf(child1, child2, child3)
        every { child1.getValue(Reservation::class.java) } returns Reservation(id = "r1", userId = "u-1", eventId = "e1")
        every { child2.getValue(Reservation::class.java) } returns Reservation(id = "r2", userId = "u-2", eventId = "e1")
        every { child3.getValue(Reservation::class.java) } returns Reservation(id = "r3", userId = "u-1", eventId = "e2")
        return snapshot
    }

    private fun notificationDeps(callShouldFail: Boolean = false, doubleDispatch: Boolean = false): NotificationDeps {
        val ref = mockk<DatabaseReference>()
        val push1 = mockk<DatabaseReference>()
        val push2 = mockk<DatabaseReference>()
        val node1 = mockk<DatabaseReference>()
        val node2 = mockk<DatabaseReference>()
        val status1 = mockk<DatabaseReference>()
        val status2 = mockk<DatabaseReference>()

        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()

        val statusValues = mutableListOf<String>()
        val channels = mutableListOf<String>()
        val jobSlot = slot<Any>()

        every { ref.push() } returns push1 andThen push2
        every { push1.key } returns "job-1"
        every { push2.key } returns "job-2"
        every { ref.child("job-1") } returns node1
        every { ref.child("job-2") } returns node2
        every { node1.child("status") } returns status1
        every { node2.child("status") } returns status2
        every { status1.setValue(any()) } answers {
            statusValues.add(firstArg<String>())
            mockSuccessTask(null)
        }
        every { status2.setValue(any()) } answers {
            statusValues.add(firstArg<String>())
            mockSuccessTask(null)
        }

        every { node1.setValue(capture(jobSlot)) } answers {
            channels.add((jobSlot.captured as NotificationJob).channel)
            mockSuccessTask(null)
        }
        every { node2.setValue(any()) } answers {
            val job = firstArg<Any>() as NotificationJob
            channels.add(job.channel)
            mockSuccessTask(null)
        }

        every { functions.getHttpsCallable("dispatchConfirmation") } returns callable
        every { callable.call(any<Map<String, Any>>()) } answers {
            if (callShouldFail) {
                mockFailureTask<HttpsCallableResult>(RuntimeException())
            } else {
                mockSuccessTask(mockk())
            }
        }

        if (!doubleDispatch) {
            every { node2.setValue(any()) } returns mockSuccessTask(null)
            every { status2.setValue(any()) } returns mockSuccessTask(null)
        }

        return NotificationDeps(
            ref = ref,
            functions = functions,
            jobSlot = jobSlot,
            channelsCaptured = channels,
            getLastStatus = { statusValues.lastOrNull() }
        )
    }

    private data class NotificationDeps(
        val ref: DatabaseReference,
        val functions: FirebaseFunctions,
        val jobSlot: CapturingSlot<Any>,
        val channelsCaptured: List<String>,
        private val getLastStatus: () -> String?
    ) {
        val lastStatusSet: String?
            get() = getLastStatus()
    }

    private fun <T> mockSuccessTask(result: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<in T>>()) } answers {
            firstArg<OnSuccessListener<in T>>().onSuccess(result)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun <T> mockFailureTask(error: Exception): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<in T>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        return task
    }
}
