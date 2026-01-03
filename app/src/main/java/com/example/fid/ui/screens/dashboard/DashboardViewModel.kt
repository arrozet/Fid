package com.example.fid.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fid.data.database.entities.FoodEntry
import com.example.fid.data.database.entities.User
import com.example.fid.data.database.entities.WellnessEntry
import com.example.fid.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for DashboardScreen
 * Handles all business logic and state management for the dashboard
 */
class DashboardViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _foodEntries = MutableStateFlow<List<FoodEntry>>(emptyList())
    val foodEntries: StateFlow<List<FoodEntry>> = _foodEntries.asStateFlow()

    private val _wellnessEntry = MutableStateFlow<WellnessEntry?>(null)
    val wellnessEntry: StateFlow<WellnessEntry?> = _wellnessEntry.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Date range for today
    private val startOfDay: Long
    private val endOfDay: Long

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfDay = calendar.timeInMillis
        endOfDay = startOfDay + 24 * 60 * 60 * 1000

        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = repository.getCurrentUser()
                _user.value = currentUser

                currentUser?.let { u ->
                    // Calculate and save daily summary
                    repository.calculateAndSaveDailySummary(u.id, System.currentTimeMillis())

                    // Load wellness entry
                    _wellnessEntry.value = repository.getTodayWellnessEntry(u.id)

                    // Load food entries with Flow
                    repository.getFoodEntriesByDateRange(u.id, startOfDay, endOfDay).collect { entries ->
                        _foodEntries.value = entries
                        updateUiState()
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshWellness() {
        viewModelScope.launch {
            _user.value?.let { u ->
                _wellnessEntry.value = repository.getTodayWellnessEntry(u.id)
            }
        }
    }

    fun addWaterIntake(amountMl: Float) {
        viewModelScope.launch {
            _user.value?.let { u ->
                val updatedEntry = repository.addWaterIntake(u.id, amountMl)
                _wellnessEntry.value = updatedEntry
            }
        }
    }

    fun resetWaterIntake() {
        viewModelScope.launch {
            _user.value?.let { u ->
                val updatedEntry = repository.resetWaterIntake(u.id)
                _wellnessEntry.value = updatedEntry
            }
        }
    }

    fun setSleepHours(hours: Float) {
        viewModelScope.launch {
            _user.value?.let { u ->
                val updatedEntry = repository.setSleepHours(u.id, hours)
                _wellnessEntry.value = updatedEntry
            }
        }
    }

    fun deleteFoodEntry(foodEntry: FoodEntry) {
        viewModelScope.launch {
            repository.deleteFoodEntry(foodEntry)
            // The Flow will automatically update the list
        }
    }

    private fun updateUiState() {
        val entries = _foodEntries.value
        _uiState.value = DashboardUiState(
            totalCalories = entries.sumOf { it.calories.toDouble() }.toFloat(),
            totalProtein = entries.sumOf { it.proteinG.toDouble() }.toFloat(),
            totalFat = entries.sumOf { it.fatG.toDouble() }.toFloat(),
            totalCarbs = entries.sumOf { it.carbG.toDouble() }.toFloat()
        )
    }
}

/**
 * UI State for Dashboard
 */
data class DashboardUiState(
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalFat: Float = 0f,
    val totalCarbs: Float = 0f
)
