---
name: Currency Display Standards
description: Centralized rules for currency formatting, including spacing, symbol sizing, and code formats.
---

# Currency Display Standards

This skill defines the visual hierarchy and formatting rules for all currency displays in Varisankya.

## Formatting Rules

1. **Standard Spacing:** A non-breaking space MUST exist between the currency symbol/code and the numeric amount.
   - Example: `$ 120.00`
   - Example: `KES 4,500`

2. **Adaptive Sizing:** The currency symbol or 3-letter code MUST be rendered at **exactly 50%** of the amount's text size.
   - This creates a visual hierarchy where the amount is the focus.
   - This prevents long codes (like `KES`) from overwhelming the UI.

3. **Symbol Format:** All currencies should use the **"CODE Symbol"** format for selection and settings, but only the **Symbol/Code** for inline displays.
   - Selection: `USD $`, `INR ₹`, `KES KSh`
   - Inline: `$ 120`, `KES 450`

## Technical Implementation

### Centralized Helper
Use `CurrencyHelper.formatCurrency(context, amount, code)` to get a `SpannableStringBuilder` with the correct 50% `RelativeSizeSpan` applied.

```kotlin
fun formatCurrency(context: Context, amount: Double, currencyCode: String): CharSequence {
    val symbol = getSymbol(currencyCode)
    val formattedAmount = // ... format to 0 or 2 decimals
    val fullText = "$symbol $formattedAmount"
    val spannable = SpannableStringBuilder(fullText)
    spannable.setSpan(RelativeSizeSpan(0.5f), 0, symbol.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
```

### Animated Values
When using `AnimationHelper.animateTextCountUp`, the prefix MUST contain the trailing space (e.g., `"$symbol "`). The animator logic should automatically detect this and apply the `RelativeSizeSpan` during every frame update.

## Visual Checklist
- [ ] Is there a space between symbol and amount?
- [ ] Is the symbol notably smaller (50%) than the amount?
- [ ] Does the symbol maintain its size during count-up animations?
- [ ] Are 3-letter codes (KES) handled gracefully without breaking layout?
