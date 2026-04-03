package com.example.project.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.model.Event

class EventAdapter(
    private val canBook: Boolean,
    private val bookedEventIds: Set<String> = emptySet(),
    private val onBook: ((Event) -> Unit)? = null,
    private val onEdit: ((Event) -> Unit)? = null,
    private val onCancel: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val items: MutableList<Event> = mutableListOf()

    fun submitList(events: List<Event>) {
        items.clear()
        items.addAll(events)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position], canBook, bookedEventIds, onBook, onEdit, onCancel)
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.eventNameText)
        private val locationText: TextView = itemView.findViewById(R.id.eventLocationText)
        private val dateText: TextView = itemView.findViewById(R.id.eventDateText)
        private val categoryText: TextView = itemView.findViewById(R.id.eventCategoryText)
        private val statusText: TextView = itemView.findViewById(R.id.eventStatusText)
        private val bookButton: Button = itemView.findViewById(R.id.bookButton)
        private val editButton: Button = itemView.findViewById(R.id.editButton)
        private val cancelButton: Button = itemView.findViewById(R.id.cancelButton)

        fun bind(
            event: Event,
            canBook: Boolean,
            bookedEventIds: Set<String>,
            onBook: ((Event) -> Unit)?,
            onEdit: ((Event) -> Unit)?,
            onCancel: ((Event) -> Unit)?
        ) {
            val isBooked = bookedEventIds.contains(event.id)
            nameText.text = event.name
            locationText.text = event.location
            dateText.text = event.date
            categoryText.text = event.category
            statusText.text = if (event.cancelled) "Cancelled" else "Available"

            bookButton.visibility = if (canBook && !event.cancelled) View.VISIBLE else View.GONE
            editButton.visibility = if (onEdit != null && !event.cancelled) View.VISIBLE else View.GONE
            cancelButton.visibility = if (onCancel != null && !event.cancelled) View.VISIBLE else View.GONE

            if (canBook && !event.cancelled) {
                if (isBooked) {
                    bookButton.text = itemView.context.getString(R.string.booked)
                    bookButton.isEnabled = false
                    bookButton.alpha = 0.5f
                } else {
                    bookButton.text = itemView.context.getString(R.string.book)
                    bookButton.isEnabled = true
                    bookButton.alpha = 1f
                    bookButton.setOnClickListener { onBook?.invoke(event) }
                }
            } else {
                bookButton.isEnabled = false
                bookButton.alpha = 0.5f
            }

            editButton.setOnClickListener { onEdit?.invoke(event) }
            cancelButton.setOnClickListener { onCancel?.invoke(event) }
        }
    }
}
