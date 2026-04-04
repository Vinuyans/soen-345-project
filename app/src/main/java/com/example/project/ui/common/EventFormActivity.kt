package com.example.project.ui.common

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.model.Event
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.EventRepository
import com.example.project.data.repository.NotificationRepository
import com.example.project.data.repository.ReservationRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventFormActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private val eventRepository = EventRepository()
    private val reservationRepository = ReservationRepository()
    private val notificationRepository = NotificationRepository()

    private val dateFormatter = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_form)

        val eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()

        val nameInput = findViewById<EditText>(R.id.eventNameInput)
        val locationInput = findViewById<EditText>(R.id.eventLocationInput)
        val dateInput = findViewById<EditText>(R.id.eventDateInput)
        val categoryInput = findViewById<AutoCompleteTextView>(R.id.eventCategoryInput)
        val descriptionInput = findViewById<EditText>(R.id.eventDescriptionInput)
        val priceInput = findViewById<EditText>(R.id.eventPriceInput)
        val saveButton = findViewById<Button>(R.id.saveEventButton)

        val categories = resources.getStringArray(R.array.event_categories).drop(1)
        categoryInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        )

        dateInput.setOnClickListener {
            showDatePicker(dateInput)
        }

        dateInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker(dateInput)
            }
        }

        if (eventId.isNotBlank()) {
            title = getString(R.string.edit_event)
            nameInput.setText(intent.getStringExtra(EXTRA_NAME).orEmpty())
            locationInput.setText(intent.getStringExtra(EXTRA_LOCATION).orEmpty())
            dateInput.setText(intent.getStringExtra(EXTRA_DATE).orEmpty())
            categoryInput.setText(intent.getStringExtra(EXTRA_CATEGORY).orEmpty(), false)
            descriptionInput.setText(intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty())
            val savedPrice = intent.getDoubleExtra(EXTRA_PRICE, 0.0)
            if (savedPrice > 0.0) priceInput.setText(savedPrice.toString())
        } else {
            title = getString(R.string.add_event)
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val date = dateInput.text.toString().trim()
            val category = categoryInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val price = priceInput.text.toString().trim().toDoubleOrNull() ?: 0.0

            if (name.isBlank() || location.isBlank() || date.isBlank() || category.isBlank()) {
                Toast.makeText(this, getString(R.string.error_fill_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = authRepository.currentUserId() ?: return@setOnClickListener
            val event = Event(
                id = eventId,
                name = name,
                location = location,
                date = date,
                category = category,
                description = description,
                price = price,
                createdBy = uid,
                cancelled = false
            )

            saveButton.isEnabled = false
            if (eventId.isBlank()) {
                eventRepository.addEvent(
                    event = event,
                    onSuccess = {
                        saveButton.isEnabled = true
                        Toast.makeText(this, getString(R.string.save), Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onError = {
                        saveButton.isEnabled = true
                        Toast.makeText(this, it.ifBlank { getString(R.string.error_event_save_failed) }, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                eventRepository.updateEvent(
                    event = event,
                    onSuccess = {
                        reservationRepository.getReservationsByEvent(event.id) { reservations ->
                            reservations.forEach { reservation ->
                                notificationRepository.enqueueConfirmation(
                                    userId = reservation.userId,
                                    destination = reservation.contact,
                                    message = "${event.name} was updated. New details: ${event.date}, ${event.location}.",
                                    onDone = {},
                                    onError = {}
                                )
                            }
                        }
                        saveButton.isEnabled = true
                        Toast.makeText(this, getString(R.string.save), Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onError = {
                        saveButton.isEnabled = true
                        Toast.makeText(this, it.ifBlank { getString(R.string.error_event_save_failed) }, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_NAME = "event_name"
        const val EXTRA_LOCATION = "event_location"
        const val EXTRA_DATE = "event_date"
        const val EXTRA_CATEGORY = "event_category"
        const val EXTRA_DESCRIPTION = "event_description"
        const val EXTRA_PRICE = "event_price"

        private const val DATE_PATTERN = "yyyy-MM-dd"
    }

    private fun showDatePicker(dateInput: EditText) {
        val calendar = Calendar.getInstance()
        val currentDate = dateInput.text.toString().trim()
        if (currentDate.isNotBlank()) {
            runCatching { dateFormatter.parse(currentDate) }
                .getOrNull()
                ?.let { parsedDate ->
                    calendar.time = parsedDate
                }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                dateInput.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
