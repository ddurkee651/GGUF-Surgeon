package com.ggufsurgeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GGUFSurgeonApp()
        }
        
        // Handle intent if opened from a GGUF file
        intent?.data?.let { uri ->
            // Model will be loaded via ViewModel
        }
    }
}
