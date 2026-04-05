package com.example.project.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.model.AppUser
import com.example.project.data.model.UserRole
import com.example.project.data.repository.AuthRepository
import com.example.project.ui.admin.AdminHomeActivity
import com.example.project.ui.user.UserHomeActivity
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val firstNameInput = findViewById<EditText>(R.id.firstNameInput)
        val lastNameInput = findViewById<EditText>(R.id.lastNameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val contactInput = findViewById<EditText>(R.id.contactInput)
        val addressInput = findViewById<EditText>(R.id.addressInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val adminCheck = findViewById<CheckBox>(R.id.adminCheck)
        val adminCodeLayout = findViewById<TextInputLayout>(R.id.adminCodeLayout)
        val adminCodeInput = findViewById<EditText>(R.id.adminCodeInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        val phoneRegex = Regex("^(\\+\\d{1,2}\\s?)?1?\\-?\\.?\\s?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\$")

        adminCheck.setOnCheckedChangeListener { _, isChecked ->
            adminCodeLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            adminCodeInput.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        registerButton.setOnClickListener {
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val contact = contactInput.text.toString().trim()
            val address = addressInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val wantsAdmin = adminCheck.isChecked
            val adminCode = adminCodeInput.text.toString().trim()

            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || contact.isBlank() || address.isBlank() || password.isBlank()) {
                Toast.makeText(this, getString(R.string.error_fill_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(!phoneRegex.containsMatchIn(contact)) {
                Toast.makeText(this, getString(R.string.error_invalid_phone), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, getString(R.string.error_password_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (wantsAdmin && adminCode != "1234") {
                Toast.makeText(this, getString(R.string.error_admin_code_invalid), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (wantsAdmin) UserRole.ADMIN else UserRole.USER

            registerButton.isEnabled = false
            val appUser = AppUser(
                firstName = firstName,
                lastName = lastName,
                email = email,
                contact = contact,
                homeAddress = address,
                role = role.value
            )

            authRepository.register(
                email = email,
                password = password,
                user = appUser,
                onSuccess = {
                    registerButton.isEnabled = true
                    if (role == UserRole.ADMIN) {
                        AdminHomeActivity.start(this)
                    } else {
                        UserHomeActivity.start(this)
                    }
                    finish()
                },
                onError = {
                    registerButton.isEnabled = true
                    Toast.makeText(this, it.ifBlank { getString(R.string.error_register_failed) }, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
