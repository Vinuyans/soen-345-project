package com.example.project.data.model

data class Event(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val date: String = "",
    val category: String = "",
    val createdBy: String = "",
    val cancelled: Boolean = false
)
