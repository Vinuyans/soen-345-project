package com.example.project.data.repository

import com.example.project.data.model.AppUser
import com.example.project.data.model.UserRole
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserRepository(
    private val usersRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("users")
) {

    fun getUser(uid: String, onResult: (AppUser?) -> Unit) {
        usersRef.child(uid).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.getValue(AppUser::class.java))
            }
            .addOnFailureListener { onResult(null) }
    }

    fun getUserRole(uid: String, onResult: (UserRole?) -> Unit) {
        getUser(uid) { user ->
            onResult(user?.let { UserRole.fromValue(it.role) })
        }
    }
}
