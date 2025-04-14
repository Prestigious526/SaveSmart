package com.devdroid.savesmart

import BudgetScreen
import ExpenseScreen
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.Icon
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdroid.savesmart.ui.theme.SaveSmartTheme
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.navigation.compose.currentBackStackEntryAsState
import com.devdroid.savesmart.viewmodel.TransactionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devdroid.savesmart.ui.TransactionItem
import com.devdroid.savesmart.ui.TransactionScreen
import com.devdroid.savesmart.viewmodel.BudgetViewModel

class Homescreen : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaveSmartTheme {
                val transactionViewModel: TransactionViewModel = viewModel()
                val budgetViewModel: BudgetViewModel = viewModel()
                FinanceTrackerScreen(transactionViewModel, budgetViewModel)

            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FinanceTrackerScreen(
    viewModel: TransactionViewModel = viewModel(),
    transactionViewModel: BudgetViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route ?: "home" // Default to home if null
    
    // Collect income and expense values from the view model
    val totalIncome by viewModel.totalIncome.collectAsState(0)
    val totalExpenses by viewModel.totalExpenses.collectAsState(0)
    val netBalance = totalIncome + totalExpenses

    // Fetch data when screen first loads
    LaunchedEffect(Unit) {
        viewModel.fetchAllTransactions()
    }

    Scaffold(
        bottomBar = { BottomNavigation(navController = navController, currentRoute = currentRoute) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F8F0)) // Light cream background
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    TopBar()
                    
                    // Display the account balance (income + expenses)
                    AccountBalance(balance = netBalance)
                    
                    // Pass income and expense values to the row
                    IncomeExpenseRow(totalIncome, totalExpenses, navController)
                    SpendFrequencySection()
                    RecentTransactionsSection(viewModel, navController)
                }
            }

            // Keep the rest of the composables as is
            composable("income") { IncomeScreen(navController, viewModel) }
            composable("expense") { ExpenseScreen(navController, viewModel) }
            composable("transactions") { TransactionScreen(navController, viewModel) }
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    onAccountClick = {},
                    onSettingsClick = { navController.navigate("settings") },
                    onExportDataClick = {},
                    onLogoutClick = {}
                )
            }
            composable("settings") { SettingsScreen(navController) }
            composable("about") { AboutScreen(navController) }
            composable("help") { HelpScreen(navController) }
            composable("budget") { BudgetScreen(navController, budgetViewModel) }
            composable("detailedBudgetScreen") { DetailedBudgetScreen(budgetViewModel) }
        }
    }
}

@Composable
fun BottomNavigation(navController: NavController, currentRoute: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val selectedColor = Color(0xFF6949FF)
        val defaultColor = Color.Gray

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = if (currentRoute == "home") selectedColor else defaultColor) },
            label = { Text("Home", color = if (currentRoute == "home") selectedColor else defaultColor) },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") },
            modifier = Modifier.weight(1f)
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "Transactions", tint = if (currentRoute == "transactions") selectedColor else defaultColor) },
            label = { Text("Transactions", color = if (currentRoute == "transactions") selectedColor else defaultColor) },
            selected = currentRoute == "transactions",
            onClick = { navController.navigate("transactions") },
            modifier = Modifier.weight(1f)
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Budget", tint = if (currentRoute == "budget") selectedColor else defaultColor) },
            label = { Text("Budget", color = if (currentRoute == "budget") selectedColor else defaultColor) },
            selected = currentRoute == "budget",
            onClick = { navController.navigate("budget") },
            modifier = Modifier.weight(1f)
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = if (currentRoute == "profile") selectedColor else defaultColor) },
            label = { Text("Profile", color = if (currentRoute == "profile") selectedColor else defaultColor) },
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User image on left - using Box instead of Image to avoid the unresolved reference
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(20.dp)) // Using RoundedCornerShape instead of CircleShape
            ) {
                // Avatar placeholder
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }

            // Notification bell on right
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color(0xFF6949FF), // Purple notification bell
                modifier = Modifier.size(28.dp)
            )
        }

        // Month selector dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
        ) {
            MonthDropdown()
        }
    }
}

