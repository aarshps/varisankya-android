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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.hora.varisankya.SubscriptionAdapter
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import android.view.ViewGroup
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.hora.varisankya.util.BiometricAuthManager
import com.hora.varisankya.util.ThemeHelper
import com.hora.varisankya.util.AnimationHelper
import com.google.android.material.transition.platform.MaterialSharedAxis
import android.view.Window
import com.hora.varisankya.util.SubscriptionActionHelper
import com.hora.varisankya.viewmodel.MainViewModel
import android.widget.Toast
import android.widget.FrameLayout


class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    
    // ViewModel & Helper
    private lateinit var viewModel: MainViewModel
    private lateinit var actionHelper: SubscriptionActionHelper

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
    private lateinit var loadingSkeleton: View
    private lateinit var mainContentWrapper: View


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
        window.exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = Constants.ANIM_DURATION_LONG
        }
        window.reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = Constants.ANIM_DURATION_LONG
        }
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

    override fun onResume() {
        super.onResume()
        // Refresh adapter to pick up any settings changes (like currency)
        subscriptionsRecyclerView.adapter?.notifyDataSetChanged()
        // Re-observe hero state to update currency display
        viewModel.heroState.value?.let { updateHeroSection(it) }
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
        
        loadingSkeleton = findViewById(R.id.loading_skeleton)
        mainContentWrapper = findViewById(R.id.main_content_wrapper)
        
        // Root layout for content hiding is already assigned as android.R.id.content
        mainContentRoot = findViewById(android.R.id.content)
        appBar = findViewById(R.id.app_bar)
        
        // Edge-to-Edge insets handling
        ViewCompat.setOnApplyWindowInsetsListener(mainContentRoot) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply padding to the *NestedScrollView* so the content can scroll past the FAB
            // FAB is ~56dp high + 16dp margin = 72dp. We use 88dp for comfortable clearance.
            mainNestedScrollView.setPadding(
                mainNestedScrollView.paddingLeft,
                mainNestedScrollView.paddingTop,
                mainNestedScrollView.paddingRight,
                insets.bottom + (88 * resources.displayMetrics.density).toInt()
            )
            mainNestedScrollView.clipToPadding = false
            
            // Allow list itself to also have safe bottom space relative to its container
            subscriptionsRecyclerView.setPadding(
                subscriptionsRecyclerView.paddingLeft,
                subscriptionsRecyclerView.paddingTop,
                subscriptionsRecyclerView.paddingRight,
                (24 * resources.displayMetrics.density).toInt()
            )
            
            // Float the FAB above the nav bar
            val fabParams = fabAddSubscription.layoutParams as ViewGroup.MarginLayoutParams
            fabParams.bottomMargin = insets.bottom + (16 * resources.displayMetrics.density).toInt()
            fabAddSubscription.layoutParams = fabParams
            
            windowInsets
        }

        // Initialize Firebase and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        
        // Initialize ViewModel and ActionHelper
        viewModel = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        actionHelper = SubscriptionActionHelper(this, viewModel)
        
        // Initialize RecyclerView Adapter once
        adapter = SubscriptionAdapter(emptyList()) { subscription ->
            showAddSubscriptionSheet(subscription)
        }
        subscriptionsRecyclerView.layoutManager = LinearLayoutManager(this)
        subscriptionsRecyclerView.adapter = adapter
        
        // Check current user and update UI
        updateUI(auth.currentUser != null)
        
        // Load data if user is signed in
        if (auth.currentUser != null) {
            setupNotifications()
            viewModel.loadSubscriptions()
            observeViewModel()
        } else {
            isDataLoaded = true
        }


        // Handle App Shortcuts
        handleIntent(intent)

        // Setup Swipe Refresh
        setupSwipeRefresh()
        
        // Setup Swipe Actions
        setupSwipeActions()

        // Set click listeners
        profileImage.setOnClickListener { view ->
            PreferenceHelper.performClickHaptic(view)
            // Premium Logo Shake (Slower)
            view.animate().rotationBy(15f).setDuration(Constants.ANIM_DURATION_SHORT).withEndAction {
                view.animate().rotationBy(-30f).setDuration(Constants.ANIM_DURATION_MEDIUM).withEndAction {
                    view.animate().rotation(0f).setDuration(Constants.ANIM_DURATION_SHORT).start()
                }.start()
            }.start()
            val options = android.app.ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            startActivity(Intent(this, SettingsActivity::class.java), options)
        }

        searchTriggerLayout.setOnClickListener { view ->
            PreferenceHelper.performClickHaptic(view)
            val options = android.app.ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            startActivity(Intent(this, SearchActivity::class.java), options)
        }

        btnSignIn.setOnClickListener { view ->
            PreferenceHelper.performSuccessHaptic(view)
            signInWithGoogle()
        }

        fabAddSubscription.setOnClickListener { view ->
            PreferenceHelper.performSuccessHaptic(view)
            showAddSubscriptionSheet()
        }
        // Expressive Touch

        heroSection.setOnClickListener { view ->
            PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CLOCK_TICK)
            val options = android.app.ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            startActivity(Intent(this, UnifiedHistoryActivity::class.java), options)
        }
        // Expressive Touch


        checkNotificationPermission()

        // Coordinate SwipeRefresh with AppBar/RecyclerView
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            swipeRefreshLayout.isEnabled = verticalOffset == 0
            
            // Show FAB when scrolled back to top
            if (verticalOffset == 0) {
                 if (fabAddSubscription.visibility != View.VISIBLE && auth.currentUser != null) fabAddSubscription.show()
            } 
        }

        // NestedScrollView Scroll Listener for FAB hiding AND M3E Haptics
        var accumulatedDy = 0
        val thresholdPx = (40 * resources.displayMetrics.density).toInt()
        
        mainNestedScrollView.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            
            // 1. M3E Mechanical Scroll Haptics
            if (PreferenceHelper.isHapticsEnabled(this)) {
                accumulatedDy += dy
                if (Math.abs(accumulatedDy) >= thresholdPx) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    } else {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    accumulatedDy %= thresholdPx
                }
            }

            // 2. FAB Hiding Logic
            if (dy > 12 && fabAddSubscription.visibility == View.VISIBLE) {
                 fabAddSubscription.hide()
            } else if (dy < -12 && fabAddSubscription.visibility != View.VISIBLE) {
                 fabAddSubscription.show()
            }
        })

        // Safety fallback: if data fails to load or takes too long, dismiss splash anyway after 5s
        mainNestedScrollView.postDelayed({
            if (!isDataLoaded) {
                isDataLoaded = true
            }
        }, 5000)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.subscriptions.collect { subscriptions ->
                        if (subscriptions.isEmpty()) {
                            val isLoading = viewModel.isLoading.value
                            if (!isLoading) {
                                checkReadyState()
                                emptyStateContainer.visibility = View.VISIBLE
                                subscriptionsRecyclerView.visibility = View.GONE
                            }
                        } else {
                            emptyStateContainer.visibility = View.GONE
                            subscriptionsRecyclerView.visibility = View.VISIBLE
                            adapter.updateData(subscriptions)
                            checkReadyState()
                        }
                    }
                }
                
                launch {
                    viewModel.heroState.collect { state ->
                        updateHeroSection(state)
                        checkReadyState()
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { loading ->
                        if (loading) {
                            swipeRefreshLayout.isRefreshing = true
                            if (mainContentWrapper.visibility != View.VISIBLE) {
                                loadingSkeleton.alpha = 1f
                                loadingSkeleton.visibility = View.VISIBLE
                                mainContentWrapper.visibility = View.GONE
                                emptyStateContainer.visibility = View.GONE 
                            }
                        } else {
                            swipeRefreshLayout.isRefreshing = false
                            if (loadingSkeleton.visibility == View.VISIBLE) {
                                loadingSkeleton.visibility = View.GONE
                                mainContentWrapper.visibility = View.VISIBLE
                                AnimationHelper.animateReveal(mainContentWrapper)
                                PreferenceHelper.performClickHaptic(mainContentWrapper)
                            }
                            
                            val currentList = viewModel.subscriptions.value
                            if (currentList.isEmpty()) {
                                emptyStateContainer.visibility = View.VISIBLE
                                subscriptionsRecyclerView.visibility = View.GONE
                            } else {
                                emptyStateContainer.visibility = View.GONE
                                subscriptionsRecyclerView.visibility = View.VISIBLE
                            }
                            checkReadyState()
                        }
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        if (errorMsg != null) {
                            android.widget.Toast.makeText(this@MainActivity, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                            checkReadyState()
                        }
                    }
                }
            }
        }

        checkReadyState()
    }

    private fun checkReadyState() {
        val isLoading = viewModel.isLoading.value
        if (!isLoading) {
             isDataLoaded = true
        }
    }

    
    private fun setupSwipeActions() {
        // Delegated to Helper
        actionHelper.setupSwipeActions(subscriptionsRecyclerView, adapter, mainContentRoot)
    }



    private fun updateHeroSection(state: MainViewModel.HeroState) {
        val today = Calendar.getInstance()
        val currentMonthName = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(today.time)
        

        
        // Reset defaults
        heroLabel.text = "Remaining in $currentMonthName"
        heroLabel.setTextColor(ThemeHelper.getOnSurfaceVariantColor(this))
        totalExpenseText.setTextColor(ThemeHelper.getOnSurfaceColor(this))
        heroSubtitlePill.setCardBackgroundColor(ThemeHelper.getSurfaceContainerHighestColor(this))
        heroSubtitlePill.strokeColor = ThemeHelper.getOutlineVariantColor(this)

        val activeSubs = state.activeSubscriptions
        
        // --- Global Currency ---
        val primaryCurrency = PreferenceHelper.getCurrency(this)
        val primaryTotal = state.totalAmount
        val symbol = CurrencyHelper.getSymbol(primaryCurrency)
        
        val nextPayment = state.nextPayment
        
        if (nextPayment != null) {
            val format = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            expenseSubtitle.text = "Next: ${nextPayment.name} on ${format.format(nextPayment.dueDate!!)}"
            expenseSubtitle.setTextColor(ThemeHelper.getSecondaryColor(this))
            
            // Pass symbol with space for AnimationHelper logic
            AnimationHelper.animateTextCountUp(totalExpenseText, primaryTotal, "$symbol ")
        } else {
             // Check Overdue
             val overdueSubs = state.overdueSubscriptions
             
             if (overdueSubs.isNotEmpty()) {
                 // Priority Alert State
                 heroLabel.text = "Overdue Actions"
                 heroLabel.setTextColor(ThemeHelper.getErrorColor(this))
                 
                 val overdueAmount = overdueSubs.sumOf { it.cost } // Simplification
                 
                 totalExpenseText.text = "${overdueSubs.size} Items"
                 expenseSubtitle.text = "Payments are past due"
                 
                 // Urgent Styling
                 totalExpenseText.setTextColor(ThemeHelper.getErrorColor(this))
                 expenseSubtitle.setTextColor(ThemeHelper.getErrorColor(this))
                 heroSubtitlePill.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                 heroSubtitlePill.strokeColor = ThemeHelper.getErrorColor(this)
             } else if (activeSubs.isNotEmpty()) {
                 // All Clear - FREEDOM STATE
                 if (primaryTotal == 0.0) {
                     heroLabel.text = "Financial Zen"
                     totalExpenseText.text = "All Clear"
                     expenseSubtitle.text = "No payments left for $currentMonthName. Enjoy!"
                     
                     // Friendly Green/Teal
                     val zenColor = ThemeHelper.getPrimaryColor(this) 
                     totalExpenseText.setTextColor(zenColor)
                     expenseSubtitle.setTextColor(zenColor)
                     heroSubtitlePill.setCardBackgroundColor(ThemeHelper.getReferenceColor(this, com.google.android.material.R.attr.colorSecondaryContainer))
                 } else {
                       // Has liability but no 'next payment'
                       AnimationHelper.animateTextCountUp(totalExpenseText, primaryTotal, "$symbol ")
                       expenseSubtitle.text = "Total remaining for $currentMonthName"
                 }

             } else {
                 // True Empty State
                 val defaultSym = CurrencyHelper.getSymbol(primaryCurrency)
                 heroLabel.text = "Monthly Expenses"
                 totalExpenseText.text = CurrencyHelper.formatCurrency(this, 0.0, primaryCurrency)
                 expenseSubtitle.text = "No active subscriptions"
                 expenseSubtitle.setTextColor(ThemeHelper.getSecondaryColor(this))
                 
                 // Friendly "Start"
                 heroLabel.text = "Get Started"
                 totalExpenseText.text = "Welcome"
                 totalExpenseText.textSize = 32f // Slightly smaller than numbers logic usually
             }
        }
    }


    private fun setupSwipeRefresh() {
        val colorPrimary = MaterialColors.getColor(this, android.R.attr.colorPrimary, android.graphics.Color.BLACK)
        val colorSurfaceContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, android.graphics.Color.WHITE)
        
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorSurfaceContainer)
        swipeRefreshLayout.setColorSchemeColors(colorPrimary)

        swipeRefreshLayout.setOnRefreshListener {
            PreferenceHelper.performHaptics(swipeRefreshLayout, HapticFeedbackConstants.CONTEXT_CLICK)
            viewModel.loadSubscriptions() // Use ViewModel
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
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    // Removed setupRecyclerView as we do it in onCreate now





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
                    viewModel.loadSubscriptions() // Reload data for new user
                    observeViewModel()
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

            auth.currentUser?.photoUrl?.let { url ->
                // Lightweight coroutine image loader instead of heavy Picasso library
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = URL(url.toString()).openStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        withContext(Dispatchers.Main) {
                            profileImage.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
