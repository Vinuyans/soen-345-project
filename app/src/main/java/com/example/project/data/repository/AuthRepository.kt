package com.example.project.data.repository

import com.example.project.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val databaseRef: DatabaseReference = database.reference
) {

    companion object {
        private val database: FirebaseDatabase by lazy {
            FirebaseDatabase.getInstance().apply {
                setPersistenceEnabled(true)
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Login failed") }
    }

    fun register(
        email: String,
        password: String,
        user: AppUser,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onError("Invalid user id")
                    return@addOnSuccessListener
                }
                
                val userWithId = user.copy(uid = uid)
                databaseRef.child("users").child(uid).setValue(userWithId)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.message ?: "Could not save profile") }
            }
            .addOnFailureListener { e -> onError(e.message ?: "Registration failed") }
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUserId(): String? = auth.currentUser?.uid
}
