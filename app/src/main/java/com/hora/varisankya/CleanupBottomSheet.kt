package com.hora.varisankya

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hora.varisankya.util.AnimationHelper

class CleanupBottomSheet(
    private val invalidCount: Int,
    private val onConfirm: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_cleanup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageView = view.findViewById<TextView>(R.id.cleanup_message)
        val confirmButton = view.findViewById<Button>(R.id.btn_confirm_cleanup)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel_cleanup)
        val dragHandle = view.findViewById<View>(R.id.drag_handle)

        dragHandle.setOnClickListener {
            PreferenceHelper.performHaptics(it, android.view.HapticFeedbackConstants.CLOCK_TICK)
        }

        messageView.text = "Found $invalidCount payment record${if (invalidCount > 1) "s" else ""} with missing subscription details. These appear to be orphans and should be removed."
        confirmButton.text = "Delete $invalidCount Record${if (invalidCount > 1) "s" else ""}"

        confirmButton.setOnClickListener {
            PreferenceHelper.performErrorHaptic(it)
            dismiss()
            onConfirm()
        }

        cancelButton.setOnClickListener {
            PreferenceHelper.performClickHaptic(it)
            dismiss()
        }
    }
}
