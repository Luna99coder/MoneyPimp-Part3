package com.moneypimpworking.ui.reports

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity() {

    private lateinit var tvDateRange: TextView
    private lateinit var btnSelectDates: Button
    private lateinit var barChart: BarChart
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var progressBar: ProgressBar

    private var userId: Long = -1
    private lateinit var dbHelper: DatabaseHelper
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val minGoal = 1000.0
    private val maxGoal = 5000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

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
        loadChartData()
    }

    private fun initViews() {
        tvDateRange = findViewById(R.id.tvDateRange)
        btnSelectDates = findViewById(R.id.btnSelectDates)
        barChart = findViewById(R.id.barChart)
        tvMinGoal = findViewById(R.id.tvMinGoal)
        tvMaxGoal = findViewById(R.id.tvMaxGoal)
        progressBar = findViewById(R.id.progressBar)

        tvMinGoal.text = "Min Goal: ${NumberFormat.getCurrencyInstance().format(minGoal)}"
        tvMaxGoal.text = "Max Goal: ${NumberFormat.getCurrencyInstance().format(maxGoal)}"

        // Setup chart
        barChart.description.isEnabled = false
        barChart.setTouchEnabled(true)
        barChart.setDragEnabled(true)
        barChart.setScaleEnabled(true)
        barChart.setPinchZoom(true)
        barChart.legend.isEnabled = true
        barChart.axisLeft.axisMinimum = 0f
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
                        loadChartData()
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

    private fun loadChartData() {
        if (startDate == null || endDate == null) return

        progressBar.visibility = ProgressBar.VISIBLE

        Thread {
            val categoryTotals = dbHelper.getCategoryTotals(userId, startDate!!, endDate!!)
            runOnUiThread {
                progressBar.visibility = ProgressBar.GONE
                displayChart(categoryTotals)
            }
        }.start()
    }

    private fun displayChart(categoryTotals: List<com.moneypimpworking.database.CategoryTotal>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        categoryTotals.forEachIndexed { index, category ->
            entries.add(BarEntry(index.toFloat(), category.totalSpent.toFloat()))
            labels.add(category.categoryName)
            try {
                colors.add(Color.parseColor(category.color))
            } catch (e: Exception) {
                colors.add(Color.parseColor("#4CAF50"))
            }
        }

        val dataSet = BarDataSet(entries, "Spending by Category")
        dataSet.setColors(colors)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.data = barData

        val formatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.valueFormatter = formatter
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.setDrawGridLines(true)

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = (maxGoal * 1.2).toFloat()

        addGoalLinesToChart()
        barChart.invalidate()
    }

    private fun addGoalLinesToChart() {
        barChart.axisLeft.removeAllLimitLines()
        val limitLineMax = LimitLine(maxGoal.toFloat(), "Max Goal")
        limitLineMax.lineWidth = 2f
        limitLineMax.lineColor = Color.RED
        limitLineMax.textColor = Color.RED

        val limitLineMin = LimitLine(minGoal.toFloat(), "Min Goal")
        limitLineMin.lineWidth = 2f
        limitLineMin.lineColor = Color.parseColor("#FFD700")
        limitLineMin.textColor = Color.parseColor("#FFD700")

        barChart.axisLeft.addLimitLine(limitLineMax)
        barChart.axisLeft.addLimitLine(limitLineMin)
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