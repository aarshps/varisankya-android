package com.hora.varisankya.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.hora.varisankya.Subscription
import com.hora.varisankya.PaymentRecord
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var snapshotListener: ListenerRegistration? = null


    
    // Unified Data Holder
    val subscriptions = MutableLiveData<List<Subscription>>(emptyList())
    
    val isLoading = MutableLiveData(true)
    val error = MutableLiveData<String?>(null)
    
    init {
        // No more tutorial observer needed
    }
    
    // Hero Section State
    data class HeroState(
        val totalAmount: Double = 0.0,
        val nextPayment: Subscription? = null,
        val overdueSubscriptions: List<Subscription> = emptyList(),
        val activeSubscriptions: List<Subscription> = emptyList()
    )
    val heroState = MutableLiveData(HeroState())

    fun loadSubscriptions() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            isLoading.value = false
            return
        }
        
        // Remove existing listener if any
        snapshotListener?.remove()

        isLoading.value = true
        
        snapshotListener = firestore.collection("users").document(userId).collection("subscriptions")
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("MainViewModel", "Listen failed.", e)
                    error.value = e.message
                    isLoading.value = false
                    return@addSnapshotListener
                }

                val subs = snapshots?.toObjects(Subscription::class.java) ?: emptyList()
                
                // Sort: Active first, then by due date
                val sortedSubscriptions = subs.sortedWith(compareByDescending<Subscription> { it.active }.thenBy { it.dueDate })
                
                subscriptions.value = sortedSubscriptions
                calculateHeroData(sortedSubscriptions)
                isLoading.value = false
            }
    }
    
    private fun calculateHeroData(allSubs: List<Subscription>) {
        val activeSubs = allSubs.filter { it.active && it.dueDate != null }
        
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)
        
        var totalAmount = 0.0
        val overdue = mutableListOf<Subscription>() // RESTORED
        
        for (sub in activeSubs) {
            val subDate = Calendar.getInstance()
            subDate.time = sub.dueDate!!
            subDate.set(Calendar.HOUR_OF_DAY, 0)
            subDate.set(Calendar.MINUTE, 0)
            subDate.set(Calendar.SECOND, 0)
            subDate.set(Calendar.MILLISECOND, 0)
            
            val isOverdue = subDate.before(today)
            val isCurrentMonth = subDate.get(Calendar.MONTH) == currentMonth && subDate.get(Calendar.YEAR) == currentYear
            
            if (isOverdue) {
                overdue.add(sub)
                totalAmount += sub.cost
            } else if (isCurrentMonth) {
                totalAmount += sub.cost
            }
        }
        
        // Next future payment
        val nextPayment = activeSubs
            .filter { 
                val d = Calendar.getInstance()
                d.time = it.dueDate!!
                d.set(Calendar.HOUR_OF_DAY, 0)
                d.set(Calendar.MINUTE, 0)
                d.set(Calendar.SECOND, 0)
                d.set(Calendar.MILLISECOND, 0)
                !d.before(today)
            }
            .minByOrNull { it.dueDate!! }
            
        heroState.value = HeroState(
            totalAmount = totalAmount,
            nextPayment = nextPayment,
            overdueSubscriptions = overdue,
            activeSubscriptions = activeSubs
        )
    }
    fun markAsPaid(subscription: Subscription, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val subId = subscription.id ?: return


        val payment = PaymentRecord(
            date = Date(),
            amount = subscription.cost,
            subscriptionName = subscription.name,
            subscriptionId = subId,
            currency = subscription.currency,
            userId = userId
        )
        
        val nextDueDate = calculateNextDueDate(subscription.dueDate ?: Date(), subscription.recurrence)

        val batch = firestore.batch()
        val subRef = firestore.collection("users").document(userId).collection("subscriptions").document(subId)
        val paymentRef = subRef.collection("payments").document()

        batch.set(paymentRef, payment)
        if (nextDueDate != null) {
            batch.update(subRef, "dueDate", nextDueDate)
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
    
    fun updateSubscriptionStatus(subscription: Subscription, isActive: Boolean, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val subId = subscription.id ?: return

        firestore.collection("users").document(userId).collection("subscriptions").document(subId)
            .update("active", isActive)
            .addOnSuccessListener { onSuccess() }
    }

    fun deleteSubscription(subscription: Subscription, onSuccess: () -> Unit) {
         val userId = auth.currentUser?.uid ?: return
         val subId = subscription.id ?: return

         firestore.collection("users").document(userId).collection("subscriptions").document(subId)
            .delete()
            .addOnSuccessListener { onSuccess() }
    }

    private fun calculateNextDueDate(fromDate: Date, recurrence: String): Date? {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = fromDate
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (recurrence == "Custom") return null

        if (recurrence.startsWith("Every ")) {
            val parts = recurrence.split(" ")
            if (parts.size >= 3) {
                val freq = parts[1].toIntOrNull() ?: 1
                val unit = parts[2]
                when (unit) {
                    "Months", "Month" -> cal.add(Calendar.MONTH, freq)
                    "Years", "Year" -> cal.add(Calendar.YEAR, freq)
                    "Weeks", "Week" -> cal.add(Calendar.WEEK_OF_YEAR, freq)
                    "Days", "Day" -> cal.add(Calendar.DAY_OF_YEAR, freq)
                    else -> cal.add(Calendar.MONTH, freq)
                }
            }
        } else {
             when (recurrence) {
                "Monthly" -> cal.add(Calendar.MONTH, 1)
                "Yearly" -> cal.add(Calendar.YEAR, 1)
                "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
        }
        return cal.time
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }
}
