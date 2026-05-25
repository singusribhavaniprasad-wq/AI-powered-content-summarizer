package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val inputContent: String,
    val category: String,
    val summaryText: String,
    val keyPointsJson: String, // JSON List of Strings
    val keywordsJson: String, // JSON List of Strings
    val simplifiedExplanation: String,
    val actionableInsightsJson: String, // JSON List of Strings
    val smartHighlightsJson: String, // JSON of SmartHighlight elements
    val sentiment: String,
    val suggestedTopicsJson: String, // JSON List of Strings
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String,
    val language: String,
    val difficulty: String
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val summaryId: Long,
    val front: String,
    val back: String
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val summaryId: Long,
    val question: String,
    val optionsJson: String, // JSON List of Strings
    val correctIndex: Int,
    val explanation: String
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
