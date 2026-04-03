package com.example.project.ui.admin

import android.content.Intent
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
import com.example.project.data.model.Event
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.EventRepository
import com.example.project.data.repository.NotificationRepository
import com.example.project.data.repository.ReservationRepository
import com.example.project.ui.common.EventAdapter
import com.example.project.ui.common.EventFormActivity

class AdminEventsFragment : Fragment() {
    private val authRepository = AuthRepository()
    private val eventRepository = EventRepository()
    private val reservationRepository = ReservationRepository()
    private val notificationRepository = NotificationRepository()

    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.adminEventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = EventAdapter(
            canBook = false,
            onEdit = { event -> openEdit(event) },
            onCancel = { event -> cancelEvent(event) }
        )

        recyclerView.adapter = adapter
        refresh()
    }

    fun refresh() {
        val root = view ?: return
        val uid = authRepository.currentUserId() ?: return
        val emptyText = root.findViewById<TextView>(R.id.emptyAdminEventsText)

        eventRepository.getEventsByAdmin(uid) { events ->
            adapter.submitList(events)
            emptyText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun openEdit(event: Event) {
        val intent = Intent(requireContext(), EventFormActivity::class.java)
        intent.putExtra(EventFormActivity.EXTRA_EVENT_ID, event.id)
        intent.putExtra(EventFormActivity.EXTRA_NAME, event.name)
        intent.putExtra(EventFormActivity.EXTRA_LOCATION, event.location)
        intent.putExtra(EventFormActivity.EXTRA_DATE, event.date)
        intent.putExtra(EventFormActivity.EXTRA_CATEGORY, event.category)
        startActivity(intent)
    }

    private fun cancelEvent(event: Event) {
        eventRepository.cancelEvent(
            event,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.event_cancelled), Toast.LENGTH_SHORT).show()
                reservationRepository.getReservationsByEvent(event.id) { reservations ->
                    reservations.forEach { reservation ->
                        notificationRepository.enqueueConfirmation(
                            userId = reservation.userId,
                            destination = reservation.contact,
                            message = "${event.name} was cancelled by the organizer.",
                            onDone = {},
                            onError = {}
                        )
                    }
                }
                refresh()
            },
            onError = {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        )
    }
}
