---
description: Guidelines for high-performance rendering and memory management in Varisankya to maintain smoothness.
---

# App Performance & Rendering Standards

Varisankya aims to feel incredibly fast, lightweight, and smooth, ensuring 60fps+ rendering at all times. Below are the mandated practices for avoiding frame drops, reducing garbage collection stutter, and keeping the APK size small.

## 1. Eliminate Main Thread "Heavy Lifting"
Calculations, formatting, and sorting must NEVER happen on the Main UI thread if they involve lists of data. Even if operations seem fast, doing them on dynamic lists will cause dropped frames during entry animations or fast scrolling.

### Offload Requirements:
- **Date/Timestamp Manipulation:** Creating `Calendar.getInstance()`, extracting days/months, or running `Date.before()` logic inside a list iteration must be wrapped in `Dispatchers.Default`.
- **Sorting/Grouping:** Use Kotlin Coroutines (`viewModelScope.launch(Dispatchers.Default)` or `lifecycleScope.launch(Dispatchers.Default)`) to execute `.groupBy()`, `.sortedBy()`, or `.filter()` on large lists (like Payment History or Subscriptions).
- **Return to Main:** Only touch the Main thread at the very end to push the resulting compiled UI states using `withContext(Dispatchers.Main)`.

## 2. Eliminate Object Allocation in Adapters
RecyclerView adapters bind rows continually as the user scrolls. Allocating new objects inside `onBindViewHolder` causes excessive Garbage Collection (GC) which presents as visual stuttering to the user.

### Caching Requirements:
- **Theme Colors:** Resolve colors ONCE at the class level and reuse them. Do not call `ThemeHelper.get*` or `MaterialColors.getColor()` on every row bind. Store them in `private var colorPrimary = 0` and flag when resolved.
- **Date Formatters:** Keep `SimpleDateFormat` as a `private val` property. NEVER instantiate it inside `onBindViewHolder`.
- **Calendars:** Use a single shared `private val calendar = Calendar.getInstance()` and use `calendar.timeInMillis` to do long-based arithmetic instead of spawning new `Calendar` instances per row to evaluate relative dates.

## 3. Avoid Heavy 3rd-Party Dependencies
Varisankya should remain extremely lightweight.

- Avoid adding massive third-party libraries (e.g., Picasso, Glide) if you only need them for a single, simple operation.
- For basic tasks like loading a profile picture thumbnail, prefer using standard framework tooling under a Coroutine `Dispatchers.IO` scope alongside `URL(url).openStream()` and `BitmapFactory`. 
- Every dependency adds APK bloat and memory overhead; ask if there is a lightweight native alternative first.

## 4. Enable R8 Minification
Release builds must have R8 Minification enabled to strip unused code boundaries from the included core libraries. Ensure `isMinifyEnabled = true` and `isShrinkResources = true` is declared in `build.gradle.kts`.
