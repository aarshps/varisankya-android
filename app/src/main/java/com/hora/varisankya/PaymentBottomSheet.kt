package com.hora.varisankya

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PaymentBottomSheet(
    private val subscription: Subscription,
    private val onPaymentRecorded: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var historyRecycler: RecyclerView
    private lateinit var noHistoryContainer: View
    private lateinit var textNoHistory: TextView
    private lateinit var progressHistory: ProgressBar
    private lateinit var btnPayCurrent: Button
    private lateinit var btnPayCustom: Button
    private lateinit var textDueInfo: TextView
    private lateinit var textNextPreview: TextView

    private var currentDueDate: Date? = null
    private var projectedNextDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_payment, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        historyRecycler = view.findViewById(R.id.recycler_history)
        noHistoryContainer = view.findViewById(R.id.no_history_container)
        textNoHistory = view.findViewById(R.id.text_no_history)
        progressHistory = view.findViewById(R.id.progress_history)
        btnPayCurrent = view.findViewById(R.id.btn_pay_current)
        btnPayCustom = view.findViewById(R.id.btn_pay_custom)
        textDueInfo = view.findViewById(R.id.text_due_date_info)
        textNextPreview = view.findViewById(R.id.text_next_due_date_preview)

        currentDueDate = subscription.dueDate ?: Date()
        setupUI()
        loadHistory()
        calculateDates(currentDueDate!!)

        return view
    }

    private fun setupUI() {
        historyRecycler.layoutManager = LinearLayoutManager(context)

        btnPayCurrent.setOnClickListener {
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
            PreferenceHelper.performHaptics(it, haptic)
            recordPayment(currentDueDate!!, projectedNextDate)
        }

        btnPayCustom.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Payment Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { ts ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = ts
                val selectedDate = calendar.time
                
                val next = calculateNextDueDate(selectedDate, subscription.recurrence)
                recordPayment(selectedDate, next)
            }
            datePicker.show(childFragmentManager, "PAY_DATE_PICKER")
        }
    }

    private fun calculateDates(baseDate: Date) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        textDueInfo.text = "Due: ${dateFormat.format(baseDate)}"
        
        projectedNextDate = calculateNextDueDate(baseDate, subscription.recurrence)
        projectedNextDate?.let {
            textNextPreview.text = "Next bill will be: ${dateFormat.format(it)}"
        } ?: run {
             textNextPreview.text = "Next due date: Undefined (Custom/None)"
        }
    }

    private fun calculateNextDueDate(fromDate: Date, recurrence: String): Date? {
        val cal = Calendar.getInstance()
        cal.time = fromDate
        cal.set(Calendar.HOUR_OF_DAY, 12)
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

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return
        val subId = subscription.id ?: run {
            progressHistory.visibility = View.GONE
            noHistoryContainer.visibility = View.VISIBLE
            textNoHistory.text = "Error: Subscription ID missing"
            return
        }

        progressHistory.visibility = View.VISIBLE
        noHistoryContainer.visibility = View.GONE

        firestore.collection("users").document(userId)
            .collection("subscriptions").document(subId)
            .collection("payments")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!isAdded) return@addOnSuccessListener
                
                val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 200 }
                progressHistory.startAnimation(fadeOut)
                progressHistory.visibility = View.GONE
                
                val payments = snapshots.toObjects(PaymentRecord::class.java)
                if (payments.isEmpty()) {
                    historyRecycler.visibility = View.GONE
                    noHistoryContainer.visibility = View.VISIBLE
                    val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }
                    noHistoryContainer.startAnimation(fadeIn)
                    textNoHistory.text = "No Payment History"
                } else {
                    noHistoryContainer.visibility = View.GONE
                    historyRecycler.visibility = View.VISIBLE
                    val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }
                    historyRecycler.startAnimation(fadeIn)
                    historyRecycler.adapter = PaymentAdapter(payments, subscription.currency, 
                        onEditClicked = { record -> editPaymentDate(record) },
                        onDeleteClicked = { record -> confirmDeletePayment(record) }
                    )
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Log.e("PaymentBottomSheet", "History Error: ${e.message}", e)
                loadHistorySimple(userId, subId)
            }
    }

    private fun loadHistorySimple(userId: String, subId: String) {
        firestore.collection("users").document(userId)
            .collection("subscriptions").document(subId)
            .collection("payments")
            .limit(10)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!isAdded) return@addOnSuccessListener
                progressHistory.visibility = View.GONE
                val payments = snapshots.toObjects(PaymentRecord::class.java).sortedByDescending { it.date }
                if (payments.isEmpty()) {
                    historyRecycler.visibility = View.GONE
                    noHistoryContainer.visibility = View.VISIBLE
                } else {
                    noHistoryContainer.visibility = View.GONE
                    historyRecycler.visibility = View.VISIBLE
                    historyRecycler.adapter = PaymentAdapter(payments, subscription.currency,
                        onEditClicked = { record -> editPaymentDate(record) },
                        onDeleteClicked = { record -> confirmDeletePayment(record) }
                    )
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                progressHistory.visibility = View.GONE
                noHistoryContainer.visibility = View.VISIBLE
                textNoHistory.text = "Permission Denied: Ensure Firestore rules allow access."
            }
    }

    private fun confirmDeletePayment(record: PaymentRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Payment Record")
            .setMessage("Are you sure you want to delete this payment record from your history?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deletePayment(record)
            }
            .show()
    }

    private fun deletePayment(record: PaymentRecord) {
        val userId = auth.currentUser?.uid ?: return
        val subId = subscription.id ?: return
        val paymentId = record.id ?: return

        firestore.collection("users").document(userId)
            .collection("subscriptions").document(subId)
            .collection("payments").document(paymentId)
            .delete()
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Payment deleted", Toast.LENGTH_SHORT).show()
                    loadHistory() 
                }
            }
    }

    private fun editPaymentDate(record: PaymentRecord) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Edit Payment Date")
            .setSelection(record.date?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { ts ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = ts
            val newDate = calendar.time
            updatePaymentDate(record, newDate)
        }
        datePicker.show(childFragmentManager, "EDIT_PAY_DATE_PICKER")
    }

    private fun updatePaymentDate(record: PaymentRecord, newDate: Date) {
        val userId = auth.currentUser?.uid ?: return
        val subId = subscription.id ?: return
        val paymentId = record.id ?: return

        firestore.collection("users").document(userId)
            .collection("subscriptions").document(subId)
            .collection("payments").document(paymentId)
            .update("date", newDate)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Payment date updated", Toast.LENGTH_SHORT).show()
                    loadHistory()
                }
            }
    }

    private fun recordPayment(paymentDate: Date, nextDueDate: Date?) {
        val userId = auth.currentUser?.uid ?: return
        val subId = subscription.id ?: run {
            Toast.makeText(context, "Error: Subscription ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        btnPayCurrent.isEnabled = false
        btnPayCustom.isEnabled = false

        val payment = PaymentRecord(
            date = paymentDate,
            amount = subscription.cost,
            subscriptionName = subscription.name,
            subscriptionId = subId,
            currency = subscription.currency,
            userId = userId
        )

        val batch = firestore.batch()
        val subRef = firestore.collection("users").document(userId).collection("subscriptions").document(subId)
        val paymentRef = subRef.collection("payments").document()

        batch.set(paymentRef, payment)
        if (nextDueDate != null) {
            batch.update(subRef, "dueDate", nextDueDate)
        }

        batch.commit().addOnSuccessListener {
            if (isAdded) {
                onPaymentRecorded()
                dismiss()
            }
        }.addOnFailureListener { e ->
            if (isAdded) {
                btnPayCurrent.isEnabled = true
                btnPayCustom.isEnabled = true
                Log.e("PaymentBottomSheet", "Payment Failed", e)
                Toast.makeText(context, "Failed: ${e.message}. Check Firestore Rules.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CONTEXT_CLICK)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val behavior = dialog?.behavior
        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    PreferenceHelper.performHaptics(bottomSheet, HapticFeedbackConstants.GESTURE_END)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }
}