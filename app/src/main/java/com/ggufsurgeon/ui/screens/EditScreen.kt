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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    vm: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val model = vm.currentModel
    
    var contextLength by remember { mutableStateOf(model?.contextLength?.toString() ?: "") }
    var ropeScaling by remember { mutableStateOf(model?.ropeScaling?.toString() ?: "") }
    var modelName by remember { mutableStateOf(model?.name ?: "") }
    var outputFileName by remember { mutableStateOf("") }
    
    val outputFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val outputFile = File(uri.path ?: return@let)
            val updates = mutableMapOf<String, String>()
            
            if (contextLength.isNotBlank()) {
                updates["llama.context_length"] = contextLength
            }
            if (ropeScaling.isNotBlank()) {
                updates["rope.scaling.linear"] = ropeScaling
            }
            if (modelName.isNotBlank()) {
                updates["general.name"] = modelName
            }
            
            vm.currentModel?.filePath?.let { path ->
                vm.editMetadata(
                    originalFile = File(path),
                    outputFile = outputFile,
                    updates = updates
                )
            }
        }
    }
    
    LaunchedEffect(model) {
        model?.let {
            contextLength = it.contextLength.toString()
            ropeScaling = it.ropeScaling.toString()
            modelName = it.name
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            outputFileName = "${it.name}_edited_$timestamp.gguf"
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Metadata Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (model != null) {
                        IconButton(
                            onClick = {
                                outputFileLauncher.launch(outputFileName)
                            },
                            enabled = !vm.isOperationInProgress
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
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
            if (model == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "No model loaded. Please open a model first.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                return@LazyColumn
            }
            
            item {
                InfoCard(
                    title = "Safe Metadata Editing",
                    description = "These changes only modify metadata, not tensor data. The original file remains untouched.",
                    icon = Icons.Default.Info
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
                            "Current Model: ${model.name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Divider()
                        
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            label = { Text("Model Name") },
                            placeholder = { Text(model.name) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = contextLength,
                            onValueChange = { contextLength = it },
                            label = { Text("Context Length") },
                            placeholder = { Text(model.contextLength.toString()) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = contextLength.isNotBlank() && contextLength.toIntOrNull() == null,
                            supportingText = {
                                if (contextLength.isNotBlank() && contextLength.toIntOrNull() == null) {
                                    Text("Must be a valid number")
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = ropeScaling,
                            onValueChange = { ropeScaling = it },
                            label = { Text("RoPE Scaling Factor") },
                            placeholder = { Text(model.ropeScaling.toString()) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = ropeScaling.isNotBlank() && ropeScaling.toFloatOrNull() == null,
                            supportingText = {
                                if (ropeScaling.isNotBlank() && ropeScaling.toFloatOrNull() == null) {
                                    Text("Must be a valid number")
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "⚠️ Changing these values affects model behavior. Only modify if you understand the implications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.warning
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Original Values",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        PropertyRow("Name", model.name)
                        PropertyRow("Context Length", "${model.contextLength}")
                        PropertyRow("RoPE Scaling", "${model.ropeScaling}")
                        PropertyRow("Architecture", model.architecture)
                        PropertyRow("Quantization", model.quantization)
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
fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
