package ai.opencode.mobile.navigation

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
    fun onSessionClicked(sessionId: String, sessionTitle: String)
    fun onSettingsClicked()
}

interface ChatComponent {
    val sessionId: String
    val sessionTitle: String
}

interface SettingsComponent

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val _stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.SessionList,
        key = "RootStack",
        handleBackButton = true,
        childFactory = ::child,
    )

    override val stack: Value<ChildStack<Config, RootComponent.Child>> = _stack

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.SessionList -> RootComponent.Child.SessionListChild(
                DefaultSessionListComponent(componentContext) { id, title ->
                    navigation.push(Config.Chat(sessionId = id, sessionTitle = title))
                },
            )
            is Config.Chat -> RootComponent.Child.ChatChild(
                DefaultChatComponent(componentContext, config.sessionId, config.sessionTitle),
            )
            is Config.Settings -> RootComponent.Child.SettingsChild(
                DefaultSettingsComponent(componentContext),
            )
        }

    override fun onBackClicked() {
        navigation.pop()
    }
}

class DefaultSessionListComponent(
    componentContext: ComponentContext,
    private val onSessionSelected: (String, String) -> Unit,
) : SessionListComponent, ComponentContext by componentContext {

    override fun onSessionClicked(sessionId: String, sessionTitle: String) {
        onSessionSelected(sessionId, sessionTitle)
    }

    override fun onSettingsClicked() {
        // Will be wired to navigation in the RootComponent
    }
}

class DefaultChatComponent(
    componentContext: ComponentContext,
    override val sessionId: String,
    override val sessionTitle: String,
) : ChatComponent, ComponentContext by componentContext

class DefaultSettingsComponent(
    componentContext: ComponentContext,
) : SettingsComponent, ComponentContext by componentContext