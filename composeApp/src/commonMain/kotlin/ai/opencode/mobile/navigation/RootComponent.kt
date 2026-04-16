package ai.opencode.mobile.navigation

import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.network.OpenCodeApiClient
import ai.opencode.mobile.repository.DefaultSessionRepository
import ai.opencode.mobile.repository.SessionRepository
import ai.opencode.mobile.viewmodel.ChatViewModel
import ai.opencode.mobile.viewmodel.SessionListViewModel
import ai.opencode.mobile.viewmodel.SettingsViewModel
import com.arkivanov.decompose.ComponentContext
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
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val apiClient: OpenCodeApiClient,
    private val serverConfig: ServerConfig = ServerConfig(),
) : RootComponent, ComponentContext by componentContext {

    private val sessionRepository: SessionRepository = DefaultSessionRepository(apiClient)

    private val navigation = StackNavigation<Config>()

    private val _stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.SessionList,
        key = "RootStack",
        handleBackButton = true,
        childFactory = { config, context -> child(config, context, serverConfig) },
    )

    override val stack: Value<ChildStack<Config, RootComponent.Child>> = _stack

    private fun child(config: Config, componentContext: ComponentContext, config2: ServerConfig): RootComponent.Child =
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
                ),
            )
            is Config.Settings -> RootComponent.Child.SettingsChild(
                DefaultSettingsComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    serverConfig = config2,
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
) : ChatComponent, ComponentContext by componentContext {

    override val viewModel = ChatViewModel(sessionRepository, sessionId)
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    apiClient: OpenCodeApiClient,
    serverConfig: ServerConfig,
) : SettingsComponent, ComponentContext by componentContext {

    override val viewModel = SettingsViewModel(apiClient, serverConfig)
}