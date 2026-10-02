package com.mid.varagh.feature.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mid.varagh.core.designsystem.component.BookCover
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghPrimaryButton
import com.mid.varagh.core.designsystem.component.VaraghProgressBar
import com.mid.varagh.core.designsystem.component.VaraghStatRow
import com.mid.varagh.core.designsystem.component.VaraghTextButton
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.errorMessageRes
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.motion.AppearAnimated
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.core.model.UserProfile
import com.mid.varagh.feature.profile.share.ShareCardText
import java.io.File
import kotlin.math.roundToInt
import com.mid.varagh.core.designsystem.R as DesignR

@Composable
internal fun ProfileScreenRoute(
    onOpenBook: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.profile_share_chooser)
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ProfileEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, chooserTitle))
                }
                is ProfileEvent.Failed -> snackbar.showSnackbar(context.getString(errorMessageRes(event.error)))
            }
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onAvatarPicked(it.toString()) }
    }
    ProfileScreen(
        state = state,
        flags = viewModel.featureFlags,
        snackbarHostState = snackbar,
        onPickAvatar = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onSaveProfile = viewModel::onSaveProfile,
        onPublicChange = viewModel::onPublicChange,
        onOpenBook = onOpenBook,
        onOpenLibrary = onOpenLibrary,
        onShare = viewModel::onShare,
        onSignIn = onSignIn,
        onLogout = viewModel::onLogout,
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    flags: FeatureFlags,
    snackbarHostState: SnackbarHostState,
    onPickAvatar: () -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onShare: (ShareCardText) -> Unit,
    onSignIn: () -> Unit,
    onLogout: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { VaraghTopAppBar(title = stringResource(R.string.profile_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.loading) {
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
            AppearAnimated(0) { HeaderCard(state, onPickAvatar, onEdit = { editing = true }) }
            AppearAnimated(1) { CurrentlyReadingCard(state, onOpenBook, onOpenLibrary, onShare) }
            if (flags.isPublicProfileEnabled) {
                AppearAnimated(2) { PublicProfileCard(state.profile.isPublic, onPublicChange) }
            }
            if (flags.isAuthEnabled) {
                AppearAnimated(3) { AccountCard(state.auth, onSignIn, onLogout) }
            }
        }
    }
    if (editing) {
        EditProfileDialog(
            profile = state.profile,
            onSave = { name, username, bio ->
                editing = false
                onSaveProfile(name, username, bio)
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun HeaderCard(state: ProfileUiState, onPickAvatar: () -> Unit, onEdit: () -> Unit) {
    val profile = state.profile
    val locale = currentLocale()
    VaraghCard(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Avatar(profile, Modifier.size(104.dp))
            SmallFloatingActionButton(
                onClick = onPickAvatar,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("pick_avatar"),
            ) {
                Icon(VaraghIcons.Camera, contentDescription = stringResource(R.string.profile_change_photo))
            }
        }
        Text(
            text = profile.displayName.ifBlank { stringResource(R.string.profile_default_name) },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = VaraghSpacing.Medium)
                .testTag("profile_name"),
        )
        if (profile.username.isNotBlank()) {
            Text(
                "@${profile.username}",
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (profile.bio.isNotBlank()) {
            Text(
                profile.bio,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = VaraghSpacing.Small),
            )
        }
        VaraghTextButton(text = stringResource(R.string.profile_edit), onClick = onEdit, modifier = Modifier.testTag("edit_profile"))
        VaraghStatRow(
            stats = listOf(
                formatNumber(state.stats.totalBooksFinished.toLong(), locale) to stringResource(R.string.profile_stat_finished),
                formatNumber(state.stats.totalPagesRead.toLong(), locale) to stringResource(R.string.profile_stat_pages),
                formatNumber(state.stats.currentStreakDays.toLong(), locale) to stringResource(R.string.profile_stat_streak),
            ),
            modifier = Modifier.padding(top = VaraghSpacing.Small),
        )
    }
}

@Composable
private fun Avatar(profile: UserProfile, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val initial = profile.displayName.trim().firstOrNull()?.uppercase()
        if (initial != null) {
            Text(initial, style = MaterialTheme.typography.displaySmall)
        } else {
            Icon(VaraghIcons.ProfileOutlined, contentDescription = null, modifier = Modifier.size(48.dp))
        }
        profile.avatarPath?.let {
            AsyncImage(
                model = File(it),
                contentDescription = stringResource(R.string.profile_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CurrentlyReadingCard(
    state: ProfileUiState,
    onOpenBook: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onShare: (ShareCardText) -> Unit,
) {
    val reading: BookWithProgress? = state.currentlyReading
    val locale = currentLocale()
    VaraghCard(title = stringResource(R.string.profile_currently_reading)) {
        if (reading == null) {
            Text(
                stringResource(R.string.profile_nothing_reading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VaraghPrimaryButton(
                text = stringResource(R.string.profile_open_library),
                onClick = onOpenLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VaraghSpacing.Large),
            )
            return@VaraghCard
        }
        val book = reading.book
        val percent = ((reading.progress?.percent ?: 0f) * 100).roundToInt()
        val percentText = stringResource(R.string.profile_percent_read, formatNumber(percent.toLong(), locale))
        Row(horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book.coverPath, book.title, Modifier.width(80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XSmall)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("currently_reading_title"))
                book.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                VaraghProgressBar(reading.progress?.percent ?: 0f)
                Text(percentText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val eyebrow = stringResource(R.string.profile_share_eyebrow)
        val appName = stringResource(R.string.profile_app_name)
        val readerName = state.profile.displayName
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = VaraghSpacing.Large),
            horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small),
        ) {
            VaraghPrimaryButton(
                text = stringResource(R.string.profile_continue),
                onClick = { onOpenBook(book.id) },
                modifier = Modifier.weight(1f),
            )
            VaraghPrimaryButton(
                text = stringResource(R.string.profile_share),
                onClick = { onShare(ShareCardText(eyebrow, book.title, book.author, percentText, readerName, appName)) },
                leadingIcon = VaraghIcons.Share,
                enabled = !state.sharing,
                inverted = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("share_card"),
            )
        }
    }
}

@Composable
private fun PublicProfileCard(isPublic: Boolean, onChange: (Boolean) -> Unit) {
    VaraghCard {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = isPublic, role = Role.Switch, onValueChange = onChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_public), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.profile_public_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isPublic, onCheckedChange = null, modifier = Modifier.testTag("public_switch"))
        }
    }
}

@Composable
private fun AccountCard(auth: AuthState?, onSignIn: () -> Unit, onLogout: () -> Unit) {
    VaraghCard(title = stringResource(R.string.profile_account)) {
        when (auth) {
            is AuthState.SignedIn -> {
                Text(stringResource(R.string.profile_signed_in_as, auth.username), style = MaterialTheme.typography.bodyLarge)
                VaraghTextButton(text = stringResource(R.string.profile_sign_out), onClick = onLogout)
            }
            else -> {
                Text(
                    stringResource(R.string.profile_sign_in_summary),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VaraghPrimaryButton(
                    text = stringResource(R.string.profile_sign_in),
                    onClick = onSignIn,
                    leadingIcon = VaraghIcons.Login,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = VaraghSpacing.Large)
                        .testTag("sign_in"),
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(profile: UserProfile, onSave: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile.displayName) }
    var username by rememberSaveable { mutableStateOf(profile.username) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    val usernameValid = ProfileViewModel.isValidUsername(username)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(ProfileViewModel.MAX_NAME) },
                    label = { Text(stringResource(R.string.profile_field_name)) },
                    singleLine = true,
                    modifier = Modifier.testTag("field_name"),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.take(USERNAME_INPUT_LIMIT) },
                    label = { Text(stringResource(R.string.profile_field_username)) },
                    supportingText = { Text(stringResource(R.string.profile_username_hint)) },
                    isError = !usernameValid,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(ProfileViewModel.MAX_BIO) },
                    label = { Text(stringResource(R.string.profile_field_bio)) },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = usernameValid, onClick = { onSave(name, username, bio) }, modifier = Modifier.testTag("save_profile")) {
                Text(stringResource(DesignR.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.common_cancel)) } },
    )
}

private const val USERNAME_INPUT_LIMIT = 24
