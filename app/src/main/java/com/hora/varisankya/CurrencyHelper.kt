package com.hora.varisankya

/**
 * Currency helper with minimal list + custom option
 */
object CurrencyHelper {

    data class CurrencyItem(
        val code: String,
        val name: String,
        val symbol: String
    )

    /**
     * Top 15 world currencies + Issue requests
     * Symbols verified for Google Sans Flex compatibility
     */
    val ALL_CURRENCIES: List<CurrencyItem> = listOf(
        CurrencyItem("INR", "Indian Rupee", "₹"),
        CurrencyItem("USD", "US Dollar", "$"),
        CurrencyItem("EUR", "Euro", "€"),
        CurrencyItem("GBP", "British Pound", "£"),
        CurrencyItem("JPY", "Japanese Yen", "¥"),
        CurrencyItem("AUD", "Australian Dollar", "$"),
        CurrencyItem("CAD", "Canadian Dollar", "$"),
        CurrencyItem("CHF", "Swiss Franc", "₣"),
        CurrencyItem("CNY", "Chinese Yuan", "¥"),
        CurrencyItem("HKD", "Hong Kong Dollar", "$"),
        CurrencyItem("NZD", "New Zealand Dollar", "$"),
        CurrencyItem("SEK", "Swedish Krona", "kr"),
        CurrencyItem("KRW", "South Korean Won", "₩"),
        CurrencyItem("SGD", "Singapore Dollar", "$"),
        CurrencyItem("MXN", "Mexican Peso", "$"),
        CurrencyItem("KES", "Kenyan Shilling", "KSh"),
        CurrencyItem("UNT", "Generic Unit", "#")
    )

    /**
     * Standardized currency formatting:
     * - Adds a space between symbol and amount
     * - Reduces symbol size by 50%
     */
    fun formatCurrency(context: android.content.Context, amount: Double, currencyCode: String): CharSequence {
        val symbol = getSymbol(currencyCode)
        val formattedAmount = if (amount % 1.0 == 0.0) String.format("%.0f", amount) else String.format("%.2f", amount)
        val fullText = "$symbol $formattedAmount"
        
        val spannable = android.text.SpannableStringBuilder(fullText)
        val symbolEnd = symbol.length
        
        spannable.setSpan(
            android.text.style.RelativeSizeSpan(0.5f),
            0,
            symbolEnd,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        return spannable
    }

    /**
     * Get display strings for SelectionBottomSheet in "CODE Symbol" format
     */
    fun getCurrencyDisplayList(): Array<String> {
        return ALL_CURRENCIES.map { "${it.code} ${it.symbol}" }.toTypedArray()
    }

    /**
     * Extract currency code from display string
     */
    fun getCodeFromDisplay(display: String): String {
        return display.split(" ").firstOrNull() ?: "INR"
    }

    /**
     * Get currency symbol by code
     */
    fun getSymbol(currencyCode: String): String {
        val symbol = ALL_CURRENCIES.find { it.code == currencyCode }?.symbol ?: "$"
        return symbol
    }

    /**
     * Get CurrencyItem by code
     */
    fun getByCode(code: String): CurrencyItem? {
        return ALL_CURRENCIES.find { it.code == code }
    }
}
