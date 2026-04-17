package ai.opencode.mobile.navigation

import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.network.OpenCodeApiClient
import ai.opencode.mobile.network.SSEClient
import ai.opencode.mobile.repository.DefaultSessionRepository
import ai.opencode.mobile.repository.SessionRepository
import ai.opencode.mobile.repository.SettingsStorage
import ai.opencode.mobile.viewmodel.ChatViewModel
import ai.opencode.mobile.viewmodel.ProjectListViewModel
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
        class ProjectListChild(val component: ProjectListComponent) : Child()
        class SessionListChild(val component: SessionListComponent) : Child()
        class ChatChild(val component: ChatComponent) : Child()
        class SettingsChild(val component: SettingsComponent) : Child()
    }
}

interface ProjectListComponent {
    val viewModel: ProjectListViewModel
    fun onProjectSelected(projectDirectory: String, projectName: String)
    fun onSettingsClicked()
}

interface SessionListComponent {
    val viewModel: SessionListViewModel
    val projectName: String
    fun onSessionClicked(sessionId: String, sessionTitle: String)
    fun onSettingsClicked()
    fun onSwitchProject()
}

interface ChatComponent {
    val sessionId: String
    val sessionTitle: String
    val viewModel: ChatViewModel
}

interface SettingsComponent {
    val viewModel: SettingsViewModel
    fun saveAndPersist()
    /** Save settings, persist them, and navigate back to project list. */
    fun saveAndGoBack()
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val apiClient: OpenCodeApiClient,
    private val settingsStorage: SettingsStorage,
) : RootComponent, ComponentContext by componentContext {

    private val sessionRepository: SessionRepository = DefaultSessionRepository(apiClient)

    private val sseClient: SSEClient = SSEClient(OpenCodeApiClient.createHttpClient(apiClient.engine))

    private val navigation = StackNavigation<Config>()

    /**
     * Decide initial screen: if user has never connected (no saved server), go to Settings first.
     * Otherwise, go to ProjectList (which auto-loads projects).
     */
    private val initialConfig: Config = run {
        val config = settingsStorage.load()
        if (config.isConnected) Config.ProjectList() else Config.Settings
    }

    private val _stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = initialConfig,
        key = "RootStack",
        handleBackButton = true,
        childFactory = { config, context -> child(config, context) },
    )

    override val stack: Value<ChildStack<Config, RootComponent.Child>> = _stack

    /**
     * Keep a reference to the current SessionListComponent so we can
     * trigger a reload after saving settings.
     */
    private var currentSessionListComponent: DefaultSessionListComponent? = null

    @OptIn(DelicateDecomposeApi::class)
    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.ProjectList -> {
                RootComponent.Child.ProjectListChild(
                    DefaultProjectListComponent(
                        componentContext = componentContext,
                        sessionRepository = sessionRepository,
                        onProjectSelected = { directory, name ->
                            navigation.push(Config.SessionList(directory = directory, projectName = name))
                        },
                        onSettingsRequested = {
                            navigation.push(Config.Settings)
                        },
                    ),
                )
            }
            is Config.SessionList -> {
                val component = DefaultSessionListComponent(
                    componentContext = componentContext,
                    sessionRepository = sessionRepository,
                    directory = config.directory,
                    projectName = config.projectName,
                    onSessionSelected = { id, title ->
                        navigation.push(Config.Chat(sessionId = id, sessionTitle = title))
                    },
                    onSettingsRequested = {
                        navigation.push(Config.Settings)
                    },
                    onSwitchProjectRequested = {
                        navigation.push(Config.ProjectList())
                    },
                )
                currentSessionListComponent = component
                RootComponent.Child.SessionListChild(component)
            }
            is Config.Chat -> RootComponent.Child.ChatChild(
                DefaultChatComponent(
                    componentContext = componentContext,
                    sessionId = config.sessionId,
                    sessionTitle = config.sessionTitle,
                    sessionRepository = sessionRepository,
                    // Always read fresh config from storage — never cache a stale copy
                    config = settingsStorage.load(),
                    sseClient = sseClient,
                ),
            )
            is Config.Settings -> RootComponent.Child.SettingsChild(
                DefaultSettingsComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    settingsStorage = settingsStorage,
                    onSaved = {
                        // After saving: pop back and reload
                        navigation.pop()
                        currentSessionListComponent?.reloadSessions()
                    },
                ),
            )
        }

    override fun onBackClicked() {
        navigation.pop()
    }
}

class DefaultProjectListComponent(
    componentContext: ComponentContext,
    sessionRepository: SessionRepository,
    private val onProjectSelected: (String, String) -> Unit,
    private val onSettingsRequested: () -> Unit,
) : ProjectListComponent, ComponentContext by componentContext {

    override val viewModel = ProjectListViewModel(repository = sessionRepository)

    override fun onProjectSelected(projectDirectory: String, projectName: String) {
        onProjectSelected(projectDirectory, projectName)
    }

    override fun onSettingsClicked() {
        onSettingsRequested()
    }
}

class DefaultSessionListComponent(
    componentContext: ComponentContext,
    sessionRepository: SessionRepository,
    private val directory: String?,
    override val projectName: String,
    private val onSessionSelected: (String, String) -> Unit,
    private val onSettingsRequested: () -> Unit,
    private val onSwitchProjectRequested: () -> Unit,
) : SessionListComponent, ComponentContext by componentContext {

    override val viewModel = SessionListViewModel(sessionRepository = sessionRepository).also {
        it.directoryFilter = directory
    }

    override fun onSessionClicked(sessionId: String, sessionTitle: String) {
        onSessionSelected(sessionId, sessionTitle)
    }

    override fun onSettingsClicked() {
        onSettingsRequested()
    }

    override fun onSwitchProject() {
        onSwitchProjectRequested()
    }

    /** Reload sessions — called after settings are saved. */
    fun reloadSessions() {
        viewModel.loadSessions()
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
    )
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    apiClient: OpenCodeApiClient,
    settingsStorage: SettingsStorage,
    private val onSaved: () -> Unit = {},
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

    override fun saveAndGoBack() {
        val config = viewModel.saveSettings()
        storage.save(config)
        onSaved()
    }
}