// DetailedBudgetScreen.kt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devdroid.savesmart.Budget
import com.devdroid.savesmart.BudgetViewModel
import com.devdroid.savesmart.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedBudgetScreen(
    budgetViewModel: BudgetViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val budgets by budgetViewModel.budgets.collectAsState()
    val loading by budgetViewModel.loading.collectAsState()
    val transactions by transactionViewModel.transactions.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("") }

    // Get unique months from budgets
    val uniqueMonths = budgets.map { it.month }.distinct().sorted()

    // Initialize selected month if empty
    if (selectedMonth.isEmpty() && uniqueMonths.isNotEmpty()) {
        selectedMonth = uniqueMonths.last() // Default to most recent month
    }

    // Calculate total expenses per category for the selected month
    val categoryExpenses = remember(selectedMonth, transactions) {
        transactions
            .filter {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(Date(it.timestamp.seconds * 1000)) == selectedMonth &&
                        it.amount < 0
            }
            .groupBy { it.category }
            .mapValues { (_, transactions) ->
                transactions.sumOf { -it.amount.toDouble() } // Convert to positive number
            }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (uniqueMonths.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextField(
                    value = selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Month") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uniqueMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                selectedMonth = month
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredBudgets = budgets.filter { it.month == selectedMonth }

            if (filteredBudgets.isEmpty() && categoryExpenses.isNotEmpty()) {
                // Show warning if there are expenses but no budgets
                AlertCard(
                    message = "You have expenses but no budgets set for $selectedMonth",
                    color = Color(0xFFFF9800) // Orange
                )

                // Show expenses without budgets
                categoryExpenses.forEach { (category, amount) ->
                    BudgetCategoryItem(
                        budget = Budget(
                            id = "",
                            category = category,
                            month = selectedMonth,
                            totalAmount = 0.0, // No budget set
                            spentAmount = amount
                        ),
                        onDelete = {},
                        showDelete = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (filteredBudgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No budgets for $selectedMonth", fontSize = 18.sp)
                }
            } else {
                // Show budgets with actual spending
                filteredBudgets.forEach { budget ->
                    val actualSpent = categoryExpenses[budget.category] ?: 0.0
                    BudgetCategoryItem(
                        budget = budget.copy(spentAmount = actualSpent),
                        onDelete = { budgetViewModel.deleteBudget(budget.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show expenses that don't have corresponding budgets
                val budgetCategories = filteredBudgets.map { it.category }
                categoryExpenses
                    .filter { (category, _) -> !budgetCategories.contains(category) }
                    .forEach { (category, amount) ->
                        BudgetCategoryItem(
                            budget = Budget(
                                id = "",
                                category = category,
                                month = selectedMonth,
                                totalAmount = 0.0, // No budget set
                                spentAmount = amount
                            ),
                            onDelete = {},
                            showDelete = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No budgets created yet", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun AlertCard(message: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message,
                color = Color.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BudgetCategoryItem(
    budget: Budget,
    onDelete: () -> Unit,
    showDelete: Boolean = true
) {
    val remainingAmount = budget.totalAmount - budget.spentAmount
    val isOverBudget = remainingAmount < 0
    val progress = if (budget.totalAmount > 0) {
        (budget.spentAmount / budget.totalAmount).toFloat().coerceIn(0f, 1f)
    } else {
        1f // If no budget set, show full progress
    }

    // Determine warning level
    val warningLevel = when {
        isOverBudget -> 2 // Over budget
        budget.totalAmount > 0 && budget.spentAmount > budget.totalAmount * 0.8 -> 1 // Nearing budget (80%)
        else -> 0 // Within budget
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = budget.month,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                if (showDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (warningLevel) {
                    2 -> Color.Red // Over budget
                    1 -> Color(0xFFFFA500) // Orange - nearing budget
                    else -> Color.Green // Within budget
                },
                trackColor = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: $${"%.2f".format(budget.spentAmount)}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                if (budget.totalAmount > 0) {
                    Text(
                        text = "Total: $${"%.2f".format(budget.totalAmount)}",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "No budget set",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (budget.totalAmount > 0) {
                Text(
                    text = "Remaining: $${"%.2f".format(remainingAmount)}",
                    fontSize = 16.sp,
                    color = when (warningLevel) {
                        2 -> Color.Red
                        1 -> Color(0xFFFFA500) // Orange
                        else -> Color.Green
                    }
                )
            } else {
                Text(
                    text = "No budget set for this category",
                    fontSize = 16.sp,
                    color = Color(0xFFFFA500) // Orange
                )
            }

            when (warningLevel) {
                2 -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are over budget by $${"%.2f".format(-remainingAmount)}!",
                        fontSize = 14.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                1 -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You've spent ${(progress * 100).toInt()}% of your budget",
                        fontSize = 14.sp,
                        color = Color(0xFFFFA500), // Orange
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}