package com.hora.varisankya

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.hora.varisankya.util.AnimationHelper

class AddSubscriptionBottomSheet(
    private val subscription: Subscription? = null,
    private val onSave: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return dialog
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedDueDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_add_subscription, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        val titleTextView = view.findViewById<TextView>(R.id.bottom_sheet_title)
        val nameEditText = view.findViewById<TextInputEditText>(R.id.edit_text_name)
        val dueDateEditText = view.findViewById<TextInputEditText>(R.id.edit_text_due_date)
        val costEditText = view.findViewById<TextInputEditText>(R.id.edit_text_cost)
        val currencyAutoComplete = view.findViewById<AutoCompleteTextView>(R.id.auto_complete_currency)
        val recurrenceAutoComplete = view.findViewById<AutoCompleteTextView>(R.id.auto_complete_recurrence)
        val frequencyEditText = view.findViewById<TextInputEditText>(R.id.edit_text_frequency)
        val tilFrequency = view.findViewById<TextInputLayout>(R.id.til_frequency)
        val categoryAutoComplete = view.findViewById<AutoCompleteTextView>(R.id.auto_complete_category)
        val statusSwitch = view.findViewById<MaterialSwitch>(R.id.switch_active_status)
        val saveButton = view.findViewById<Button>(R.id.button_save)
        val deleteButton = view.findViewById<Button>(R.id.button_delete)
        val markPaidButton = view.findViewById<Button>(R.id.button_mark_paid)
        val dragHandle = view.findViewById<View>(R.id.drag_handle)
        val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.add_subscription_scroll_view)
        
        // M3E Mechanical Scroll Feel
        PreferenceHelper.attachNestedScrollHaptics(scrollView)
        
        // Expressive Buttons
        AnimationHelper.applySpringOnTouch(saveButton)
        AnimationHelper.applySpringOnTouch(deleteButton)
        AnimationHelper.applySpringOnTouch(markPaidButton)
        
        dragHandle.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(Constants.ANIM_DURATION_CLICK).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(Constants.ANIM_DURATION_CLICK).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
            }.start()
        }

        titleTextView.text = if (subscription == null) "Add Subscription" else "Edit Subscription"

        val addHaptic = { v: View -> 
            PreferenceHelper.performClickHaptic(v)
        }
        val addStrongHaptic = { v: View -> 
            PreferenceHelper.performSuccessHaptic(v)
        }

        nameEditText.setOnFocusChangeListener { v, hasFocus -> if(hasFocus) addHaptic(v) }
        costEditText.setOnFocusChangeListener { v, hasFocus -> if(hasFocus) addHaptic(v) }
        frequencyEditText.setOnFocusChangeListener { v, hasFocus -> if(hasFocus) addHaptic(v) }

        dueDateEditText.isFocusable = false
        dueDateEditText.setOnClickListener {
            addHaptic(it)
            clearCurrentFocus()
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Due Date")
                .setSelection(subscription?.dueDate?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { ts ->
                selectedDueDate = Date(ts)
                val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                dueDateEditText.setText(format.format(selectedDueDate!!))
            }
            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        val currencies = PreferenceHelper.getPersonalizedList(requireContext(), "currency", arrayOf("USD", "EUR", "GBP", "INR"))
        val recurrenceOptions = PreferenceHelper.getPersonalizedList(requireContext(), "recurrence", arrayOf("Monthly", "Yearly", "Weekly", "Daily", "Custom"))
        val categories = PreferenceHelper.getPersonalizedList(requireContext(), "category", Constants.CATEGORIES)

        setupSelection(currencyAutoComplete, "Select Currency", currencies, addHaptic)
        setupSelection(recurrenceAutoComplete, "Select Recurrence", recurrenceOptions, addHaptic) { selected ->
             PreferenceHelper.performHaptics(view, HapticFeedbackConstants.SEGMENT_TICK)
             if (selected == "Custom") {
                 tilFrequency.visibility = View.GONE
                 frequencyEditText.setText("")
             } else {
                 tilFrequency.visibility = View.VISIBLE
                 frequencyEditText.isEnabled = true
             }
        }
        setupSelection(categoryAutoComplete, "Select Category", categories, addHaptic)

        if (subscription != null) {
            statusSwitch.visibility = View.VISIBLE
            statusSwitch.isChecked = subscription.active
            statusSwitch.setOnCheckedChangeListener { _, _ -> addHaptic(statusSwitch) }

            nameEditText.setText(subscription.name)
            selectedDueDate = subscription.dueDate
            subscription.dueDate?.let { date ->
                val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dueDateEditText.setText(format.format(date))
            }
            costEditText.setText(subscription.cost.toString())
            currencyAutoComplete.setText(subscription.currency, false)
            // Default to disabled/dimmed while we check for payment history
            currencyAutoComplete.isEnabled = false
            currencyAutoComplete.alpha = 0.5f

            val userId = auth.currentUser?.uid
            val subId = subscription.id
            if (userId != null && subId != null) {
                firestore.collection("users").document(userId)
                    .collection("subscriptions").document(subId)
                    .collection("payments")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshots ->
                        if (snapshots.isEmpty && isAdded) {
                            // No payments exist, safe to unlock currency editing
                            currencyAutoComplete.isEnabled = true
                            currencyAutoComplete.alpha = 1.0f
                        }
                    }
            }
            
            categoryAutoComplete.setText(subscription.category, false)

            val rec = subscription.recurrence
            if (rec == "Custom") {
                recurrenceAutoComplete.setText("Custom", false)
                tilFrequency.visibility = View.GONE
                frequencyEditText.setText("")
            } else if (rec.startsWith("Every ")) {
                val parts = rec.split(" ")
                if (parts.size >= 3) {
                     frequencyEditText.setText(parts[1])
                     val unit = parts[2]
                     val mapped = when(unit) {
                         "Months" -> "Monthly"
                         "Years" -> "Yearly"
                         "Weeks" -> "Weekly"
                         "Days" -> "Daily"
                         else -> "Monthly"
                     }
                     recurrenceAutoComplete.setText(mapped, false)
                     tilFrequency.visibility = View.VISIBLE
                }
            } else {
                 frequencyEditText.setText("1")
                 recurrenceAutoComplete.setText(rec, false)
                 tilFrequency.visibility = View.VISIBLE
            }

            deleteButton.visibility = View.VISIBLE
            deleteButton.setOnClickListener {
                addStrongHaptic(it)
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                firestore.collection("users").document(userId).collection("subscriptions").document(subscription.id!!).delete()
                dismiss()
            }

            markPaidButton.visibility = View.VISIBLE
            markPaidButton.setOnClickListener {
                addHaptic(it)
                val currentSubscription = Subscription(
                    id = subscription.id,
                    name = nameEditText.text.toString(),
                    dueDate = selectedDueDate,
                    cost = costEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    currency = currencyAutoComplete.text.toString(),
                    recurrence = getRecurrenceString(recurrenceAutoComplete.text.toString(), frequencyEditText.text.toString()),
                    category = categoryAutoComplete.text.toString(),
                    active = statusSwitch.isChecked
                )
                val paymentSheet = PaymentBottomSheet(currentSubscription) {
                    onSave() 
                    dismiss() 
                }
                paymentSheet.show(parentFragmentManager, "PAYMENT_SHEET")
            }

        } else {
             statusSwitch.visibility = View.GONE
             frequencyEditText.setText("")
             recurrenceAutoComplete.setText("")
             tilFrequency.visibility = View.VISIBLE
        }
        
        currencyAutoComplete.setOnDismissListener { currencyAutoComplete.clearFocus() }
        recurrenceAutoComplete.setOnDismissListener { recurrenceAutoComplete.clearFocus() }
        categoryAutoComplete.setOnDismissListener { categoryAutoComplete.clearFocus() }

        saveButton.setOnClickListener {
            addStrongHaptic(it)
            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val currency = currencyAutoComplete.text.toString()
            val category = categoryAutoComplete.text.toString()
            val finalRecurrence = getRecurrenceString(recurrenceAutoComplete.text.toString(), frequencyEditText.text.toString())

            PreferenceHelper.recordUsage(requireContext(), "currency", currency)
            PreferenceHelper.recordUsage(requireContext(), "category", category)
            PreferenceHelper.recordUsage(requireContext(), "recurrence", recurrenceAutoComplete.text.toString())

            val isActiveStatus = if (subscription != null) statusSwitch.isChecked else true

            val dataMap = hashMapOf(
                "name" to nameEditText.text.toString(),
                "dueDate" to selectedDueDate,
                "cost" to (costEditText.text.toString().toDoubleOrNull() ?: 0.0),
                "currency" to currency,
                "recurrence" to finalRecurrence,
                "category" to category,
                "active" to isActiveStatus
            )

            val collection = firestore.collection("users").document(userId).collection("subscriptions")
            if (subscription?.id != null) {
                collection.document(subscription.id).set(dataMap)
            } else {
                collection.add(dataMap)
            }
            
            onSave()
            dismiss()
        }

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
                            PreferenceHelper.performSuccessHaptic(dragHandle)
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                            dragHandle.animate().scaleX(0.9f).scaleY(0.9f).setDuration(Constants.ANIM_DURATION_SHORT).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
                            PreferenceHelper.performClickHaptic(dragHandle)
                        }
                        BottomSheetBehavior.STATE_SETTLING, BottomSheetBehavior.STATE_COLLAPSED -> {
                            dragHandle.animate().scaleX(1f).scaleY(1f).setDuration(Constants.ANIM_DURATION_SHORT).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
            
            // Add scroll listener to provide dragging haptics only on initial start of drag if handled by touch listener
            // logic above in click listener handles simple touch, but callback handles sheet drag
        }
    }

    private fun clearCurrentFocus() {
        val currentFocus = dialog?.currentFocus
        if (currentFocus != null) {
            currentFocus.clearFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun getRecurrenceString(recUnit: String, freqText: String): String {
        val freq = freqText.toIntOrNull() ?: 1
        return if (recUnit == "Custom") {
            "Custom"
        } else if (freq == 1) {
            recUnit
        } else {
            val pluralUnit = when(recUnit) {
                "Monthly" -> "Months"
                "Yearly" -> "Years"
                "Weekly" -> "Weeks"
                "Daily" -> "Days"
                else -> recUnit
            }
            "Every $freq $pluralUnit"
        }
    }

    private fun setupSelection(
        view: AutoCompleteTextView, 
        title: String, 
        options: Array<String>,
        addHaptic: (View) -> Unit,
        onItemSelected: ((String) -> Unit)? = null
    ) {
        view.isFocusable = false
        view.isClickable = true
        view.setOnClickListener {
            addHaptic(it)
            clearCurrentFocus()
            val bottomSheet = SelectionBottomSheet(
                title = title,
                options = options,
                selectedOption = view.text.toString()
            ) { selected ->
                view.setText(selected, false)
                onItemSelected?.invoke(selected)
            }
            bottomSheet.show(childFragmentManager, title)
        }
    }
}