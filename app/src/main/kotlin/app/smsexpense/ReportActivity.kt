package app.smsexpense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.smsexpense.ui.ReportScreen
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch

/** Builds the problem report on the phone and offers it to the share sheet (#100). */
class ReportActivity : ComponentActivity() {
    private var report by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        lifecycleScope.launch { report = ProblemReport.build(app) }
        setContent {
            ExpenseTheme {
                Surface(Modifier.fillMaxSize()) {
                    ReportScreen(
                        report = report,
                        onShare = {
                            report?.let {
                                startActivity(Intent.createChooser(ProblemReport.shareIntent(it, ProblemReport.version(this)), "Send report"))
                            }
                        },
                        onBack = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context) = Intent(context, ReportActivity::class.java)
    }
}
