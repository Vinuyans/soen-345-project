package com.example.project.data.model

data class Reservation(
    val id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val eventName: String = "",
    val location: String = "",
    val date: String = "",
    val category: String = "",
    val contact: String = "",
    val status: String = "active"
)
