package com.mid.varagh.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mid.varagh.core.designsystem.component.ReadingThemeSwatch
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.errorMessageRes
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.reading.ReadingPalette
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.designsystem.theme.supportsDynamicTheming
import com.mid.varagh.core.model.AppLanguage
import com.mid.varagh.core.model.DarkThemeConfig
import com.mid.varagh.core.model.ReadingMode
import com.mid.varagh.core.model.ReadingTheme
import com.mid.varagh.core.model.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SettingsScreenRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val text = when (event) {
                is SettingsEvent.Exported -> context.getString(R.string.settings_backup_exported, event.summary.books)
                is SettingsEvent.Imported -> context.getString(
                    R.string.settings_backup_imported,
                    event.summary.books,
                    event.summary.sessions,
                    event.summary.bookmarks,
                )
                is SettingsEvent.Failed -> context.getString(errorMessageRes(event.error))
            }
            snackbar.showSnackbar(text)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        uri?.let { viewModel.export(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.import(it.toString()) }
    }
    SettingsScreen(
        preferences = preferences,
        busy = busy,
        snackbarHostState = snackbar,
        appVersion = remember(context) {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        },
        onUpdate = viewModel::update,
        onExport = { exportLauncher.launch(backupFileName()) },
        onImport = { importLauncher.launch(arrayOf(BACKUP_MIME, "text/plain", "application/octet-stream")) },
    )
}

@Composable
internal fun SettingsScreen(
    preferences: UserPreferences?,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    appVersion: String = "",
    onUpdate: ((UserPreferences) -> UserPreferences) -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { VaraghTopAppBar(title = stringResource(R.string.settings_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (preferences == null) {
            VaraghLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            ReadingCard(preferences, onUpdate)
            AppearanceCard(preferences, onUpdate)
            LanguageCard(preferences.language) { lang -> onUpdate { it.copy(language = lang) } }
            BackupCard(busy = busy, onExport = onExport, onImport = onImport)
            VaraghCard(title = stringResource(R.string.settings_section_about), contentPadding = PaddingValues(vertical = VaraghSpacing.XSmall)) {
                SettingsRow(
                    icon = VaraghIcons.Info,
                    title = stringResource(R.string.settings_version),
                    subtitle = appVersion,
                )
                SettingsRow(
                    icon = VaraghIcons.Offline,
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_summary),
                )
            }
        }
    }
}

@Composable
private fun ReadingCard(preferences: UserPreferences, onUpdate: ((UserPreferences) -> UserPreferences) -> Unit) {
    VaraghCard(title = stringResource(R.string.settings_section_reading)) {
        Text(stringResource(R.string.settings_default_theme), style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = VaraghSpacing.Small),
            horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small),
        ) {
            ReadingTheme.entries.forEach { theme ->
                ReadingThemeSwatch(
                    theme = theme,
                    palette = ReadingPalette.forTheme(theme, preferences.customReadingColors),
                    selected = theme == preferences.readingTheme,
                    onClick = { onUpdate { it.copy(readingTheme = theme) } },
                )
            }
        }
        Text(
            stringResource(R.string.settings_reading_mode),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = VaraghSpacing.Small, bottom = VaraghSpacing.Small),
        )
        val modes = listOf(ReadingMode.VERTICAL_SCROLL to R.string.settings_mode_vertical, ReadingMode.HORIZONTAL_PAGED to R.string.settings_mode_paged)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = preferences.readingMode == mode,
                    onClick = { onUpdate { it.copy(readingMode = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(stringResource(label)) }
            }
        }
        SwitchRow(
            title = stringResource(R.string.settings_rtl_paging),
            subtitle = stringResource(R.string.settings_rtl_paging_summary),
            checked = preferences.rightToLeftPaging,
            onChange = { v -> onUpdate { it.copy(rightToLeftPaging = v) } },
        )
        SwitchRow(
            title = stringResource(R.string.settings_keep_screen_on),
            subtitle = null,
            checked = preferences.keepScreenOn,
            onChange = { v -> onUpdate { it.copy(keepScreenOn = v) } },
            modifier = Modifier.testTag("keep_screen_on"),
        )
    }
}

@Composable
private fun AppearanceCard(preferences: UserPreferences, onUpdate: ((UserPreferences) -> UserPreferences) -> Unit) {
    VaraghCard(title = stringResource(R.string.settings_section_appearance)) {
        val options = listOf(
            DarkThemeConfig.FOLLOW_SYSTEM to R.string.settings_theme_system,
            DarkThemeConfig.LIGHT to R.string.settings_theme_light,
            DarkThemeConfig.DARK to R.string.settings_theme_dark,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (config, label) ->
                SegmentedButton(
                    selected = preferences.darkThemeConfig == config,
                    onClick = { onUpdate { it.copy(darkThemeConfig = config) } },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    modifier = Modifier.testTag("theme_${config.name}"),
                ) { Text(stringResource(label)) }
            }
        }
        if (supportsDynamicTheming()) {
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_summary),
                checked = preferences.useDynamicColor,
                onChange = { v -> onUpdate { it.copy(useDynamicColor = v) } },
            )
        }
    }
}

@Composable
private fun LanguageCard(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    VaraghCard(title = stringResource(R.string.settings_section_language), contentPadding = PaddingValues(vertical = VaraghSpacing.XSmall)) {
        Column(Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { language ->
                val label = when (language) {
                    AppLanguage.PERSIAN -> "فارسی"
                    AppLanguage.ENGLISH -> "English"
                }
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { RadioButton(selected = current == language, onClick = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .selectable(selected = current == language, role = Role.RadioButton) { onSelect(language) }
                        .testTag("language_${language.tag}"),
                )
            }
        }
    }
}

@Composable
private fun BackupCard(busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    VaraghCard(title = stringResource(R.string.settings_section_backup), contentPadding = PaddingValues(vertical = VaraghSpacing.XSmall)) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.settings_backup_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
        )
        SettingsRow(
            icon = VaraghIcons.Export,
            title = stringResource(R.string.settings_backup_export),
            subtitle = null,
            onClick = if (busy) null else onExport,
            modifier = Modifier.testTag("backup_export"),
        )
        SettingsRow(
            icon = VaraghIcons.ImportBackup,
            title = stringResource(R.string.settings_backup_import),
            subtitle = null,
            onClick = if (busy) null else onImport,
            modifier = Modifier.testTag("backup_import"),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = onClick?.let { { Icon(VaraghIcons.Chevron, contentDescription = null) } },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = VaraghSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private const val BACKUP_MIME = "application/json"

private fun backupFileName(): String =
    "varagh-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"