@Composable
fun MonthDropdown() {
    var selectedMonth by remember { mutableStateOf("October") } // Initial selected month
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.width(200.dp)
    ) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6949FF))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selectedMonth,
                    fontSize = 18.sp,
                    color = Color(0xFF6949FF), // Purple text for month
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Icon",
                    tint = Color(0xFF6949FF) // Purple dropdown icon
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = month,
                            fontSize = 16.sp,
                            color = if (month == selectedMonth) Color(0xFF6949FF) else Color.Black
                        )
                    },
                    onClick = {
                        selectedMonth = month
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AccountBalance(balance: Int) {
    val isNegative = balance < 0
    val displayBalance = if (isNegative) "-$${Math.abs(balance)}" else "$${balance}"
    val balanceColor = if (isNegative) Color(0xFFFF5252) else Color(0xFF4CAF50) // Red for negative, green for positive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Account Balance",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = displayBalance,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = balanceColor
        )
    }
}

@Composable
fun IncomeExpenseRow(income: Int, expenses: Int, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .clickable { navController.navigate("income") },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50) // Green card for income
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Income",
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Income",
                        color = Color.White
                    )
                    Text(
                        text = "$$income",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .clickable { navController.navigate("expense") },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF5252) // Red card for expenses
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Expenses",
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Expenses",
                        color = Color.White
                    )
                    Text(
                        // Show the absolute value of expenses for display purposes
                        text = "$${Math.abs(expenses)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SpendFrequencySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Spend Frequency",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            color = Color.Black
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            // Purple line chart for spend frequency
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                val width = size.width
                val height = size.height

                // Create a wave pattern similar to screenshot
                path.moveTo(0f, height * 0.7f)
                path.cubicTo(
                    width * 0.2f, height * 0.9f,
                    width * 0.4f, height * 0.3f,
                    width * 0.6f, height * 0.5f
                )
                path.cubicTo(
                    width * 0.8f, height * 0.7f,
                    width * 0.9f, height * 0.1f,
                    width, height * 0.4f
                )

                // Draw the path with purple color and stroke
                drawPath(
                    path = path,
                    color = Color(0xFF6949FF),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Time filter chips with the first one selected
            FilterChip(
                selected = true,
                onClick = { /* TODO */ },
                label = { Text("Today") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFF4D9), // Light yellow background
                    selectedLabelColor = Color.Black
                )
            )
            FilterChip(
                selected = false,
                onClick = { /* TODO */ },
                label = { Text("Week") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFFF0F0F0),
                    labelColor = Color.Gray
                )
            )
            FilterChip(
                selected = false,
                onClick = { /* TODO */ },
                label = { Text("Month") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFFF0F0F0),
                    labelColor = Color.Gray
                )
            )
            FilterChip(
                selected = false,
                onClick = { /* TODO */ },
                label = { Text("Year") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFFF0F0F0),
                    labelColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun RecentTransactionsSection(viewModel: TransactionViewModel, navController: NavController) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())

    // Show up to 3 recent transactions
    val recentTransactions = transactions.sortedByDescending { it.timestamp.seconds }.take(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            TextButton(
                onClick = { navController.navigate("transactions") },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6949FF))
            ) {
                Text(
                    "See All",
                    modifier = Modifier
                        .background(
                            color = Color(0xFFEFE9FF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color(0xFF6949FF)
                )
            }
        }

        if (recentTransactions.isNotEmpty()) {
            recentTransactions.forEach { transaction ->
                TransactionItem(transaction = transaction)
            }
        } else {
            Text(
                text = "No recent transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun FinanceTrackerPreview() {
    SaveSmartTheme {
        FinanceTrackerScreen()
    }
}
