package com.hora.varisankya

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hora.varisankya.util.AnimationHelper
import com.google.android.material.transition.platform.MaterialSharedAxis
import android.view.Window

class SearchActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var searchEditText: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var autopayChip: Chip
    private lateinit var notAutopayChip: Chip
    private lateinit var activeChip: Chip
    private lateinit var inactiveChip: Chip
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var adapter: SubscriptionAdapter
    
    private var allSubscriptions: List<Subscription> = emptyList()

    private var lastFirstVisibleItem = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = Constants.ANIM_DURATION_LONG
        }
        window.returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = Constants.ANIM_DURATION_LONG
        }
        setContentView(R.layout.activity_search)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        searchEditText = findViewById(R.id.search_edit_text)
        categoryChipGroup = findViewById(R.id.search_category_chip_group)
        autopayChip = findViewById(R.id.chip_autopay_filter)
        notAutopayChip = findViewById(R.id.chip_not_autopay_filter)
        activeChip = findViewById(R.id.chip_active_filter)
        inactiveChip = findViewById(R.id.chip_inactive_filter)
        searchRecyclerView = findViewById(R.id.search_recycler_view)
        emptyStateContainer = findViewById(R.id.empty_state_container)

        searchRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Initialize adapter with empty list
        adapter = SubscriptionAdapter(emptyList()) { subscription ->
            val addSubscriptionBottomSheet = AddSubscriptionBottomSheet(subscription) {
                // Determine views again or just a simple reload (could be cleaner, but for now looking up works)
                val contentContainer = findViewById<View>(R.id.content_container)
                val loadingContainer = findViewById<View>(R.id.loading_container)
                val loadingStatus = findViewById<TextView>(R.id.loading_status)
                
                // For reload, maybe minimal loading or same full loading?
                // Let's do full loading for consistency
                loadingContainer.alpha = 1f
                loadingContainer.visibility = View.VISIBLE
                contentContainer.visibility = View.GONE
                loadAllSubscriptions(contentContainer, loadingContainer, loadingStatus) 
            }
            addSubscriptionBottomSheet.show(supportFragmentManager, "AddSubscriptionBottomSheet")
        }
        searchRecyclerView.adapter = adapter
        
        // Scroll Haptics
        PreferenceHelper.attachScrollHaptics(searchRecyclerView)
        
        setupCategories()
        setupFilters()
        
        // Initial Loading State
        val contentContainer = findViewById<View>(R.id.content_container)
        val loadingContainer = findViewById<View>(R.id.loading_container)
        val loadingStatus = findViewById<TextView>(R.id.loading_status)
        
        loadingContainer.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        
        loadAllSubscriptions(contentContainer, loadingContainer, loadingStatus)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        categoryChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            performSearch()
        }
        
        searchEditText.requestFocus()
    }



    private fun setupCategories() {
        categoryChipGroup.removeAllViews()
        Constants.CATEGORIES.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = false
                
                updateChipStyle(this)
                
                setOnClickListener {
                    PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
                    updateChipStyle(this)
                }
                
                // Expressive Touch
                AnimationHelper.applySpringOnTouch(this)
            }
            categoryChipGroup.addView(chip)
        }
    }

    private fun updateChipStyle(chip: Chip) {
        com.hora.varisankya.util.ChipHelper.styleChip(chip)
    }






    private fun loadAllSubscriptions(contentContainer: View, loadingContainer: View, loadingStatus: TextView) {
        auth.currentUser?.uid?.let { userId ->
            loadingStatus.text = "Syncing..."
            
            firestore.collection("users").document(userId)
                .collection("subscriptions")
                .get()
                .addOnSuccessListener { snapshots ->
                    allSubscriptions = snapshots.toObjects(Subscription::class.java)
                    
                    val targetView = if (allSubscriptions.isEmpty()) emptyStateContainer else contentContainer
                    val otherView = if (allSubscriptions.isEmpty()) contentContainer else emptyStateContainer

                    if (allSubscriptions.isNotEmpty()) {
                        performSearch()
                    }
                    
                    // Smooth Crossfade
                    targetView.alpha = 0f
                    targetView.visibility = View.VISIBLE
                    otherView.visibility = View.GONE
                    
                    loadingContainer.animate()
                        .alpha(0f)
                        .setDuration(Constants.ANIM_DURATION_MEDIUM)
                        .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                        .withEndAction { loadingContainer.visibility = View.GONE }
                        .start()

                    targetView.animate()
                        .alpha(1f)
                        .setDuration(Constants.ANIM_DURATION_LONG)
                        .setStartDelay(Constants.ANIM_DURATION_SHORT)
                        .setInterpolator(androidx.interpolator.view.animation.LinearOutSlowInInterpolator())
                        .start()
                }
                .addOnFailureListener {
                    loadingContainer.visibility = View.GONE
                    contentContainer.visibility = View.VISIBLE
                }
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().lowercase().trim()
        val selectedCategories = getSelectedCategories()

        val filtered = allSubscriptions.filter { sub ->
            val matchText = sub.name.lowercase().contains(query) || 
                            sub.category.lowercase().contains(query)
            
            val matchCategory = if (selectedCategories.isEmpty()) true else sub.category in selectedCategories
            
            val matchAutopay = if (autopayChip.isChecked) sub.autopay else true
            val matchNotAutopay = if (notAutopayChip.isChecked) !sub.autopay else true
            
            val matchActive = if (activeChip.isChecked) sub.active else true
            val matchInactive = if (inactiveChip.isChecked) !sub.active else true
            
            matchText && matchCategory && matchAutopay && matchNotAutopay && matchActive && matchInactive
        }
        
        // Sort: Active first, then query match, then due date
        val sortedFiltered = filtered.sortedWith(
            compareByDescending<Subscription> { it.active }
                .thenByDescending { it.name.lowercase() == query }
                .thenBy { it.dueDate }
        )
        
        // Update existing adapter instead of creating a new one
        adapter.updateData(sortedFiltered)
    }

    private fun getSelectedCategories(): List<String> {
        val checkedIds = categoryChipGroup.checkedChipIds
        val categories = mutableListOf<String>()
        for (id in checkedIds) {
            val chip = categoryChipGroup.findViewById<Chip>(id)
            if (chip != null) {
                categories.add(chip.text.toString())
            }
        }
        return categories
    }

    private fun setupFilters() {
        val chips = listOf(autopayChip, notAutopayChip, activeChip, inactiveChip)
        
        chips.forEach { chip ->
            // M3E Expressive styling
            com.hora.varisankya.util.ChipHelper.styleChip(chip)
            AnimationHelper.applySpringOnTouch(chip)
            
            chip.setOnClickListener {
                PreferenceHelper.performHaptics(it, HapticFeedbackConstants.CLOCK_TICK)
                com.hora.varisankya.util.ChipHelper.styleChip(chip)
                performSearch()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        finish()
        return true
    }
}