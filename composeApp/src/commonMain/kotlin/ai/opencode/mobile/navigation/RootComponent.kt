package ai.opencode.mobile.navigation

import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.network.OpenCodeApiClient
import ai.opencode.mobile.network.SSEClient
import ai.opencode.mobile.repository.DefaultSessionRepository
import ai.opencode.mobile.repository.SessionRepository
import ai.opencode.mobile.repository.SettingsStorage
import ai.opencode.mobile.viewmodel.ChatViewModel
import ai.opencode.mobile.viewmodel.SessionListViewModel
import ai.opencode.mobile.viewmodel.SettingsViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val stack: Value<ChildStack<Config, Child>>

    fun onBackClicked()

    sealed class Child {
        class SessionListChild(val component: SessionListComponent) : Child()
        class ChatChild(val component: ChatComponent) : Child()
        class SettingsChild(val component: SettingsComponent) : Child()
    }
}

interface SessionListComponent {
    val viewModel: SessionListViewModel
    fun onSessionClicked(sessionId: String, sessionTitle: String)
    fun onSettingsClicked()
}

interface ChatComponent {
    val sessionId: String
    val sessionTitle: String
    val viewModel: ChatViewModel
}

interface SettingsComponent {
    val viewModel: SettingsViewModel
    fun saveAndPersist()
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val apiClient: OpenCodeApiClient,
    private val settingsStorage: SettingsStorage,
) : RootComponent, ComponentContext by componentContext {

    private val serverConfig: ServerConfig = settingsStorage.load()

    private val sessionRepository: SessionRepository = DefaultSessionRepository(apiClient)

    private val sseClient: SSEClient = SSEClient(OpenCodeApiClient.createHttpClient(apiClient.engine))

    private val navigation = StackNavigation<Config>()

    private val _stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.SessionList,
        key = "RootStack",
        handleBackButton = true,
        childFactory = { config, context -> child(config, context) },
    )

    override val stack: Value<ChildStack<Config, RootComponent.Child>> = _stack

    @OptIn(DelicateDecomposeApi::class)
    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.SessionList -> RootComponent.Child.SessionListChild(
                DefaultSessionListComponent(
                    componentContext = componentContext,
                    sessionRepository = sessionRepository,
                    onSessionSelected = { id, title ->
                        navigation.push(Config.Chat(sessionId = id, sessionTitle = title))
                    },
                    onSettingsRequested = {
                        navigation.push(Config.Settings)
                    },
                ),
            )
            is Config.Chat -> RootComponent.Child.ChatChild(
                DefaultChatComponent(
                    componentContext = componentContext,
                    sessionId = config.sessionId,
                    sessionTitle = config.sessionTitle,
                    sessionRepository = sessionRepository,
                    config = serverConfig,
                    sseClient = sseClient,
                ),
            )
            is Config.Settings -> RootComponent.Child.SettingsChild(
                DefaultSettingsComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    settingsStorage = settingsStorage,
                ),
            )
        }

    override fun onBackClicked() {
        navigation.pop()
    }
}

class DefaultSessionListComponent(
    componentContext: ComponentContext,
    sessionRepository: SessionRepository,
    private val onSessionSelected: (String, String) -> Unit,
    private val onSettingsRequested: () -> Unit,
) : SessionListComponent, ComponentContext by componentContext {

    override val viewModel = SessionListViewModel(sessionRepository)

    override fun onSessionClicked(sessionId: String, sessionTitle: String) {
        onSessionSelected(sessionId, sessionTitle)
    }

    override fun onSettingsClicked() {
        onSettingsRequested()
    }
}

class DefaultChatComponent(
    componentContext: ComponentContext,
    override val sessionId: String,
    override val sessionTitle: String,
    sessionRepository: SessionRepository,
    config: ServerConfig = ServerConfig(),
    sseClient: SSEClient? = null,
) : ChatComponent, ComponentContext by componentContext {

    override val viewModel = ChatViewModel(
        sessionRepository = sessionRepository,
        sessionId = sessionId,
        sseClient = sseClient,
        baseUrl = config.serverUrl.ifBlank { OpenCodeApiClient.DEFAULT_URL },
        basicAuth = if (config.username.isNotEmpty() || config.password.isNotEmpty()) {
            OpenCodeApiClient.encodeCredentials(config.username, config.password)
        } else {
            ""
        },
    ).also {
        if (config.modelId.isNotBlank() || config.providerId.isNotBlank()) {
            it.setModel(
                modelId = config.modelId.ifBlank { "default" },
                providerId = config.providerId.ifBlank { "default" },
            )
        }
    }
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    apiClient: OpenCodeApiClient,
    settingsStorage: SettingsStorage,
) : SettingsComponent, ComponentContext by componentContext {

    private val storage = settingsStorage

    override val viewModel = SettingsViewModel(
        apiClient = apiClient,
        initialConfig = settingsStorage.load(),
    )

    override fun saveAndPersist() {
        val config = viewModel.saveSettings()
        storage.save(config)
    }
}