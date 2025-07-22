package com.example.smsguard.service

import android.content.Context
import android.util.Log
import com.example.smsguard.data.model.SMSClassification
import com.example.smsguard.data.model.SMSMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.mediapipe.tasks.genai.llminference.LlmInference
//import com.google.mediapipe.tasks.genai.llminference.LlmInferenceOptions
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withTimeout

/**
 * Implementation of AIClassifier using Gemma 3n model
 * Uses MediaPipe LLM Inference API for inference
 */
@Singleton
class Gemma3nClassifier @Inject constructor(
    private val context: Context,
    private val embeddingService: EmbeddingService,
    private val semanticSearchService: SemanticSearchService,
    private val promptBuilder: PromptBuilder
) : AIClassifier {
    
    companion object {
        private const val TAG = "Gemma3nClassifier"
        private const val MODEL_PATH = "models/gemma3n/gemma-3n-E2B-it-int4.task"
        private const val TIMEOUT_MS = 30000L // Increased timeout to 30 seconds
        private const val MAX_RETRIES = 2
        private const val MAX_TOKENS = 1024*8 // Increased token limit
        private const val MAX_SMS_INPUT_LENGTH = 400 // Limit input length to prevent token overflow
        private const val MAX_PROMPT_INPUT_LENGTH = 14000 // Limit input length to prevent token overflow
    }
    
    private var llmInference: LlmInference? = null
    private var isInitialized = false
    
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Gemma 3n classifier (MediaPipe LLM Inference)...")
            
            // Initialize embedding and semantic search services
            val embeddingInitialized = embeddingService.initialize()
            val semanticSearchInitialized = semanticSearchService.initialize()
            val promptBuilderInitialized = promptBuilder.initialize()
            
            if (!embeddingInitialized || !semanticSearchInitialized || !promptBuilderInitialized) {
                Log.e(TAG, "Failed to initialize required services")
                return@withContext false
            }
            
            // First try to use the model from /data/local/tmp/llm/ (if pushed via adb)
            val externalModelPath = "/data/local/tmp/llm/gemma-3n-E2B-it-int4.task"
            val externalModelFile = File(externalModelPath)
            
            val modelPath = if (externalModelFile.exists() && externalModelFile.canRead()) {
                Log.d(TAG, "Using external model from: $externalModelPath")
                externalModelPath
            } else {
                // Fallback to copying from assets
                Log.d(TAG, "External model not found, copying from assets...")
                val modelFile = copyModelFromAssets()
                modelFile.absolutePath
            }
            
            Log.d(TAG, "Using model path: $modelPath")
            
            val taskOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTopK(32) // Reduced from 64 to save memory
                .setMaxTokens(MAX_TOKENS) // Set maximum tokens
                .build()
            llmInference = LlmInference.createFromOptions(context, taskOptions)
            isInitialized = true
            Log.d(TAG, "Gemma 3n classifier initialized successfully (MediaPipe LLM Inference)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemma 3n classifier (MediaPipe LLM Inference)", e)
            isInitialized = false
            false
        }
    }
    
    override fun isModelReady(): Boolean = isInitialized && llmInference != null
    
    override suspend fun classifySMS(message: String): SMSClassification = withContext(Dispatchers.IO) {
        var retryCount = 0
        while (retryCount <= MAX_RETRIES) {
            try {
                Log.d(TAG, "=== Starting SMS classification (attempt ${retryCount + 1}) ===")
                Log.d(TAG, "Original message length: ${message.length} characters")
                
                if (!isModelReady()) {
                    Log.w(TAG, "Model not ready, initializing...")
                    if (!initialize()) {
                        Log.e(TAG, "Failed to initialize model, returning UNCLASSIFIED")
                        return@withContext SMSClassification.UNCLASSIFIED
                    }
                }
                
                // Truncate message if too long to prevent token overflow
                val truncatedMessage = if (message.length > MAX_SMS_INPUT_LENGTH) {
                    Log.w(TAG, "Message too long (${message.length} chars), truncating to $MAX_SMS_INPUT_LENGTH")
                    message.take(MAX_SMS_INPUT_LENGTH) + "..."
                } else {
                    message
                }
                
                Log.d(TAG, "Step 1: Generating embedding for truncated message (${truncatedMessage.length} chars)")
                // Generate embedding for the SMS with timeout
                val embedding = withTimeout(TIMEOUT_MS) {
                    embeddingService.generateEmbedding(truncatedMessage)
                }
                
                if (embedding == null) {
                    Log.e(TAG, "Failed to generate embedding, using fallback prompt")
                    val prompt = buildFallbackClassificationPrompt(truncatedMessage)
                    val result = withTimeout(TIMEOUT_MS) {
                        llmInference?.generateResponse(prompt)
                    }
                    return@withContext parseClassificationResult(result)
                }
                
                Log.d(TAG, "Step 2: Embedding generated successfully (${embedding.size} dimensions)")
                Log.d(TAG, "Step 3: Finding similar examples via semantic search...")
                
                // Find similar examples with timeout
                val (benignExamples, smishingExamples) = withTimeout(TIMEOUT_MS) {
                    semanticSearchService.findSimilarExamples(embedding)
                }
                
                Log.d(TAG, "Step 4: Building prompt with ${benignExamples.size} benign and ${smishingExamples.size} smishing examples")
                
                // Build prompt with examples and limit length
                val prompt = promptBuilder.buildPrompt(truncatedMessage, benignExamples, smishingExamples)
                
                // Check prompt length and truncate if necessary
                val finalPrompt = if (prompt.length > MAX_PROMPT_INPUT_LENGTH) {
                    Log.w(TAG, "Prompt too long (${prompt.length} chars), truncating")
                    prompt.take(MAX_PROMPT_INPUT_LENGTH) + "..."
                } else {
                    prompt
                }
                
                Log.d(TAG, "Step 5: Sending prompt to Gemma 3n model (${finalPrompt.length} chars)")
                Log.d(TAG, "Step 6: Starting LLM inference (timeout: ${TIMEOUT_MS}ms)...")
                
                val result = withTimeout(TIMEOUT_MS) {
                    llmInference?.generateResponse(finalPrompt)
                }
                
                Log.d(TAG, "Step 7: LLM inference completed")
                Log.d(TAG, "Raw LLM response: \"${result?.take(200)}${if (result?.length ?: 0 > 200) "..." else ""}\"")
                
                val classification = parseClassificationResult(result)
                Log.d(TAG, "Step 8: Parsed classification result: $classification")
                Log.d(TAG, "=== SMS classification completed successfully ===")
                return@withContext classification
                
            } catch (e: Exception) {
                retryCount++
                Log.e(TAG, "Error during SMS classification (attempt $retryCount)", e)
                
                if (retryCount > MAX_RETRIES) {
                    Log.e(TAG, "Max retries reached, returning UNCLASSIFIED")
                    return@withContext SMSClassification.UNCLASSIFIED
                }
                
                // Wait before retry
                kotlinx.coroutines.delay(1000L * retryCount)
            }
        }
        
        SMSClassification.UNCLASSIFIED
    }
    
    override suspend fun getExplanation(sms: SMSMessage): String = withContext(Dispatchers.IO) {
        try {
            if (!isModelReady()) {
                return@withContext "Unable to generate explanation - model not ready"
            }
            
            // Truncate message if too long
            val truncatedMessage = if (sms.message.length > MAX_SMS_INPUT_LENGTH) {
                sms.message.take(MAX_SMS_INPUT_LENGTH) + "..."
            } else {
                sms.message
            }
            
            val prompt = buildExplanationPrompt(sms.copy(message = truncatedMessage))
            val result = withTimeout(TIMEOUT_MS) {
                llmInference?.generateResponse(prompt)
            }
            parseExplanationResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating explanation", e)
            "Unable to generate explanation due to an error"
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            llmInference?.close()
            llmInference = null
            isInitialized = false
            embeddingService.cleanup()
            Log.d(TAG, "Gemma3nClassifier cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up Gemma3nClassifier", e)
        }
    }
    
    private fun copyModelFromAssets(): File {
        val modelDir = File(context.filesDir, "models/gemma3n")
        modelDir.mkdirs()
        
        val modelFile = File(modelDir, "gemma-3n-E2B-it-int4.task")
        
        if (!modelFile.exists()) {
            Log.d(TAG, "Copying model from assets...")
            try {
                context.assets.open(MODEL_PATH).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Model copied successfully to: ${modelFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy model from assets", e)
                throw e
            }
        } else {
            Log.d(TAG, "Model already exists at: ${modelFile.absolutePath}")
        }
        
        return modelFile
    }
    
    private fun buildFallbackClassificationPrompt(message: String): String {
        return """
        Analyze the following SMS message and classify it as either "BENIGN" or "SMISHING".
        
        SMS: "$message"
        
        Consider these indicators for smishing:
        - Urgent requests for personal information
        - Suspicious links or phone numbers
        - Requests for immediate action
        - Offers that seem too good to be true
        - Threats or pressure tactics
        - Requests for financial information
        
        Respond with only: BENIGN or SMISHING
        """.trimIndent()
    }
    
    private fun buildExplanationPrompt(sms: SMSMessage): String {
        return """
        Explain why this SMS message is classified as smishing:
        
        From: ${sms.sender}
        Message: ${sms.message}
        
        Provide a brief explanation of the suspicious elements detected in less than 2 sentences.
        """.trimIndent()
    }
    
    private fun parseClassificationResult(result: String?): SMSClassification {
        return when {
            result == null -> SMSClassification.UNCLASSIFIED
            result.contains("SMISHING", ignoreCase = true) -> SMSClassification.SMISHING
            result.contains("BENIGN", ignoreCase = true) -> SMSClassification.BENIGN
            else -> SMSClassification.UNCLASSIFIED
        }
    }
    
    private fun parseExplanationResult(result: String?): String {
        return result ?: "Unable to generate explanation"
    }
} 