package com.hora.varisankya

object Constants {
    val CATEGORIES = arrayOf(
        "Entertainment",
        "Utilities",
        "Work",
        "Loan",
        "Software",
        "Family",
        "Health",
        "Investment",
        "Insurance",
        "Productivity",
        "Other"
    )

    // M3 Animation Durations (Scaled for a deliberate, premium feel)
    const val ANIM_DURATION_SHORT = 200L
    const val ANIM_DURATION_MEDIUM = 400L
    const val ANIM_DURATION_LONG = 500L
    const val ANIM_DURATION_EXTRA_LONG = 1500L

    // Action Specific Durations
    const val ANIM_DURATION_CLICK_PRESS = 50L
    const val ANIM_DURATION_CLICK_RELEASE = 200L
    const val ANIM_STAGGER_BASE_DELAY = 40L
}