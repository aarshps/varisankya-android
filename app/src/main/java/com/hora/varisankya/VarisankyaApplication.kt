package com.hora.varisankya

import android.app.Application

class VarisankyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enabled Dynamic Colors (Material You)
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this)
    }
}

