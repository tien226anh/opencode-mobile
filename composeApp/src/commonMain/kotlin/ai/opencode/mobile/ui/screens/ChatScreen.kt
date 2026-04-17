package ai.opencode.mobile.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.Part
import ai.opencode.mobile.model.SessionStatus
import ai.opencode.mobile.ui.components.FileDiffCard
import ai.opencode.mobile.navigation.ChatComponent
import ai.opencode.mobile.ui.components.MarkdownText
import ai.opencode.mobile.ui.components.PermissionDialog
import ai.opencode.mobile.ui.components.ToolResultCard
import ai.opencode.mobile.ui.theme.AssistantMessageBackground
import ai.opencode.mobile.ui.theme.AssistantMessageBackgroundLight
import ai.opencode.mobile.ui.theme.UserMessageBackground
import ai.opencode.mobile.ui.theme.UserMessageBackgroundLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    component: ChatComponent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by component.viewModel.state.collectAsState()
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        component.viewModel.loadMessages()
        component.viewModel.loadProviders()
        component.viewModel.loadModes()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            component.viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = component.sessionTitle.ifEmpty { "Chat" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.currentModelId.isNotBlank()) {
                            Text(
                                text = buildString {
                                    append(state.currentProviderId)
                                    if (state.currentModelId.isNotBlank()) append(" / ${state.currentModelId}")
                                    if (state.currentModeName.isNotBlank()) append(" [${state.currentModeName}]")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Session status indicator
                        val status = state.sessionStatus
                        if (status != null && status !is SessionStatus.Idle) {
                            Text(
                                text = when (status) {
                                    is SessionStatus.Busy -> "\u26A1 Processing..."
                                    is SessionStatus.Retry -> "\u21BB Retrying (attempt ${status.attempt})..."
                                    is SessionStatus.Idle -> "" // won't reach here due to condition
                                    is SessionStatus.Unknown -> "\u25CB ${status.type}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    if (state.fileDiffs.isNotEmpty() || state.isLoadingDiffs) {
                        IconButton(onClick = { /* diffs already shown below */ }) {
                            Text("\u2B06", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column {
                // Provider/Model/Mode selection row — only show when providers are loaded
                if (state.providers.isNotEmpty() || state.modes.isNotEmpty()) {
                    ProviderModelModeBar(
                        state = state,
                        onProviderSelected = { component.viewModel.updateProvider(it) },
                        onModelSelected = { component.viewModel.updateModel(it) },
                        onModeSelected = { component.viewModel.updateMode(it) },
                    )
                }
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            component.viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onAbort = { component.viewModel.abortSession() },
                    isSending = state.isSending,
                    diffCount = state.fileDiffs.size,
                    onLoadDiffs = { component.viewModel.loadSessionDiff() },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            state.messages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Start a conversation", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Type a message below to begin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.messages, key = { it.info.id }) { message ->
                        MessageBubble(
                            message = message,
                            onRevert = { messageId -> component.viewModel.revertMessage(messageId) },
                        )
                    }
                    if (state.isSending) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                    // File diffs section — shown when there are changes
                    if (state.fileDiffs.isNotEmpty()) {
                        item {
                            Text(
                                text = "File Changes (${state.fileDiffs.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(items = state.fileDiffs, key = { it.file }) { diff ->
                            FileDiffCard(diff = diff)
                        }
                    }
                    if (state.isLoadingDiffs) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
        // Permission dialog — shown when a tool permission request is pending
        state.pendingPermission?.let { permission ->
            PermissionDialog(
                permission = permission,
                onAllow = { component.viewModel.respondToPermission(allow = true) },
                onDeny = { component.viewModel.respondToPermission(allow = false) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderModelModeBar(
    state: ai.opencode.mobile.viewmodel.ChatState,
    onProviderSelected: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(tonalElevation = 2.dp, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Provider dropdown
            if (state.providers.isNotEmpty()) {
                var providerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    val providerName = state.selectedProvider?.name?.ifEmpty { state.selectedProviderId } ?: state.selectedProviderId
                    OutlinedTextField(
                        value = providerName.ifEmpty { "Provider" },
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.labelMedium,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                    ) {
                        state.providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name.ifEmpty { provider.id }, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    onProviderSelected(provider.id)
                                    providerExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Model dropdown (cascading from selected provider)
            if (state.selectedProviderModels.isNotEmpty()) {
                var modelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    val modelName = state.selectedProviderModels.find { it.id == state.selectedModelId }?.name?.ifEmpty { state.selectedModelId } ?: state.selectedModelId
                    OutlinedTextField(
                        value = modelName.ifEmpty { "Model" },
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.labelMedium,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                    ) {
                        state.selectedProviderModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.name.ifEmpty { model.id }, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    onModelSelected(model.id)
                                    modelExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Mode dropdown
            if (state.modes.isNotEmpty()) {
                var modeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = state.selectedModeName.ifEmpty { "Mode" },
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.labelMedium,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false },
                    ) {
                        state.modes.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(mode.name, style = MaterialTheme.typography.bodySmall)
                                        mode.description?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    onModeSelected(mode.name)
                                    modeExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageResponseItem,
    onRevert: ((messageId: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isUser = message.info.role == "user"
    val isDarkTheme = !isLightTheme()

    val bubbleColor = when {
        isUser -> if (isDarkTheme) UserMessageBackground else UserMessageBackgroundLight
        else -> if (isDarkTheme) AssistantMessageBackground else AssistantMessageBackgroundLight
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .padding(12.dp),
        ) {
            // Main content parts
            message.parts.filter { it.type == "text" }.forEach { part ->
                if (isUser) {
                    Text(
                        text = part.text ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    MarkdownText(text = part.text ?: "")
                }
            }

            // Tool parts
            message.parts.filter { it.type == "tool" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                ToolResultCard(part = part)
            }

            // Step-start parts (show thinking indicator)
            message.parts.filter { it.type == "step-start" }.forEach { _ ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Step-finish parts (show cost/tokens if available)
            message.parts.filter { it.type == "step-finish" }.forEach { part ->
                if (part.cost != null || part.tokens != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            part.cost?.let { append("${(it * 10000).toInt() / 10000.0} tokens") }
                            part.tokens?.let {
                                if (isNotEmpty()) append(" \u00B7 ")
                                append("\u2191${it.input} \u2193${it.output}")
                                if (it.reasoning > 0) append(" \u2728${it.reasoning}")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // File parts
            message.parts.filter { it.type == "file" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = part.filename ?: part.url ?: "File",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            // Reasoning parts (collapsible thinking)
            message.parts.filter { it.type == "reasoning" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "[Thinking] Reasoning",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        val reasoningText = part.text ?: ""
                        if (reasoningText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reasoningText.take(500) + if (reasoningText.length > 500) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Patch parts (show changed files)
            message.parts.filter { it.type == "patch" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "\u270E Patch${part.hash?.let { " ($it)" } ?: ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        part.files?.forEach { file ->
                            Text(
                                text = file,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Agent parts (show agent reference)
            message.parts.filter { it.type == "agent" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "[Agent] ${part.name ?: "unknown"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            // Retry parts (show retry indicator)
            message.parts.filter { it.type == "retry" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = "\u21BB Retry attempt ${part.attempt ?: 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            // Compaction parts (show summary indicator)
            message.parts.filter { it.type == "compaction" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "[Compacted]${if (part.auto == true) " (auto)" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            // Subtask parts (show subtask prompt)
            message.parts.filter { it.type == "subtask" }.forEach { part ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "[Subtask] ${part.text?.take(100) ?: part.name ?: "processing"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            // Message-level cost/tokens info
            if (!isUser && (message.info.cost != null || message.info.tokens != null)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        message.info.cost?.let {
                            val rounded = (it * 10000).toInt() / 10000.0
                            append('\u0024') // dollar sign
                            append(rounded)
                        }
                        message.info.tokens?.let {
                            if (isNotEmpty()) append(" \u00B7 ")
                            append("in:${it.input} out:${it.output}")
                            if (it.reasoning > 0) append(" reason:${it.reasoning}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Revert button for assistant messages
            if (!isUser && onRevert != null && message.info.id.isNotEmpty() && !message.info.id.startsWith("temp-")) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { onRevert(message.info.id) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "\u21A9 Undo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1",
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2",
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3",
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("\u2022", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha1))
        Spacer(modifier = Modifier.width(2.dp))
        Text("\u2022", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha2))
        Spacer(modifier = Modifier.width(2.dp))
        Text("\u2022", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha3))
    }
}

@Composable
private fun isLightTheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val luminance = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue
    return luminance > 0.5f
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAbort: () -> Unit,
    isSending: Boolean,
    diffCount: Int = 0,
    onLoadDiffs: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(tonalElevation = 3.dp, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                shape = MaterialTheme.shapes.large,
                enabled = !isSending,
            )
            if (isSending) {
                TextButton(
                    onClick = onAbort,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Stop")
                }
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                if (diffCount > 0) {
                    TextButton(onClick = { /* diffs already shown above */ }) {
                        Text("$diffCount \u2B06", style = MaterialTheme.typography.labelSmall)
                    }
                } else if (onLoadDiffs != null) {
                    TextButton(onClick = onLoadDiffs) {
                        Text("Changes", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                ) {
                    Text("\u2191", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}