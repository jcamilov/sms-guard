package com.example.smsguard.di

import android.content.Context
import com.example.smsguard.service.AIClassifier
import com.example.smsguard.service.Gemma3nClassifier
import com.example.smsguard.service.EmbeddingService
import com.example.smsguard.service.SemanticSearchService
import com.example.smsguard.service.PromptBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    @Provides
    @Singleton
    fun provideAIClassifier(
        @ApplicationContext context: Context,
        embeddingService: EmbeddingService,
        semanticSearchService: SemanticSearchService,
        promptBuilder: PromptBuilder
    ): AIClassifier {
        return Gemma3nClassifier(context, embeddingService, semanticSearchService, promptBuilder)
    }
    
    @Provides
    @Singleton
    fun provideEmbeddingService(
        @ApplicationContext context: Context
    ): EmbeddingService {
        return EmbeddingService(context)
    }
    
    @Provides
    @Singleton
    fun provideSemanticSearchService(
        @ApplicationContext context: Context
    ): SemanticSearchService {
        return SemanticSearchService(context)
    }
    
    @Provides
    @Singleton
    fun providePromptBuilder(
        @ApplicationContext context: Context
    ): PromptBuilder {
        return PromptBuilder(context)
    }
} 