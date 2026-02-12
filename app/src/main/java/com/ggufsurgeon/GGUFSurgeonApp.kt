package com.ggufsurgeon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GGUFSurgeonApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize native libraries
        try {
            System.loadLibrary("gguf_native")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }
}
