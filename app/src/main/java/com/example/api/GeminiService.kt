package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

// Structure of Gemini API Response
data class RawGeminiResponse(
    val title: String,
    val category: String,
    val summaryText: String,
    val keyPoints: List<String>,
    val keywords: List<String>,
    val simplifiedExplanation: String,
    val actionableInsights: List<String>,
    val smartHighlights: List<HighlightItem>,
    val sentiment: String,
    val suggestedTopics: List<String>,
    val flashcards: List<FlashcardItem>,
    val quizQuestions: List<QuizQuestionItem>
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun generateContentDeconstruction(
        content: String,
        mode: String,
        language: String,
        difficulty: String,
        customPrompt: String = ""
    ): ContentSummary? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API key is not configured or is placeholder")
            return@withContext null
        }

        // Construct the Gemini prompt
        val systemInstructions = """
            You are an expert Content Deconstruction & AI Learning Assistant. Your goal is to analyze the input content and output a completely structured JSON representation of its summary, deconstructed notes, and interactive learning elements.
            
            Depending on the summary mode chosen by the user, adjust your tone, format, and depth of details:
            - "SHORT SUMMARY": A super brief 2-5 line high impact concise overview.
            - "BULLET SUMMARY": Core essential details structured into precise, beautiful bullet points.
            - "STUDENT NOTES": Exam-oriented, syllabus-style note format with key definitions, concepts, and prospective oral/interview questions.
            - "BEGINNER EXPLANATION": Teach like the reader is a absolute novice. Use extremely simple analogies, paint vivid mental pictures, and fully exclude technical jargon.
            - "PROFESSIONAL": A formal, business-oriented executive brief, centering high level operations and objectives.
            - "STORY STYLE": Transform the material into an engaging narrative storytelling story, showing instead of just telling.
            - "SOCIAL MEDIA": Convert facts into a highly clickable multi-part format (e.g., a Twitter/X thread format, LinkedIn executive snippet, or Instagram visual captions).
            
            You MUST output a valid, parsable JSON object exactly matching the schema below. Keep formatting neat and use correct escapes:
            {
              "title": "A compelling, clear title based on the content",
              "category": "Pick exactly one: Educational / Technical / Medical / Business / News / Motivational / Research / Entertainment",
              "summaryText": "The actual full summary based on the requested mode",
              "keyPoints": ["Bullet point 1", "Bullet point 2", "..."],
              "keywords": ["significant keyword 1", "keyword 2", "..."],
              "simplifiedExplanation": "Explanatory teach-in of difficult concepts for complete beginners.",
              "actionableInsights": ["Action 1", "Action 2", "..."],
              "smartHighlights": [
                {
                  "text": "precise quote or sentence from original text to highlight",
                  "type": "concept" or "action" or "deadline" or "term"
                }
              ],
              "sentiment": "Pick exactly one: Positive / Negative / Neutral / Motivational / Stressful",
              "suggestedTopics": ["Follow-up learning topic 1", "Topic 2", "..."],
              "flashcards": [
                {
                  "front": "Front of card (Concept or Question)",
                  "back": "Back of card (Exact explanation or answer)"
                }
              ],
              "quizQuestions": [
                {
                  "question": "A multiple choice question testing understanding",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 0,
                  "explanation": "Detailed explanation of why the correct option is right and others are wrong"
                }
              ]
            }
            
            CRITICAL RULES:
            - The final summary output MUST be in: $language.
            - The level of vocabulary, concept explanations, and depth of notes must match: $difficulty.
            - Factual Accuracy: Strictly preserve meanings. Never hallucinate terms.
            - Do not return any Markdown fencing (such as ```json) around the response. Only output raw JSON block.
        """.trimIndent()

        val prompt = """
            $systemInstructions
            
            Input Content to Deconstruct and Summarize:
            ---
            $content
            ---
            
            Additional Special Instructions from User:
            $customPrompt
        """.trimIndent()

        // Create API REQUEST
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val requestUrl = "$BASE_URL?key=$apiKey"
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Request failed: ${response.code} - $errorBody")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    Log.e(TAG, "No candidate array found in response")
                    return@withContext null
                }
                
                val firstCandidate = candidates.getJSONObject(0)
                val responseText = firstCandidate.getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                Log.d(TAG, "Received raw text from Gemini: $responseText")
                
                // Parse the text which is expected to be a direct JSON object
                val deconstructedJson = JSONObject(responseText)
                
                // Read and fallback safely
                val title = deconstructedJson.optString("title", "Untitled Content")
                val category = deconstructedJson.optString("category", "Educational")
                val summaryText = deconstructedJson.optString("summaryText", "")
                val simplifiedExplanation = deconstructedJson.optString("simplifiedExplanation", "")
                val sentiment = deconstructedJson.optString("sentiment", "Neutral")
                
                val keyPoints = mutableListOf<String>()
                deconstructedJson.optJSONArray("keyPoints")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        keyPoints.add(arr.getString(i))
                    }
                }
                
                val keywords = mutableListOf<String>()
                deconstructedJson.optJSONArray("keywords")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        keywords.add(arr.getString(i))
                    }
                }
                
                val actionableInsights = mutableListOf<String>()
                deconstructedJson.optJSONArray("actionableInsights")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        actionableInsights.add(arr.getString(i))
                    }
                }
                
                val suggestedTopics = mutableListOf<String>()
                deconstructedJson.optJSONArray("suggestedTopics")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        suggestedTopics.add(arr.getString(i))
                    }
                }

                val smartHighlights = mutableListOf<HighlightItem>()
                deconstructedJson.optJSONArray("smartHighlights")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        smartHighlights.add(
                            HighlightItem(
                                text = obj.optString("text", ""),
                                type = obj.optString("type", "concept")
                            )
                        )
                    }
                }

                val flashcards = mutableListOf<FlashcardItem>()
                deconstructedJson.optJSONArray("flashcards")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        flashcards.add(
                            FlashcardItem(
                                front = obj.optString("front", ""),
                                back = obj.optString("back", "")
                            )
                        )
                    }
                }

                val quizQuestions = mutableListOf<QuizQuestionItem>()
                deconstructedJson.optJSONArray("quizQuestions")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val optArr = obj.optJSONArray("options")
                        val opts = mutableListOf<String>()
                        if (optArr != null) {
                            for (j in 0 until optArr.length()) {
                                opts.add(optArr.getString(j))
                            }
                        }
                        quizQuestions.add(
                            QuizQuestionItem(
                                question = obj.optString("question", ""),
                                options = opts,
                                correctIndex = obj.optInt("correctIndex", 0),
                                explanation = obj.optString("explanation", "")
                            )
                        )
                    }
                }

                return@withContext ContentSummary(
                    id = 0L,
                    title = title,
                    inputContent = content,
                    category = category,
                    summaryText = summaryText,
                    keyPoints = keyPoints,
                    keywords = keywords,
                    simplifiedExplanation = simplifiedExplanation,
                    actionableInsights = actionableInsights,
                    smartHighlights = smartHighlights,
                    sentiment = sentiment,
                    suggestedTopics = suggestedTopics,
                    timestamp = System.currentTimeMillis(),
                    mode = mode,
                    language = language,
                    difficulty = difficulty,
                    flashcards = flashcards,
                    quizQuestions = quizQuestions
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
            null
        }
    }
}
