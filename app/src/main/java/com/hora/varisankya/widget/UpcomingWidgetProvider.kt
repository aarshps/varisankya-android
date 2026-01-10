package com.hora.varisankya.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.hora.varisankya.MainActivity
import com.hora.varisankya.R

class UpcomingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_COUNT = "sub_count"
        private const val KEY_NAME_PREFIX = "sub_name_"
        private const val KEY_DUE_PREFIX = "sub_due_"
        
        data class WidgetData(val items: List<Pair<String, String>>, val totalCount: Int)
        
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // Check font preference
            // Direct check to avoid compilation issues with helper
            val appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val useGoogleFont = appPrefs.getBoolean("use_google_font", true)
            
            // If google font is enabled, we use the default layout (with google sans)
            // If NOT enabled (device font), we use device layout
            val layoutId = if (useGoogleFont) R.layout.widget_upcoming else R.layout.widget_upcoming_device
            val views = RemoteViews(context.packageName, layoutId)
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_COUNT, 0)
            
            val itemsList = (0 until minOf(count, 3)).map { i ->
                Pair(
                    prefs.getString("${KEY_NAME_PREFIX}$i", "") ?: "",
                    prefs.getString("${KEY_DUE_PREFIX}$i", "") ?: ""
                )
            }
            val widgetData = WidgetData(itemsList, count)
            
            val testData = widgetData.items
            val totalCount = widgetData.totalCount
            
            if (testData.isEmpty()) {
                // Empty State
                views.setViewVisibility(R.id.empty_view, View.VISIBLE)
                views.setViewVisibility(R.id.item_1, View.GONE)
                views.setViewVisibility(R.id.item_2, View.GONE)
                views.setViewVisibility(R.id.item_3, View.GONE)
                views.setViewVisibility(R.id.text_more, View.GONE)
            } else {
                // Data State
                views.setViewVisibility(R.id.empty_view, View.GONE)
                
                val itemIds = listOf(
                    Triple(R.id.item_1, R.id.text_name_1, R.id.text_due_1),
                    Triple(R.id.item_2, R.id.text_name_2, R.id.text_due_2),
                    Triple(R.id.item_3, R.id.text_name_3, R.id.text_due_3)
                )
                
                testData.forEachIndexed { index, (name, due) ->
                    if (index < itemIds.size) {
                        val (itemId, nameId, dueId) = itemIds[index]
                        views.setViewVisibility(itemId, View.VISIBLE)
                        views.setTextViewText(nameId, name)
                        views.setTextViewText(dueId, due)

                        // Show separator if this is not the last visible item
                        // Spacer 1 is between item 1 and 2
                         if (index == 0 && testData.size > 1) {
                             // Spacer 1 is implicitly handled by ImageView but we need to ensure visibility ??
                             // No, layout has spacers. Spacer 1 is between 1 and 2. Spacer 2 is between 2 and 3.
                             // Actually the spacers in XML are hardcoded. We need logic for Spacer 2
                             // In current XML: Item 1, Spacer 1 (invisible), Item 2, Spacer 2 (invisible), Item 3
                         }
                         if (index == 1 && testData.size > 2) {
                             views.setViewVisibility(R.id.spacer_2, View.VISIBLE)
                         }
                    }
                }
                
                // Show "more" indicator if there are additional items (>3)
                if (totalCount > 3) {
                    views.setViewVisibility(R.id.text_more, View.VISIBLE)
                    views.setTextViewText(R.id.text_more, "+${totalCount - 3} more...")
                    // If showing more, items 2 AND 3 should be middle
                    views.setInt(R.id.item_2, "setBackgroundResource", R.drawable.bg_widget_item_middle)
                    views.setInt(R.id.item_3, "setBackgroundResource", R.drawable.bg_widget_item_middle)
                } else {
                    views.setViewVisibility(R.id.text_more, View.GONE)
                }
                
                // Adjust backgrounds based on item count (when no "more" text)
                if (totalCount <= 3) {
                    if (testData.size == 1) {
                         views.setInt(R.id.item_1, "setBackgroundResource", R.drawable.bg_widget_item_single)
                    } else if (testData.size == 2) {
                         views.setInt(R.id.item_1, "setBackgroundResource", R.drawable.bg_widget_item_first)
                         views.setInt(R.id.item_2, "setBackgroundResource", R.drawable.bg_widget_item_last)
                    } else if (testData.size == 3) {
                         views.setInt(R.id.item_1, "setBackgroundResource", R.drawable.bg_widget_item_first)
                         views.setInt(R.id.item_2, "setBackgroundResource", R.drawable.bg_widget_item_middle)
                         views.setInt(R.id.item_3, "setBackgroundResource", R.drawable.bg_widget_item_last)
                    }
                }
                
                // Hide unused items
                for (i in testData.size until 3) {
                    views.setViewVisibility(itemIds[i].first, View.GONE)
                }
            }

            // Click to open App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun saveData(context: Context, items: List<Pair<String, String>>, totalCount: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt(KEY_COUNT, totalCount)
            items.forEachIndexed { index, (name, due) ->
                editor.putString("${KEY_NAME_PREFIX}$index", name)
                editor.putString("${KEY_DUE_PREFIX}$index", due)
            }
            editor.apply()
        }
    }
}
