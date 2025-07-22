package com.example.smsguard.di

import android.content.Context
import com.example.smsguard.data.repository.SMSRepository
import com.example.smsguard.data.repository.SMSRepositoryImpl
import com.example.smsguard.service.MemoryOptimizationService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindSMSRepository(
        smsRepositoryImpl: SMSRepositoryImpl
    ): SMSRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideMemoryOptimizationService(
        @ApplicationContext context: Context
    ): MemoryOptimizationService {
        return MemoryOptimizationService(context)
    }
} 