package com.example.project.data.repository

import com.example.project.data.model.NotificationJob
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions

class NotificationRepository {
    private val notificationRef = FirebaseDatabase.getInstance().reference.child("notificationJobs")
    private val functions = FirebaseFunctions.getInstance()

    fun enqueueConfirmation(
        userId: String,
        destination: String,
        message: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val channel = if (destination.contains("@")) "email" else "sms"
        val id = notificationRef.push().key ?: return onError("Could not create notification job")

        val job = NotificationJob(
            id = id,
            userId = userId,
            channel = channel,
            destination = destination,
            message = message,
            status = "pending"
        )

        notificationRef.child(id).setValue(job)
            .addOnSuccessListener {
                val payload = hashMapOf(
                    "jobId" to id,
                    "channel" to channel,
                    "destination" to destination,
                    "message" to message
                )
                functions
                    .getHttpsCallable("dispatchConfirmation")
                    .call(payload)
                    .addOnSuccessListener {
                        notificationRef.child(id).child("status").setValue("sent")
                        onDone()
                    }
                    .addOnFailureListener {
                        notificationRef.child(id).child("status").setValue("failed")
                        onError(it.message ?: "Dispatch failed")
                    }
            }
            .addOnFailureListener { onError(it.message ?: "Could not enqueue notification") }
    }
}
