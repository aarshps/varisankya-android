package com.hora.varisankya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.hora.varisankya.util.BiometricAuthManager
import com.hora.varisankya.util.ThemeHelper
import com.hora.varisankya.util.AnimationHelper
import android.widget.Toast
import android.widget.FrameLayout


class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var firestore: FirebaseFirestore

    // UI Views
    private lateinit var btnSignIn: Button
    private lateinit var profileImage: ImageView
    private lateinit var searchTriggerLayout: LinearLayout
    private lateinit var loginContainer: LinearLayout
    private lateinit var appBar: AppBarLayout
    private lateinit var subscriptionsRecyclerView: RecyclerView
    private lateinit var fabAddSubscription: FloatingActionButton
    private lateinit var emptyStateContainer: View
    private lateinit var mainNestedScrollView: androidx.core.widget.NestedScrollView


    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: SubscriptionAdapter
    
    // Hero Section Views
    private lateinit var heroSection: View
    private lateinit var heroLabel: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var expenseSubtitle: TextView
    private lateinit var heroSubtitlePill: com.google.android.material.card.MaterialCardView




    // Root layout for content hiding
    private lateinit var mainContentRoot: View
    private var isAuthSuccessful = false

    private val WEB_CLIENT_ID = "663138385072-bke7f5oflsl2cg0e5maks0ef3n6o113u.apps.googleusercontent.com"

    private var lastFirstVisibleItem = -1
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        
        // Keep splash visible until BOTH biometric auth is successful (if enabled) AND data is loaded
        splashScreen.setKeepOnScreenCondition { 
            !isAuthSuccessful || !isDataLoaded 
        }
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Capture root view for content hiding logic
        mainContentRoot = findViewById(android.R.id.content)

        // Biometric App Lock Check
        if (PreferenceHelper.isBiometricEnabled(this)) {
            // Hide content while authenticating - splash screen covers the app
            mainContentRoot.visibility = View.INVISIBLE
            
            // Pre-initialize app in background so it's ready when auth succeeds
            initializeApp()
            
            BiometricAuthManager.authenticate(this,
                onSuccess = {
                    isAuthSuccessful = true
                    
                    // Haptic feedback on successful unlock
                    val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
                        HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                    mainContentRoot.performHapticFeedback(haptic)
                    
                    // Show content - splash will dismiss automatically via condition
                    mainContentRoot.visibility = View.VISIBLE
                },
                onError = {
                    // On error, close the app
                    finish()
                }
            )
        } else {
            // No auth needed
             isAuthSuccessful = true
             initializeApp()
        }
    }

    private fun initializeApp() {
        // Initialize views

        btnSignIn = findViewById(R.id.btnSignIn)
        profileImage = findViewById(R.id.profile_image)
        searchTriggerLayout = findViewById(R.id.search_trigger_layout)
        loginContainer = findViewById(R.id.login_container)
        appBar = findViewById(R.id.app_bar)
        subscriptionsRecyclerView = findViewById(R.id.subscriptions_recycler_view)

        fabAddSubscription = findViewById(R.id.fab_add_subscription)
        emptyStateContainer = findViewById(R.id.empty_state_container)

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        mainNestedScrollView = findViewById(R.id.main_nested_scroll_view) // Need to add ID to XML
        
        // Hero Init
        heroSection = findViewById(R.id.hero_section)
        heroLabel = findViewById(R.id.hero_label)
        totalExpenseText = findViewById(R.id.total_expense_text)
        expenseSubtitle = findViewById(R.id.expense_subtitle)
        heroSubtitlePill = findViewById(R.id.subtitle_pill)


        // Initialize Firebase and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize RecyclerView Adapter once
        adapter = SubscriptionAdapter(emptyList()) { subscription ->
            showAddSubscriptionSheet(subscription)
        }
        subscriptionsRecyclerView.layoutManager = LinearLayoutManager(this)
        subscriptionsRecyclerView.adapter = adapter
        
        // M3E Mechanical Scroll Feel - Attached to the scrolling container
        PreferenceHelper.attachNestedScrollHaptics(mainNestedScrollView)

        // Check current user and update UI
        updateUI(auth.currentUser != null)
        if (auth.currentUser != null) {
            setupNotifications()
            loadSubscriptions() // Start loading data
        } else {
            isDataLoaded = true
            isDataLoaded = true
        }

        // Handle App Shortcuts
        handleIntent(intent)

        // Setup Swipe Refresh
        setupSwipeRefresh()

        // Set click listeners
        profileImage.setOnClickListener { view ->
            PreferenceHelper.performClickHaptic(view)
            // Premium Logo Shake
            view.animate().rotationBy(15f).setDuration(100).withEndAction {
                view.animate().rotationBy(-30f).setDuration(200).withEndAction {
                    view.animate().rotation(0f).setDuration(100).start()
                }.start()
            }.start()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        AnimationHelper.applySpringOnTouch(profileImage)

        searchTriggerLayout.setOnClickListener { view ->
            PreferenceHelper.performClickHaptic(view)
            startActivity(Intent(this, SearchActivity::class.java))
        }
        AnimationHelper.applySpringOnTouch(searchTriggerLayout)

        btnSignIn.setOnClickListener { view ->
            PreferenceHelper.performSuccessHaptic(view)
            signInWithGoogle()
        }
        AnimationHelper.applySpringOnTouch(btnSignIn)

        fabAddSubscription.setOnClickListener { view ->
            PreferenceHelper.performSuccessHaptic(view)
            showAddSubscriptionSheet()
        }
        // Expressive Touch
        AnimationHelper.applySpringOnTouch(fabAddSubscription)

        heroSection.setOnClickListener { view ->
            PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CLOCK_TICK)
            startActivity(Intent(this, UnifiedHistoryActivity::class.java))
        }
        // Expressive Touch
        AnimationHelper.applySpringOnTouch(heroSection)


        checkNotificationPermission()

        // Coordinate SwipeRefresh with AppBar/RecyclerView
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            swipeRefreshLayout.isEnabled = verticalOffset == 0
            
            // Hide FAB on scroll if needed, or animate scale
            if (verticalOffset == 0) {
                 if (fabAddSubscription.visibility != View.VISIBLE && auth.currentUser != null) fabAddSubscription.show()
            } 
        }

        // Additional Scroll Listener for FAB hiding
        subscriptionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fabAddSubscription.visibility == View.VISIBLE) {
                     fabAddSubscription.hide()
                } else if (dy < 0 && fabAddSubscription.visibility != View.VISIBLE) {
                     fabAddSubscription.show()
                }
            }
        })


    }

    private fun setupSwipeRefresh() {

        val colorPrimary = MaterialColors.getColor(this, android.R.attr.colorPrimary, android.graphics.Color.BLACK)
        val colorSurfaceContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, android.graphics.Color.WHITE)
        
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorSurfaceContainer)
        swipeRefreshLayout.setColorSchemeColors(colorPrimary)

        swipeRefreshLayout.setOnRefreshListener {
            PreferenceHelper.performHaptics(swipeRefreshLayout, HapticFeedbackConstants.CONTEXT_CLICK)
            loadSubscriptions()
        }
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }

    private fun setupNotifications() {
        val hour = PreferenceHelper.getNotificationHour(this)
        val minute = PreferenceHelper.getNotificationMinute(this)
        
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<SubscriptionNotificationWorker>(
            24, TimeUnit.HOURS
        )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "subscription_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    // Removed setupRecyclerView as we do it in onCreate now

    private fun loadSubscriptions() {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection("subscriptions")
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    isDataLoaded = true
                    swipeRefreshLayout.isRefreshing = false
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    val subscriptions = snapshots?.toObjects(Subscription::class.java) ?: emptyList()
                    
                    // Mechanical Scroll Haptics are attached once in initializeApp() for efficiency
                    
                    // Sort: Active first, then by due date
                    val sortedSubscriptions = subscriptions.sortedWith(compareByDescending<Subscription> { it.active }.thenBy { it.dueDate })

                    if (sortedSubscriptions.isEmpty()) {
                        emptyStateContainer.visibility = View.VISIBLE
                        subscriptionsRecyclerView.visibility = View.GONE
                    } else {
                        emptyStateContainer.visibility = View.GONE
                        subscriptionsRecyclerView.visibility = View.VISIBLE
                        // Update existing adapter
                        adapter.updateData(sortedSubscriptions)
                    }
                    
                    updateHeroSection(subscriptions)
                }
        } ?: run {
            isDataLoaded = true
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateHeroSection(subscriptions: List<Subscription>) {
        val activeSubs = subscriptions.filter { it.active && it.dueDate != null }
        
        val today = Calendar.getInstance()
        // Reset to end of today for strict "future" comparison (items due today are arguably "remaining" if not paid, 
        // but typically "remaining" implies future liability. Let's include Today for safety).
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)
        
        var remainingLiability = 0.0
        
        // Logic to calculate remaining liability for CURRENT MONTH
        for (sub in activeSubs) {
            val subDate = Calendar.getInstance()
            subDate.time = sub.dueDate!!
            subDate.set(Calendar.HOUR_OF_DAY, 0)
            subDate.set(Calendar.MINUTE, 0)
            subDate.set(Calendar.SECOND, 0)
            subDate.set(Calendar.MILLISECOND, 0)

            // We need to determine if this subscription has a due date strictly appearing 
            // between TODAY (inclusive) and END OF MONTH.
            
            // Normalize subDate to current month/year for recurrence checks
            // (Simplified assumption: Simple monthly recurrence on the same day)
            // If it's weekly/daily, it's harder.
            // Let's stick to the "Next Occurrence" logic already present in the data or infer it? 
            // The Subscription model has `dueDate`. 
            // IMPORTANT: The app logic updates `dueDate` to the next future date automatically (via Worker/App logic).
            // So `sub.dueDate` IS the next due date.
            
            // So simply: Is `sub.dueDate` in the current month and >= today?
            
            // Calculate Liability:
            // 1. Overdue Items (Before Today)
            // 2. Upcoming Items (After/On Today AND In Current Month)
            
            val isOverdue = subDate.before(today)
            val isCurrentMonth = subDate.get(Calendar.MONTH) == currentMonth && subDate.get(Calendar.YEAR) == currentYear
            
            if (isOverdue || isCurrentMonth) {
                remainingLiability += sub.cost
            }
        }
        
        // Find the absolute next payment (could be next month if nothing left this month)
        val nextPayment = activeSubs
            .filter { 
                val d = Calendar.getInstance()
                d.time = it.dueDate!!
                d.set(Calendar.HOUR_OF_DAY, 0)
                d.set(Calendar.MINUTE, 0)
                d.set(Calendar.SECOND, 0)
                d.set(Calendar.MILLISECOND, 0)
                !d.before(today)
             }
            .minByOrNull { it.dueDate!! }

        // Update UI
        val currentMonthName = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(today.time)
        heroLabel.text = "Remaining in $currentMonthName"
        heroLabel.setTextColor(ThemeHelper.getOnSurfaceVariantColor(this)) // Reset
        
        val symbol = try {
            if (activeSubs.isNotEmpty()) java.util.Currency.getInstance(activeSubs[0].currency).symbol else "₹"
        } catch (e: Exception) { "₹" }
        
        // Use a stable color by default
        totalExpenseText.setTextColor(ThemeHelper.getOnSurfaceColor(this))
        heroSubtitlePill.setCardBackgroundColor(ThemeHelper.getSurfaceContainerHighestColor(this))
        heroSubtitlePill.strokeColor = ThemeHelper.getOutlineVariantColor(this)

        // Animate Count Up
        AnimationHelper.animateTextCountUp(totalExpenseText, remainingLiability, "$symbol ")

        if (nextPayment != null) {
            val format = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            expenseSubtitle.text = "Next: ${nextPayment.name} on ${format.format(nextPayment.dueDate!!)}"
            expenseSubtitle.setTextColor(ThemeHelper.getSecondaryColor(this))
        } else {
             // If no FUTURE payment found, check for OVERDUE items
             val overdueSubs = activeSubs.filter { 
                val d = Calendar.getInstance()
                d.time = it.dueDate!!
                d.set(Calendar.HOUR_OF_DAY, 0)
                d.set(Calendar.MINUTE, 0)
                d.set(Calendar.SECOND, 0)
                d.set(Calendar.MILLISECOND, 0)
                d.before(today)
             }
             
             if (overdueSubs.isNotEmpty()) {
                 // Priority Alert State
                 heroLabel.text = "Overdue Actions"
                 heroLabel.setTextColor(ThemeHelper.getErrorColor(this))
                 
                 // Show total overdue amount
                 val overdueAmount = overdueSubs.sumOf { it.cost }
                 val sym = try { java.util.Currency.getInstance(overdueSubs[0].currency).symbol } catch(e:Exception){"₹"}
                 
                 totalExpenseText.text = "$sym ${if(overdueAmount % 1.0 == 0.0) String.format("%.0f", overdueAmount) else overdueAmount}"
                 expenseSubtitle.text = "${overdueSubs.size} payments are past due"
                 
                 // Urgent Styling
                 totalExpenseText.setTextColor(ThemeHelper.getErrorColor(this))
                 expenseSubtitle.setTextColor(ThemeHelper.getErrorColor(this))
                 heroSubtitlePill.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                 heroSubtitlePill.strokeColor = ThemeHelper.getErrorColor(this)
             } else if (activeSubs.isNotEmpty()) {
                 // All Clear
                 totalExpenseText.text = "All Clear"
                 expenseSubtitle.text = "Relax! No payments left via $currentMonthName"
                 totalExpenseText.setTextColor(ThemeHelper.getPrimaryColor(this))
                 expenseSubtitle.setTextColor(ThemeHelper.getPrimaryColor(this))
             } else {
                 val symbol = try { java.util.Currency.getInstance("INR").symbol } catch (e: Exception) { "₹" }
                 heroLabel.text = "Monthly Expenses"
                 totalExpenseText.text = "${symbol}0"
                 expenseSubtitle.text = "No active subscriptions"
                 expenseSubtitle.setTextColor(ThemeHelper.getSecondaryColor(this))
             }
        }
    }

    private fun getDaysDiff(date: java.util.Date): Long {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val target = Calendar.getInstance()
        target.time = date
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        val diff = target.timeInMillis - today.timeInMillis
        return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
    }


    private fun showAddSubscriptionSheet(subscription: Subscription? = null) {

        val addSubscriptionBottomSheet = AddSubscriptionBottomSheet(subscription) {
            // Firestore's snapshot listener handles reload
        }
        addSubscriptionBottomSheet.show(supportFragmentManager, "AddSubscriptionBottomSheet")
    }
    
    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                // Attempt to clear state to force account chooser
                // Use a timeout so we don't block the UI indefinitely if the system is slow
                // timeout block removed


                val result = credentialManager.getCredential(this@MainActivity, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                Log.e("Auth", "Credential Manager Error", e)
                if (e is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    // User cancelled
                } else {
                   updateUI(false)
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    updateUI(true)
                    setupNotifications()
                    loadSubscriptions() // Reload data for new user
                } else {
                    updateUI(false)
                }
            }
    }

    private fun updateUI(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            loginContainer.visibility = View.GONE
            appBar.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.VISIBLE
            fabAddSubscription.visibility = View.VISIBLE
            
            profileImage.visibility = View.VISIBLE

            auth.currentUser?.photoUrl?.let {
                Picasso.get().load(it).into(profileImage)
            }
        } else {
            loginContainer.visibility = View.VISIBLE
            appBar.visibility = View.GONE
            swipeRefreshLayout.visibility = View.GONE
            fabAddSubscription.visibility = View.GONE

            emptyStateContainer.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            ACTION_ADD_SUBSCRIPTION -> {
                // Delay slightly to allow UI to settle if coming from cold start
                mainContentRoot.postDelayed({
                    showAddSubscriptionSheet(null) 
                }, 300)
            }
            ACTION_VIEW_HISTORY -> {
                mainContentRoot.postDelayed({
                    val intentHistory = Intent(this, UnifiedHistoryActivity::class.java)
                    startActivity(intentHistory)
                }, 300)
            }
        }
    }


    companion object {
        const val ACTION_ADD_SUBSCRIPTION = "com.hora.varisankya.ACTION_ADD_SUBSCRIPTION"
        const val ACTION_VIEW_HISTORY = "com.hora.varisankya.ACTION_VIEW_HISTORY"
    }
}