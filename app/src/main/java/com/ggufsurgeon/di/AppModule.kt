package com.ggufsurgeon.di

import android.content.Context
import com.ggufsurgeon.core.GgufBinaryEditor
import com.ggufsurgeon.core.GgufValidator
import com.ggufsurgeon.core.python.PythonGgufBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGgufValidator(): GgufValidator = GgufValidator()
    
    @Provides
    @Singleton
    fun provideGgufBinaryEditor(): GgufBinaryEditor = GgufBinaryEditor()
    
    // ✅ ADD THIS - Python bridge provider
    @Provides
    @Singleton
    fun providePythonGgufBridge(@ApplicationContext context: Context): PythonGgufBridge = 
        PythonGgufBridge(context)
}
