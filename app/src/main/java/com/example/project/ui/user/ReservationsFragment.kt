package com.example.project.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.NotificationRepository
import com.example.project.data.repository.ReservationRepository
import com.example.project.ui.common.ReservationAdapter

class ReservationsFragment : Fragment() {
    private val authRepository = AuthRepository()
    private val reservationRepository = ReservationRepository()
    private val notificationRepository = NotificationRepository()

    private lateinit var adapter: ReservationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reservations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.reservationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ReservationAdapter { reservation ->
            reservationRepository.cancelReservation(
                reservation,
                onSuccess = {
                    Toast.makeText(requireContext(), getString(R.string.reservation_cancelled), Toast.LENGTH_SHORT).show()
                    notificationRepository.enqueueConfirmation(
                        userId = reservation.userId,
                        destination = reservation.contact,
                        message = "Your reservation for ${reservation.eventName} has been cancelled.",
                        onDone = {},
                        onError = {}
                    )
                    loadReservations(view)
                },
                onError = {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            )
        }

        recyclerView.adapter = adapter
        loadReservations(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadReservations(it) }
    }

    private fun loadReservations(root: View) {
        val uid = authRepository.currentUserId() ?: return
        val emptyState = root.findViewById<TextView>(R.id.emptyReservationsText)

        reservationRepository.getReservationsForUser(uid) { reservations ->
            adapter.submitList(reservations)
            emptyState.visibility = if (reservations.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
