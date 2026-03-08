---
name: Payment Date Prompting
description: How to correctly handle custom dates for extra subscription payments using MaterialDatePicker.
---

# Extra Payment Date Prompting

When implementing custom payment entries (like "Add Extra Payment") in Varisankya, the date of the payment should **never** be hardcoded to `Date()` (today) automatically to match the standard behavior.

For standard "Mark as Paid" actions, the speed of one-tap recording is preferred. However, explicit extra payments require precision.

## Prompting Standard
1. Use `MaterialDatePicker.newBuilder().datePicker()` initialized to `MaterialDatePicker.todayInUtcMilliseconds()`.
2. Convert the timestamp result to UTC:
```kotlin
val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
calendar.timeInMillis = timestamp
val selectedDate = calendar.time
```
3. Pass this user-specified `selectedDate` along with a `null` `nextDueDate` into the payment recording batch function.
