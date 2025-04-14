package com.devdroid.savesmart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.devdroid.savesmart.R
import com.devdroid.savesmart.model.Transaction
import com.devdroid.savesmart.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
///hello
@Composable
fun TransactionScreen(navController: NavController, transactionViewModel: TransactionViewModel = viewModel()) {
    val transactions by transactionViewModel.transactions.collectAsState()

    LaunchedEffect(Unit) {
        transactionViewModel.fetchTransactions()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F0)) // Light cream background
    ) {
        var selectedMonth by remember { mutableStateOf(getCurrentMonthYear()) }
        val filteredTransactions = transactions.filter {
            SimpleDateFormat(
                "MMMM yyyy",
                Locale.getDefault()
            ).format(Date(it.timestamp.seconds * 1000)) == selectedMonth
        }

        // Create a map to hold grouped transactions
        val transactionsByDate = filteredTransactions
            .sortedByDescending { it.timestamp.seconds }
            .groupBy { transaction ->
                val transactionDate = Date(transaction.timestamp.seconds * 1000)
                
                // Create calendar instances with the default timezone
                val today = Calendar.getInstance()
                val yesterday = Calendar.getInstance()
                yesterday.add(Calendar.DAY_OF_YEAR, -1)
                
                // Reset time part to start of day for comparison
                clearTimeFields(today)
                clearTimeFields(yesterday)
                
                // Create a calendar for transaction date
                val transactionCal = Calendar.getInstance()
                transactionCal.time = transactionDate
                clearTimeFields(transactionCal)
                
                // Compare dates
                when {
                    // Same day as today
                    isSameDay(transactionCal, today) -> "Today"
                    // Same day as yesterday
                    isSameDay(transactionCal, yesterday) -> "Yesterday"
                    // Any other day
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(transactionDate)
                }
            }
            
        // Create a sorted map to ensure correct display order (Today, Yesterday, then dates in descending order)
        val groupedTransactions = LinkedHashMap<String, List<Transaction>>()
        
        // First add Today if it exists
        if (transactionsByDate.containsKey("Today")) {
            groupedTransactions["Today"] = transactionsByDate["Today"]!!
        }
        
        // Then add Yesterday if it exists
        if (transactionsByDate.containsKey("Yesterday")) {
            groupedTransactions["Yesterday"] = transactionsByDate["Yesterday"]!!
        }
        
        // Then add all other dates in descending order
        transactionsByDate
            .filter { it.key != "Today" && it.key != "Yesterday" }
            .toList()
            .sortedByDescending { (dateStr, _) ->
                // Parse the date string back to Date for comparison
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(dateStr)?.time ?: 0
            }
            .forEach { (date, transactions) ->
                groupedTransactions[date] = transactions
            }

        Column(modifier = Modifier.fillMaxSize().padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
            top = 24.dp // Increased top padding
        )) {

            // Month Dropdown
            MonthDropdown(selectedMonth) { newMonth -> selectedMonth = newMonth }

            Spacer(modifier = Modifier.height(16.dp)) // Increased spacing

            // Financial Report Box
            FinancialReportBox()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.padding(bottom = 80.dp) // Add bottom padding to avoid navigation bar overlap
            ) {
                groupedTransactions.forEach { (date, transactions) ->
                    item {
                        Text(
                            text = date,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }
        }
    }
}

// Month Dropdown Component
@Composable
fun MonthDropdown(selectedMonth: String, onMonthSelected: (String) -> Unit) {
    val months = getLastSixMonths()
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Month",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Box {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { expanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMonth,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Icon(
                        painter = painterResource(id = if (expanded) R.drawable.ic_arrow_right else R.drawable.ic_arrow_right),
                        contentDescription = "Dropdown Arrow",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = month,
                                fontSize = 16.sp,
                                fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (month == selectedMonth) Color(0xFF6200EE) else Color.Black
                            ) 
                        },
                        onClick = {
                            onMonthSelected(month)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Financial Report Box
@Composable
fun FinancialReportBox() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E6FF)), // Light purple background to match image
        elevation = CardDefaults.cardElevation(2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "See your financial report",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6200EE), // Purple text
                modifier = Modifier.align(Alignment.CenterStart)
            )
            
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "View Report",
                tint = Color(0xFF6200EE), // Purple icon
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
            )
        }
    }
}

// Function to get the current month in "MMMM yyyy" format
fun getCurrentMonthYear(): String {
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
}

// Function to get last six months in "MMMM yyyy" format
fun getLastSixMonths(): List<String> {
    val calendar = Calendar.getInstance()
    return List(6) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time).also {
            calendar.add(Calendar.MONTH, -1)
        }
    }
}

// Updated TransactionItem to display stored time and description with new design
@Composable
fun TransactionItem(transaction: Transaction) {
    val isIncome = transaction.amount > 0
    val amountText = if (isIncome) "+ $${transaction.amount}" else "- $${Math.abs(transaction.amount)}"
    val textColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    val icon = when (transaction.category.lowercase()) {
        "shopping" -> R.drawable.ic_shopping
        "subscription" -> R.drawable.ic_subscription
        "food" -> R.drawable.ic_food
        "salary" -> R.drawable.ic_salary
        "transportation" -> R.drawable.ic_transportation
        else -> R.drawable.ic_default
    }

    val backgroundColor = when (transaction.category.lowercase()) {
        "shopping" -> Color(0xFFFEF3E0) // Light orange
        "subscription" -> Color(0xFFE8EAF6) // Light purple
        "food" -> Color(0xFFFFEBEE) // Light red
        "salary" -> Color(0xFFE0F2F1) // Light green
        "transportation" -> Color(0xFFE1F5FE) // Light blue
        else -> Color(0xFFF5F5F5) // Light grey
    }

    val storedTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val storedTime = storedTimeFormat.format(Date(transaction.timestamp.seconds * 1000))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon with colored background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = backgroundColor,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = transaction.category,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = transaction.note.ifEmpty { "No description" },
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = storedTime,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// Helper function to clear time fields from a calendar
fun clearTimeFields(calendar: Calendar) {
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
}

// Helper function to check if two calendar dates are the same day
fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
