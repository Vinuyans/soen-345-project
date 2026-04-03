package com.example.project.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.model.Event
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.EventRepository
import com.example.project.data.repository.NotificationRepository
import com.example.project.data.repository.ReservationRepository
import com.example.project.data.repository.UserRepository
import com.example.project.ui.common.EventAdapter

class UserFeedFragment : Fragment() {
    private val eventRepository = EventRepository()
    private val reservationRepository = ReservationRepository()
    private val notificationRepository = NotificationRepository()
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private lateinit var eventAdapter: EventAdapter
    private var allEvents: List<Event> = emptyList()
    private var bookedEventIds: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_event_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.eventsRecyclerView)
        val searchButton = view.findViewById<Button>(R.id.searchButton)

        eventAdapter = EventAdapter(
            canBook = true,
            bookedEventIds = bookedEventIds,
            onBook = { event -> reserve(event) }
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = eventAdapter

        setupCategoryDropdown(view)
        searchButton.setOnClickListener { applyFilters(view) }

        loadEvents(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadEvents(it) }
    }

    private fun setupCategoryDropdown(root: View) {
        val categoryInput = root.findViewById<AutoCompleteTextView>(R.id.categoryInput)
        val categories = resources.getStringArray(R.array.event_categories)
        categoryInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        )
        categoryInput.setText(getString(R.string.category_all), false)
    }

    private fun loadEvents(root: View) {
        val uid = authRepository.currentUserId()
        if (uid == null) {
            allEvents = emptyList()
            bookedEventIds = emptySet()
            applyFilters(root)
            return
        }

        reservationRepository.getReservationsForUser(uid) { reservations ->
            bookedEventIds = reservations
                .filter { it.status == "active" }
                .map { it.eventId }
                .toSet()

            eventRepository.getEvents { events ->
                allEvents = events.filter { !it.cancelled }
                eventAdapter = EventAdapter(
                    canBook = true,
                    bookedEventIds = bookedEventIds,
                    onBook = { event -> reserve(event) }
                )
                val recyclerView = root.findViewById<RecyclerView>(R.id.eventsRecyclerView)
                recyclerView.adapter = eventAdapter
                applyFilters(root)
            }
        }
    }

    private fun applyFilters(root: View) {
        val emptyText = root.findViewById<TextView>(R.id.emptyStateText)
        val dateFilter = root.findViewById<EditText>(R.id.dateFilterInput).text.toString().trim()
        val locationFilter = root.findViewById<EditText>(R.id.locationFilterInput).text.toString().trim()
        val categoryFilter = root.findViewById<AutoCompleteTextView>(R.id.categoryInput).text.toString().trim()

        val filtered = allEvents.filter { event ->
            val matchesDate = dateFilter.isBlank() || event.date.contains(dateFilter, ignoreCase = true)
            val matchesLocation = locationFilter.isBlank() || event.location.contains(locationFilter, ignoreCase = true)
            val matchesCategory = categoryFilter.isBlank() || categoryFilter == getString(R.string.category_all) || event.category.equals(categoryFilter, ignoreCase = true)
            matchesDate && matchesLocation && matchesCategory
        }

        eventAdapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun reserve(event: Event) {
        val uid = authRepository.currentUserId() ?: return
        userRepository.getUser(uid) { user ->
            if (user == null) {
                Toast.makeText(requireContext(), getString(R.string.error_profile_not_found), Toast.LENGTH_LONG).show()
                return@getUser
            }

            reservationRepository.reserve(
                userId = uid,
                contact = user.contact,
                event = event,
                onSuccess = { reservation ->
                    val message = "Reservation confirmed for ${reservation.eventName} on ${reservation.date} at ${reservation.location}."
                    notificationRepository.enqueueConfirmation(
                        userId = uid,
                        destination = user.contact,
                        message = message,
                        onDone = {
                            Toast.makeText(requireContext(), getString(R.string.notification_sent), Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(requireContext(), getString(R.string.notification_failed), Toast.LENGTH_LONG).show()
                        }
                    )
                    view?.let { loadEvents(it) }
                },
                onError = {
                    Toast.makeText(requireContext(), it.ifBlank { getString(R.string.error_reservation_failed) }, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
