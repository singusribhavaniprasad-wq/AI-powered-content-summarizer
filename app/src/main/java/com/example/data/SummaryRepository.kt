package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SummaryRepository(private val summaryDao: SummaryDao) {

    // Retrieve all summaries from database ordered by recent
    val allSummaries: Flow<List<ContentSummary>> = summaryDao.getAllSummaries().map { entities ->
        entities.map { entity ->
            val cards = summaryDao.getFlashcardsListForSummary(entity.id)
            val questions = summaryDao.getQuizQuestionsListForSummary(entity.id)
            entity.toDomain(cards, questions)
        }
    }

    suspend fun getSummaryById(id: Long): ContentSummary? {
        val entity = summaryDao.getSummaryById(id) ?: return null
        val cards = summaryDao.getFlashcardsListForSummary(id)
        val questions = summaryDao.getQuizQuestionsListForSummary(id)
        return entity.toDomain(cards, questions)
    }

    suspend fun insertSummary(
        summary: ContentSummary
    ): Long {
        val entity = SummaryEntity(
            title = summary.title,
            inputContent = summary.inputContent,
            category = summary.category,
            summaryText = summary.summaryText,
            keyPointsJson = JsonConverter.listToJson(summary.keyPoints),
            keywordsJson = JsonConverter.listToJson(summary.keywords),
            simplifiedExplanation = summary.simplifiedExplanation,
            actionableInsightsJson = JsonConverter.listToJson(summary.actionableInsights),
            smartHighlightsJson = JsonConverter.highlightsToJson(summary.smartHighlights),
            sentiment = summary.sentiment,
            suggestedTopicsJson = JsonConverter.listToJson(summary.suggestedTopics),
            timestamp = System.currentTimeMillis(),
            mode = summary.mode,
            language = summary.language,
            difficulty = summary.difficulty
        )
        val insertedId = summaryDao.insertSummary(entity)

        // Save related flashcards
        if (summary.flashcards.isNotEmpty()) {
            val cardEntities = summary.flashcards.map {
                FlashcardEntity(
                    summaryId = insertedId,
                    front = it.front,
                    back = it.back
                )
            }
            summaryDao.insertFlashcards(cardEntities)
        }

        // Save related quiz questions
        if (summary.quizQuestions.isNotEmpty()) {
            val quizEntities = summary.quizQuestions.map {
                QuizQuestionEntity(
                    summaryId = insertedId,
                    question = it.question,
                    optionsJson = JsonConverter.listToJson(it.options),
                    correctIndex = it.correctIndex,
                    explanation = it.explanation
                )
            }
            summaryDao.insertQuizQuestions(quizEntities)
        }

        return insertedId
    }

    suspend fun deleteSummary(id: Long) {
        summaryDao.deleteSummaryById(id)
        summaryDao.deleteFlashcardsBySummaryId(id)
        summaryDao.deleteQuizQuestionsBySummaryId(id)
    }

    // --- Dynamic User preferences ---
    suspend fun savePreference(key: String, value: String) {
        summaryDao.insertPreference(PreferenceEntity(key, value))
    }

    fun getPreferenceFlow(key: String, defaultValue: String): Flow<String> {
        return summaryDao.getPreferenceFlow(key).map { it?.value ?: defaultValue }
    }

    suspend fun getPreferenceDirect(key: String, defaultValue: String): String {
        return summaryDao.getPreferenceDirect(key)?.value ?: defaultValue
    }
}

// Extension to map SummaryEntity to domain ContentSummary
fun SummaryEntity.toDomain(
    flashcards: List<FlashcardEntity> = emptyList(),
    quizQuestions: List<QuizQuestionEntity> = emptyList()
): ContentSummary {
    return ContentSummary(
        id = id,
        title = title,
        inputContent = inputContent,
        category = category,
        summaryText = summaryText,
        keyPoints = JsonConverter.jsonToList(keyPointsJson),
        keywords = JsonConverter.jsonToList(keywordsJson),
        simplifiedExplanation = simplifiedExplanation,
        actionableInsights = JsonConverter.jsonToList(actionableInsightsJson),
        smartHighlights = JsonConverter.jsonToHighlights(smartHighlightsJson),
        sentiment = sentiment,
        suggestedTopics = JsonConverter.jsonToList(suggestedTopicsJson),
        timestamp = timestamp,
        mode = mode,
        language = language,
        difficulty = difficulty,
        flashcards = flashcards.map { FlashcardItem(it.front, it.back) },
        quizQuestions = quizQuestions.map {
            QuizQuestionItem(
                question = it.question,
                options = JsonConverter.jsonToList(it.optionsJson),
                correctIndex = it.correctIndex,
                explanation = it.explanation
            )
        }
    )
}
