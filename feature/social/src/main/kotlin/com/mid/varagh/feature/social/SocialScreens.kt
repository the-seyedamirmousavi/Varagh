package com.mid.varagh.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghEmptyState
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghPrimaryButton
import com.mid.varagh.core.designsystem.component.VaraghProgressBar
import com.mid.varagh.core.designsystem.component.VaraghTextButton
import com.mid.varagh.core.designsystem.component.VaraghTextField
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.errorMessageRes
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.model.FeedItem
import com.mid.varagh.core.model.FeedItemType
import com.mid.varagh.core.model.PublicReader
import com.mid.varagh.core.designsystem.R as DesignR

// ---------------------------------------------------------------- Feed

@Composable
internal fun FeedScreenRoute(
    onOpenReader: (String) -> Unit,
    onDiscover: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeedScreen(state, onOpenReader, onDiscover, onSignIn, viewModel::refresh, viewModel::loadMore)
}

@Composable
internal fun FeedScreen(
    state: FeedUiState,
    onOpenReader: (String) -> Unit,
    onDiscover: () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Scaffold(
        topBar = {
            VaraghTopAppBar(
                title = stringResource(R.string.social_title),
                actions = {
                    IconButton(onClick = onDiscover) {
                        Icon(VaraghIcons.Search, contentDescription = stringResource(R.string.social_discover))
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when {
            !state.signedIn -> VaraghEmptyState(
                icon = VaraghIcons.Login,
                title = stringResource(R.string.social_sign_in_title),
                message = stringResource(R.string.social_sign_in_message),
                actionLabel = stringResource(R.string.social_sign_in),
                onAction = onSignIn,
                modifier = modifier.testTag("feed_sign_in"),
            )
            state.loading && state.items.isEmpty() -> VaraghLoading(modifier)
            state.error != null && state.items.isEmpty() -> ErrorState(state.error, onRetry, modifier)
            state.items.isEmpty() -> VaraghEmptyState(
                icon = VaraghIcons.SocialOutlined,
                title = stringResource(R.string.social_empty_title),
                message = stringResource(R.string.social_empty_message),
                actionLabel = stringResource(R.string.social_discover),
                onAction = onDiscover,
                modifier = modifier,
            )
            else -> {
                val listState = rememberLazyListState()
                val nearEnd by remember {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
                    }
                }
                LaunchedEffect(nearEnd) { if (nearEnd && !state.endReached) onLoadMore() }
                LazyColumn(
                    state = listState,
                    modifier = modifier
                        .fillMaxSize()
                        .testTag("feed_list"),
                    contentPadding = PaddingValues(VaraghSpacing.ScreenGutter),
                    verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
                ) {
                    items(state.items, key = { it.id }) { FeedCard(it, onOpenReader) }
                    if (state.loadingMore) {
                        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem, onOpenReader: (String) -> Unit) {
    val name = item.reader.displayName.ifBlank { "@" + item.reader.username }
    val action = stringResource(
        when (item.type) {
            FeedItemType.STARTED -> R.string.social_started
            FeedItemType.READING -> R.string.social_reading
            FeedItemType.FINISHED -> R.string.social_finished
        },
        name,
    )
    VaraghCard(onClick = { onOpenReader(item.reader.username) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            ReaderAvatar(item.reader, 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XSmall)) {
                Text(action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.book.title, style = MaterialTheme.typography.titleMedium)
                item.book.author?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item.progressPercent?.takeIf { item.type == FeedItemType.READING }?.let { VaraghProgressBar(it) }
            }
        }
    }
}

// ---------------------------------------------------------------- Discover

@Composable
internal fun DiscoverScreenRoute(
    onBack: () -> Unit,
    onOpenReader: (String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(
        topBar = {
            VaraghTopAppBar(
                title = stringResource(R.string.social_discover),
                centered = false,
                onBack = onBack,
                backContentDescription = stringResource(DesignR.string.common_back),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            VaraghTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.social_find_by_username),
                placeholder = "@username",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            VaraghPrimaryButton(
                text = stringResource(R.string.social_search),
                onClick = { viewModel.search(query) },
                leadingIcon = VaraghIcons.Search,
                enabled = query.trim().removePrefix("@").length >= 3,
                modifier = Modifier.fillMaxWidth(),
            )
            when (val r = result) {
                null -> Unit
                Remote.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is Remote.Failed -> Text(
                    stringResource(if (r.error is VaraghException.NotFound) R.string.social_no_such_reader else errorMessageRes(r.error)),
                    color = MaterialTheme.colorScheme.error,
                )
                is Remote.Loaded -> ReaderRow(r.value, onOpenReader)
            }
        }
    }
}

@Composable
private fun ReaderRow(reader: PublicReader, onOpen: (String) -> Unit) {
    VaraghCard(onClick = { onOpen(reader.username) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            ReaderAvatar(reader, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(reader.displayName.ifBlank { reader.username }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${reader.username}",
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(VaraghIcons.Chevron, contentDescription = null)
        }
    }
}

// ---------------------------------------------------------------- Public profile

@Composable
internal fun PublicProfileScreenRoute(
    onBack: () -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel(),
) {
    val reader by viewModel.reader.collectAsStateWithLifecycle()
    val busy by viewModel.followBusy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.errorFlow.collect { snackbar.showSnackbar(context.getString(errorMessageRes(it))) }
    }
    val locale = currentLocale()
    Scaffold(
        topBar = {
            VaraghTopAppBar(
                title = "@${viewModel.username}",
                centered = false,
                onBack = onBack,
                backContentDescription = stringResource(DesignR.string.common_back),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val r = reader) {
            Remote.Loading -> VaraghLoading(Modifier.padding(padding))
            is Remote.Failed -> ErrorState(r.error, viewModel::load, Modifier.padding(padding))
            is Remote.Loaded -> Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(VaraghSpacing.ScreenGutter),
                verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
            ) {
                val p = r.value
                VaraghCard(horizontalAlignment = Alignment.CenterHorizontally) {
                    ReaderAvatar(p, 96.dp)
                    Text(p.displayName.ifBlank { p.username }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = VaraghSpacing.Medium))
                    if (p.bio.isNotBlank()) Text(p.bio, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Text(
                        stringResource(R.string.social_books_finished, formatNumber(p.booksFinished.toLong(), locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = VaraghSpacing.Small),
                    )
                    VaraghPrimaryButton(
                        text = stringResource(if (p.isFollowedByMe) R.string.social_unfollow else R.string.social_follow),
                        onClick = viewModel::toggleFollow,
                        leadingIcon = if (p.isFollowedByMe) null else VaraghIcons.Follow,
                        inverted = p.isFollowedByMe,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("follow_button"),
                    )
                }
                p.currentlyReading?.let { book ->
                    VaraghCard(title = stringResource(R.string.social_currently_reading)) {
                        Text(book.title, style = MaterialTheme.typography.titleMedium)
                        book.author?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Login / register

@Composable
internal fun LoginScreenRoute(
    onDone: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    LoginScreen(state, onBack = onDone, onModeChange = viewModel::setRegistering, onSubmit = viewModel::submit)
}

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onModeChange: (Boolean) -> Unit,
    onSubmit: (email: String, password: String, username: String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    val valid = LoginViewModel.isValid(state.registering, email, password, username)
    Scaffold(
        topBar = {
            VaraghTopAppBar(
                title = stringResource(if (state.registering) R.string.social_register else R.string.social_sign_in),
                centered = false,
                onBack = onBack,
                backContentDescription = stringResource(DesignR.string.common_back),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            VaraghCard {
                Column(verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Large)) {
                    if (state.registering) {
                        VaraghTextField(
                            value = username,
                            onValueChange = { username = it.lowercase().take(20) },
                            label = stringResource(R.string.social_username),
                            supportingText = stringResource(R.string.social_username_hint),
                            modifier = Modifier.testTag("login_username"),
                        )
                    }
                    VaraghTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = stringResource(R.string.social_email),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("login_email"),
                    )
                    PasswordField(password, { password = it })
                    state.error?.let { error ->
                        val message = (error as? VaraghException.Rejected)?.serverMessage
                            ?: stringResource(if (error is VaraghException.Unauthorized) R.string.social_wrong_credentials else errorMessageRes(error))
                        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("login_error"))
                    }
                    VaraghPrimaryButton(
                        text = stringResource(if (state.registering) R.string.social_register else R.string.social_sign_in),
                        onClick = { onSubmit(email, password, username) },
                        enabled = valid && !state.busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_submit"),
                    )
                    VaraghTextButton(
                        text = stringResource(if (state.registering) R.string.social_have_account else R.string.social_no_account),
                        onClick = { onModeChange(!state.registering) },
                    )
                }
            }
            Text(
                stringResource(R.string.social_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.social_password)) },
        supportingText = { Text(stringResource(R.string.social_password_hint, LoginViewModel.MIN_PASSWORD)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_password"),
    )
}

// ---------------------------------------------------------------- Readers of a book

/** "Readers of this book" section for the book details screen (remote backend only). */
@Composable
fun ReadersOfBookSection(
    remoteBookId: String,
    onOpenReader: (String) -> Unit,
    viewModel: ReadersOfBookViewModel = hiltViewModel(key = "readers-$remoteBookId"),
) {
    LaunchedEffect(remoteBookId) { viewModel.load(remoteBookId) }
    val readers by viewModel.readers.collectAsStateWithLifecycle()
    VaraghCard(title = stringResource(R.string.social_readers_of_book), contentPadding = PaddingValues(vertical = VaraghSpacing.Small)) {
        when (val r = readers) {
            Remote.Loading -> Box(Modifier.fillMaxWidth().padding(VaraghSpacing.Large), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Remote.Failed -> Text(
                stringResource(errorMessageRes(r.error)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
            )
            is Remote.Loaded -> if (r.value.isEmpty()) {
                Text(
                    stringResource(R.string.social_no_readers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
                )
            } else {
                r.value.forEach { reader ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReader(reader.username) }
                            .padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReaderAvatar(reader, 36.dp)
                        Text(reader.displayName.ifBlank { reader.username }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Shared bits

@Composable
private fun ReaderAvatar(reader: PublicReader, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text((reader.displayName.ifBlank { reader.username }).take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
        reader.avatarUrl?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun ErrorState(error: Throwable, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    VaraghEmptyState(
        icon = if (error is VaraghException.Network) VaraghIcons.Offline else VaraghIcons.Error,
        title = stringResource(if (error is VaraghException.Network) R.string.social_offline_title else R.string.social_error_title),
        message = stringResource(errorMessageRes(error)),
        actionLabel = stringResource(DesignR.string.common_retry),
        onAction = onRetry,
        modifier = modifier.testTag("social_error"),
    )
}

private const val LOAD_MORE_THRESHOLD = 3
