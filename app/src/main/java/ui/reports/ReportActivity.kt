package com.moneypimpworking.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.moneypimpworking.R
import com.moneypimpworking.database.CategoryTotal
import com.moneypimpworking.database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var tvDateRange: TextView
    private lateinit var btnSelectDates: Button
    private lateinit var rvCategoryTotals: RecyclerView
    private lateinit var tvTotalSpent: TextView
    private lateinit var progressBar: ProgressBar

    private var userId: Long = -1
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var reportAdapter: ReportAdapter
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) {
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        initViews()
        setupListeners()

        val calendar = Calendar.getInstance()
        startDate = getStartOfMonth(calendar)
        endDate = getEndOfMonth(calendar)
        updateDateRangeDisplay()
        loadReport()
    }

    private fun initViews() {
        tvDateRange = findViewById(R.id.tvDateRange)
        btnSelectDates = findViewById(R.id.btnSelectDates)
        rvCategoryTotals = findViewById(R.id.rvCategoryTotals)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        progressBar = findViewById(R.id.progressBar)

        rvCategoryTotals.layoutManager = LinearLayoutManager(this)
        reportAdapter = ReportAdapter()
        rvCategoryTotals.adapter = reportAdapter
    }

    private fun setupListeners() {
        btnSelectDates.setOnClickListener { showDateRangePicker() }
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val startCal = Calendar.getInstance()
                startCal.set(year, month, day)
                startDate = startCal.time

                DatePickerDialog(
                    this,
                    { _, y, m, d ->
                        val endCal = Calendar.getInstance()
                        endCal.set(y, m, d, 23, 59, 59)
                        endDate = endCal.time
                        updateDateRangeDisplay()
                        loadReport()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateRangeDisplay() {
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val startStr = startDate?.let { format.format(it) } ?: "Start"
        val endStr = endDate?.let { format.format(it) } ?: "End"
        tvDateRange.text = "$startStr - $endStr"
    }

    private fun loadReport() {
        if (startDate == null || endDate == null) return

        progressBar.visibility = View.VISIBLE

        Thread {
            val categoryTotals = dbHelper.getCategoryTotals(userId, startDate!!, endDate!!)
            val totalSpent = dbHelper.getTotalSpent(userId, startDate!!, endDate!!)

            runOnUiThread {
                progressBar.visibility = View.GONE
                tvTotalSpent.text = NumberFormat.getCurrencyInstance().format(totalSpent)
                reportAdapter.submitList(categoryTotals)
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

    inner class ReportAdapter : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {

        private var categories = listOf<CategoryTotal>()

        fun submitList(newList: List<CategoryTotal>) {
            categories = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_category_total, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(categories[position])
        }

        override fun getItemCount(): Int {
            return categories.size
        }

        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val tvTotalSpent: TextView = itemView.findViewById(R.id.tvTotalSpent)
            private val viewColor: View = itemView.findViewById(R.id.viewCategoryColor)

            fun bind(categoryTotal: CategoryTotal) {
                tvCategoryName.text = categoryTotal.categoryName
                tvTotalSpent.text = NumberFormat.getCurrencyInstance().format(categoryTotal.totalSpent)
                try {
                    viewColor.setBackgroundColor(android.graphics.Color.parseColor(categoryTotal.color))
                } catch (e: Exception) {
                    viewColor.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                }
            }
        }
    }
}