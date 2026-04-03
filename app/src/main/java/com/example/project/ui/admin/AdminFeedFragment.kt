package com.example.project.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.model.Event
import com.example.project.data.repository.EventRepository
import com.example.project.ui.common.EventAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import java.text.SimpleDateFormat
import java.util.Locale

class AdminFeedFragment : Fragment() {
    private val eventRepository = EventRepository()
    private lateinit var eventAdapter: EventAdapter
    private var allEvents: List<Event> = emptyList()
    private val dateFormatter = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
    private var selectedStartDateMillis: Long? = null
    private var selectedEndDateMillis: Long? = null

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
        val resetButton = view.findViewById<Button>(R.id.searchButton)

        eventAdapter = EventAdapter(canBook = false)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = eventAdapter

        setupDateRangeInput(view)
        setupCategoryDropdown(view)
        setupAutomaticFiltering(view)
        resetButton.setOnClickListener { resetFilters(view) }

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

    private fun setupDateRangeInput(root: View) {
        val dateRangeInput = root.findViewById<EditText>(R.id.dateRangeFilterInput)
        dateRangeInput.setOnClickListener {
            showDateRangePicker(dateRangeInput)
        }
    }

    private fun setupAutomaticFiltering(root: View) {
        val locationInput = root.findViewById<EditText>(R.id.locationFilterInput)
        val categoryInput = root.findViewById<AutoCompleteTextView>(R.id.categoryInput)

        locationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                applyFilters(root)
            }
        })

        categoryInput.setOnItemClickListener { _, _, _, _ ->
            applyFilters(root)
        }
    }

    private fun resetFilters(root: View) {
        val dateRangeInput = root.findViewById<EditText>(R.id.dateRangeFilterInput)
        val locationInput = root.findViewById<EditText>(R.id.locationFilterInput)
        val categoryInput = root.findViewById<AutoCompleteTextView>(R.id.categoryInput)

        selectedStartDateMillis = null
        selectedEndDateMillis = null
        dateRangeInput.setText("")
        locationInput.setText("")
        categoryInput.setText(getString(R.string.category_all), false)
        applyFilters(root)
    }

    private fun loadEvents(root: View) {
        eventRepository.getEvents { events ->
            allEvents = events
            applyFilters(root)
        }
    }

    private fun applyFilters(root: View) {
        val emptyText = root.findViewById<TextView>(R.id.emptyStateText)
        val locationFilter = root.findViewById<EditText>(R.id.locationFilterInput).text.toString().trim()
        val categoryFilter = root.findViewById<AutoCompleteTextView>(R.id.categoryInput).text.toString().trim()

        val filtered = allEvents.filter { event ->
            val eventDate = parseDate(event.date)?.time
            val matchesDate = when {
                eventDate == null -> selectedStartDateMillis == null || selectedEndDateMillis == null
                selectedStartDateMillis != null && selectedEndDateMillis != null ->
                    eventDate in selectedStartDateMillis!!..selectedEndDateMillis!!
                else -> true
            }
            val matchesLocation = locationFilter.isBlank() || event.location.contains(locationFilter, ignoreCase = true)
            val matchesCategory = categoryFilter.isBlank() || categoryFilter == getString(R.string.category_all) || event.category.equals(categoryFilter, ignoreCase = true)
            matchesDate && matchesLocation && matchesCategory
        }

        eventAdapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDateRangePicker(targetInput: EditText) {
        val pickerBuilder = MaterialDatePicker.Builder.dateRangePicker()
        pickerBuilder.setTitleText(getString(R.string.filter_date_range))
        if (selectedStartDateMillis != null && selectedEndDateMillis != null) {
            pickerBuilder.setSelection(androidx.core.util.Pair(selectedStartDateMillis, selectedEndDateMillis))
        }
        val picker = pickerBuilder.build()
        picker.addOnPositiveButtonClickListener(
            MaterialPickerOnPositiveButtonClickListener { selection ->
                selectedStartDateMillis = selection.first
                selectedEndDateMillis = selection.second
                val startText = selection.first?.let { dateFormatter.format(it) }.orEmpty()
                val endText = selection.second?.let { dateFormatter.format(it) }.orEmpty()
                targetInput.setText("$startText - $endText")
                view?.let { applyFilters(it) }
            }
        )
        picker.show(parentFragmentManager, DATE_RANGE_PICKER_TAG)
    }

    private fun parseDate(value: String) = runCatching {
        if (value.isBlank()) null else dateFormatter.parse(value)
    }.getOrNull()

    companion object {
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private const val DATE_RANGE_PICKER_TAG = "admin_date_range_picker"
    }
}
