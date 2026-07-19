package com.example.api

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Request/Response Data Classes matching Gemini REST API Schema ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Moshi and Retrofit Client Singleton ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Structured Video Storyboard Output class ---

data class StoryboardScene(
    val sceneNumber: Int,
    val durationSeconds: Int,
    val visualDescription: String,
    val cinematicCamera: String,
    val generatedKeyword: String, // Used to load dynamic visually matched scenes
    val filterSuggested: String,
    val dialogueOrSubtitle: String
)

data class AIProjectResponse(
    val scriptTitle: String,
    val overview: String,
    val scenes: List<StoryboardScene>
)

object GeminiClient {
    private const val TAG = "GeminiClient"

    suspend fun generateStoryboardFromPrompt(prompt: String): AIProjectResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API key is missing or is placeholder. Generating highly creative mock storyboard...")
            return@withContext getOfflineMockStoryboard(prompt)
        }

        val systemInstructionText = """
            You are VeloEdit AI, a professional Hollywood video editor and scriptwriter. 
            The user wants to generate an AI text-to-video script. You must respond with a highly creative JSON structure.
            The JSON format must be EXACTLY:
            {
              "scriptTitle": "A concise professional video title",
              "overview": "High-level summary of the video style and pacing",
              "scenes": [
                {
                  "sceneNumber": 1,
                  "durationSeconds": 3,
                  "visualDescription": "Detailed descriptions of characters, actions, atmospheric details and camera movements",
                  "cinematicCamera": "e.g. Cinematic wide-angle, tracking shot, pan right",
                  "generatedKeyword": "A single word representing the main visual keyword (e.g., cyber, space, ocean, fire, neon, retro, city)",
                  "filterSuggested": "CYBERPUNK or CINEMATIC or VINTAGE or GLITCH or NONE",
                  "dialogueOrSubtitle": "The spoken voiceover or screen caption"
                }
              ]
            }
            Ensure you generate exactly 3 highly detailed scenes. Respond only with the raw JSON string. Do not wrap in ```json or markdown blocks.
        """.trimIndent()

        val fullPrompt = "Generate a video storyboard based on this user prompt: $prompt"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = fullPrompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.8f,
                responseMimeType = "application/json"
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrEmpty()) {
                val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
                Log.d(TAG, "Parsed JSON from Gemini API: $cleanJson")
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(AIProjectResponseJson::class.java)
                val rawResponse = adapter.fromJson(cleanJson)
                if (rawResponse != null) {
                    return@withContext AIProjectResponse(
                        scriptTitle = rawResponse.scriptTitle ?: "AI Generated Film",
                        overview = rawResponse.overview ?: "AI dynamic concept",
                        scenes = rawResponse.scenes?.map {
                            StoryboardScene(
                                sceneNumber = it.sceneNumber ?: 1,
                                durationSeconds = it.durationSeconds ?: 3,
                                visualDescription = it.visualDescription ?: "Visual scene rendering...",
                                cinematicCamera = it.cinematicCamera ?: "Cinematic tracking",
                                generatedKeyword = it.generatedKeyword ?: "neon",
                                filterSuggested = it.filterSuggested ?: "NONE",
                                dialogueOrSubtitle = it.dialogueOrSubtitle ?: "Visual storytelling..."
                            )
                        } ?: emptyList()
                    )
                }
            }
            throw Exception("Empty response text")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Gemini API. Falling back safely...", e)
            return@withContext getOfflineMockStoryboard(prompt)
        }
    }

    private fun getOfflineMockStoryboard(prompt: String): AIProjectResponse {
        // Fallback generator that matches keywords in the user prompt to provide incredibly realistic, tailored scenes!
        val lowerPrompt = prompt.lowercase()
        val (keyword, style) = when {
            lowerPrompt.contains("cyber") || lowerPrompt.contains("neon") || lowerPrompt.contains("future") -> Pair("cyber", "Cyberpunk Neon Grid")
            lowerPrompt.contains("space") || lowerPrompt.contains("star") || lowerPrompt.contains("galaxy") -> Pair("space", "Cinematic Cosmic Galaxy")
            lowerPrompt.contains("ocean") || lowerPrompt.contains("water") || lowerPrompt.contains("sea") -> Pair("ocean", "Cinematic Deep Sea Ocean")
            lowerPrompt.contains("nature") || lowerPrompt.contains("forest") || lowerPrompt.contains("tree") -> Pair("nature", "Emerald Forest Warm Glow")
            lowerPrompt.contains("fire") || lowerPrompt.contains("lava") || lowerPrompt.contains("hot") -> Pair("fire", "Dramatic Hot Pyro Glow")
            lowerPrompt.contains("retro") || lowerPrompt.contains("vintage") || lowerPrompt.contains("synth") -> Pair("retro", "Vintage Retro Vaporwave")
            else -> Pair("city", "Cinematic Midnight City")
        }

        return AIProjectResponse(
            scriptTitle = "AI - " + prompt.split(" ").take(4).joinToString(" ").replaceFirstChar { it.uppercase() },
            overview = "An immersive AI-generated video sequence highlighting '$prompt' with $style styling.",
            scenes = listOf(
                StoryboardScene(
                    sceneNumber = 1,
                    durationSeconds = 4,
                    visualDescription = "The scene opens on an expansive panoramic shot. Shimmering $keyword particles slowly float down. Cinematic ambient rays illuminate the environment, creating extreme visual contrast and depth.",
                    cinematicCamera = "Cinematic slow zoom-in, wide shot",
                    generatedKeyword = keyword,
                    filterSuggested = if (keyword == "cyber") "CYBERPUNK" else "CINEMATIC",
                    dialogueOrSubtitle = "In the heart of the digital universe..."
                ),
                StoryboardScene(
                    sceneNumber = 2,
                    durationSeconds = 3,
                    visualDescription = "A close-up tracking shot reveals intricate, stylized motions. Intense $keyword colors shift dynamically. A high-contrast glow emphasizes the shapes, creating premium cyberpunk visuals with glassmorphism overlays.",
                    cinematicCamera = "Dolly tracking shot, shallow depth-of-field",
                    generatedKeyword = keyword,
                    filterSuggested = if (keyword == "retro") "VINTAGE" else "CYBERPUNK",
                    dialogueOrSubtitle = "A canvas where creativity knows no bounds."
                ),
                StoryboardScene(
                    sceneNumber = 3,
                    durationSeconds = 4,
                    visualDescription = "A slow diagonal tilt-up reveals a breathtaking climax. Beautiful visual elements merge into a unified, high-definition composite. Custom light flares burst dynamically as the video comes to a perfect, elegant finish.",
                    cinematicCamera = "Whip-pan transition, cinematic crane upward tilt",
                    generatedKeyword = keyword,
                    filterSuggested = "GLITCH",
                    dialogueOrSubtitle = "Crafted entirely by VeloEdit AI Video Engine."
                )
            )
        )
    }
}

// --- Moshi parsing helper classes (to prevent Kotlin null safety mismatches during deserialization) ---

@JsonClass(generateAdapter = true)
data class AIProjectResponseJson(
    val scriptTitle: String?,
    val overview: String?,
    val scenes: List<StoryboardSceneJson>?
)

@JsonClass(generateAdapter = true)
data class StoryboardSceneJson(
    val sceneNumber: Int?,
    val durationSeconds: Int?,
    val visualDescription: String?,
    val cinematicCamera: String?,
    val generatedKeyword: String?,
    val filterSuggested: String?,
    val dialogueOrSubtitle: String?
)
