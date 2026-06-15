package com.moneypimpworking.ui.expenses

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import com.moneypimpworking.database.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var tvDateRange: TextView
    private lateinit var btnSelectDates: Button
    private lateinit var rvExpenses: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoExpenses: TextView

    private var userId: Long = -1
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var expenseAdapter: ExpenseAdapter
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

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
        loadExpenses()
    }

    private fun initViews() {
        tvDateRange = findViewById(R.id.tvDateRange)
        btnSelectDates = findViewById(R.id.btnSelectDates)
        rvExpenses = findViewById(R.id.rvExpenses)
        progressBar = findViewById(R.id.progressBar)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)

        rvExpenses.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(
            onDeleteClick = { expense -> deleteExpense(expense) }
        )
        rvExpenses.adapter = expenseAdapter
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
                        loadExpenses()
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

    private fun loadExpenses() {
        if (startDate == null || endDate == null) return

        progressBar.visibility = ProgressBar.VISIBLE

        Thread {
            val expenses = dbHelper.getExpensesBetweenDates(userId, startDate!!, endDate!!)
            runOnUiThread {
                progressBar.visibility = ProgressBar.GONE
                if (expenses.isEmpty()) {
                    tvNoExpenses.visibility = TextView.VISIBLE
                    rvExpenses.visibility = TextView.GONE
                } else {
                    tvNoExpenses.visibility = TextView.GONE
                    rvExpenses.visibility = RecyclerView.VISIBLE
                    expenseAdapter.submitList(expenses)
                }
            }
        }.start()
    }

    private fun deleteExpense(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Delete ${expense.description}?")
            .setPositiveButton("Delete") { _, _ ->
                Thread {
                    dbHelper.deleteExpense(expense.id)
                    runOnUiThread { loadExpenses() }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    inner class ExpenseAdapter(
        private val onDeleteClick: (Expense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        private var expenses = listOf<Expense>()

        fun submitList(newList: List<Expense>) {
            expenses = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_expense, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(expenses[position], onDeleteClick)
        }

        override fun getItemCount() = expenses.size

        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
            private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
            private val ivReceiptIcon: ImageView = itemView.findViewById(R.id.ivReceiptIcon)

            fun bind(expense: Expense, onDelete: (Expense) -> Unit) {
                tvAmount.text = NumberFormat.getCurrencyInstance().format(expense.amount)
                tvDescription.text = expense.description
                tvDate.text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(expense.date)
                
                val moodStr = if (expense.mood != null) " (${expense.mood})" else ""
                tvCategory.text = "${expense.characterization ?: "Unknown"}$moodStr"

                ivReceiptIcon.visibility = if (expense.receiptPhotoPath != null) android.view.View.VISIBLE else android.view.View.GONE
                btnDelete.setOnClickListener { onDelete(expense) }
            }
        }
    }
}