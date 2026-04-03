package com.example.project.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.model.UserRole
import com.example.project.data.repository.AuthRepository
import com.example.project.data.repository.UserRepository
import com.example.project.ui.admin.AdminHomeActivity
import com.example.project.ui.user.UserHomeActivity

class LoginActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val createAccountText = findViewById<TextView>(R.id.createAccountText)

        createAccountText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, getString(R.string.error_fill_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            authRepository.login(
                email = email,
                password = password,
                onSuccess = {
                    val uid = authRepository.currentUserId() ?: return@login
                    userRepository.getUserRole(uid) { role ->
                        loginButton.isEnabled = true
                        when (role) {
                            UserRole.ADMIN -> {
                                AdminHomeActivity.start(this)
                                finish()
                            }
                            UserRole.USER -> {
                                UserHomeActivity.start(this)
                                finish()
                            }
                            null -> Toast.makeText(
                                this,
                                getString(R.string.error_profile_not_found),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onError = {
                    loginButton.isEnabled = true
                    Toast.makeText(this, it.ifBlank { getString(R.string.error_login_failed) }, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }
    }
}
