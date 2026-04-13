package com.example.project.data.model

data class AppUser(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val homeAddress: String = "",
    val role: String = UserRole.USER.value,
    val notificationsEnabled: Boolean = true
)

enum class UserRole(val value: String) {
    USER("user"),
    ADMIN("admin");

    companion object {
        fun fromValue(value: String): UserRole {
            return entries.firstOrNull { it.value == value } ?: USER
        }
    }
}
