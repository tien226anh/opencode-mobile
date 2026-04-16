package ai.opencode.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {
    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            val blocks = parseMarkdownBlocks(text)
            blocks.forEachIndexed { index, block ->
                when (block) {
                    is MarkdownBlock.CodeBlock -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = block.content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                    is MarkdownBlock.InlineText -> {
                        if (block.content.isNotBlank()) {
                            val annotated = parseInlineCode(block.content)
                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                if (index < blocks.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class CodeBlock(val content: String) : MarkdownBlock()
    data class InlineText(val content: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val parts = text.split("```")
    var isCode = false

    for (part in parts) {
        if (isCode) {
            // Code block content — strip optional language hint from first line
            val content = part.trimStart('\n', '\r')
            val contentWithoutLangHint = content.lineSequence().let { lines ->
                val first = lines.firstOrNull() ?: ""
                if (first.length < 20 && first.all { it.isLetterOrDigit() || it == '+' || it == '#' }) {
                    // Looks like a language hint (e.g. "kotlin", "python")
                    content.removePrefix(first).trimStart('\n', '\r')
                } else {
                    content
                }
            }
            if (contentWithoutLangHint.isNotBlank()) {
                blocks.add(MarkdownBlock.CodeBlock(contentWithoutLangHint.trimEnd()))
            }
        } else {
            if (part.isNotBlank()) {
                blocks.add(MarkdownBlock.InlineText(part))
            }
        }
        isCode = !isCode
    }

    return blocks
}

private fun parseInlineCode(text: String): AnnotatedString {
    return buildAnnotatedString {
        val codeSpanStyle = SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = androidx.compose.ui.graphics.Color.Transparent,
        )

        var remaining = text
        while (remaining.contains('`')) {
            val start = remaining.indexOf('`')
            if (start == -1) break

            // Text before backtick
            if (start > 0) {
                append(remaining.substring(0, start))
            }

            // Find closing backtick
            val end = remaining.indexOf('`', start + 1)
            if (end == -1) {
                // No closing backtick, just append the rest
                append(remaining.substring(start))
                remaining = ""
            } else {
                // Inline code between backticks
                withStyle(codeSpanStyle) {
                    append(remaining.substring(start + 1, end))
                }
                remaining = remaining.substring(end + 1)
            }
        }
        if (remaining.isNotEmpty()) {
            append(remaining)
        }
    }
}