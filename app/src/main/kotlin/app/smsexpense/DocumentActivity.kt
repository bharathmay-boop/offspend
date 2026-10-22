package app.smsexpense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.smsexpense.ui.DocBlock
import app.smsexpense.ui.DocumentScreen
import app.smsexpense.ui.parseDoc
import app.smsexpense.ui.theme.ExpenseTheme

/** Shows text that ships inside the app, so reading it needs no browser and no internet (#84). */
class DocumentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val blocks = when (intent.getStringExtra(EXTRA_DOC)) {
            PRIVACY -> parseDoc(asset("privacy-policy.md"))
            else -> LICENCES.flatMap { (font, file) -> listOf(DocBlock.Heading(font)) + parseDoc(asset(file)) }
        }
        val title = if (intent.getStringExtra(EXTRA_DOC) == PRIVACY) "Privacy policy" else "Open source licences"
        setContent {
            ExpenseTheme {
                Surface(Modifier.fillMaxSize()) { DocumentScreen(title, blocks, onBack = ::finish) }
            }
        }
    }

    private fun asset(name: String) = assets.open(name).bufferedReader().use { it.readText() }

    companion object {
        private const val EXTRA_DOC = "doc"
        private const val PRIVACY = "privacy"
        private const val LICENCES_DOC = "licences"

        /** The bundled fonts and their licence files in assets. */
        private val LICENCES = listOf("Bricolage Grotesque" to "OFL-bricolage.txt", "Inter" to "OFL-inter.txt")

        fun privacyPolicy(context: Context) = Intent(context, DocumentActivity::class.java).putExtra(EXTRA_DOC, PRIVACY)
        fun licences(context: Context) = Intent(context, DocumentActivity::class.java).putExtra(EXTRA_DOC, LICENCES_DOC)
    }
}
