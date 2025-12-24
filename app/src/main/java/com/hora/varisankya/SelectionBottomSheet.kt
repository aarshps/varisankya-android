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

    private val chipMap = mutableMapOf<String, Chip>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_selection, container, false)

        val titleTextView = view.findViewById<TextView>(R.id.selection_title)
        val chipGroup = view.findViewById<ChipGroup>(R.id.selection_chip_group)
        val searchEditText = view.findViewById<TextInputEditText>(R.id.edit_text_search)

        titleTextView.text = title

        options.forEach { option ->
            val chip = Chip(context)
            chip.text = option
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            chip.isChecked = option == selectedOption
            
            // Ensure spacing is visually balanced by disabling automatic vertical expansion
            chip.setEnsureMinTouchTargetSize(false)
            
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