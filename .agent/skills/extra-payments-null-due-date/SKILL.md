---
name: Payment History Null Due Date
description: How to safely record extra payments without advancing the Varisankya subscription next due date.
---

# Extra Payment Recording (Null Due Date Strategy)

When adding a payment in `PaymentBottomSheet.kt`, Varisankya's standard flow advances the `dueDate` of the Subscription document based on its recurrence rule so the cycle continues properly.

However, users occasionally need to log an "Extra Payment" (e.g. paying down extra principal on a loan) without skipping the next scheduled cycle.

## Implementation Standard
The `recordPayment` function in `MainViewModel` (and its counterpart used via batch writes inside `PaymentBottomSheet`) accepts a `nextDueDate: Date?` parameter.

To record a payment without modifying the subscription's schedule, you must pass **`null`** to this `nextDueDate` argument. The batch commit logic will then conditionally bypass the `"dueDate"` update.

**Example Implementation inside PaymentBottomSheet:**
```kotlin
btnAddPaymentOnly.setOnClickListener {
    // 1. Prompt for payment date (using MaterialDatePicker)
    // 2. Call recordPayment with the selected date AND a null nextDueDate
    recordPayment(selectedDate, null)
}
```

Never forcefully calculate and pass the *current* due date as the *next* due date. Always pass `null` to explicitly trigger the conditional skip in the batch write.
