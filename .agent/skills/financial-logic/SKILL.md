---
name: Financial Calculation Rules
description: core business logic for calculating liabilities, overdue amounts, and totals
---

# Financial Calculation Rules

This skill defines the canonical business logic for financial data display in Varisankya.

## 1. Remaining Monthly Liability (Hero Card)

The "Remaining" amount displayed on the Home Screen Hero is **NOT** just future payments.

**Formula:**
```
Remaining = (Sum of All Overdue Items) + (Sum of Unpaid Items Due Today or Later in Current Month)
```

### Critical Rules:
1.  **Include Overdue**: Any unpaid subscription from the *past* is an immediate liability and MUST be included in the "Remaining" count.
2.  **Current Month Boundary**: Future items are only included if they fall within the current calendar month. Next month's rent is NOT "Remaining" (it is "Upcoming").
3.  **Strict "Before" Check**: "Overdue" is defined strictly as `dueDate < today (start of day)`.

## 2. Widget "Upcoming" Logic

The Home Screen Widget displays the "Top 3" most relevant payments.

**Sorting Priority:**
1.  **Overdue First**: Oldest overdue item is top priority.
2.  **Due Soon**: Then items sorted by closest future due date.

## 3. "All Clear" State

The "All Clear" state (Green Hero) should ONLY trigger if:
- `Overdue List` is Empty AND
- `Current Month Future List` is Empty.

If there are no current month payments but there IS an overdue payment from last year, the state is **Overdue (Red)**, not All Clear.
