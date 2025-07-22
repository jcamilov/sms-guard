package com.example.smsguard.service

import android.content.Context
import android.util.Log
import com.example.smsguard.data.model.EmbeddingEntry
import com.example.smsguard.data.model.EmbeddingsFile
import com.example.smsguard.data.model.SimilarityResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for semantic search using cosine similarity
 */
@Singleton
class SemanticSearchService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SemanticSearchService"
        private const val BENIGN_EMBEDDINGS_FILE = "models/embeddings/benign_embeddings.json"
        private const val SMISHING_EMBEDDINGS_FILE = "models/embeddings/smishing_embeddings.json"
        private const val EXAMPLES_PER_CLASS = 2
    }
    
    private var benignEmbeddings: List<EmbeddingEntry> = emptyList()
    private var smishingEmbeddings: List<EmbeddingEntry> = emptyList()
    private var isInitialized = false
    
    /**
     * Initialize the semantic search service by loading embeddings
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing semantic search service...")
            
            // Load embeddings from JSON files
            benignEmbeddings = loadEmbeddingsFromFile(BENIGN_EMBEDDINGS_FILE, "benign")
            smishingEmbeddings = loadEmbeddingsFromFile(SMISHING_EMBEDDINGS_FILE, "smishing")
            
            isInitialized = true
            Log.d(TAG, "Semantic search service initialized successfully. " +
                    "Loaded ${benignEmbeddings.size} benign and ${smishingEmbeddings.size} smishing embeddings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize semantic search service", e)
            isInitialized = false
            false
        }
    }
    
    /**
     * Find similar examples for a given embedding
     */
    fun findSimilarExamples(queryEmbedding: List<Float>): Pair<List<EmbeddingEntry>, List<EmbeddingEntry>> {
        if (!isInitialized) {
            Log.w(TAG, "Service not initialized")
            return Pair(emptyList(), emptyList())
        }
        
        return try {
            Log.d(TAG, "Starting semantic search with embedding dimension: ${queryEmbedding.size}")
            Log.d(TAG, "Available embeddings - Benign: ${benignEmbeddings.size}, Smishing: ${smishingEmbeddings.size}")
            
            val benignSimilar = findTopKSimilar(queryEmbedding, benignEmbeddings, EXAMPLES_PER_CLASS)
            val smishingSimilar = findTopKSimilar(queryEmbedding, smishingEmbeddings, EXAMPLES_PER_CLASS)
            
            Log.d(TAG, "Semantic search completed:")
            Log.d(TAG, "  - Found ${benignSimilar.size} benign examples")
            benignSimilar.forEachIndexed { index, example ->
                Log.d(TAG, "    Benign example ${index + 1}: \"${example.text.take(100)}${if (example.text.length > 100) "..." else ""}\"")
            }
            
            Log.d(TAG, "  - Found ${smishingSimilar.size} smishing examples")
            smishingSimilar.forEachIndexed { index, example ->
                Log.d(TAG, "    Smishing example ${index + 1}: \"${example.text.take(100)}${if (example.text.length > 100) "..." else ""}\"")
            }
            
            Pair(benignSimilar, smishingSimilar)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding similar examples", e)
            Pair(emptyList(), emptyList())
        }
    }
    
    /**
     * Check if the service is ready
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Load embeddings from JSON file
     */
    private fun loadEmbeddingsFromFile(filePath: String, className: String): List<EmbeddingEntry> {
        return try {
            val jsonString = context.assets.open(filePath).bufferedReader().use { it.readText() }
            val embeddingsFile = Gson().fromJson(jsonString, EmbeddingsFile::class.java)
            
            embeddingsFile.embeddings.mapIndexed { index, embedding ->
                EmbeddingEntry(
                    id = embeddingsFile.ids.getOrNull(index) ?: "id_$index",
                    text = embeddingsFile.texts.getOrNull(index) ?: "",
                    embedding = embedding,
                    className = className
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading embeddings from $filePath", e)
            emptyList()
        }
    }
    
    /**
     * Find top K most similar embeddings using cosine similarity
     */
    private fun findTopKSimilar(
        queryEmbedding: List<Float>,
        embeddings: List<EmbeddingEntry>,
        k: Int
    ): List<EmbeddingEntry> {
        if (embeddings.isEmpty()) return emptyList()
        
        val similarities = embeddings.map { entry ->
            SimilarityResult(
                entry = entry,
                similarity = cosineSimilarity(queryEmbedding, entry.embedding)
            )
        }
        
        return similarities
            .sortedByDescending { it.similarity }
            .take(k)
            .map { it.entry }
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private fun cosineSimilarity(vectorA: List<Float>, vectorB: List<Float>): Float {
        if (vectorA.size != vectorB.size) {
            Log.w(TAG, "Vector dimensions don't match: ${vectorA.size} vs ${vectorB.size}")
            return 0f
        }
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in vectorA.indices) {
            val a = vectorA[i].toDouble()
            val b = vectorB[i].toDouble()
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }
        
        val denominator = Math.sqrt(normA) * Math.sqrt(normB)
        return if (denominator > 0) (dotProduct / denominator).toFloat() else 0f
    }
} 