package com.devdroid.savesmart.model

import java.util.*

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val month: String = "",
    val year: String = "",
    val category: String = "",
    val totalAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val createdAt: Date = Date()
)