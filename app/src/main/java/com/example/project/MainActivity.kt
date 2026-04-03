package com.example.project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.project.data.model.UserRole
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.UserRepository
import com.example.project.ui.admin.AdminHomeActivity
import com.example.project.ui.auth.LoginActivity
import com.example.project.ui.user.UserHomeActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val uid = authRepository.currentUserId()
        if (uid == null) {
            LoginActivity.start(this)
            finish()
            return
        }

        userRepository.getUserRole(uid) { role ->
            when (role) {
                UserRole.ADMIN -> AdminHomeActivity.start(this)
                UserRole.USER -> UserHomeActivity.start(this)
                null -> {
                    Snackbar.make(
                        findViewById(R.id.main),
                        getString(R.string.error_profile_not_found),
                        Snackbar.LENGTH_LONG
                    ).show()
                    authRepository.logout()
                    LoginActivity.start(this)
                }
            }
            finish()
        }
    }
}