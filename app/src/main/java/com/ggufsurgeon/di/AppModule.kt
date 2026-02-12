package com.ggufsurgeon.di

import android.content.Context
import androidx.room.Room
import com.ggufsurgeon.core.GgufBinaryEditor
import com.ggufsurgeon.core.GgufValidator
import com.ggufsurgeon.core.native.NativeGgufMerger
import com.ggufsurgeon.core.native.NativeGgufQuantizer
import com.ggufsurgeon.data.ModelRepository
import com.ggufsurgeon.data.db.OperationDatabase
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
    
    @Provides
    @Singleton
    fun provideNativeGgufMerger(): NativeGgufMerger = NativeGgufMerger()
    
    @Provides
    @Singleton
    fun provideNativeGgufQuantizer(): NativeGgufQuantizer = NativeGgufQuantizer()
    
    @Provides
    @Singleton
    fun provideModelRepository(
        validator: GgufValidator,
        binaryEditor: GgufBinaryEditor,
        merger: NativeGgufMerger,
        quantizer: NativeGgufQuantizer
    ): ModelRepository = ModelRepository(
        validator = validator,
        binaryEditor = binaryEditor,
        nativeMerger = merger,
        nativeQuantizer = quantizer
    )
    
    @Provides
    @Singleton
    fun provideOperationDatabase(
        @ApplicationContext context: Context
    ): OperationDatabase = Room.databaseBuilder(
        context,
        OperationDatabase::class.java,
        "operation_database"
    ).build()
}