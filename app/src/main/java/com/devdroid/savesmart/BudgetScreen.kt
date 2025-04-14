// BudgetScreen.kt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devdroid.savesmart.BudgetViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetApp()
        }
    }
}

@Composable
fun BudgetApp() {
    val navController = rememberNavController()
    val viewModel: BudgetViewModel = viewModel()

    NavHost(navController = navController, startDestination = "budgetScreen") {
        composable("budgetScreen") {
            BudgetScreen(navController, viewModel)
        }
        composable("detailedBudgetScreen") {
            DetailedBudgetScreen(viewModel)
        }
    }
}

@Composable
fun BudgetScreen(navController: NavController, viewModel: BudgetViewModel) {
    var budget by remember { mutableStateOf("") }
    var displayText by remember { mutableStateOf("Your budget: $0.00") }
    var selectedMonth by remember { mutableStateOf("Select Month") }
    var selectedCategory by remember { mutableStateOf("Select Category") }
    var monthExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val categories = listOf(
        "Groceries", "Transportation", "Entertainment", "Food & Dining",
        "Shopping", "Bills & Utilities", "Healthcare", "Education", "Travel", "Other"
    )

    // Check if user is logged in
    val auth = Firebase.auth
    if (auth.currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") // You should implement login screen
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Budget Planner",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Month Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = selectedMonth,
                onValueChange = {},
                label = { Text("Select Month") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { monthExpanded = true },
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown",
                        modifier = Modifier.clickable { monthExpanded = true })
                }
            )
            DropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month) },
                        onClick = {
                            selectedMonth = month
                            monthExpanded = false
                        }
                    )
                }
            }
        }

        // Category Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                label = { Text("Select Category") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categoryExpanded = true },
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown",
                        modifier = Modifier.clickable { categoryExpanded = true })
                }
            )
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Budget Input
        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it },
            label = { Text("Enter your budget") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        // Create Budget Button
        Button(
            onClick = {
                val budgetValue = budget.toDoubleOrNull()
                if (budgetValue != null && budgetValue >= 0 &&
                    selectedMonth != "Select Month" &&
                    selectedCategory != "Select Category") {

                    viewModel.createBudget(selectedMonth, selectedCategory, budgetValue)
                    displayText = "Budget for $selectedMonth ($selectedCategory): $${"%.2f".format(budgetValue)}"
                } else {
                    displayText = "Invalid input. Please enter a valid number and select month/category."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text("Create Budget")
        }

        // Display Budget
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // View Detailed Budget Button
        Button(
            onClick = { navController.navigate("detailedBudgetScreen") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
            )
        ) {
            Text("View Detailed Budget")
        }
    }
}