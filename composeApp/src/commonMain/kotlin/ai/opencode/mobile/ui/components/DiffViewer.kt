package ai.opencode.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ai.opencode.mobile.ui.theme.DiffAddedBackground
import ai.opencode.mobile.ui.theme.DiffAddedBackgroundLight
import ai.opencode.mobile.ui.theme.DiffAddedText
import ai.opencode.mobile.ui.theme.DiffRemovedBackground
import ai.opencode.mobile.ui.theme.DiffRemovedBackgroundLight
import ai.opencode.mobile.ui.theme.DiffRemovedText

@Composable
fun DiffViewer(
    diffText: String,
    modifier: Modifier = Modifier,
) {
    val isLight = isLightTheme()
    val addedBg = if (isLight) DiffAddedBackgroundLight else DiffAddedBackground
    val removedBg = if (isLight) DiffRemovedBackgroundLight else DiffRemovedBackground

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                diffText.lines().forEach { line ->
                    val bgColor = when {
                        line.startsWith("+") -> addedBg
                        line.startsWith("-") -> removedBg
                        line.startsWith("@@") -> MaterialTheme.colorScheme.surfaceVariant
                        else -> null
                    }

                    val textColor = when {
                        line.startsWith("+") -> DiffAddedText
                        line.startsWith("-") -> DiffRemovedText
                        line.startsWith("@@") -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (bgColor != null) Modifier.background(bgColor) else Modifier
                            ),
                    ) {
                        Text(
                            text = line.ifEmpty { " " },
                            style = textStyle,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun isLightTheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val luminance = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue
    return luminance > 0.5f
}