package com.example.smsguard.service

import android.content.Context
import android.util.Log
import com.example.smsguard.data.model.EmbeddingEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for building prompts with similar examples
 */
@Singleton
class PromptBuilder @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PromptBuilder"
        private const val PROMPT_TEMPLATE_FILE = "models/embeddings/prompt.md"
    }
    
    private var promptTemplate: String = ""
    
    /**
     * Initialize the prompt builder by loading the template
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing prompt builder...")
            promptTemplate = loadPromptTemplate()
            Log.d(TAG, "Prompt builder initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize prompt builder", e)
            false
        }
    }
    
    /**
     * Build a prompt with similar examples
     */
    fun buildPrompt(
        smsText: String,
        benignExamples: List<EmbeddingEntry>,
        smishingExamples: List<EmbeddingEntry>
    ): String {
        Log.d(TAG, "Building prompt for SMS: \"${smsText.take(100)}${if (smsText.length > 100) "..." else ""}\"")
        Log.d(TAG, "Input examples - Benign: ${benignExamples.size}, Smishing: ${smishingExamples.size}")
        
        // Use optimized prompt for testing
        val finalPrompt = buildOptimizedPrompt(smsText, benignExamples, smishingExamples)
        
        Log.d(TAG, "Final prompt length: ${finalPrompt.length} characters")
        Log.d(TAG, "Prompt preview (first 500 chars): ${finalPrompt.take(500)}...")
        
        return finalPrompt
    }
    
    /**
     * Build optimized prompt (shorter version for testing)
     */
    private fun buildOptimizedPrompt(
        smsText: String,
        benignExamples: List<EmbeddingEntry>,
        smishingExamples: List<EmbeddingEntry>
    ): String {
        // Take only 1 example of each type to reduce prompt size
        val singleBenignExample = benignExamples.take(1)
        val singleSmishingExample = smishingExamples.take(1)
        
        val examplesBlock = buildExamplesBlock(singleBenignExample, singleSmishingExample)
        
        return """
        ## GOAL ##
        Classify SMS as 'smishing' or 'benign' based on intent to deceive.

        ## ROLE ##
        SMS cybersecurity analyst detecting fraudulent attempts to gain sensitive information or induce clicks on malicious links.

        ## DEFINITIONS ##
        - 'Smishing': Fraudulent SMS aiming to deceive (malicious links, credential requests, impersonation)
        - 'Benign': Legitimate SMS without fraudulent intent

        ## EXAMPLES ##
        $examplesBlock

        ## INPUT MESSAGE ##
        "$smsText"

        ## OUTPUT FORMAT ##
        ## Classification: smishing or benign
        ## Explanation: [Key indicators - max 25 words]
        ## Counterfactual: [Minimal change to flip intent]
        """.trimIndent()
    }
    
    /**
     * Build original prompt (commented out for future use)
     */
    private fun buildOriginalPrompt(
        smsText: String,
        benignExamples: List<EmbeddingEntry>,
        smishingExamples: List<EmbeddingEntry>
    ): String {
        val examplesBlock = buildExamplesBlock(benignExamples, smishingExamples)
        
        val finalPrompt = promptTemplate
            .replace("{example_block}", examplesBlock)
            .replace("{sms_text}", smsText)
        
        return finalPrompt
    }
    
    /**
     * Build the examples block for the prompt
     */
    private fun buildExamplesBlock(
        benignExamples: List<EmbeddingEntry>,
        smishingExamples: List<EmbeddingEntry>
    ): String {
        val benignExamplesText = benignExamples.joinToString("\n") { example ->
            "benign: \"${example.text}\""
        }
        
        val smishingExamplesText = smishingExamples.joinToString("\n") { example ->
            "smishing: \"${example.text}\""
        }
        
        return buildString {
            if (benignExamples.isNotEmpty()) {
                appendLine(benignExamplesText)
            }
            if (smishingExamples.isNotEmpty()) {
                if (benignExamples.isNotEmpty()) {
                    appendLine()
                }
                appendLine(smishingExamplesText)
            }
        }
    }
    
    /**
     * Load the prompt template from assets
     */
    private fun loadPromptTemplate(): String {
        return try {
            context.assets.open(PROMPT_TEMPLATE_FILE).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading prompt template", e)
            // Fallback template
            """
            ## GOAL ##
            Classify SMS messages as 'smishing' or 'benign' based solely on **intent to deceive or defraud**, not on emotion, tone, or urgency.

            ## ROLE ##
            You are an SMS cybersecurity analyst specializing in detecting benign and SMS phishing with a focus on verifiable fraudulent attempts to gain sensitive information (e.g, personal identity information,  passwords, credentials, financial details, account access) or to induce clicks on demonstrably malicious links or call a number leading to fraud or compromise.

            ## DEFINITIONS ##
            - 'Smishing': A fraudulent SMS aiming to deceive the recipient into doing harm to themselves (e.g., clicking a malicious link, sharing financial and identity credentials, sending money).

            - 'Benign': a legitimate and harmless SMS that does not explicitly seek to defraud and phish for sensitive information. This includes casual, personal, informal, or conversational messages, even if they contain slang, emotional language, express urgency, or are socially inappropriate, as long as they lack a direct, verifiable fraudulent intent related to financial or personal identity data compromise.

            ## GUIDELINES ##
            The purpose of the message is paramount to classify the message: Is it trying to defraud or steal sensitive information/money, or is it a normal, albeit informal or urgent, communication?
            1. Classify only if there is a clear **malicious objective** like phishing, impersonation, or trickery.
            2. Do **not** classify based on:
               - Flirtation or emotional tone
               - Urgency or imperative verbs alone
               - Mentions of money, sex, or violence if not tied to deception
               - Personal or sensitive questions *without* an obvious fraud tactic

            ## COUNTERFACTUAL RULE ##
            You must provide a counterfactual, counterfactual must be the **minimum change** that removes or adds **intent to deceive**. Not tone. Not formatting. Not urgency.

            ## EXAMPLES ##
            {example_block}

            ## INPUT MESSAGE ##
            "{sms_text}"

            ## OUTPUT FORMAT ##
            ## Classification: smishing or benign
            ## Explanation: Highlight only the **intent-driven clues** (e.g., impersonation, deceptive link, fraudulent ask). Avoid tone-based reasoning— no more than 35 words.
            ## Counterfactual: [Minimal, plausible change that flips **intent** – e.g., remove phishing link, remove impersonation, add credential request ]
            """.trimIndent()
        }
    }
} 