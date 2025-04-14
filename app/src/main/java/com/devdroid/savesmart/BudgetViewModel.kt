package com.devdroid.savesmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devdroid.savesmart.model.Budget
import com.devdroid.savesmart.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BudgetViewModel(
    private val transactionViewModel: TransactionViewModel
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = Firebase.firestore

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _selectedMonth = MutableStateFlow(getCurrentMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth

    private val _selectedYear = MutableStateFlow(getCurrentYear())
    val selectedYear: StateFlow<String> = _selectedYear

    init {
        loadBudgets()
        setupExpenseListener()
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
        updateBudgetsWithExpenses()
    }

    fun setSelectedYear(year: String) {
        _selectedYear.value = year
        updateBudgetsWithExpenses()
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = auth.currentUser?.uid ?: return@launch

                db.collection("budgets")
                    .whereEqualTo("userId", currentUser)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _loading.value = false
                            return@addSnapshotListener
                        }

                        _budgets.value = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Budget::class.java)?.copy(id = doc.id)
                        } ?: emptyList()

                        updateBudgetsWithExpenses()
                        _loading.value = false
                    }
            } catch (e: Exception) {
                _loading.value = false
            }
        }
    }

    private fun setupExpenseListener() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser?.uid ?: return@launch

                db.collection("transactions")
                    .whereEqualTo("userId", currentUser)
                    .whereEqualTo("type", "Expense")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        updateBudgetsWithExpenses()
                    }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateBudgetsWithExpenses() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = auth.currentUser?.uid ?: return@launch
                val month = _selectedMonth.value
                val year = _selectedYear.value

                // Get all transactions for the selected month/year
                val transactions = db.collection("transactions")
                    .whereEqualTo("userId", currentUser)
                    .whereEqualTo("type", "Expense")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Transaction::class.java) }
                    .filter { transaction ->
                        val transactionDate = Date(transaction.date.seconds * 1000)
                        val transactionMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(transactionDate)
                        val transactionYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(transactionDate)
                        transactionMonth.equals(month, ignoreCase = true) &&
                                transactionYear == year
                    }

                // Group transactions by category
                val expensesByCategory = transactions.groupBy { it.category }
                    .mapValues { (_, transactions) ->
                        transactions.sumOf { abs(it.amount) }
                    }

                // Update budgets with new spent amounts
                _budgets.value.forEach { budget ->
                    if (budget.month.equals(month, ignoreCase = true)) {
                        val spentAmount = expensesByCategory[budget.category] ?: 0.0
                        if (budget.spentAmount != spentAmount) {
                            db.collection("budgets")
                                .document(budget.id)
                                .update("spentAmount", spentAmount)
                                .await()
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _loading.value = false
            }
        }
    }

    fun createBudget(category: String, amount: Double) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = auth.currentUser?.uid ?: return@launch

                // Check if budget already exists for this category/month/year
                val existingBudget = db.collection("budgets")
                    .whereEqualTo("userId", currentUser)
                    .whereEqualTo("month", _selectedMonth.value)
                    .whereEqualTo("category", category)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

                if (existingBudget != null) {
                    // Update existing budget
                    db.collection("budgets")
                        .document(existingBudget.id)
                        .update("totalAmount", FieldValue.increment(amount))
                        .await()
                } else {
                    // Create new budget
                    val budget = Budget(
                        userId = currentUser,
                        month = _selectedMonth.value,
                        year = _selectedYear.value,
                        category = category,
                        totalAmount = amount,
                        spentAmount = 0.0
                    )

                    db.collection("budgets")
                        .document(budget.id)
                        .set(budget)
                        .await()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                db.collection("budgets")
                    .document(budgetId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getBudgetProgress(budget: Budget): Float {
        return if (budget.totalAmount > 0) {
            (budget.spentAmount / budget.totalAmount).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    fun isOverBudget(budget: Budget): Boolean {
        return budget.spentAmount > budget.totalAmount
    }

    fun getRemainingAmount(budget: Budget): Double {
        return budget.totalAmount - budget.spentAmount
    }

    private fun getCurrentMonth(): String {
        return SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    private fun getCurrentYear(): String {
        return SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    }
}