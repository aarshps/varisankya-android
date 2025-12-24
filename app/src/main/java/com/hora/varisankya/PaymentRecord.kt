package com.hora.varisankya

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class PaymentRecord(
    @DocumentId val id: String? = null,
    val date: Date? = null,
    val amount: Double = 0.0,
    val subscriptionName: String = "",
    val subscriptionId: String = "",
    val currency: String = "USD",
    val userId: String = ""
)