package com.example.project.data.repository

import com.example.project.data.model.Event
import com.example.project.data.model.Reservation
import com.google.firebase.database.FirebaseDatabase

class ReservationRepository {
    private val reservationRef = FirebaseDatabase.getInstance().reference.child("reservations")

    fun getReservationsForUser(uid: String, onResult: (List<Reservation>) -> Unit) {
        reservationRef.get()
            .addOnSuccessListener { snapshot ->
                val reservations = snapshot.children
                    .mapNotNull { it.getValue(Reservation::class.java) }
                    .filter { it.userId == uid }
                onResult(reservations)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getReservationsByEvent(eventId: String, onResult: (List<Reservation>) -> Unit) {
        reservationRef.get()
            .addOnSuccessListener { snapshot ->
                val reservations = snapshot.children
                    .mapNotNull { it.getValue(Reservation::class.java) }
                    .filter { it.eventId == eventId }
                onResult(reservations)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun reserve(
        userId: String,
        contact: String,
        event: Event,
        onSuccess: (Reservation) -> Unit,
        onError: (String) -> Unit
    ) {
        val id = reservationRef.push().key ?: return onError("Could not create reservation")
        val reservation = Reservation(
            id = id,
            userId = userId,
            eventId = event.id,
            eventName = event.name,
            location = event.location,
            date = event.date,
            category = event.category,
            contact = contact,
            status = "active"
        )
        reservationRef.child(id).setValue(reservation)
            .addOnSuccessListener { onSuccess(reservation) }
            .addOnFailureListener { onError(it.message ?: "Could not reserve event") }
    }

    fun cancelReservation(
        reservation: Reservation,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        reservationRef.child(reservation.id).setValue(reservation.copy(status = "cancelled"))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Could not cancel reservation") }
    }
}
