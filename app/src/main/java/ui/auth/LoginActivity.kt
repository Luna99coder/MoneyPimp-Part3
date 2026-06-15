package com.moneypimpworking.ui.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import com.moneypimpworking.ui.dashboard.DashboardActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        sharedPreferences = getSharedPreferences("MoneyPimpPrefs", Context.MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        val userId = sharedPreferences.getLong("currentUserId", -1)
        if (userId != -1L) {
            verifyUserAndNavigate(userId)
            return
        }

        initViews()
        setupListeners()
    }

    private fun verifyUserAndNavigate(userId: Long) {
        Thread {
            val user = dbHelper.getUserById(userId)
            runOnUiThread {
                if (user != null) {
                    navigateToDashboard(userId)
                } else {
                    // Stale session, clear it
                    sharedPreferences.edit().remove("currentUserId").apply()
                    initViews()
                    setupListeners()
                }
            }
        }.start()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener { performLogin() }
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        if (username.isEmpty()) {
            etUsername.error = "Username required"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password required"
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE
        btnLogin.isEnabled = false

        Thread {
            val user = dbHelper.login(username, password)
            runOnUiThread {
                progressBar.visibility = ProgressBar.GONE
                btnLogin.isEnabled = true

                if (user != null) {
                    sharedPreferences.edit().putLong("currentUserId", user.id).apply()
                    Toast.makeText(this, "Welcome back, ${user.username}!", Toast.LENGTH_SHORT).show()
                    navigateToDashboard(user.id)
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun navigateToDashboard(userId: Long) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("USER_ID", userId)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}