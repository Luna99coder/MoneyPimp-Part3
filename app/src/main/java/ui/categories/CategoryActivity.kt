package com.moneypimpworking.ui.categories

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.moneypimpworking.R
import com.moneypimpworking.database.DatabaseHelper
import com.moneypimpworking.database.ExpenseCategory
import java.text.NumberFormat
import android.view.View
import android.widget.Spinner

class CategoryActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var categoryAdapter: CategoryAdapter
    private var userId: Long = -1
    private var categories = mutableListOf<ExpenseCategory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        userId = intent.getLongExtra("USER_ID", -1)
        if (userId == -1L) {
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        initViews()
        setupListeners()
        loadCategories()
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        fabAddCategory = findViewById(R.id.fabAddCategory)
        rvCategories.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(
            onEditClick = { category -> editCategory(category) },
            onDeleteClick = { category -> deleteCategory(category) }
        )
        rvCategories.adapter = categoryAdapter
    }

    private fun setupListeners() {
        fabAddCategory.setOnClickListener { showAddCategoryDialog() }
    }

    private fun loadCategories() {
        Thread {
            categories = dbHelper.getCategoriesForUser(userId).toMutableList()
            runOnUiThread {
                categoryAdapter.submitList(categories)
            }
        }.start()
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_category, null)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerCategoryType)
        val etName = view.findViewById<EditText>(R.id.etCategoryName)
        val etBudget = view.findViewById<EditText>(R.id.etCategoryBudget)

        builder.setTitle("Add Characterization")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val type = spinnerType.selectedItem.toString()
                val subName = etName.text.toString().trim()
                val name = if (subName.isNotEmpty()) "$type - $subName" else type
                val budgetStr = etBudget.text.toString().trim()

                if (budgetStr.isEmpty()) {
                    Toast.makeText(this, "Budget required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val budget = budgetStr.toDoubleOrNull()
                if (budget == null || budget <= 0) {
                    Toast.makeText(this, "Invalid budget", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                Thread {
                    val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#F44336", "#9C27B0")
                    val color = colors.random()
                    dbHelper.insertCategory(userId, name, budget, color)
                    runOnUiThread {
                        Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editCategory(category: ExpenseCategory) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_category, null)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerCategoryType)
        val etName = view.findViewById<EditText>(R.id.etCategoryName)
        val etBudget = view.findViewById<EditText>(R.id.etCategoryBudget)

        val parts = category.name.split(" - ")
        val types = resources.getStringArray(R.array.category_types)
        
        if (parts.size > 1) {
            val typeIndex = types.indexOf(parts[0])
            if (typeIndex >= 0) spinnerType.setSelection(typeIndex)
            etName.setText(parts[1])
        } else {
            val typeIndex = types.indexOf(category.name)
            if (typeIndex >= 0) spinnerType.setSelection(typeIndex)
            etName.setText("")
        }

        etBudget.setText(category.budgetLimit.toString())

        builder.setTitle("Edit Characterization")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val type = spinnerType.selectedItem.toString()
                val subName = etName.text.toString().trim()
                val name = if (subName.isNotEmpty()) "$type - $subName" else type
                val budgetStr = etBudget.text.toString().trim()

                if (budgetStr.isEmpty()) {
                    Toast.makeText(this, "Budget required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val budget = budgetStr.toDoubleOrNull()
                if (budget == null || budget <= 0) {
                    Toast.makeText(this, "Invalid budget", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                Thread {
                    val updatedCategory = category.copy(name = name, budgetLimit = budget)
                    dbHelper.updateCategory(updatedCategory)
                    runOnUiThread {
                        Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: ExpenseCategory) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Delete ${category.name}? All expenses in this category will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                Thread {
                    dbHelper.deleteCategory(category.id)
                    runOnUiThread {
                        Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class CategoryAdapter(
        private val onEditClick: (ExpenseCategory) -> Unit,
        private val onDeleteClick: (ExpenseCategory) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        private var categories = listOf<ExpenseCategory>()

        fun submitList(newList: List<ExpenseCategory>) {
            categories = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(categories[position], onEditClick, onDeleteClick)
        }

        override fun getItemCount() = categories.size

        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: android.widget.TextView = itemView.findViewById(R.id.tvCategoryName)
            private val tvBudget: android.widget.TextView = itemView.findViewById(R.id.tvCategoryBudget)
            private val btnEdit: android.widget.ImageButton = itemView.findViewById(R.id.btnEditCategory)
            private val btnDelete: android.widget.ImageButton = itemView.findViewById(R.id.btnDeleteCategory)

            fun bind(category: ExpenseCategory, onEdit: (ExpenseCategory) -> Unit, onDelete: (ExpenseCategory) -> Unit) {
                tvName.text = category.name
                tvBudget.text = "Budget: ${NumberFormat.getCurrencyInstance().format(category.budgetLimit)}"
                btnEdit.setOnClickListener { onEdit(category) }
                btnDelete.setOnClickListener { onDelete(category) }
            }
        }
    }
}