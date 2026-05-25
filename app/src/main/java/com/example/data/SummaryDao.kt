package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestionEntity>)

    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<SummaryEntity>>

    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummaryById(id: Long): SummaryEntity?

    @Query("SELECT * FROM flashcards WHERE summaryId = :summaryId")
    fun getFlashcardsForSummary(summaryId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE summaryId = :summaryId")
    suspend fun getFlashcardsListForSummary(summaryId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM quiz_questions WHERE summaryId = :summaryId")
    fun getQuizQuestionsForSummary(summaryId: Long): Flow<List<QuizQuestionEntity>>

    @Query("SELECT * FROM quiz_questions WHERE summaryId = :summaryId")
    suspend fun getQuizQuestionsListForSummary(summaryId: Long): List<QuizQuestionEntity>

    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Long)

    @Query("DELETE FROM flashcards WHERE summaryId = :id")
    suspend fun deleteFlashcardsBySummaryId(id: Long)

    @Query("DELETE FROM quiz_questions WHERE summaryId = :id")
    suspend fun deleteQuizQuestionsBySummaryId(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)

    @Query("SELECT * FROM preferences WHERE `key` = :key")
    fun getPreferenceFlow(key: String): Flow<PreferenceEntity?>

    @Query("SELECT * FROM preferences WHERE `key` = :key")
    suspend fun getPreferenceDirect(key: String): PreferenceEntity?
}
