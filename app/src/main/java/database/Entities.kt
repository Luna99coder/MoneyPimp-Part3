package com.moneypimpworking.database

import java.util.Date

data class User(
    val id: Long = 0,
    val username: String,
    val password: String,
    val email: String,
    val points: Int = 0,
    val createdAt: Date = Date()
)

data class ExpenseCategory(
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val budgetLimit: Double,
    val color: String = "#4CAF50",
    val createdAt: Date = Date()
)

data class Expense(
    val id: Long = 0,
    val userId: Long,
    val categoryId: Long,
    val amount: Double,
    val date: Date,
    val description: String,
    val receiptPhotoPath: String? = null,
    val mood: String? = null,
    val characterization: String? = null
)

data class CategoryTotal(
    val categoryName: String,
    val color: String,
    val totalSpent: Double
)