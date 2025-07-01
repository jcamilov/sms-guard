package com.example.smsguard.di

import com.example.smsguard.data.repository.SMSRepository
import com.example.smsguard.data.repository.SMSRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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