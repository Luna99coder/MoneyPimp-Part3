package com.moneypimpworking.ui.expenses

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.moneypimpworking.NotificationHelper
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import com.moneypimpworking.database.ExpenseCategory
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvDate: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvNoCategories: TextView
    private lateinit var spinnerMood: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var ivReceipt: ImageView
    private lateinit var progressBar: ProgressBar

    private var userId: Long = -1
    private var selectedDate = Date()
    private var receiptPhotoPath: String? = null
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private var categories = listOf<ExpenseCategory>()
    private lateinit var categoryAdapter: ArrayAdapter<String>

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
            imageBitmap?.let {
                saveReceiptImage(it)
                ivReceipt.setImageBitmap(it)
                ivReceipt.visibility = ImageView.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) {
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        initViews()
        setupListeners()
        loadCategories()
    }

    private fun initViews() {
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        tvDate = findViewById(R.id.tvDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvNoCategories = findViewById(R.id.tvNoCategories)
        spinnerMood = findViewById(R.id.spinnerMood)
        btnSave = findViewById(R.id.btnSave)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        ivReceipt = findViewById(R.id.ivReceipt)
        progressBar = findViewById(R.id.progressBar)

        tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)
    }

    private fun setupListeners() {
        tvDate.setOnClickListener { showDatePicker() }
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnSave.setOnClickListener { saveExpense() }
        tvNoCategories.setOnClickListener {
            val intent = Intent(this, com.moneypimpworking.ui.categories.CategoryActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun loadCategories() {
        Thread {
            categories = dbHelper.getCategoriesForUser(userId)
            val categoryNames = categories.map { "${it.name} (${NumberFormat.getCurrencyInstance().format(it.budgetLimit)})" }
            runOnUiThread {
                if (categories.isEmpty()) {
                    tvNoCategories.visibility = android.view.View.VISIBLE
                    spinnerCategory.isEnabled = false
                } else {
                    tvNoCategories.visibility = android.view.View.GONE
                    spinnerCategory.isEnabled = true
                }
                categoryAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = categoryAdapter
            }
        }.start()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePhotoLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveReceiptImage(bitmap: android.graphics.Bitmap) {
        try {
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outStream = FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outStream)
            outStream.flush()
            outStream.close()
            receiptPhotoPath = file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val mood = spinnerMood.selectedItem.toString()

        if (amountStr.isEmpty()) {
            etAmount.error = "Amount required"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Invalid amount"
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Description required"
            return
        }

        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories. Please add a category first.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategory = categories[spinnerCategory.selectedItemPosition]

        progressBar.visibility = ProgressBar.VISIBLE
        btnSave.isEnabled = false

        Thread {
            val currentSpent = dbHelper.getCategorySpentThisMonth(userId, selectedCategory.id)
            if (currentSpent + amount > selectedCategory.budgetLimit) {
                runOnUiThread {
                    notificationHelper.showSassyNotification(selectedCategory.name)
                }
            }

            val result = dbHelper.insertExpense(
                userId, selectedCategory.id, amount,
                selectedDate, description, receiptPhotoPath, mood, selectedCategory.name
            )
            runOnUiThread {
                progressBar.visibility = ProgressBar.GONE
                btnSave.isEnabled = true
                if (result > 0) {
                    Toast.makeText(this, "Expense saved! +10 points", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error saving expense", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}