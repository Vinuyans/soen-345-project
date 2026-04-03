package com.example.project.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.model.Reservation

class ReservationAdapter(
    private val onCancel: (Reservation) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

    private val items = mutableListOf<Reservation>()

    fun submitList(reservations: List<Reservation>) {
        items.clear()
        items.addAll(reservations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(items[position], onCancel)
    }

    override fun getItemCount(): Int = items.size

    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.reservationTitleText)
        private val detailsText: TextView = itemView.findViewById(R.id.reservationDetailsText)
        private val statusText: TextView = itemView.findViewById(R.id.reservationStatusText)
        private val cancelButton: Button = itemView.findViewById(R.id.cancelReservationButton)

        fun bind(item: Reservation, onCancel: (Reservation) -> Unit) {
            titleText.text = item.eventName
            detailsText.text = "${item.location} • ${item.date} • ${item.category}"
            statusText.text = item.status
            cancelButton.visibility = if (item.status == "active") View.VISIBLE else View.GONE
            cancelButton.setOnClickListener { onCancel(item) }
        }
    }
}
