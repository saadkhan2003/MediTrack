package com.meditrack.app.presentation.screens.addmedicine

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meditrack.app.domain.model.Frequency
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val cardColors = listOf(
    0xFF1954A3.toInt(),
    0xFF22875A.toInt(),
    0xFFE65100.toInt(),
    0xFF7B1FA2.toInt(),
    0xFFC62828.toInt(),
    0xFF00838F.toInt(),
    0xFF4E342E.toInt(),
    0xFF37474F.toInt()
)

private fun formatToAmPm(time24: String): String {
    return runCatching {
        val input = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        LocalTime.parse(time24, input).format(output)
    }.getOrElse { time24 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddMedicineViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            when (result) {
                is SaveResult.Success -> onNavigateBack()
                is SaveResult.Error -> snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (formState.isEditing) "Edit Medicine" else "Add Medicine",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Go back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Section: Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Medicine Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = { Text("Medicine Name") },
                placeholder = { Text("e.g., Metformin 500mg") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Medicine name input" },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            OutlinedTextField(
                value = formState.dosage,
                onValueChange = { viewModel.onDosageChanged(it) },
                label = { Text("Dosage") },
                placeholder = { Text("e.g., 1 tablet") },
                isError = formState.dosageError != null,
                supportingText = formState.dosageError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Dosage input" },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            // Color Picker inside Details section
            Text("Label Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cardColors.forEach { color ->
                    val isSelected = formState.color == color
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (isSelected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { viewModel.onColorSelected(color) }
                            .semantics { contentDescription = "Color option" },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            } // End Details Section

            // Section: Schedule
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

            // Frequency
            Text("Frequency", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = showFrequencyDropdown,
                onExpandedChange = { showFrequencyDropdown = it }
            ) {
                OutlinedTextField(
                    value = formState.frequency.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFrequencyDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .semantics { contentDescription = "Frequency selector" },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                ExposedDropdownMenu(
                    expanded = showFrequencyDropdown,
                    onDismissRequest = { showFrequencyDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    Frequency.entries.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.displayName, fontWeight = if (formState.frequency == freq) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.onFrequencyChanged(freq)
                                showFrequencyDropdown = false
                            }
                        )
                    }
                }
            }

            // Scheduled Times
            Text("Scheduled Times", style = MaterialTheme.typography.labelLarge)
            if (formState.timesError != null) {
                Text(
                    formState.timesError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            formState.scheduledTimes.forEachIndexed { index, time ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = formatToAmPm(time),
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Time slot ${index + 1}" }
                        )
                        // Transparent overlay to capture clicks (OutlinedTextField swallows touches)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    val parts = time.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(
                                        context, { _, h, m ->
                                            viewModel.onTimeUpdated(index, "%02d:%02d".format(h, m))
                                        }, hour, minute, false
                                    ).show()
                                }
                        )
                    }
                    if (formState.scheduledTimes.size > 1) {
                        IconButton(
                            onClick = { viewModel.onTimeRemoved(index) },
                            modifier = Modifier.semantics { contentDescription = "Remove time slot" }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }

            if (formState.frequency == Frequency.CUSTOM && formState.scheduledTimes.size < 6) {
                TextButton(
                    onClick = {
                        TimePickerDialog(
                            context, { _, h, m ->
                                val millis = java.time.LocalDate.now()
                                    .atTime(h, m)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                viewModel.onTimeAdded(millis)
                            }, 12, 0, false
                        ).show()
                    },
                    modifier = Modifier.semantics { contentDescription = "Add time slot" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Time")
                }
            }

            // Start Date
            Box {
                OutlinedTextField(
                    value = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(formState.startDate)),
                    onValueChange = {},
                    label = { Text("Start Date") },
                    readOnly = true,
                    
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick start date")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Start date" },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showStartDatePicker = true }
                )
            }

            // End Date toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Has End Date", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = formState.hasEndDate,
                    onCheckedChange = { viewModel.onHasEndDateChanged(it) },
                    modifier = Modifier.semantics { contentDescription = "Toggle end date" }
                )
            }

            if (formState.hasEndDate) {
                Box {
                    OutlinedTextField(
                        value = formState.endDate?.let {
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(it))
                        } ?: "",
                        onValueChange = {},
                        label = { Text("End Date") },
                        readOnly = true,
                        
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick end date")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "End date" }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showEndDatePicker = true }
                    )
                }
            }

            } // End Schedule Section

            // Section: Inventory & Notes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Inventory & Extra",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

            // Total Stock
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = formState.totalStock.toString(),
                    onValueChange = { viewModel.onStockChanged(it.toIntOrNull() ?: 0) },
                    label = { Text("Total Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Total stock input" },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Refill Threshold
                OutlinedTextField(
                    value = formState.refillThreshold.toString(),
                    onValueChange = { viewModel.onRefillThresholdChanged(it.toIntOrNull() ?: 5) },
                    label = { Text("Refill Alert At") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Refill threshold input" },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label = { Text("Notes") },
                placeholder = { Text("Optional doctor's notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .semantics { contentDescription = "Notes input" },
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Save Button
            Button(
                onClick = { viewModel.saveMedicine() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp)
                    .semantics { contentDescription = "Save medicine" },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Save Medicine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            } // End Inventory Section
        }
    }

    // Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onStartDateChanged(it) }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.endDate ?: formState.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onEndDateChanged(it) }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddMedicineScreenPreview() {
    AddMedicineScreen(onNavigateBack = {})
}
