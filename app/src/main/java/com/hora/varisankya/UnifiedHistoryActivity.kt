package com.hora.varisankya

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UnifiedHistoryActivity : BaseActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var chartView: PaymentHistoryChart
    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var adapter: PaymentAdapter
    private lateinit var toolbar: MaterialToolbar
    private lateinit var loadingContainer: View
    private lateinit var loadingStatus: TextView
    private lateinit var noHistoryContainer: View


    private var allPayments: List<PaymentRecord> = emptyList()
    
    // Drill Down State
    private sealed class ViewLevel {
        object Overview : ViewLevel() // Monthly View
        data class MonthDetail(val monthKey: String, val monthLabel: String, val payments: List<PaymentRecord>) : ViewLevel()
        data class DayDetail(val dayKey: String, val dayLabel: String, val payments: List<PaymentRecord>) : ViewLevel()
    }

    private var currentLevel: ViewLevel = ViewLevel.Overview
    private val dateFormatMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val dateFormatDay = SimpleDateFormat("MMM dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unified_history)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        chartView = findViewById(R.id.unified_chart)
        recyclerView = findViewById(R.id.history_recycler_view)

        backButton = findViewById(R.id.btn_chart_back)
        loadingContainer = findViewById(R.id.loading_container)
        loadingStatus = findViewById(R.id.loading_status)
        noHistoryContainer = findViewById(R.id.no_history_container)

        // Initialize Adapter
        // Initialize Adapter (Read-Only Mode but with Haptics)
        adapter = PaymentAdapter(
            defaultCurrency = "USD",
            onEditClicked = { 
                // Just for haptic feedback
                PreferenceHelper.performHaptics(recyclerView, HapticFeedbackConstants.CONTEXT_CLICK) 
            },
            onDeleteClicked = null
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Scroll Haptics
        var lastFirstVisibleItem = -1
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val firstVisibleItem = layoutManager?.findFirstVisibleItemPosition() ?: -1
                
                if (firstVisibleItem != lastFirstVisibleItem && firstVisibleItem != -1) {
                    PreferenceHelper.performHaptics(recyclerView, HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                    lastFirstVisibleItem = firstVisibleItem
                }
            }
        })
        
        // Haptics for chart interaction defined in View
        chartView.setOnBarClickListener { data ->
            handleChartClick(data)
        }

        backButton.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
            navigateUp()
        }

        loadAllPayments()
    }

    private fun loadAllPayments() {
        val userId = auth.currentUser?.uid ?: return
        
        // Start "Expensive" feel
        val contentContainer = findViewById<View>(R.id.content_container)
        
        // Hide EVERYTHING except loading
        loadingContainer.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        
        loadingStatus.text = "Syncing Subscriptions..."
        
        // Step 1: Fetch all subscriptions
        firestore.collection("users").document(userId).collection("subscriptions")
            .get()
            .addOnSuccessListener { subSnapshots ->
                val subscriptions = subSnapshots.documents
                if (subscriptions.isEmpty()) {
                    onDataReady(emptyList())
                    return@addOnSuccessListener
                }
                

                loadingStatus.text = "Analysing Payment History..."

                // Step 2: Fetch payments and inject parent IDs
                val paymentTasks = subscriptions.map { subDoc ->
                    subDoc.reference.collection("payments").get().continueWith { task ->
                        if (task.isSuccessful) {
                            task.result.toObjects(PaymentRecord::class.java).map { record ->
                                record.copy(
                                    subscriptionId = subDoc.id,
                                    userId = userId
                                )
                            }
                        } else {
                            emptyList<PaymentRecord>()
                        }
                    }
                }

                // Step 3: Wait for all
                com.google.android.gms.tasks.Tasks.whenAllSuccess<List<PaymentRecord>>(paymentTasks)
                    .addOnSuccessListener { paymentLists ->
                        val allFetchedPayments = paymentLists.flatten()
                        
                        // Check for invalid records (missing subscription name)
                        val invalidPayments = allFetchedPayments.filter { it.subscriptionName.isNullOrEmpty() }
                        val validPayments = allFetchedPayments.filter { !it.subscriptionName.isNullOrEmpty() }
                        
                        // Always load valid content so it's visible behind the sheet
                        onDataReady(validPayments)
                        
                        if (invalidPayments.isNotEmpty()) {
                            val cleanupSheet = CleanupBottomSheet(invalidPayments.size) {
                                deleteInvalidRecords(invalidPayments)
                            }
                            cleanupSheet.isCancelable = true
                            cleanupSheet.show(supportFragmentManager, "CleanupBottomSheet")
                        }
                    }
                    .addOnFailureListener { e ->
                        loadingContainer.visibility = View.GONE
                        e.printStackTrace()
                    }
            }
            .addOnFailureListener { e ->
                loadingContainer.visibility = View.GONE
                e.printStackTrace()
            }
    }

    private fun onDataReady(payments: List<PaymentRecord>) {
        allPayments = payments.sortedByDescending { it.date }
        
        // Smooth Crossfade Animation: Loading Out, Content In
        val contentContainer = findViewById<View>(R.id.content_container)
        
        // Prepare content
        contentContainer.alpha = 0f
        contentContainer.visibility = View.VISIBLE
        
        // Show/Hide Empty State
        if (allPayments.isEmpty()) {
            noHistoryContainer.visibility = View.VISIBLE
            contentContainer.visibility = View.GONE
        } else {
            noHistoryContainer.visibility = View.GONE
            contentContainer.visibility = View.VISIBLE
            findViewById<View>(R.id.chart_scroll_container).visibility = View.VISIBLE
            chartView.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE
        }

        loadingContainer.animate()
            .alpha(0f)
            .setDuration(Constants.ANIM_DURATION_MEDIUM)
            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
            .withEndAction { 
                loadingContainer.visibility = View.GONE 
                loadingContainer.alpha = 1f // Reset for next time if needed
            }
            .start()

        contentContainer.animate()
            .alpha(1f)
            .setDuration(Constants.ANIM_DURATION_LONG)
            .setStartDelay(Constants.ANIM_DURATION_SHORT)
            .setInterpolator(androidx.interpolator.view.animation.LinearOutSlowInInterpolator())
            .start()

        // "Feel it" Haptics
        PreferenceHelper.performHaptics(chartView, HapticFeedbackConstants.CONFIRM)

        showOverview()
    }

    private fun showOverview() {
        currentLevel = ViewLevel.Overview
        
        // Aggregate by (Month + Currency)
        // Key: "2025-10|USD", "2025-10|INR"
        val grouped = allPayments.groupBy { 
            val cal = Calendar.getInstance()
            cal.time = it.date ?: Date()
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}|${it.currency}"
        }

        // Sort by Time only, then Currency
        val sortedKeys = grouped.keys.sortedWith(compareBy<String> { 
             val parts = it.split("|")[0].split("-")
             parts[0].toInt() * 12 + parts[1].toInt()
        }.thenBy { it.split("|")[1] })

        val chartData = sortedKeys.map { key ->
            val parts = key.split("|")
            val monthKey = parts[0]
            val currency = parts[1]
            val payments = grouped[key] ?: emptyList()
            
            val total = payments.sumOf { it.amount }
            val symbol = CurrencyHelper.getSymbol(currency)
            val date = payments.firstOrNull()?.date ?: Date()
            val monthLabel = dateFormatMonth.format(date)
            
            PaymentHistoryChart.ChartItem(
                monthLabel, 
                total, 
                symbol, 
                ViewLevel.MonthDetail(monthKey, monthLabel, payments)
            )
        }

        chartView.setChartData(chartData)
        updateList(allPayments)
        
        // Hide Back Button
        if (backButton.visibility == View.VISIBLE) {
            backButton.animate().alpha(0f)
                .setDuration(Constants.ANIM_DURATION_SHORT)
                .setInterpolator(androidx.interpolator.view.animation.FastOutLinearInInterpolator())
                .withEndAction {
                    backButton.visibility = View.GONE
                }.start()
        }

        scrollToEnd()
        
        val hScrollView = findViewById<android.widget.HorizontalScrollView>(R.id.chart_scroll_container)
        hScrollView?.let { scrollView ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                var lastHapticScrollX = 0
                scrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
                    if (Math.abs(scrollX - lastHapticScrollX) > 50) {
                        PreferenceHelper.performHaptics(scrollView, HapticFeedbackConstants.CLOCK_TICK)
                        lastHapticScrollX = scrollX
                    }
                }
            }
        }
    }

    private fun showMonthDetail(level: ViewLevel.MonthDetail) {
        currentLevel = level

        // Group by Day + Currency
        val groupedDay = level.payments.groupBy { 
            val cal = Calendar.getInstance()
            cal.time = it.date ?: Date()
            "${cal.get(Calendar.DAY_OF_MONTH)}|${it.currency}"
        }

        val sortedKeys = groupedDay.keys.sortedWith(compareBy<String> { 
             it.split("|")[0].toInt()
        }.thenBy { it.split("|")[1] })

        val chartData = sortedKeys.map { key ->
            val parts = key.split("|")
            val day = parts[0]
            val currency = parts[1]
            val payments = groupedDay[key] ?: emptyList()
            
            val total = payments.sumOf { it.amount }
            val symbol = CurrencyHelper.getSymbol(currency)
            val date = payments.firstOrNull()?.date ?: Date()
            val label = dateFormatDay.format(date)
            
            val dayKey = "${level.monthKey}-$day"
            
            PaymentHistoryChart.ChartItem(
                label,
                total,
                symbol,
                ViewLevel.DayDetail(dayKey, label, payments)
            )
        }
        
        chartView.setChartData(chartData)
        updateList(level.payments.sortedByDescending { it.date })
        
        scrollToEnd()
        
        // Show Back Button
        if (backButton.visibility != View.VISIBLE) {
            backButton.alpha = 0f
            backButton.visibility = View.VISIBLE
            backButton.animate().alpha(1f).setDuration(200).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
        }
    }

    private fun showDayDetail(level: ViewLevel.DayDetail) {
        currentLevel = level

        val groupedSub = level.payments.groupBy { "${it.subscriptionName ?: "Unknown"}|${it.currency}" }
        
        val chartData = groupedSub.map { entry ->
            val parts = entry.key.split("|")
            val subName = parts[0]
            val currency = parts[1]
            val payments = entry.value
            
            val total = payments.sumOf { it.amount }
            val symbol = CurrencyHelper.getSymbol(currency)
            
            PaymentHistoryChart.ChartItem(
                subName,
                total,
                symbol,
                null
            )
        }

        chartView.setChartData(chartData)
        updateList(level.payments.sortedByDescending { it.date })
        
        scrollToEnd()

        // Show Back Button
        if (backButton.visibility != View.VISIBLE) {
            backButton.alpha = 0f
            backButton.visibility = View.VISIBLE
            backButton.animate().alpha(1f).setDuration(200).setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator()).start()
        }
    }

    private fun scrollToEnd() {
        val hScrollView = findViewById<android.widget.HorizontalScrollView>(R.id.chart_scroll_container)
        hScrollView?.let { scrollView ->
            // Use GlobalLayoutListener to ensure we scroll AFTER layout is calculated
            scrollView.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    scrollView.fullScroll(android.widget.HorizontalScrollView.FOCUS_RIGHT)
                }
            })
        }
    }

    private fun handleChartClick(payload: Any?) {
        when (payload) {
            is ViewLevel.MonthDetail -> showMonthDetail(payload)
            is ViewLevel.DayDetail -> showDayDetail(payload)
            else -> {}
        }
    }

    private fun navigateUp() {
        when (currentLevel) {
            is ViewLevel.DayDetail -> {
                val dayLevel = currentLevel as ViewLevel.DayDetail
                val keyParts = dayLevel.dayKey.split("-").toMutableList()
                if (keyParts.size >= 3) {
                    val monthKey = "${keyParts[0]}-${keyParts[1]}"
                    val monthPayments = allPayments.filter { 
                         val cal = Calendar.getInstance()
                         cal.time = it.date ?: Date()
                         "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}" == monthKey
                    }
                    val date = monthPayments.firstOrNull()?.date ?: Date()
                    showMonthDetail(ViewLevel.MonthDetail(monthKey, dateFormatMonth.format(date), monthPayments))
                } else {
                    showOverview()
                }
            }
            is ViewLevel.MonthDetail -> showOverview()
            ViewLevel.Overview -> finish()
        }
    }

    private fun updateList(payments: List<PaymentRecord>) {
        // Automatically animates changes using DiffUtil
        adapter.submitList(payments)
    }

    override fun onSupportNavigateUp(): Boolean {
        PreferenceHelper.performHaptics(toolbar, HapticFeedbackConstants.CLOCK_TICK)
        finish()
        return true
    }
    
    override fun finish() {
        super.finish()
        // Default transition to match other activities
    }

    private fun deleteInvalidRecords(invalidPayments: List<PaymentRecord>) {
        loadingContainer.visibility = View.VISIBLE
        loadingStatus.text = "Cleaning up..."
        
        val batch = firestore.batch()
        var count = 0
        
        invalidPayments.forEach { payment ->
            if (payment.userId.isNotEmpty() && payment.subscriptionId.isNotEmpty() && payment.id != null) {
                val ref = firestore.collection("users").document(payment.userId)
                    .collection("subscriptions").document(payment.subscriptionId)
                    .collection("payments").document(payment.id)
                batch.delete(ref)
                count++
            }
        }
        
        if (count == 0) {
            loadAllPayments()
            return
        }

        batch.commit()
            .addOnSuccessListener {
                loadAllPayments()
            }
            .addOnFailureListener { e ->
                loadingContainer.visibility = View.GONE
                loadAllPayments()
            }
    }
}
