package com.example.smsguard.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the structure of embeddings JSON files
 */
data class EmbeddingsFile(
    @SerializedName("class") val className: String,
    @SerializedName("model_name") val modelName: String,
    @SerializedName("embedding_dimension") val embeddingDimension: Int,
    @SerializedName("total_embeddings") val totalEmbeddings: Int,
    @SerializedName("embeddings") val embeddings: List<List<Float>>,
    @SerializedName("texts") val texts: List<String>,
    @SerializedName("ids") val ids: List<String>
)

/**
 * Data class representing a single embedding entry with its metadata
 */
data class EmbeddingEntry(
    val id: String,
    val text: String,
    val embedding: List<Float>,
    val className: String
)

/**
 * Data class representing a search result with similarity score
 */
data class SimilarityResult(
    val entry: EmbeddingEntry,
    val similarity: Float
)

/**
 * Data class representing the examples block for the prompt
 */
data class PromptExamples(
    val benignExamples: List<String>,
    val smishingExamples: List<String>
) 