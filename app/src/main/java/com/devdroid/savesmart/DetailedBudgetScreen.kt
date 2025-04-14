package com.devdroid.savesmart

import androidx.compose.foundation.clickable
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
import com.devdroid.savesmart.model.Budget
import com.devdroid.savesmart.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun DetailedBudgetScreen(viewModel: BudgetViewModel = viewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var selectedMonth by remember { mutableStateOf(getCurrentMonth()) }

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
        MonthFilterDropdown(
            selectedMonth = selectedMonth,
            onMonthSelected = { selectedMonth = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        BudgetSummary(
            budgets = budgets.filter {
                it.month.equals(selectedMonth, ignoreCase = true)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val filteredBudgets = budgets.filter {
            it.month.equals(selectedMonth, ignoreCase = true)
        }

        if (filteredBudgets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No budgets for $selectedMonth",
                    fontSize = 18.sp
                )
            }
        } else {
            filteredBudgets.forEach { budget ->
                BudgetCategoryItem(
                    budget = budget,
                    onDelete = { viewModel.deleteBudget(budget.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MonthFilterDropdown(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedMonth,
            onValueChange = {},
            label = { Text("Filter by Month") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable { expanded = true }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BudgetSummary(budgets: List<Budget>) {
    val totalBudget = budgets.sumOf { it.totalAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val remaining = totalBudget - totalSpent
    val isOverBudget = remaining < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Budget", style = MaterialTheme.typography.bodySmall)
                    Text("$${"%.2f".format(totalBudget)}")
                }
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.bodySmall)
                    Text("$${"%.2f".format(totalSpent)}")
                }
                Column {
                    Text("Remaining", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "$${"%.2f".format(abs(remaining))}",
                        color = if (isOverBudget) Color.Red else Color.Green
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetCategoryItem(
    budget: Budget,
    onDelete: () -> Unit
) {
    val remainingAmount = budget.totalAmount - budget.spentAmount
    val isOverBudget = remainingAmount < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (budget.spentAmount / budget.totalAmount).toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isOverBudget) Color.Red else Color.Green,
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
                Text(
                    text = "Total: $${"%.2f".format(budget.totalAmount)}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Remaining: $${"%.2f".format(remainingAmount)}",
                fontSize = 16.sp,
                color = if (isOverBudget) Color.Red else Color.Green
            )

            if (isOverBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are over budget!",
                    fontSize = 14.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun getCurrentMonth(): String {
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
}