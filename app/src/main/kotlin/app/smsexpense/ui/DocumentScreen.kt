package app.smsexpense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.smsexpense.R
import app.smsexpense.ui.theme.Spacing

/** One piece of a document shown in the app: a heading, a bullet or a paragraph. */
sealed interface DocBlock {
    data class Heading(val text: String) : DocBlock
    data class Bullet(val text: String) : DocBlock
    data class Paragraph(val text: String) : DocBlock
}

/**
 * Reads the little Markdown the repo's docs use (`#` and `##` headings, `-` bullets, `**bold**`), so the privacy
 * policy shown in the app is the same file published on the web. The `#` title is left out; the screen shows its own.
 */
fun parseDoc(text: String): List<DocBlock> {
    val blocks = mutableListOf<DocBlock>()
    val paragraph = StringBuilder()
    fun flush() {
        if (paragraph.isNotBlank()) blocks += DocBlock.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }
    for (raw in text.lines()) {
        val line = raw.trim()
        when {
            line.isEmpty() -> flush()
            line.startsWith("# ") -> flush()
            line.startsWith("## ") || line.startsWith("### ") -> { flush(); blocks += DocBlock.Heading(line.trimStart('#').trim()) }
            line.startsWith("- ") -> { flush(); blocks += DocBlock.Bullet(line.removePrefix("- ")) }
            else -> paragraph.append(if (paragraph.isEmpty()) line else " $line")
        }
    }
    flush()
    return blocks
}

/** `**bold**` as bold, everything else as written. */
private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part -> if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part) }
}

/** A read-only page of text with a back button, for the privacy policy and the font licences (#84). */
@Composable
fun DocumentScreen(title: String, blocks: List<DocBlock>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        LazyColumn(
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            items(blocks) { block ->
                when (block) {
                    is DocBlock.Heading -> Text(
                        block.text,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                    )
                    is DocBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text("•", style = MaterialTheme.typography.bodyLarge)
                        Text(inline(block.text), style = MaterialTheme.typography.bodyLarge)
                    }
                    is DocBlock.Paragraph -> Text(inline(block.text), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
