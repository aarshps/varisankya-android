package com.hora.varisankya

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import com.google.android.material.button.MaterialButton
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

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
import com.hora.varisankya.util.AnimationHelper
import com.hora.varisankya.util.DateHelper

class PaymentBottomSheet(
    private val subscription: Subscription,
    private val onPaymentRecorded: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return dialog
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var historyRecycler: RecyclerView
    private lateinit var noHistoryContainer: View
    private lateinit var textNoHistory: TextView
    private lateinit var progressHistory: ProgressBar
    private lateinit var btnPayCurrent: Button

    private lateinit var textDueInfo: TextView
    private lateinit var textNextPreview: TextView

    private var currentDueDate: Date? = null
    private var projectedNextDate: Date? = null

    private lateinit var paymentSheetRoot: ViewGroup // Added property

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_payment, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        paymentSheetRoot = view.findViewById(R.id.payment_sheet_root) // Initialized
        historyRecycler = view.findViewById(R.id.recycler_history)
        // ... (rest of caching)
        noHistoryContainer = view.findViewById(R.id.no_history_container)
        textNoHistory = view.findViewById(R.id.text_no_history)
        progressHistory = view.findViewById(R.id.progress_history)
        btnPayCurrent = view.findViewById(R.id.btn_pay_current)

        textDueInfo = view.findViewById(R.id.text_due_date_info)
        textNextPreview = view.findViewById(R.id.text_next_due_date_preview)
        val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.payment_scroll_view)
        
        // M3E Mechanical Scroll Feel
        PreferenceHelper.attachNestedScrollHaptics(scrollView)
        
        view.findViewById<View>(R.id.drag_handle).setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(Constants.ANIM_DURATION_CLICK).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(Constants.ANIM_DURATION_CLICK).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
            }.start()
        }

        currentDueDate = subscription.dueDate?.let { DateHelper.normalizeDueDate(it) }
            ?: DateHelper.normalizeDueDate(Date())
        setupUI()
        loadHistory()
        calculateDates(currentDueDate!!)

        return view
    }
    
    override fun onStart() {
        super.onStart()
        val bottomSheet = (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = (dialog as? BottomSheetDialog)?.behavior
        val dragHandle = view?.findViewById<View>(R.id.drag_handle)

        if (bottomSheet != null && behavior != null && dragHandle != null) {
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            dragHandle.animate().scaleX(1.2f).scaleY(1.2f).setDuration(Constants.ANIM_DURATION_SHORT).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                                .withEndAction { dragHandle.animate().scaleX(1f).scaleY(1f).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start() }
                                .start()
                            PreferenceHelper.performHaptics(dragHandle, HapticFeedbackConstants.CONFIRM)
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                            dragHandle.animate().scaleX(0.9f).scaleY(0.9f).setDuration(Constants.ANIM_DURATION_SHORT).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
                            PreferenceHelper.performHaptics(dragHandle, HapticFeedbackConstants.CLOCK_TICK)
                        }
                        BottomSheetBehavior.STATE_SETTLING, BottomSheetBehavior.STATE_COLLAPSED -> {
                            dragHandle.animate().scaleX(1f).scaleY(1f).setDuration(Constants.ANIM_DURATION_SHORT).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    private fun setupUI() {
        historyRecycler.layoutManager = LinearLayoutManager(context)
        PreferenceHelper.attachScrollHaptics(historyRecycler)

        btnPayCurrent.setOnClickListener {
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
            PreferenceHelper.performHaptics(it, haptic)
            recordPayment(currentDueDate!!, projectedNextDate)
        }
        AnimationHelper.applySpringOnTouch(btnPayCurrent)


    }

    private fun calculateDates(baseDate: Date) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        textDueInfo.text = "Due: ${dateFormat.format(baseDate)}"
        
        projectedNextDate = DateHelper.calculateNextDueDate(baseDate, subscription.recurrence)
        projectedNextDate?.let {
            textNextPreview.text = "Next bill will be: ${dateFormat.format(it)}"
        } ?: run {
             textNextPreview.text = "Next due date: Undefined (Custom/None)"
        }
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
                    historyRecycler.adapter = null
                    historyRecycler.visibility = View.GONE
                    noHistoryContainer.visibility = View.VISIBLE
                    val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }
                    noHistoryContainer.startAnimation(fadeIn)
                    textNoHistory.text = "No Payment History"
                } else {
                    noHistoryContainer.visibility = View.GONE
                    historyRecycler.visibility = View.VISIBLE
                    
                    val adapter = PaymentAdapter(subscription.currency, 
                        onEditClicked = { record -> editPaymentDate(record) },
                        onDeleteClicked = { record -> confirmDeletePayment(record) }
                    )
                    historyRecycler.adapter = adapter
                    adapter.submitList(payments)
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
                    historyRecycler.adapter = null
                    historyRecycler.visibility = View.GONE
                    noHistoryContainer.visibility = View.VISIBLE
                } else {
                    noHistoryContainer.visibility = View.GONE
                    historyRecycler.visibility = View.VISIBLE
                    val adapter = PaymentAdapter(subscription.currency,
                        onEditClicked = { record -> editPaymentDate(record) },
                        onDeleteClicked = { record -> confirmDeletePayment(record) }
                    )
                    historyRecycler.adapter = adapter
                    adapter.submitList(payments)
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
                    loadHistory()
                }
            }
    }

    private fun recordPayment(paymentDate: Date, nextDueDate: Date?) {
        val userId = auth.currentUser?.uid ?: return
        


        val subId = subscription.id ?: run {
            return
        }

        btnPayCurrent.isEnabled = false


        val payment = PaymentRecord(
            date = Date(), // Use today's date for payment
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

                Log.e("PaymentBottomSheet", "Payment Failed", e)
            }
        }
    }
}
