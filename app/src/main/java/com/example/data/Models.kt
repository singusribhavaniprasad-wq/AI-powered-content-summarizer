package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- UI / Domain Models ---

data class ContentSummary(
    val id: Long,
    val title: String,
    val inputContent: String,
    val category: String,
    val summaryText: String,
    val keyPoints: List<String>,
    val keywords: List<String>,
    val simplifiedExplanation: String,
    val actionableInsights: List<String>,
    val smartHighlights: List<HighlightItem>,
    val sentiment: String,
    val suggestedTopics: List<String>,
    val timestamp: Long,
    val mode: String,
    val language: String,
    val difficulty: String,
    val flashcards: List<FlashcardItem> = emptyList(),
    val quizQuestions: List<QuizQuestionItem> = emptyList()
)

data class HighlightItem(
    val text: String,
    val type: String // "concept", "action", "deadline", "term"
)

data class FlashcardItem(
    val front: String,
    val back: String
)

data class QuizQuestionItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

// --- JSON Helpers using Moshi (which exists in libs) ---

object JsonConverter {
    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    
    private val highlightListType = Types.newParameterizedType(List::class.java, HighlightItem::class.java)
    private val highlightListAdapter = moshi.adapter<List<HighlightItem>>(highlightListType)

    fun listToJson(list: List<String>): String {
        return try {
            stringListAdapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun highlightsToJson(list: List<HighlightItem>): String {
        return try {
            highlightListAdapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToHighlights(json: String?): List<HighlightItem> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            highlightListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
