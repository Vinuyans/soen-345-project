package com.example.project.data.model

data class NotificationJob(
    val id: String = "",
    val userId: String = "",
    val channel: String = "",
    val destination: String = "",
    val message: String = "",
    val status: String = "pending"
)
