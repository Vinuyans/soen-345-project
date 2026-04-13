package com.example.project.data.repository

import com.example.project.data.model.Event
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EventRepository(
    private val eventsRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("events")
) {

    fun getEvents(onResult: (List<Event>) -> Unit) {
        eventsRef.get()
            .addOnSuccessListener { snapshot ->
                val events = snapshot.children.mapNotNull { it.getValue(Event::class.java) }
                onResult(events)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun addEvent(event: Event, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = eventsRef.push().key ?: return onError("Could not create id")
        val toSave = event.copy(id = id)
        eventsRef.child(id).setValue(toSave)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Could not add event") }
    }

    fun updateEvent(event: Event, onSuccess: () -> Unit, onError: (String) -> Unit) {
        eventsRef.child(event.id).setValue(event)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Could not update event") }
    }

    fun cancelEvent(event: Event, onSuccess: () -> Unit, onError: (String) -> Unit) {
        updateEvent(event.copy(cancelled = true), onSuccess, onError)
    }

    fun getEventsByAdmin(adminUid: String, onResult: (List<Event>) -> Unit) {
        getEvents { events ->
            onResult(events.filter { it.createdBy == adminUid })
        }
    }
}
