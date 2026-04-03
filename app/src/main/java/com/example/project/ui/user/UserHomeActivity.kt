package com.example.project.ui.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.repository.AuthRepository
import com.example.project.ui.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserHomeActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)
        title = getString(R.string.feed)

        val bottomNav = findViewById<BottomNavigationView>(R.id.userBottomNav)
        supportFragmentManager.beginTransaction()
            .replace(R.id.userContentContainer, UserFeedFragment())
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_feed -> {
                    title = getString(R.string.feed)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.userContentContainer, UserFeedFragment())
                        .commit()
                    true
                }

                R.id.menu_reservations -> {
                    title = getString(R.string.reservations)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.userContentContainer, ReservationsFragment())
                        .commit()
                    true
                }

                R.id.menu_user_logout -> {
                    authRepository.logout()
                    LoginActivity.start(this)
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, UserHomeActivity::class.java))
        }
    }
}
