package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val summary: ContentSummary) : UiState()
    data class Error(val message: String) : UiState()
}

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SummaryRepository
    
    // UI state for active summarization
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Database state flows
    val summaries: StateFlow<List<ContentSummary>>
    
    // Preferences live states (backed by Room DB preferences)
    val preferredLanguage: StateFlow<String>
    val preferredStyle: StateFlow<String>
    val difficultyLevel: StateFlow<String>
    val customInstructions: StateFlow<String>

    // Currently selected summary for detailed view
    private val _selectedSummary = MutableStateFlow<ContentSummary?>(null)
    val selectedSummary: StateFlow<ContentSummary?> = _selectedSummary.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.summaryDao()
        repository = SummaryRepository(dao)

        summaries = repository.allSummaries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        preferredLanguage = repository.getPreferenceFlow("PREF_LANG", "English").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "English"
        )

        preferredStyle = repository.getPreferenceFlow("PREF_STYLE", "BULLET SUMMARY").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "BULLET SUMMARY"
        )

        difficultyLevel = repository.getPreferenceFlow("PREF_DIFFICULTY", "Intermediate").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Intermediate"
        )

        customInstructions = repository.getPreferenceFlow("PREF_CUSTOM", "").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    }

    fun summarizeContent(
        content: String,
        mode: String,
        lang: String,
        difficulty: String,
        customPrompt: String = ""
    ) {
        if (content.isBlank()) {
            _uiState.value = UiState.Error("Please enter some content to summarize.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = GeminiService.generateContentDeconstruction(
                    content = content,
                    mode = mode,
                    language = lang,
                    difficulty = difficulty,
                    customPrompt = customPrompt
                )
                
                if (result != null) {
                    // Check if title is blank and set template
                    val finalResult = if (result.title.isBlank()) result.copy(title = "Untitled") else result
                    
                    // Save to Room DB persistence
                    val insertedId = repository.insertSummary(finalResult)
                    val saved = finalResult.copy(id = insertedId)
                    
                    _uiState.value = UiState.Success(saved)
                    _selectedSummary.value = saved
                } else {
                    _uiState.value = UiState.Error(
                        "Failed to generate summary. Please check your Gemini API Key in the Secrets panel, verify internet connectivity, and try again."
                    )
                }
            } catch (e: Exception) {
                Log.e("SummaryViewModel", "Summarization failed", e)
                _uiState.value = UiState.Error("An error occurred during summarization: ${e.message}")
            }
        }
    }

    fun selectSummary(summary: ContentSummary?) {
        _selectedSummary.value = summary
        if (summary != null) {
            _uiState.value = UiState.Success(summary)
        } else {
            _uiState.value = UiState.Idle
        }
    }

    fun deleteSummary(id: Long) {
        viewModelScope.launch {
            repository.deleteSummary(id)
            if (_selectedSummary.value?.id == id) {
                _selectedSummary.value = null
                _uiState.value = UiState.Idle
            }
        }
    }

    fun savePreference(key: String, value: String) {
        viewModelScope.launch {
            repository.savePreference(key, value)
        }
    }

    fun setIdleState() {
        _uiState.value = UiState.Idle
    }
}
