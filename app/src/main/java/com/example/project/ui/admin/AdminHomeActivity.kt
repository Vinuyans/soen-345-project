package com.example.project.ui.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.data.repository.AuthRepository
import com.example.project.ui.auth.LoginActivity
import com.example.project.ui.common.EventFormActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminHomeActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private var currentTab: Int = R.id.menu_admin_feed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)
        title = getString(R.string.feed)

        val bottomNav = findViewById<BottomNavigationView>(R.id.adminBottomNav)
        val addEventFab = findViewById<FloatingActionButton>(R.id.addEventFab)
        supportFragmentManager.beginTransaction()
            .replace(R.id.adminContentContainer, AdminFeedFragment())
            .commit()

        addEventFab.setOnClickListener {
            startActivity(Intent(this, EventFormActivity::class.java))
        }

        bottomNav.setOnItemSelectedListener { item ->
            currentTab = item.itemId
            when (item.itemId) {
                R.id.menu_admin_feed -> {
                    title = getString(R.string.feed)
                    addEventFab.visibility = android.view.View.GONE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.adminContentContainer, AdminFeedFragment())
                        .commit()
                    invalidateOptionsMenu()
                    true
                }

                R.id.menu_admin_events -> {
                    title = getString(R.string.my_events)
                    addEventFab.visibility = android.view.View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.adminContentContainer, AdminEventsFragment())
                        .commit()
                    invalidateOptionsMenu()
                    true
                }

                R.id.menu_admin_logout -> {
                    authRepository.logout()
                    LoginActivity.start(this)
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == R.id.menu_admin_events) {
            supportFragmentManager.findFragmentById(R.id.adminContentContainer)
                ?.let { fragment ->
                    if (fragment is AdminEventsFragment) {
                        fragment.refresh()
                    }
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout_action -> {
                authRepository.logout()
                LoginActivity.start(this)
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AdminHomeActivity::class.java))
        }
    }
}
