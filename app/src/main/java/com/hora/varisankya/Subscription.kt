package com.hora.varisankya

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Subscription(
    @DocumentId 
    val id: String? = null,
    
    val name: String = "",
    
    val dueDate: Date? = null,
    
    val cost: Double = 0.0,
    
    val currency: String = "USD",
    
    val recurrence: String = "Monthly",
    
    val category: String = "Entertainment",
    
    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean = true,

    @get:PropertyName("autopay")
    @set:PropertyName("autopay")
    var autopay: Boolean = false
) {
    // Zero-argument constructor required for Firestore
    constructor() : this(null, "", null, 0.0, "USD", "Monthly", "Entertainment", true, false)
}