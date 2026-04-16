package ai.opencode.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import ai.opencode.mobile.navigation.DefaultRootComponent
import ai.opencode.mobile.navigation.RootComponent
import ai.opencode.mobile.network.OpenCodeApiClient
import ai.opencode.mobile.repository.SettingsStorage
import ai.opencode.mobile.ui.screens.ChatScreen
import ai.opencode.mobile.ui.screens.SessionListScreen
import ai.opencode.mobile.ui.screens.SettingsScreen
import ai.opencode.mobile.ui.theme.OpenCodeTheme

@Composable
fun App() {
    val settingsStorage = remember { createSettingsStorage() }
    val apiClient = remember { createOpenCodeApiClient(settingsStorage) }
    val rootComponent = remember {
        val lifecycle = LifecycleRegistry()
        lifecycle.resume()
        DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            apiClient = apiClient,
            settingsStorage = settingsStorage,
        )
    }

    OpenCodeTheme {
        AppContent(rootComponent)
    }
}

@Composable
fun AppContent(rootComponent: RootComponent) {
    Children(
        stack = rootComponent.stack,
        animation = stackAnimation(fade()),
        modifier = Modifier.fillMaxSize(),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.SessionListChild -> SessionListScreen(
                component = instance.component,
            )
            is RootComponent.Child.ChatChild -> ChatScreen(
                component = instance.component,
                onBack = { rootComponent.onBackClicked() },
            )
            is RootComponent.Child.SettingsChild -> SettingsScreen(
                component = instance.component,
                onBack = { rootComponent.onBackClicked() },
            )
        }
    }
}

expect fun createOpenCodeApiClient(settingsStorage: SettingsStorage): OpenCodeApiClient

expect fun createSettingsStorage(): SettingsStorage