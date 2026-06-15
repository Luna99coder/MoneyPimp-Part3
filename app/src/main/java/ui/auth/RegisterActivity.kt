package com.moneypimpworking.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import java.util.Date

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener { performRegistration() }
        tvLoginLink.setOnClickListener { finish() }
    }

    private fun performRegistration() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        when {
            username.isEmpty() -> {
                etUsername.error = "Username required"
                return
            }
            email.isEmpty() -> {
                etEmail.error = "Email required"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Invalid email"
                return
            }
            password.isEmpty() -> {
                etPassword.error = "Password required"
                return
            }
            password.length < 4 -> {
                etPassword.error = "Password must be 4+ characters"
                return
            }
            password != confirmPassword -> {
                etConfirmPassword.error = "Passwords don't match"
                return
            }
        }

        progressBar.visibility = ProgressBar.VISIBLE
        btnRegister.isEnabled = false

        Thread {
            val userId = dbHelper.insertUser(username, password, email)
            runOnUiThread {
                progressBar.visibility = ProgressBar.GONE
                btnRegister.isEnabled = true

                if (userId > 0) {
                    createDefaultCategories(userId)
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun createDefaultCategories(userId: Long) {
        val defaultCategories = listOf(
            Triple("😈 Necessary Evil", 1000.0, "#F44336"),
            Triple("🌟 Actually Enriching", 500.0, "#4CAF50"),
            Triple("🗑️ Impulse Trash", 200.0, "#9C27B0"),
            Triple("🙏 Future Me Will Thank You", 1000.0, "#2196F3")
        )

        for ((name, limit, color) in defaultCategories) {
            dbHelper.insertCategory(userId, name, limit, color)
        }
    }
}