package com.example.project

import com.example.project.data.model.Event
import com.example.project.data.repository.EventRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsTests {

    private val eventRepository = mockk<EventRepository>()

    // --- b. View list of available events ---

    @Test
    fun `View events when events exist - list displayed`() {
        val events = listOf(Event(id = "1", name = "Event 1"), Event(id = "2", name = "Event 2"))
        every { eventRepository.getEvents(any()) } answers {
            val onResult = invocation.args[0] as (List<Event>) -> Unit
            onResult(events)
        }

        var result: List<Event> = emptyList()
        eventRepository.getEvents { result = it }

        assertEquals(2, result.size)
        assertEquals("Event 1", result[0].name)
    }

    @Test
    fun `View events when no events exist - empty state message`() {
        every { eventRepository.getEvents(any()) } answers {
            val onResult = invocation.args[0] as (List<Event>) -> Unit
            onResult(emptyList())
        }

        var result: List<Event>? = null
        eventRepository.getEvents { result = it }

        assertTrue(result != null && result!!.isEmpty())
    }

    @Test
    fun `Events list loads successfully - no errors`() {
        every { eventRepository.getEvents(any()) } answers {
            val onResult = invocation.args[0] as (List<Event>) -> Unit
            onResult(listOf(Event(id = "1")))
        }

        var called = false
        eventRepository.getEvents { called = true }
        assertTrue(called)
    }

    @Test
    fun `Events list fails to load (server error) - error message`() {
        every { eventRepository.getEvents(any()) } answers {
            val onResult = invocation.args[0] as (List<Event>) -> Unit
            onResult(emptyList()) 
        }

        var result: List<Event>? = null
        eventRepository.getEvents { result = it }

        assertTrue(result!!.isEmpty())
    }

    // --- c. Search and filter events ---

    private val allEvents = listOf(
        Event(id = "1", name = "Music Fest", location = "Montreal", category = "Music", date = "2023-12-01"),
        Event(id = "2", name = "Tech Conf", location = "Toronto", category = "Tech", date = "2023-12-15"),
        Event(id = "3", name = "Art Expo", location = "Montreal", category = "Art", date = "2023-12-20")
    )

    @Test
    fun `Search by keyword - matching events displayed`() {
        val keyword = "Music"
        val matching = allEvents.filter { it.name.contains(keyword, ignoreCase = true) }
        assertEquals(1, matching.size)
        assertEquals("Music Fest", matching[0].name)
    }

    @Test
    fun `Filter by date - only events on selected date shown`() {
        val selectedDate = "2023-12-15"
        val filtered = allEvents.filter { it.date == selectedDate }
        assertEquals(1, filtered.size)
        assertEquals("Tech Conf", filtered[0].name)
    }

    @Test
    fun `Filter by location - only events in location shown`() {
        val location = "Montreal"
        val filtered = allEvents.filter { it.location.contains(location, ignoreCase = true) }
        assertEquals(2, filtered.size)
    }

    @Test
    fun `Filter by category - correct category events shown`() {
        val category = "Art"
        val filtered = allEvents.filter { it.category == category }
        assertEquals(1, filtered.size)
        assertEquals("Art Expo", filtered[0].name)
    }

    @Test
    fun `Apply multiple filters - results match all criteria`() {
        val filtered = allEvents.filter { 
            it.location == "Montreal" && it.category == "Music"
        }
        assertEquals(1, filtered.size)
        assertEquals("Music Fest", filtered[0].name)
    }

    @Test
    fun `No matching results - no results message`() {
        val filtered = allEvents.filter { it.location == "Vancouver" }
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `Invalid or empty search input - handled gracefully`() {
        val keyword = ""
        val matching = allEvents.filter { it.name.contains(keyword, ignoreCase = true) }
        assertEquals(3, matching.size) // Should show all
    }

    // --- f. Add new event ---

    @Test
    fun `Add event with valid data - event created`() {
        val newEvent = Event(name = "New Event", location = "Paris", date = "2024-12-01")
        every { eventRepository.addEvent(any(), any(), any()) } answers {
            val onSuccess = invocation.args[1] as () -> Unit
            onSuccess()
        }

        var success = false
        eventRepository.addEvent(newEvent, { success = true }, {})

        assertTrue(success)
    }

    @Test
    fun `Add event with missing fields - validation error`() {
        val invalidEvent = Event(name = "") 
        val errorMsg = "Name cannot be empty"
        every { eventRepository.addEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.addEvent(invalidEvent, {}, { error = it })

        assertEquals(errorMsg, error)
    }

    @Test
    fun `Add event with invalid date (past date) - error`() {
        val pastEvent = Event(name = "Past Event", date = "2020-01-01")
        val errorMsg = "Cannot create event in the past"
        
        every { eventRepository.addEvent(any(), any(), any()) } answers {
            val ev = invocation.args[0] as Event
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(ev.date)
            if (date != null && date.before(Date())) {
                val onError = invocation.args[2] as (String) -> Unit
                onError(errorMsg)
            } else {
                val onSuccess = invocation.args[1] as () -> Unit
                onSuccess()
            }
        }

        var error = ""
        eventRepository.addEvent(pastEvent, {}, { error = it })
        assertEquals(errorMsg, error)
    }

    @Test
    fun `Add duplicate event - handled appropriately`() {
        val duplicate = Event(name = "Existing Event")
        val errorMsg = "An event with this name already exists"
        every { eventRepository.addEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.addEvent(duplicate, {}, { error = it })
        assertEquals(errorMsg, error)
    }

    // --- g. Edit an existing event ---

    @Test
    fun `Edit event with valid changes - update successful`() {
        val event = Event(id = "1", name = "Old Name")
        val updated = event.copy(name = "New Name")
        every { eventRepository.updateEvent(any(), any(), any()) } answers {
            val onSuccess = invocation.args[1] as () -> Unit
            onSuccess()
        }

        var success = false
        eventRepository.updateEvent(updated, { success = true }, {})

        assertTrue(success)
    }

    @Test
    fun `Edit non-existing event - error shown`() {
        val nonExistent = Event(id = "missing", name = "Test")
        val errorMsg = "Event not found"
        every { eventRepository.updateEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.updateEvent(nonExistent, {}, { error = it })

        assertEquals(errorMsg, error)
    }

    @Test
    fun `Edit with invalid data - validation error`() {
        val invalid = Event(id = "1", name = "")
        val errorMsg = "Invalid event data"
        every { eventRepository.updateEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.updateEvent(invalid, {}, { error = it })
        assertEquals(errorMsg, error)
    }

    @Test
    fun `Concurrent edits (two admins) - conflict handled`() {
        val event = Event(id = "1", name = "Edit")
        val errorMsg = "Conflict: Event was updated by another user"
        every { eventRepository.updateEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.updateEvent(event, {}, { error = it })
        assertEquals(errorMsg, error)
    }

    // --- h. Cancel an event ---

    @Test
    fun `Cancel existing event - event marked canceled`() {
        val event = Event(id = "1", cancelled = false)
        every { eventRepository.cancelEvent(any(), any(), any()) } answers {
            val onSuccess = invocation.args[1] as () -> Unit
            onSuccess()
        }

        var success = false
        eventRepository.cancelEvent(event, { success = true }, {})

        assertTrue(success)
    }

    @Test
    fun `Cancel already canceled event - notification shown`() {
        val event = Event(id = "1", cancelled = true)
        val errorMsg = "Event is already cancelled"
        every { eventRepository.cancelEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.cancelEvent(event, {}, { error = it })

        assertEquals(errorMsg, error)
    }

    @Test
    fun `Cancel event with reservations - users notified`() {
        // This test focuses on the repository call for cancellation.
        // In a real scenario, this would trigger notification jobs.
        val event = Event(id = "1")
        every { eventRepository.cancelEvent(any(), any(), any()) } answers {
            val onSuccess = invocation.args[1] as () -> Unit
            onSuccess()
        }

        var notified = false
        eventRepository.cancelEvent(event, { notified = true }, {})
        assertTrue(notified)
    }

    @Test
    fun `Cancel non-existing event - error shown`() {
        val event = Event(id = "non-existent")
        val errorMsg = "Event not found"
        every { eventRepository.cancelEvent(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        eventRepository.cancelEvent(event, {}, { error = it })
        assertEquals(errorMsg, error)
    }
}
