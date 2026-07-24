package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.data.prefs.*
import com.example.golfdistancetracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val currentUnit by viewModel.distanceUnit.collectAsState()
    val currentTheme by viewModel.themePreference.collectAsState()
    val userHandicap by viewModel.handicap.collectAsState()
    val gpsSource by viewModel.gpsSource.collectAsState()
    val autoImpact by viewModel.autoImpact.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile
            SettingsGroup(title = "Profile") {
                OutlinedTextField(
                    value = userHandicap,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            viewModel.updateHandicap(it)
                        }
                    },
                    label = { Text("My Handicap (0-54)") },
                    placeholder = { Text("e.g. 15.2") },
                    isError = userHandicap.isNotEmpty() && (userHandicap.toDoubleOrNull() ?: -1.0) !in 0.0..54.0,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }

            // Wear OS Settings
            SettingsGroup(title = "Watch Settings (Galaxy Watch)") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto Shot Detection", style = MaterialTheme.typography.bodyLarge)
                        Text("Watch detects swings automatically", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(checked = autoImpact, onCheckedChange = { viewModel.updateAutoImpact(it) })
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                Text("Location Source", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                UnitOption(
                    label = "Phone GPS (Best Battery)",
                    selected = gpsSource == "Phone",
                    onClick = { viewModel.updateGpsSource("Phone") }
                )
                UnitOption(
                    label = "Watch GPS (Standalone)",
                    selected = gpsSource == "Watch",
                    onClick = { viewModel.updateGpsSource("Watch") }
                )
            }

            // Units
            SettingsGroup(title = stringResource(R.string.settings_units)) {
                UnitOption(
                    label = stringResource(R.string.settings_meters),
                    selected = currentUnit == DistanceUnit.METERS,
                    onClick = { viewModel.updateUnit(DistanceUnit.METERS) }
                )
                UnitOption(
                    label = stringResource(R.string.settings_yards),
                    selected = currentUnit == DistanceUnit.YARDS,
                    onClick = { viewModel.updateUnit(DistanceUnit.YARDS) }
                )
            }

            // Theme
            SettingsGroup(title = stringResource(R.string.settings_theme)) {
                UnitOption(
                    label = stringResource(R.string.settings_theme_system),
                    selected = currentTheme == ThemePreference.SYSTEM,
                    onClick = { viewModel.updateTheme(ThemePreference.SYSTEM) }
                )
                UnitOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = currentTheme == ThemePreference.LIGHT,
                    onClick = { viewModel.updateTheme(ThemePreference.LIGHT) }
                )
                UnitOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = currentTheme == ThemePreference.DARK,
                    onClick = { viewModel.updateTheme(ThemePreference.DARK) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.settings_build_date), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.settings_device), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun UnitOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = onClick)
    }
}
