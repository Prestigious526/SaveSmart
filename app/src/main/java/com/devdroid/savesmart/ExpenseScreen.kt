import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifiera
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devdroid.savesmart.viewmodel.TransactionViewModel
import com.devdroid.savesmart.model.Transaction
//import com.devdroid.savesmart.viewmodel.TransactionViewModel
import java.util.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    navController: NavController? = null,
    viewModel: TransactionViewModel
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }  // Added description field
    var isCategoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Date input fields
    var dayInput by remember { mutableStateOf("") }
    var monthInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("") }

    val categories = listOf(
        "Groceries", "Transportation", "Entertainment", "Food & Dining",
        "Shopping", "Bills & Utilities", "Healthcare", "Education", "Travel", "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF4558))
    ) {
        TopAppBar(
            title = { Text("Add Expense", style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)) },
            navigationIcon = {
                IconButton(onClick = { navController?.navigateUp() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF4558))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        placeholder = { Text("Enter Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color(0xFFF5F5F5),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF6C63FF),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCategoryExpanded,
                        onExpandedChange = { isCategoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFFF5F5F5),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF6C63FF),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryExpanded,
                            onDismissRequest = { isCategoryExpanded = false }
                        ) {
                            categories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        category = option
                                        isCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Enter Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color(0xFFF5F5F5),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF6C63FF),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    // Date Input Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = dayInput,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) dayInput = it },
                            placeholder = { Text("DD") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFFF5F5F5),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF6C63FF),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) monthInput = it },
                            placeholder = { Text("MM") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFFF5F5F5),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF6C63FF),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) yearInput = it },
                            placeholder = { Text("YYYY") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFFF5F5F5),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color(0xFF6C63FF),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (amount.isEmpty() || category.isEmpty() || dayInput.isEmpty() || monthInput.isEmpty() || yearInput.isEmpty()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            } else {
                                val dateString = "$dayInput/$monthInput/$yearInput"
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val dateObject: Date? = sdf.parse(dateString)

                                val firebaseTimestamp: Timestamp = dateObject?.let { Timestamp(it) } ?: Timestamp.now()

                                val transaction = Transaction(
                                    id = UUID.randomUUID().toString(),
                                    amount = -amount.toInt(),  // Ensuring amount is stored as negative
                                    category = category,
                                    note = description,  // Saving description
                                    type = "Expense",
                                    date = firebaseTimestamp
                                )

                                viewModel.addExpense(amount.toInt(), category, description) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Expense added!", Toast.LENGTH_SHORT).show()
                                        navController?.navigateUp()
                                    } else {
                                        Toast.makeText(context, "Failed to add expense", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Expense", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

