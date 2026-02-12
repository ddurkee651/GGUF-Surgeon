package com.ggufsurgeon.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ggufsurgeon.core.native.QuantizationType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizeScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedQuantType by remember { mutableStateOf(QuantizationType.Q4_K) }
    var outputFileName by remember { mutableStateOf("") }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_PERMISSION
            )
            selectedFile = File(uri.path ?: return@let)
            outputFileName = "${selectedFile?.nameWithoutExtension}-${selectedQuantType.value}.gguf"
        }
    }
    
    val outputFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val outputFile = File(uri.path ?: return@let)
            selectedFile?.let { file ->
                vm.quantizeModel(
                    inputFile = file,
                    outputFile = outputFile,
                    quantizationType = selectedQuantType
                )
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Model Quantization") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    title = "Model Optimization",
                    description = "Reduce model size through quantization. This operation will create a new GGUF file with lower precision weights.",
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
                            "Input Model",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
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
                                        selectedFile?.name ?: "Select GGUF File",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (selectedFile != null) {
                                        Text(
                                            "Size: ${selectedFile!!.length() / (1024 * 1024)} MB",
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Quantization Format",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        QuantizationSelector(
                            selectedType = selectedQuantType,
                            onTypeSelected = { selectedQuantType = it }
                        )
                        
                        Divider()
                        
                        InfoRow(
                            label = "Expected size reduction",
                            value = when (selectedQuantType) {
                                QuantizationType.Q2_K -> "75-80%"
                                QuantizationType.Q3_K -> "70-75%"
                                QuantizationType.Q4_K -> "60-65%"
                                QuantizationType.Q5_K -> "50-55%"
                                QuantizationType.Q6_K -> "40-45%"
                                QuantizationType.Q8_0 -> "25-30%"
                                else -> "Varies"
                            }
                        )
                        
                        InfoRow(
                            label = "Quality impact",
                            value = when (selectedQuantType) {
                                QuantizationType.Q2_K -> "Significant"
                                QuantizationType.Q3_K -> "Moderate"
                                QuantizationType.Q4_K -> "Minimal"
                                QuantizationType.Q5_K -> "Very minimal"
                                QuantizationType.Q6_K -> "Negligible"
                                QuantizationType.Q8_0 -> "Almost lossless"
                                else -> "Minimal"
                            }
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Output Settings",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = outputFileName,
                            onValueChange = { outputFileName = it },
                            label = { Text("Output filename") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedFile != null
                        )
                        
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val defaultName = "${selectedFile?.nameWithoutExtension}-${selectedQuantType.value}-$timestamp.gguf"
                                outputFilePickerLauncher.launch(defaultName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedFile != null && outputFileName.isNotBlank(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Compress,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Quantization")
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
fun QuantizationSelector(
    selectedType: QuantizationType,
    onTypeSelected: (QuantizationType) -> Unit
) {
    val quantTypes = listOf(
        QuantizationType.Q2_K to "Q2_K - Extreme compression",
        QuantizationType.Q3_K to "Q3_K - High compression",
        QuantizationType.Q4_K to "Q4_K - Balanced",
        QuantizationType.Q5_K to "Q5_K - High quality",
        QuantizationType.Q6_K to "Q6_K - Very high quality",
        QuantizationType.Q8_0 to "Q8_0 - Near lossless"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quantTypes.forEach { (type, description) ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(description) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
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
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OperationProgressCard(
    progress: Int,
    details: String,
    onCancel: () -> Unit
) {
    Card(
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
                "Quantization in Progress",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                details,
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onCancel,
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

@Composable
fun SuccessCard(
    message: String,
    outputPath: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Operation Complete",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Output: ${File(outputPath).name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
