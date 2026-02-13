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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showFilePicker by remember { mutableStateOf(false) }
    var showOutputDialog by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_PERMISSION
            )
            val file = File(uri.path ?: return@let)
            vm.inspectModel(file)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Open Model")
            }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "GGUF Model Surgeon",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Direct GGUF file control on your device",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            if (vm.currentModel != null) {
                item {
                    CurrentModelCard(
                        model = vm.currentModel!!,
                        onInspect = { onNavigate("viewer") }
                    )
                }
            }
            
            if (vm.isOperationInProgress) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Operation in Progress",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                vm.operationDetails,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(
                                progress = vm.operationProgress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { vm.cancelOperation() },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
            
            item {
                Text(
                    "Operations",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            item {
                OperationCard(
                    title = "Model Viewer",
                    description = "Inspect model architecture, metadata, and tensors",
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate("viewer") },
                    enabled = vm.currentModel != null
                )
            }
            
            item {
                OperationCard(
                    title = "Metadata Editor",
                    description = "Modify context length, RoPE scaling, and model name",
                    icon = Icons.Default.Edit,
                    onClick = { onNavigate("edit") },
                    enabled = vm.currentModel != null
                )
            }
            
            item {
                OperationCard(
                    title = "LoRA Merge",
                    description = "Combine base model with LoRA adapter",
                    icon = Icons.Default.Merge,
                    onClick = { onNavigate("merge") },
                    enabled = true
                )
            }
            
            item {
                OperationCard(
                    title = "Quantization",
                    description = "Reduce model size with different quantization formats",
                    icon = Icons.Default.Compress,
                    onClick = { onNavigate("optimize") },
                    enabled = vm.currentModel != null
                )
            }
            
            item {
                OperationCard(
                    title = "Export Model",
                    description = "Save edited model to a new file",
                    icon = Icons.Default.Save,
                    onClick = { showOutputDialog = true },
                    enabled = vm.currentModel != null
                )
            }
            
            item {
                StatusCard(
                    statusMessage = vm.statusMessage,
                    modelSize = vm.currentModel?.fileSize
                )
            }
        }
    }
}

@Composable
fun CurrentModelCard(model: ModelFile, onInspect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onInspect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Current Model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        model.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(model.architecture)
                }
            }
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModelStat(label = "Context", value = "${model.contextLength}")
                ModelStat(label = "Quant", value = model.quantization)
                ModelStat(label = "Tensors", value = "${model.tensorCount}")
            }
            
            if (model.fileSize > 0) {
                Text(
                    "Size: ${model.fileSize / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModelStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun OperationCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (enabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun StatusCard(statusMessage: String, modelSize: Long?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Status",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (modelSize != null) {
                AssistChip(
                    onClick = {},
                    label = { Text("Model loaded") }
                )
            }
        }
    }
}
