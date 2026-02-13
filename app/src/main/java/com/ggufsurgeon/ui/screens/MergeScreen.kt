package com.ggufsurgeon.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(
    vm: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    var baseModelFile by remember { mutableStateOf<File?>(null) }
    var loraAdapterFile by remember { mutableStateOf<File?>(null) }
    var alpha by remember { mutableStateOf("1.0") }
    var outputFileName by remember { mutableStateOf("") }
    
    val baseModelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_PERMISSION
            )
            baseModelFile = File(uri.path ?: return@let)
            outputFileName = "${baseModelFile?.nameWithoutExtension}-merged.gguf"
        }
    }
    
    val loraAdapterPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_PERMISSION
            )
            loraAdapterFile = File(uri.path ?: return@let)
        }
    }
    
    val outputFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val outputFile = File(uri.path ?: return@let)
            baseModelFile?.let { base ->
                loraAdapterFile?.let { lora ->
                    vm.mergeLora(
                        baseModel = base,
                        loraAdapter = lora,
                        alpha = alpha.toFloatOrNull() ?: 1.0f,
                        outputFile = outputFile
                    )
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LoRA Merge") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (baseModelFile != null && loraAdapterFile != null) {
                        IconButton(
                            onClick = { outputFileLauncher.launch(outputFileName) },
                            enabled = !vm.isOperationInProgress
                        ) {
                            Icon(Icons.Default.Merge, contentDescription = "Merge")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoCard(
                    title = "LoRA Adapter Merge",
                    description = "Combine a base GGUF model with a LoRA adapter to create a merged model. The adapter weights are permanently applied.",
                    icon = Icons.Default.MergeType
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Base Model",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        FileSelectorRow(
                            label = "Select Base GGUF",
                            selectedFile = baseModelFile,
                            onClick = { baseModelPicker.launch(arrayOf("application/octet-stream", "*/*")) }
                        )
                        
                        Divider()
                        
                        Text(
                            "LoRA Adapter",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        FileSelectorRow(
                            label = "Select LoRA Adapter",
                            selectedFile = loraAdapterFile,
                            onClick = { loraAdapterPicker.launch(arrayOf("application/octet-stream", "*/*")) }
                        )
                        
                        Divider()
                        
                        Text(
                            "Merge Parameters",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = alpha,
                            onValueChange = { alpha = it },
                            label = { Text("Alpha (scale factor)") },
                            placeholder = { Text("1.0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = alpha.toFloatOrNull() == null,
                            supportingText = {
                                if (alpha.toFloatOrNull() == null) {
                                    Text("Must be a valid number")
                                }
                            }
                        )
                        
                        Text(
                            "Recommended: 1.0 for most adapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (baseModelFile != null && loraAdapterFile != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Ready to Merge",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Base: ${baseModelFile!!.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Adapter: ${loraAdapterFile!!.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Alpha: $alpha",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            if (vm.isOperationInProgress) {
                item {
                    OperationProgressCard(
                        progress = vm.operationProgress,
                        details = vm.operationDetails,
                        onCancel = { vm.cancelOperation() }
                    )
                }
            }
            
            vm.lastOperationResult?.let { result ->
                if (result is OperationResult.Success) {
                    item {
                        SuccessCard(
                            message = result.details,
                            outputPath = result.outputPath
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileSelectorRow(
    label: String,
    selectedFile: File?,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FileOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    selectedFile?.name ?: label,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (selectedFile != null) {
                    Text(
                        "${selectedFile.length() / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select"
            )
        }
    }
}
