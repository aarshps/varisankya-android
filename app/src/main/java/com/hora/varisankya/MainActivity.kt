package com.hora.varisankya

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
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
    private lateinit var searchTriggerLayout: LinearLayout
    private lateinit var loginContainer: LinearLayout
    private lateinit var appBar: AppBarLayout
    private lateinit var subscriptionsRecyclerView: RecyclerView
    private lateinit var fabAddSubscription: ExtendedFloatingActionButton
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val WEB_CLIENT_ID = "663138385072-bke7f5oflsl2cg0e5maks0ef3n6o113u.apps.googleusercontent.com"

    private var lastFirstVisibleItem = -1
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
        searchTriggerLayout = findViewById(R.id.search_trigger_layout)
        loginContainer = findViewById(R.id.login_container)
        appBar = findViewById(R.id.app_bar)
        subscriptionsRecyclerView = findViewById(R.id.subscriptions_recycler_view)
        fabAddSubscription = findViewById(R.id.fab_add_subscription)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        // Initialize Firebase and Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        firestore = FirebaseFirestore.getInstance()

        // Check current user and update UI
        updateUI(auth.currentUser != null)
        if (auth.currentUser != null) {
            setupRecyclerView()
            setupNotifications()
        } else {
            isDataLoaded = true
        }

        // Setup Swipe Refresh
        setupSwipeRefresh()

        // Set click listeners
        profileImage.setOnClickListener { view ->
            PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CLOCK_TICK)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        searchTriggerLayout.setOnClickListener { view ->
            PreferenceHelper.performHaptics(view, HapticFeedbackConstants.CLOCK_TICK)
            startActivity(Intent(this, SearchActivity::class.java))
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

        // Scroll Behavior and Haptics
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
            }
        })

        checkNotificationPermission()

        // Coordinate SwipeRefresh with AppBar/RecyclerView
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            swipeRefreshLayout.isEnabled = verticalOffset == 0
        }
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
                        return@addSnapshotListener
                    }

                    val subscriptions = snapshots?.toObjects(Subscription::class.java) ?: emptyList()
                    
                    // Sort: Active first, then by due date
                    val sortedSubscriptions = subscriptions.sortedWith(compareByDescending<Subscription> { it.active }.thenBy { it.dueDate })

                    if (sortedSubscriptions.isEmpty()) {
                        emptyStateContainer.visibility = View.VISIBLE
                        subscriptionsRecyclerView.visibility = View.GONE
                    } else {
                        emptyStateContainer.visibility = View.GONE
                        subscriptionsRecyclerView.visibility = View.VISIBLE
                        subscriptionsRecyclerView.adapter = SubscriptionAdapter(sortedSubscriptions) { subscription ->
                            showAddSubscriptionSheet(subscription)
                        }
                    }
                }
        } ?: run {
            isDataLoaded = true
            swipeRefreshLayout.isRefreshing = false
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
            fabAddSubscription.visibility = View.VISIBLE
            
            profileImage.visibility = View.VISIBLE
            searchTriggerLayout.visibility = View.VISIBLE

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