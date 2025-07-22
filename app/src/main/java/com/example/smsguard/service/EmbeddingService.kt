package com.example.smsguard.service

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating embeddings using TensorFlow Lite
 */
@Singleton
class EmbeddingService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "EmbeddingService"
        private const val MODEL_PATH = "models/embeddings/sms_embedding_model.tflite"
        private const val EMBEDDING_DIMENSION = 384
        private const val MAX_SEQUENCE_LENGTH = 128
    }
    
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    
    /**
     * Initialize the embedding model
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing embedding model...")
            
            val modelFile = copyModelFromAssets()
            val options = Interpreter.Options().apply {
                // Set number of threads to reduce memory usage
                setNumThreads(2)
                // Enable GPU delegation if available
                setUseNNAPI(true)
            }
            interpreter = Interpreter(modelFile, options)
            isInitialized = true
            
            Log.d(TAG, "Embedding model initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize embedding model", e)
            isInitialized = false
            false
        }
    }
    
    /**
     * Generate embedding for a given text
     */
    fun generateEmbedding(text: String): List<Float>? {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "Model not initialized")
            return null
        }
        
        return try {
            Log.d(TAG, "Generating embedding for text (${text.length} chars): \"${text.take(100)}${if (text.length > 100) "..." else ""}\"")
            
            // Get input and output tensor details
            val inputTensor = interpreter?.getInputTensor(0)
            val outputTensor = interpreter?.getOutputTensor(0)
            
            if (inputTensor == null || outputTensor == null) {
                Log.e(TAG, "Input or output tensor is null")
                return null
            }
            
            Log.d(TAG, "Tensor shapes - Input: ${inputTensor.shape().contentToString()}, Output: ${outputTensor.shape().contentToString()}")
            
            // Create input buffer with correct size
            val inputShape = inputTensor.shape()
            val inputBuffer = TensorBuffer.createFixedSize(inputShape, inputTensor.dataType())
            
            // Fill input buffer with raw text data
            val inputArray = inputBuffer.floatArray
            text.forEachIndexed { index, char ->
                if (index < inputArray.size) {
                    inputArray[index] = char.code.toFloat()
                }
            }
            
            Log.d(TAG, "Input buffer prepared with ${inputArray.size} elements")
            
            // Create output buffer with correct size
            val outputShape = outputTensor.shape()
            val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputTensor.dataType())
            
            Log.d(TAG, "Running TensorFlow Lite inference...")
            // Run inference
            interpreter?.run(inputBuffer.buffer, outputBuffer.buffer)
            
            // Convert output to List<Float>
            val embedding = outputBuffer.floatArray.toList()
            Log.d(TAG, "Embedding generated successfully (${embedding.size} dimensions)")
            
            embedding
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            null
        }
    }
    
    /**
     * Check if the model is ready
     */
    fun isModelReady(): Boolean = isInitialized && interpreter != null
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            interpreter?.close()
            interpreter = null
            isInitialized = false
            Log.d(TAG, "Embedding service cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up embedding service", e)
        }
    }
    
    /**
     * Copy model from assets to internal storage
     */
    private fun copyModelFromAssets(): java.io.File {
        val modelDir = java.io.File(context.filesDir, "models/embeddings")
        modelDir.mkdirs()
        
        val modelFile = java.io.File(modelDir, "sms_embedding_model.tflite")
        
        if (!modelFile.exists()) {
            Log.d(TAG, "Copying embedding model from assets...")
            try {
                context.assets.open(MODEL_PATH).use { input ->
                    java.io.FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Embedding model copied successfully to: ${modelFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy embedding model from assets", e)
                throw e
            }
        } else {
            Log.d(TAG, "Embedding model already exists at: ${modelFile.absolutePath}")
        }
        
        return modelFile
    }
} 