package com.devdroid.savesmart

// BudgetViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val month: String = "",
    val category: String = "",
    val totalAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val createdAt: Date = Date()
)

class BudgetViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = Firebase.firestore

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = auth.currentUser?.uid ?: return@launch

                db.collection("budgets")
                    .whereEqualTo("userId", currentUser)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            // Handle error
                            return@addSnapshotListener
                        }

                        val budgetList = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Budget::class.java)
                        } ?: emptyList()

                        _budgets.value = budgetList
                        _loading.value = false
                    }
            } catch (e: Exception) {
                _loading.value = false
                // Handle error
            }
        }
    }

    fun createBudget(month: String, category: String, amount: Double) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = auth.currentUser?.uid ?: return@launch

                val budget = Budget(
                    userId = currentUser,
                    month = month,
                    category = category,
                    totalAmount = amount,
                    spentAmount = 0.0
                )

                db.collection("budgets").document(budget.id)
                    .set(budget)
                    .await()

                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                // Handle error
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                db.collection("budgets").document(budgetId)
                    .delete()
                    .await()

                _loading.value = false
            } catch (e: Exception) {
                _loading.value = false
                // Handle error
            }
        }
    }

    fun updateSpentAmount(budgetId: String, amount: Double) {
        viewModelScope.launch {
            try {
                db.collection("budgets").document(budgetId)
                    .update("spentAmount", amount)
                    .await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}