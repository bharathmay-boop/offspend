package app.smsexpense

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.smsexpense.ui.DocBlock
import app.smsexpense.ui.parseDoc
import app.smsexpense.ui.theme.DarkScheme
import app.smsexpense.ui.theme.ExpenseTheme
import app.smsexpense.ui.theme.LightScheme
import app.smsexpense.ui.theme.ThemeChoice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

// Look › Theme and the About section in Settings (#84).
@RunWith(RobolectricTestRunner::class)
class ThemeAndAboutTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<ExpenseApp>()

    private fun surfaceShown(): Color {
        var surface = Color.Unspecified
        compose.setContent { ExpenseTheme { surface = MaterialTheme.colorScheme.surface } }
        compose.waitForIdle()
        return surface
    }

    @Test fun darkAppliesEvenWhenThePhoneIsLight() {
        app.setTheme(ThemeChoice.DARK)
        assertEquals(DarkScheme.surface, surfaceShown())
    }

    @Test @Config(qualifiers = "night") fun lightAppliesEvenWhenThePhoneIsDark() {
        app.setTheme(ThemeChoice.LIGHT)
        assertEquals(LightScheme.surface, surfaceShown())
    }

    @Test @Config(qualifiers = "night") fun untilAChoiceIsMadeItFollowsThePhone() {
        assertEquals(DarkScheme.surface, surfaceShown())
    }

    @Test fun pickingAThemeInSettingsAppliesAtOnceAndIsRemembered() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.onNodeWithText("Follow the system").performClick()
            compose.onNodeWithText("Dark").performClick()
            assertEquals(ThemeChoice.DARK, app.theme.value)
            compose.onNodeWithText("Dark").assertExists()
        }
        assertEquals("DARK", app.getSharedPreferences("ui", Context.MODE_PRIVATE).getString("theme", null))
    }

    @Test fun aSavedThemeIsUsedOnTheNextStart() {
        app.getSharedPreferences("ui", Context.MODE_PRIVATE).edit().putString("theme", "LIGHT").commit()
        assertEquals(ThemeChoice.LIGHT, app.theme.value)
    }

    // A tall phone, so the About rows are on screen without scrolling.
    @Test @Config(qualifiers = "w411dp-h891dp") fun aboutShowsTheVersionPolicyAndLicences() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.onNodeWithText("0.1.1 (2)").assertExists()
            compose.onNodeWithText("Open source licences").assertExists()
            compose.onNodeWithText("Privacy policy").performClick()
            assertEquals(DocumentActivity::class.java.name, shadowOf(app).nextStartedActivity?.component?.className)
        }
    }

    @Test fun thePrivacyPolicyIsReadInTheApp() {
        ActivityScenario.launch<DocumentActivity>(DocumentActivity.privacyPolicy(app)).use {
            compose.onNodeWithText("What leaves your phone").assertExists()
        }
    }

    @Test fun theLicencesNameBothFonts() {
        ActivityScenario.launch<DocumentActivity>(DocumentActivity.licences(app)).use {
            compose.onNodeWithText("Bricolage Grotesque").assertExists()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Inter"))
            compose.onNodeWithText("Inter").assertExists()
        }
    }

    // The app carries its own copy so it needs no internet; this keeps it the same as the published files.
    @Test fun theBundledCopiesMatchTheRepoFiles() {
        mapOf(
            "privacy-policy.md" to "../docs/privacy-policy.md",
            "OFL-bricolage.txt" to "../licenses/OFL-bricolage.txt",
            "OFL-inter.txt" to "../licenses/OFL-inter.txt",
        ).forEach { (asset, repo) ->
            val bundled = app.assets.open(asset).bufferedReader().use { it.readText() }
            assertEquals("$asset differs from $repo: copy it again", File(repo).readText(), bundled)
        }
    }

    @Test fun headingsBulletsAndParagraphsAreReadFromTheMarkdown() {
        val doc = "# Privacy policy\n\nIntro line one\nline two.\n\n## What it keeps\n\n- **Text messages**, with permission.\n"
        assertEquals(
            listOf(DocBlock.Paragraph("Intro line one line two."), DocBlock.Heading("What it keeps"), DocBlock.Bullet("**Text messages**, with permission.")),
            parseDoc(doc),
        )
    }
}
