package com.moneypimpworking.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import java.util.*

class BadgesActivity : AppCompatActivity() {

    private lateinit var tvTotalPoints: TextView
    private lateinit var rvBadges: RecyclerView
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) {
            finish()
            return
        }
        dbHelper = DatabaseHelper(this)

        tvTotalPoints = findViewById(R.id.tvTotalPoints)
        rvBadges = findViewById(R.id.rvBadges)

        rvBadges.layoutManager = GridLayoutManager(this, 2)
        loadData()
    }

    private fun loadData() {
        Thread {
            val user = dbHelper.getUserById(userId)
            val expenseCount = dbHelper.getExpenseCount(userId)
            val photoCount = dbHelper.getPhotoExpenseCount(userId)
            val categoryCount = dbHelper.getCategoryCount(userId)
            val totalSpentThisMonth = dbHelper.getTotalSpent(userId, getStartOfMonth(), getEndOfMonth())
            val hasWeeklyWarrior = dbHelper.hasWeeklyWarrior(userId)

            var badgeBonusPoints = 0
            val badges = mutableListOf<Badge>()

            // 1. First Expense
            val firstExpenseEarned = expenseCount >= 1
            if (firstExpenseEarned) badgeBonusPoints += 50
            badges.add(Badge("First Expense", "Add 1 expense", firstExpenseEarned))

            // 2. Budget Boss
            val budgetBossEarned = totalSpentThisMonth < 5000 && expenseCount > 0
            if (budgetBossEarned) badgeBonusPoints += 50
            badges.add(Badge("Budget Boss", "Stay under $5000 total", budgetBossEarned))

            // 3. Category Creator
            val categoryCreatorEarned = categoryCount >= 3
            if (categoryCreatorEarned) badgeBonusPoints += 50
            badges.add(Badge("Category Creator", "Create 3 categories", categoryCreatorEarned))

            // 4. Photo Fan
            val photoFanEarned = photoCount >= 1
            if (photoFanEarned) badgeBonusPoints += 50
            badges.add(Badge("Photo Fan", "Add a receipt photo", photoFanEarned))

            // 5. Weekly Warrior
            if (hasWeeklyWarrior) badgeBonusPoints += 50
            badges.add(Badge("Weekly Warrior", "Log expenses for 7 days", hasWeeklyWarrior))

            val totalPoints = (user?.points ?: 0) + badgeBonusPoints

            runOnUiThread {
                tvTotalPoints.text = "$totalPoints pts"
                rvBadges.adapter = BadgeAdapter(badges)
            }
        }.start()
    }

    private fun getStartOfMonth(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        return cal.time
    }

    private fun getEndOfMonth(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        return cal.time
    }

    data class Badge(val name: String, val description: String, val isUnlocked: Boolean)

    inner class BadgeAdapter(private val badges: List<Badge>) : RecyclerView.Adapter<BadgeAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val badge = badges[position]
            holder.tvName.text = badge.name
            holder.tvDesc.text = badge.description
            if (badge.isUnlocked) {
                holder.ivIcon.alpha = 1.0f
                holder.tvStatus.text = "UNLOCKED! 🏆"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                holder.ivIcon.alpha = 0.3f
                holder.tvStatus.text = "Locked 🔒"
                holder.tvStatus.setTextColor(android.graphics.Color.GRAY)
            }
        }

        override fun getItemCount() = badges.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
            val tvName: TextView = view.findViewById(R.id.tvBadgeName)
            val tvDesc: TextView = view.findViewById(R.id.tvBadgeDesc)
            val tvStatus: TextView = view.findViewById(R.id.tvBadgeStatus)
        }
    }
}