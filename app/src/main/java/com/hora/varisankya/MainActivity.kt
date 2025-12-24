package com.hora.varisankya

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var firestore: FirebaseFirestore

    // UI Views
    private lateinit var btnSignIn: Button
    private lateinit var profileImage: ImageView
    private lateinit var settingsButton: ImageButton
    private lateinit var appLogoImage: ImageView
    private lateinit var loginContainer: LinearLayout
    private lateinit var appBar: AppBarLayout
    private lateinit var subscriptionsRecyclerView: RecyclerView
    private lateinit var fabAddSubscription: ExtendedFloatingActionButton
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Search Views
    private lateinit var searchView: SearchView
    private lateinit var searchCategoryChipGroup: ChipGroup
    private lateinit var searchRecyclerView: RecyclerView

    private val WEB_CLIENT_ID = "663138385072-bke7f5oflsl2cg0e5maks0ef3n6o113u.apps.googleusercontent.com"

    private var lastFirstVisibleItem = -1
    private var posBeforeScroll = -1
    private var allSubscriptions: List<Subscription> = emptyList()
    private var appBarOffset = 0
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isDataLoaded }
        
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        btnSignIn = findViewById(R.id.btnSignIn)
        profileImage = findViewById(R.id.profile_image)
        settingsButton = findViewById(R.id.settings_button)
        appLogoImage = findViewById(R.id.app_logo_image)
        loginContainer = findViewById(R.id.login_container)
        appBar = findViewById(R.id.app_bar)
        subscriptionsRecyclerView = findViewById(R.id.subscriptions_recycler_view)
        fabAddSubscription = findViewById(R.id.fab_add_subscription)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        
        searchView = findViewById(R.id.search_view)
        searchCategoryChipGroup = findViewById(R.id.search_category_chip_group)
        searchRecyclerView = findViewById(R.id.search_recycler_view)

        // Initialize Firebase and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        firestore = FirebaseFirestore.getInstance()

        // Check current user and update UI
        updateUI(auth.currentUser != null)
        if (auth.currentUser != null) {
            setupRecyclerView()
            setupNotifications()
            setupSearch()
        } else {
            isDataLoaded = true
        }

        // Setup Swipe Refresh
        setupSwipeRefresh()

        // Set click listeners
        settingsButton.setOnClickListener { view ->
            PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CLOCK_TICK)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSignIn.setOnClickListener { view ->
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            PreferenceHelper.performHaptics(view, haptic)
            signInWithGoogle()
        }

        fabAddSubscription.setOnClickListener { view ->
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
            PreferenceHelper.performHaptics(view, haptic)
            showAddSubscriptionSheet()
        }

        // FAB scroll behavior and Haptics
        subscriptionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) fabAddSubscription.shrink()
                else if (dy < 0) fabAddSubscription.extend()

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val firstVisibleItem = layoutManager?.findFirstVisibleItemPosition() ?: -1
                
                if (firstVisibleItem != lastFirstVisibleItem && firstVisibleItem != -1) {
                    PreferenceHelper.performHaptics(recyclerView, HapticFeedbackConstants.CLOCK_TICK)
                    lastFirstVisibleItem = firstVisibleItem
                }
                
                updateSwipeRefreshEnableState()
            }
        })

        checkNotificationPermission()

        // Coordinate SwipeRefresh with AppBar/RecyclerView
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            this.appBarOffset = verticalOffset
            updateSwipeRefreshEnableState()
        }
        
        setupSnapping()
    }
    
    private fun updateSwipeRefreshEnableState() {
        if (!::swipeRefreshLayout.isInitialized) return
        
        if (emptyStateContainer.visibility == View.VISIBLE) {
            swipeRefreshLayout.isEnabled = appBarOffset == 0
            return
        }

        val layoutManager = subscriptionsRecyclerView.layoutManager as? LinearLayoutManager
        val firstVisibleItem = layoutManager?.findFirstVisibleItemPosition() ?: -1
        val firstView = layoutManager?.findViewByPosition(0)
        
        val isSearchBarVisible = firstVisibleItem == 0 && (firstView?.top ?: -1) == 0
        
        swipeRefreshLayout.isEnabled = appBarOffset == 0 && isSearchBarVisible
    }
    
    private fun setupSnapping() {
        subscriptionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastState = RecyclerView.SCROLL_STATE_IDLE
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    posBeforeScroll = layoutManager.findFirstVisibleItemPosition()
                }

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val firstPos = layoutManager.findFirstVisibleItemPosition()
                    
                    if (firstPos == 0) {
                        val view = layoutManager.findViewByPosition(0) ?: return
                        val height = view.height
                        val top = view.top
                        val visibleHeight = height + top
                        
                        if (lastState == RecyclerView.SCROLL_STATE_SETTLING) {
                             if (posBeforeScroll > 2 && visibleHeight > 0) {
                                 PreferenceHelper.performHaptics(recyclerView, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.LONG_PRESS)
                                 recyclerView.smoothScrollBy(0, visibleHeight)
                             } else if (visibleHeight > 0 && top < 0) {
                                 PreferenceHelper.performHaptics(recyclerView, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.LONG_PRESS)
                                 recyclerView.smoothScrollBy(0, top)
                             }
                        } else {
                            if (visibleHeight < height * 0.5) {
                                PreferenceHelper.performHaptics(recyclerView, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.LONG_PRESS)
                                recyclerView.smoothScrollBy(0, visibleHeight)
                            } else {
                                PreferenceHelper.performHaptics(recyclerView, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.LONG_PRESS)
                                recyclerView.smoothScrollBy(0, top)
                            }
                        }
                    }
                }
                lastState = newState
            }
        })
    }
    
    private fun setupSearch() {
        searchRecyclerView.layoutManager = LinearLayoutManager(this)
        
        try {
            // Material 3 SearchView removal of dividers and border logic
            searchView.addTransitionListener { _, _, newState ->
                if (newState == SearchView.TransitionState.SHOWN || newState == SearchView.TransitionState.SHOWING) {
                    hideAllDividers(searchView)
                }
            }
            
            // Explicitly set surface color and 0 elevation to internal toolbar
            val toolbarId = resources.getIdentifier("search_view_toolbar", "id", "com.google.android.material")
            if (toolbarId != 0) {
                val toolbar = searchView.findViewById<androidx.appcompat.widget.Toolbar>(toolbarId)
                toolbar?.let {
                    it.elevation = 0f
                    it.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            
            hideAllDividers(searchView)
            
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not hide search view divider", e)
        }
        
        searchCategoryChipGroup.removeAllViews()
        Constants.CATEGORIES.forEach { category ->
            val chip = Chip(ContextThemeWrapper(this, com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
                text = category
                isCheckable = true
                setOnClickListener {
                    PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
                }
            }
            searchCategoryChipGroup.addView(chip)
        }

        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        searchCategoryChipGroup.setOnCheckedStateChangeListener { _, _ ->
             performSearch(searchView.text.toString())
        }
    }

    private fun hideAllDividers(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val className = child.javaClass.simpleName
                
                var isDivider = className.contains("Divider", ignoreCase = true)
                if (!isDivider && child.id != View.NO_ID) {
                    try {
                        val entryName = resources.getResourceEntryName(child.id)
                        if (entryName.contains("divider", ignoreCase = true)) {
                            isDivider = true
                        }
                    } catch (e: Exception) {
                        // Resource not found or other issue, ignore
                    }
                }
                
                if (isDivider || (className == "View" && child.height in 1..10)) {
                    child.visibility = View.GONE
                    child.layoutParams?.let {
                        it.height = 0
                        child.layoutParams = it
                    }
                }
                hideAllDividers(child)
            }
        }
    }

    private fun performSearch(query: String) {
        val lowerQuery = query.lowercase().trim()
        val selectedCategories = getSelectedCategories()
        
        val filtered = allSubscriptions.filter { sub ->
            val matchText = sub.name.lowercase().contains(lowerQuery) || 
                            sub.category.lowercase().contains(lowerQuery)
            
            val matchCategory = if (selectedCategories.isEmpty()) true else sub.category in selectedCategories
            
            matchText && matchCategory
        }
        
        val sortedFiltered = filtered.sortedWith(compareByDescending<Subscription> { 
            it.name.lowercase() == lowerQuery 
        }.thenBy { 
            it.dueDate 
        })
        
        searchRecyclerView.adapter = SubscriptionAdapter(
            sortedFiltered,
            onSubscriptionClicked = { subscription ->
                searchView.hide()
                showAddSubscriptionSheet(subscription)
            },
            showSearchHeader = false
        )
    }
    
    private fun getSelectedCategories(): List<String> {
        val checkedIds = searchCategoryChipGroup.checkedChipIds
        val categories = mutableListOf<String>()
        for (id in checkedIds) {
            val chip = searchCategoryChipGroup.findViewById<Chip>(id)
            if (chip != null) {
                categories.add(chip.text.toString())
            }
        }
        return categories
    }

    private fun setupSwipeRefresh() {
        val colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK)
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
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
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

    private fun setupRecyclerView() {
        subscriptionsRecyclerView.layoutManager = LinearLayoutManager(this)
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection("subscriptions")
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    isDataLoaded = true
                    swipeRefreshLayout.isRefreshing = false
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e)
                        updateSwipeRefreshEnableState()
                        return@addSnapshotListener
                    }

                    val subscriptions = snapshots?.toObjects(Subscription::class.java) ?: emptyList()
                    allSubscriptions = subscriptions
                    
                    if (searchView.isShowing) {
                        performSearch(searchView.text.toString())
                    }
                    
                    if (subscriptions.isEmpty()) {
                        emptyStateContainer.visibility = View.VISIBLE
                        subscriptionsRecyclerView.visibility = View.GONE
                    } else {
                        emptyStateContainer.visibility = View.GONE
                        subscriptionsRecyclerView.visibility = View.VISIBLE
                        subscriptionsRecyclerView.adapter = SubscriptionAdapter(
                            subscriptions, 
                            onSubscriptionClicked = { subscription ->
                                showAddSubscriptionSheet(subscription)
                            },
                            onSearchBarCreated = { searchBar ->
                                searchView.setupWithSearchBar(searchBar)
                                searchBar.setOnClickListener {
                                    val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
                                    PreferenceHelper.performHaptics(it, haptic)
                                    searchView.show()
                                }
                            },
                            showSearchHeader = true
                        )
                        if (subscriptions.isNotEmpty() && lastFirstVisibleItem == -1) {
                            (subscriptionsRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(1, 0)
                        }
                    }
                    updateSwipeRefreshEnableState()
                }
        } ?: run {
            isDataLoaded = true
            swipeRefreshLayout.isRefreshing = false
            updateSwipeRefreshEnableState()
        }
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
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@MainActivity, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                Log.e("Auth", "Credential Manager Error", e)
                if (e is NoCredentialException) {
                    Toast.makeText(this@MainActivity, "No accounts found. Please add a Google account.", Toast.LENGTH_LONG).show()
                } else if (e is GetCredentialException) {
                     Toast.makeText(this@MainActivity, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                updateUI(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    updateUI(true)
                    setupRecyclerView()
                    setupNotifications()
                    setupSearch()
                } else {
                    updateUI(false)
                    Toast.makeText(this, "Firebase Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUI(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            loginContainer.visibility = View.GONE
            appBar.visibility = View.VISIBLE
            
            profileImage.visibility = View.VISIBLE
            settingsButton.visibility = View.VISIBLE
            appLogoImage.visibility = View.VISIBLE

            auth.currentUser?.photoUrl?.let {
                Picasso.get().load(it).into(profileImage)
            }
        } else {
            loginContainer.visibility = View.VISIBLE
            appBar.visibility = View.GONE
            subscriptionsRecyclerView.visibility = View.GONE
            fabAddSubscription.visibility = View.GONE
        }
    }
}