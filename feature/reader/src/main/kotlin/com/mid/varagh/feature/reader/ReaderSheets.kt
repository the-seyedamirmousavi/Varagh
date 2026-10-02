package com.mid.varagh.feature.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.component.ReadingThemeSwatch
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.reading.ReadingPalette
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.ReadingMode
import com.mid.varagh.core.model.ReadingTheme
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.core.designsystem.R as DesignR

private val CustomBackgrounds = listOf(
    0xFFFFFFFF, 0xFFF4ECD8, 0xFFE8F0E3, 0xFFE3EAF2, 0xFFDCDCD7, 0xFF3A3A3A, 0xFF1F2023, 0xFF0D1B2A, 0xFF000000,
).map { it.toInt() }

private val CustomTexts = listOf(
    0xFF000000, 0xFF1F2A1C, 0xFF3B2F20, 0xFF26323F, 0xFF2A2A2A, 0xFFF4F4F4, 0xFFD6D6D0, 0xFFE8D9B5, 0xFFA88C66,
).map { it.toInt() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    preferences: UserPreferences,
    onChange: ((UserPreferences) -> UserPreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("reader_settings")) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaraghSpacing.ScreenGutter)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Large),
        ) {
            SectionTitle(stringResource(R.string.reader_page_color))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small),
            ) {
                ReadingTheme.entries.forEach { theme ->
                    ReadingThemeSwatch(
                        theme = theme,
                        palette = ReadingPalette.forTheme(theme, preferences.customReadingColors),
                        selected = theme == preferences.readingTheme,
                        onClick = { onChange { it.copy(readingTheme = theme) } },
                    )
                }
            }
            if (preferences.readingTheme == ReadingTheme.CUSTOM) {
                ColorRow(
                    label = stringResource(R.string.reader_custom_background),
                    colors = CustomBackgrounds,
                    selected = preferences.customReadingColors.backgroundArgb,
                    onSelect = { argb ->
                        onChange { it.copy(customReadingColors = it.customReadingColors.copy(backgroundArgb = argb)) }
                    },
                )
                ColorRow(
                    label = stringResource(R.string.reader_custom_text),
                    colors = CustomTexts,
                    selected = preferences.customReadingColors.textArgb,
                    onSelect = { argb ->
                        onChange { it.copy(customReadingColors = it.customReadingColors.copy(textArgb = argb)) }
                    },
                )
            }
            Text(
                stringResource(R.string.reader_images_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle(stringResource(R.string.reader_layout))
            val modes = listOf(ReadingMode.VERTICAL_SCROLL to R.string.reader_mode_vertical, ReadingMode.HORIZONTAL_PAGED to R.string.reader_mode_paged)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = preferences.readingMode == mode,
                        onClick = { onChange { it.copy(readingMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) { Text(stringResource(label)) }
                }
            }
            if (preferences.readingMode == ReadingMode.HORIZONTAL_PAGED) {
                SwitchRow(
                    title = stringResource(R.string.reader_rtl_paging),
                    subtitle = stringResource(R.string.reader_rtl_paging_summary),
                    checked = preferences.rightToLeftPaging,
                    onCheckedChange = { checked -> onChange { it.copy(rightToLeftPaging = checked) } },
                )
            }

            SectionTitle(stringResource(R.string.reader_comfort))
            SliderRow(
                icon = { Icon(VaraghIcons.WarmLight, contentDescription = null) },
                label = stringResource(R.string.reader_warm_light),
                value = preferences.warmFilter,
                valueRange = 0f..1f,
                onValueChange = { v -> onChange { it.copy(warmFilter = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.reader_system_brightness),
                subtitle = null,
                checked = preferences.readerBrightness == null,
                onCheckedChange = { system -> onChange { it.copy(readerBrightness = if (system) null else DEFAULT_BRIGHTNESS) } },
            )
            preferences.readerBrightness?.let { brightness ->
                SliderRow(
                    icon = { Icon(VaraghIcons.Brightness, contentDescription = null) },
                    label = stringResource(R.string.reader_brightness),
                    value = brightness,
                    valueRange = MIN_BRIGHTNESS..1f,
                    onValueChange = { v -> onChange { it.copy(readerBrightness = v) } },
                )
            }
            SwitchRow(
                title = stringResource(R.string.reader_keep_screen_on),
                subtitle = null,
                checked = preferences.keepScreenOn,
                onCheckedChange = { on -> onChange { it.copy(keepScreenOn = on) } },
            )
            Box(Modifier.size(VaraghSpacing.Large))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ColorRow(label: String, colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Small)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small)) {
            colors.forEach { argb ->
                val isSelected = argb == selected
                val hex = "#%06X".format(argb and 0xFFFFFF)
                Box(
                    Modifier
                        .size(40.dp)
                        .background(Color(argb), CircleShape)
                        .border(
                            BorderStroke(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            ),
                            CircleShape,
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(argb) }
                        .semantics { contentDescription = hex },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = checked, role = Role.Switch) { onCheckedChange(!checked) },
    )
}

@Composable
private fun SliderRow(
    icon: @Composable () -> Unit,
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium)) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = local,
                onValueChange = {
                    local = it
                    onValueChange(it)
                },
                valueRange = valueRange,
                modifier = Modifier.semantics { contentDescription = label },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarksSheet(
    bookmarks: List<Bookmark>,
    onOpen: (Bookmark) -> Unit,
    onEdit: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("bookmarks_sheet")) {
        Text(
            stringResource(R.string.reader_bookmarks),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = VaraghSpacing.ScreenGutter, vertical = VaraghSpacing.Small),
        )
        if (bookmarks.isEmpty()) {
            Text(
                stringResource(R.string.reader_no_bookmarks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(VaraghSpacing.ScreenGutter)
                    .navigationBarsPadding(),
            )
        } else {
            LazyColumn(Modifier.navigationBarsPadding()) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.reader_page_number, formatNumber(bookmark.page + 1L, locale)))
                        },
                        supportingContent = bookmark.note?.let { { Text(it, maxLines = 3) } },
                        leadingContent = { Icon(VaraghIcons.Bookmark, contentDescription = null) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEdit(bookmark) }) {
                                    Icon(VaraghIcons.Edit, contentDescription = stringResource(R.string.reader_edit_note))
                                }
                                IconButton(onClick = { onDelete(bookmark) }) {
                                    Icon(VaraghIcons.Delete, contentDescription = stringResource(DesignR.string.common_delete))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onOpen(bookmark) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun GoToPageDialog(pageCount: Int, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val page = text.toPersianSafeInt()
    val valid = page != null && page in 1..pageCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_go_to_page)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(6) },
                singleLine = true,
                isError = text.isNotEmpty() && !valid,
                label = { Text(stringResource(R.string.reader_page_range, pageCount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.testTag("go_to_page_field"),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onGo(page!! - 1) }) { Text(stringResource(R.string.reader_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.common_cancel)) } },
    )
}

@Composable
internal fun NoteDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_bookmark_note)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_NOTE) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(DesignR.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.common_cancel)) } },
    )
}

/** Accepts Persian/Arabic-Indic digits too (users type with a Persian keyboard). */
internal fun String.toPersianSafeInt(): Int? = buildString {
    for (c in this@toPersianSafeInt.trim()) {
        append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            },
        )
    }
}.toIntOrNull()

private const val MAX_NOTE = 1000
private const val DEFAULT_BRIGHTNESS = 0.5f
private const val MIN_BRIGHTNESS = 0.02f
