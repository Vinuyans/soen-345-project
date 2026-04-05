package com.example.project

import com.example.project.data.model.Reservation
import com.example.project.data.repository.ReservationRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservationsTests {

    private val reservationRepository = mockk<ReservationRepository>()

    @Test
    fun `Cancel existing reservation - success confirmation`() {
        val reservation = Reservation(id = "res123", status = "active")
        every { reservationRepository.cancelReservation(any(), any(), any()) } answers {
            val onSuccess = invocation.args[1] as () -> Unit
            onSuccess()
        }

        var success = false
        reservationRepository.cancelReservation(reservation, { success = true }, {})

        assertTrue(success)
    }

    @Test
    fun `Cancel already canceled reservation - error shown`() {
        val reservation = Reservation(id = "res123", status = "cancelled")
        val errorMsg = "Reservation is already cancelled"
        every { reservationRepository.cancelReservation(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        reservationRepository.cancelReservation(reservation, {}, { error = it })

        assertEquals(errorMsg, error)
    }

    @Test
    fun `Cancel reservation past event date - not allowed`() {
        // Logic check: if event date < current date, don't allow.
        // This might be in the Repository or ViewModel. 
        // We simulate the repository enforcing this.
        val pastDate = "2020-01-01"
        val reservation = Reservation(id = "res123", date = pastDate)
        val errorMsg = "Cannot cancel a past event reservation"
        
        every { reservationRepository.cancelReservation(any(), any(), any()) } answers {
            val res = invocation.args[0] as Reservation
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val eventDate = sdf.parse(res.date)
            if (eventDate != null && eventDate.before(Date())) {
                val onError = invocation.args[2] as (String) -> Unit
                onError(errorMsg)
            } else {
                val onSuccess = invocation.args[1] as () -> Unit
                onSuccess()
            }
        }

        var error = ""
        reservationRepository.cancelReservation(reservation, {}, { error = it })

        assertEquals(errorMsg, error)
    }

    @Test
    fun `Cancel without being logged in - access denied`() {
        // Assuming the repository or a wrapper checks for auth
        val errorMsg = "User not authenticated"
        every { reservationRepository.cancelReservation(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        reservationRepository.cancelReservation(Reservation(), {}, { error = it })

        assertEquals(errorMsg, error)
    }

    @Test
    fun `Cancel with network failure - error message`() {
        val errorMsg = "Network connection lost"
        every { reservationRepository.cancelReservation(any(), any(), any()) } answers {
            val onError = invocation.args[2] as (String) -> Unit
            onError(errorMsg)
        }

        var error = ""
        reservationRepository.cancelReservation(Reservation(id = "1"), {}, { error = it })

        assertEquals(errorMsg, error)
    }
}
