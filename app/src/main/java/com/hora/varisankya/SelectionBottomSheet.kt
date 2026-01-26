package com.hora.varisankya

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class SelectionBottomSheet(
    private val title: String,
    private val options: Array<String>,
    private val selectedOption: String?,
    private val onOptionSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return dialog
    }

    private val chipMap = mutableMapOf<String, Chip>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_selection, container, false)

        val titleTextView = view.findViewById<TextView>(R.id.selection_title)
        val chipGroup = view.findViewById<ChipGroup>(R.id.selection_chip_group)
        val searchEditText = view.findViewById<TextInputEditText>(R.id.edit_text_search)
        val dragHandle = view.findViewById<View>(R.id.drag_handle)

        dragHandle.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(Constants.ANIM_DURATION_CLICK).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(Constants.ANIM_DURATION_CLICK).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
            }.start()
        }

        titleTextView.text = title

        options.forEach { option ->
            val chip = Chip(requireContext())
            chip.text = option
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            chip.isChecked = option == selectedOption

            // Ensure spacing is visually balanced by disabling automatic vertical expansion
            chip.setEnsureMinTouchTargetSize(false)
            
            updateChipStyle(chip)

            chip.setOnClickListener { v ->
                PreferenceHelper.performHaptics(v, HapticFeedbackConstants.VIRTUAL_KEY)
                onOptionSelected(option)
                dismiss()
            }
            chipGroup.addView(chip)
            chipMap[option] = chip
        }

        searchEditText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase()
                chipMap.forEach { (option, chip) ->
                    chip.visibility = if (option.lowercase().contains(query)) View.VISIBLE else View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Auto focus and show keyboard
        searchEditText.requestFocus()
        searchEditText.postDelayed({
            if (isAdded) {
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 300)

        return view
    }

    private fun updateChipStyle(chip: Chip) {
        com.hora.varisankya.util.ChipHelper.styleChip(chip)
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

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }
}