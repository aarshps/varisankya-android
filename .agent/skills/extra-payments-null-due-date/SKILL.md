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

## Downstream Data Handling Warning
Because extra payments are designed to skip updating the `dueDate` by passing `null`, the `dueDate` field of a `Subscription` model object retrieved from Firestore **can be null** if a user has legacy records or if an unexpected state occurred.

**Any UI logic or ViewModels (such as `MainViewModel.kt` calculating the Hero Section stats) MUST explicitly filter out or gracefully handle `Subscription` objects where `dueDate == null`.** Attempting to forcefully unwrap (`!!`) a `dueDate` in list mapping operations will lead to immediate runtime crashes upon login.
