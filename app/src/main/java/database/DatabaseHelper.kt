package com.moneypimpworking.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date
import java.util.Calendar

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "moneypimp.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_USERS = "users"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_EXPENSES = "expenses"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                email TEXT NOT NULL,
                points INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_CATEGORIES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                budget_limit REAL NOT NULL,
                color TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_EXPENSES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                amount REAL NOT NULL,
                date INTEGER NOT NULL,
                description TEXT NOT NULL,
                receipt_photo_path TEXT,
                mood TEXT,
                characterization TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun insertUser(username: String, password: String, email: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("password", password)
            put("email", email)
            put("created_at", Date().time)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun login(username: String, password: String): User? {
        val db = readableDatabase
        return db.query(TABLE_USERS, null, "username = ? AND password = ?", arrayOf(username, password), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    points = cursor.getInt(cursor.getColumnIndexOrThrow("points")),
                    createdAt = Date(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                )
            } else null
        }
    }

    fun getUserById(userId: Long): User? {
        val db = readableDatabase
        return db.query(TABLE_USERS, null, "id = ?", arrayOf(userId.toString()), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    points = cursor.getInt(cursor.getColumnIndexOrThrow("points")),
                    createdAt = Date(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                )
            } else null
        }
    }

    fun insertCategory(userId: Long, name: String, budgetLimit: Double, color: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("name", name)
            put("budget_limit", budgetLimit)
            put("color", color)
            put("created_at", Date().time)
        }
        return db.insert(TABLE_CATEGORIES, null, values)
    }

    fun getCategoriesForUser(userId: Long): List<ExpenseCategory> {
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, "user_id = ?", arrayOf(userId.toString()), null, null, "name ASC")
        val categories = mutableListOf<ExpenseCategory>()
        while (cursor.moveToNext()) {
            categories.add(
                ExpenseCategory(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    budgetLimit = cursor.getDouble(cursor.getColumnIndexOrThrow("budget_limit")),
                    color = cursor.getString(cursor.getColumnIndexOrThrow("color")),
                    createdAt = Date(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                )
            )
        }
        cursor.close()
        return categories
    }

    fun getCategoryById(categoryId: Long): ExpenseCategory? {
        val db = readableDatabase
        return db.query(TABLE_CATEGORIES, null, "id = ?", arrayOf(categoryId.toString()), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                ExpenseCategory(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    budgetLimit = cursor.getDouble(cursor.getColumnIndexOrThrow("budget_limit")),
                    color = cursor.getString(cursor.getColumnIndexOrThrow("color")),
                    createdAt = Date(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                )
            } else null
        }
    }

    fun updateCategory(category: ExpenseCategory) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", category.name)
            put("budget_limit", category.budgetLimit)
        }
        db.update(TABLE_CATEGORIES, values, "id = ?", arrayOf(category.id.toString()))
    }

    fun deleteCategory(categoryId: Long) {
        val db = writableDatabase
        db.delete(TABLE_CATEGORIES, "id = ?", arrayOf(categoryId.toString()))
        db.delete(TABLE_EXPENSES, "category_id = ?", arrayOf(categoryId.toString()))
    }

    fun insertExpense(userId: Long, categoryId: Long, amount: Double, date: Date, description: String, receiptPhotoPath: String?, mood: String? = null, characterization: String? = null): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("category_id", categoryId)
            put("amount", amount)
            put("date", date.time)
            put("description", description)
            put("receipt_photo_path", receiptPhotoPath)
            put("mood", mood)
            put("characterization", characterization)
        }
        val id = db.insert(TABLE_EXPENSES, null, values)
        if (id > 0) {
            // Award 10 points per expense
            db.execSQL("UPDATE $TABLE_USERS SET points = points + 10 WHERE id = ?", arrayOf(userId))
        }
        return id
    }

    fun getExpensesBetweenDates(userId: Long, startDate: Date, endDate: Date): List<Expense> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EXPENSES, null,
            "user_id = ? AND date BETWEEN ? AND ?",
            arrayOf(userId.toString(), startDate.time.toString(), endDate.time.toString()),
            null, null, "date DESC"
        )
        val expenses = mutableListOf<Expense>()
        while (cursor.moveToNext()) {
            expenses.add(
                Expense(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("category_id")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    date = Date(cursor.getLong(cursor.getColumnIndexOrThrow("date"))),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    receiptPhotoPath = cursor.getString(cursor.getColumnIndexOrThrow("receipt_photo_path")),
                    mood = cursor.getString(cursor.getColumnIndexOrThrow("mood")),
                    characterization = cursor.getString(cursor.getColumnIndexOrThrow("characterization"))
                )
            )
        }
        cursor.close()
        return expenses
    }

    fun addPoints(userId: Long, points: Int) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_USERS SET points = points + ? WHERE id = ?", arrayOf(points, userId))
    }

    fun getExpenseCount(userId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_EXPENSES WHERE user_id = ?", arrayOf(userId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getPhotoExpenseCount(userId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_EXPENSES WHERE user_id = ? AND receipt_photo_path IS NOT NULL", arrayOf(userId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getCategoryCount(userId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CATEGORIES WHERE user_id = ?", arrayOf(userId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getCategorySpentThisMonth(userId: Long, categoryId: Long): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endTime = calendar.timeInMillis

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(amount) FROM $TABLE_EXPENSES WHERE user_id = ? AND category_id = ? AND date BETWEEN ? AND ?",
            arrayOf(userId.toString(), categoryId.toString(), startTime.toString(), endTime.toString())
        )
        var spent = 0.0
        if (cursor.moveToFirst()) {
            spent = cursor.getDouble(0)
        }
        cursor.close()
        return spent
    }

    fun hasWeeklyWarrior(userId: Long): Boolean {
        // Log expenses 7 days (distinct days in last 7 days)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val startTime = calendar.timeInMillis

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(DISTINCT(date / 86400000)) FROM $TABLE_EXPENSES WHERE user_id = ? AND date >= ?",
            arrayOf(userId.toString(), startTime.toString())
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count >= 7
    }

    fun deleteExpense(expenseId: Long) {
        val db = writableDatabase
        db.delete(TABLE_EXPENSES, "id = ?", arrayOf(expenseId.toString()))
    }

    fun getCategoryTotals(userId: Long, startDate: Date, endDate: Date): List<CategoryTotal> {
        val db = readableDatabase
        val query = """
            SELECT c.name as categoryName, c.color, SUM(e.amount) as totalSpent 
            FROM $TABLE_EXPENSES e 
            JOIN $TABLE_CATEGORIES c ON e.category_id = c.id 
            WHERE e.user_id = ? AND e.date BETWEEN ? AND ? 
            GROUP BY c.id 
            ORDER BY totalSpent DESC
        """
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), startDate.time.toString(), endDate.time.toString()))
        val totals = mutableListOf<CategoryTotal>()
        while (cursor.moveToNext()) {
            totals.add(
                CategoryTotal(
                    categoryName = cursor.getString(0),
                    color = cursor.getString(1),
                    totalSpent = cursor.getDouble(2)
                )
            )
        }
        cursor.close()
        return totals
    }

    fun getTotalSpent(userId: Long, startDate: Date, endDate: Date): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COALESCE(SUM(amount), 0) FROM $TABLE_EXPENSES WHERE user_id = ? AND date BETWEEN ? AND ?",
            arrayOf(userId.toString(), startDate.time.toString(), endDate.time.toString())
        )
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }
}