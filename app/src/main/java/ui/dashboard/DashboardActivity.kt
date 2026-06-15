package com.moneypimpworking.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import com.moneypimpworking.ui.auth.LoginActivity
import com.moneypimpworking.ui.categories.CategoryActivity
import com.moneypimpworking.ui.expenses.AddExpenseActivity
import com.moneypimpworking.ui.expenses.ExpenseListActivity
import com.moneypimpworking.ui.reports.ChartActivity
import com.moneypimpworking.ui.reports.ReportActivity
import java.text.NumberFormat
import java.util.*
import android.graphics.Color
class DashboardActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var fabAddExpense: FloatingActionButton
    private lateinit var btnViewAllExpenses: Button
    private lateinit var btnCategories: Button
    private lateinit var btnReports: Button
    private lateinit var btnBadges: Button
    private lateinit var btnChart: Button
    private lateinit var btnLogout: Button

    // Goal Progress Views
    private lateinit var tvProgressStatus: TextView
    private lateinit var progressGoals: ProgressBar
    private lateinit var tvMinGoalValue: TextView
    private lateinit var tvMaxGoalValue: TextView

    private val minGoal = 1000.0
    private val maxGoal = 5000.0

    private var userId: Long = -1
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) {
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        initViews()
        setupListeners()
        loadUsername()
        loadDashboardData()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining)
        progressBudget = findViewById(R.id.progressBudget)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        btnViewAllExpenses = findViewById(R.id.btnViewAllExpenses)
        btnCategories = findViewById(R.id.btnCategories)
        btnReports = findViewById(R.id.btnReports)
        btnBadges = findViewById(R.id.btnBadges)
        btnChart = findViewById(R.id.btnChart)
        btnLogout = findViewById(R.id.btnLogout)

        // Goal Progress Views
        tvProgressStatus = findViewById(R.id.tvProgressStatus)
        progressGoals = findViewById(R.id.progressGoals)
        tvMinGoalValue = findViewById(R.id.tvMinGoalValue)
        tvMaxGoalValue = findViewById(R.id.tvMaxGoalValue)

        tvMinGoalValue.text = "Min: ${NumberFormat.getCurrencyInstance().format(minGoal)}"
        tvMaxGoalValue.text = "Max: ${NumberFormat.getCurrencyInstance().format(maxGoal)}"
    }

    private fun setupListeners() {
        fabAddExpense.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnViewAllExpenses.setOnClickListener {
            val intent = Intent(this, ExpenseListActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnCategories.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnReports.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnBadges.setOnClickListener {
            val intent = Intent(this, BadgesActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnChart.setOnClickListener {
            val intent = Intent(this, ChartActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    getSharedPreferences("MoneyPimpPrefs", MODE_PRIVATE)
                        .edit().remove("currentUserId").apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun loadUsername() {
        Thread {
            val user = dbHelper.getUserById(userId)
            runOnUiThread {
                tvGreeting.text = "Welcome back, ${user?.username ?: "User"}! (${user?.points ?: 0} pts)"
            }
        }.start()
    }

    private fun updateGoalProgress(currentSpent: Double) {
        // Calculate progress percentage between min and max goals
        val percent = when {
            currentSpent <= minGoal -> 0
            currentSpent >= maxGoal -> 100
            else -> ((currentSpent - minGoal) / (maxGoal - minGoal) * 100).toInt()
        }
        progressGoals.progress = percent

        // Update status message with color
        val status = when {
            currentSpent < minGoal -> "⭐ Amazing! You're below your minimum goal! Keep saving! ⭐"
            currentSpent <= maxGoal -> "✅ Perfect! You're within your target range! ✅"
            else -> "⚠️ Warning: You've exceeded your maximum spending goal! ⚠️"
        }
        tvProgressStatus.text = status

        // Set color based on status
        when {
            currentSpent < minGoal -> tvProgressStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            currentSpent <= maxGoal -> tvProgressStatus.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            else -> tvProgressStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }
    }

    private fun loadDashboardData() {
        val calendar = Calendar.getInstance()
        val startDate = getStartOfMonth(calendar)
        val endDate = getEndOfMonth(calendar)
        val budgetLimit = 5000.0

        Thread {
            val totalSpent = dbHelper.getTotalSpent(userId, startDate, endDate)
            runOnUiThread {
                val currencyFormat = NumberFormat.getCurrencyInstance()
                tvTotalSpent.text = currencyFormat.format(totalSpent)

                val remaining = budgetLimit - totalSpent
                tvBudgetRemaining.text = currencyFormat.format(remaining)
                if (remaining < 0) {
                    tvBudgetRemaining.setTextColor(android.graphics.Color.parseColor("#F44336"))
                } else {
                    tvBudgetRemaining.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }

                val percent = ((totalSpent / budgetLimit) * 100).toInt().coerceAtMost(100)
                progressBudget.progress = percent

                // Update the goal progress display (Part 3 feature)
                updateGoalProgress(totalSpent)
            }
        }.start()
    }

    private fun getStartOfMonth(calendar: Calendar): Date {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }

    private fun getEndOfMonth(calendar: Calendar): Date {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.time
    }
}