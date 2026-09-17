package project.handson1.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EmbeddingGenerator(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val API_URL = "https://api.x.ai/v1/embeddings"

    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isEmpty()) {
                Log.w("EmbeddingGenerator", "API key is empty, using random embedding")
                return@withContext generateRandomEmbedding()
            }

            val json = JSONObject().apply {
                put("model", "text-embedding-ada-002")
                put("input", text.take(8000)) // Limit text length
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    val data = jsonResponse.getJSONArray("data")
                    if (data.length() > 0) {
                        val embeddingArray = data.getJSONObject(0).getJSONArray("embedding")
                        FloatArray(embeddingArray.length()) { index ->
                            embeddingArray.getDouble(index).toFloat()
                        }
                    } else {
                        generateRandomEmbedding()
                    }
                } else {
                    Log.e("EmbeddingGenerator", "Error: ${response.code} - ${response.message}")
                    generateRandomEmbedding()
                }
            }
        } catch (e: Exception) {
            Log.e("EmbeddingGenerator", "Error generating embedding: ${e.message}")
            generateRandomEmbedding()
        }
    }

    private fun generateRandomEmbedding(): FloatArray {
        return FloatArray(1536) { (Math.random() * 2 - 1).toFloat() }
    }
}