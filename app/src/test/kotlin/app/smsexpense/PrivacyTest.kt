package app.smsexpense

import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Properties

@RunWith(RobolectricTestRunner::class)
class PrivacyTest {
    @Test fun theMoneyDatabaseIsNotIncludedInAndroidBackups() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        val flags = app.packageManager.getApplicationInfo(app.packageName, 0).flags
        assertEquals("allowBackup must be off: the app promises nothing leaves the phone", 0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    // #186: from Android 12, allowBackup="false" no longer stops "copy apps and data" to a new phone.
    @Test fun theMoneyDatabaseIsNotCopiedToANewPhone() {
        val app = ApplicationProvider.getApplicationContext<ExpenseApp>()
        // Robolectric doesn't load this attribute, so read it from the merged manifest the build hands to the tests.
        val config = Properties().apply { PrivacyTest::class.java.getResourceAsStream("/com/android/tools/test_config.properties")!!.use(::load) }
        val manifest = File(config.getProperty("android_merged_manifest")).readText()
        val name = Regex("android:dataExtractionRules=\"@xml/([a-z_]+)\"").find(manifest)?.groupValues?.get(1)
        val rules = name?.let { app.resources.getIdentifier(it, "xml", app.packageName) } ?: 0
        assertNotEquals("dataExtractionRules must be set", 0, rules)
        val excluded = mutableMapOf<String, MutableSet<String>>()
        val xml = app.resources.getXml(rules)
        var section = ""
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType != XmlPullParser.START_TAG) continue
            when (xml.name) {
                "cloud-backup", "device-transfer" -> section = xml.name
                "exclude" -> excluded.getOrPut(section) { mutableSetOf() } += xml.getAttributeValue(null, "domain")
            }
        }
        for (s in listOf("cloud-backup", "device-transfer")) {
            assertTrue("$s must exclude the database and settings", excluded[s].orEmpty().containsAll(listOf("database", "sharedpref")))
        }
    }

    // #105: the Play upload key and its passwords must never reach git.
    @Test fun theUploadKeyCanNotBeCommitted() {
        for (path in listOf("upload.jks", "app/upload.keystore", "keystore.properties", ".env")) {
            val ignored = ProcessBuilder("git", "check-ignore", "-q", "--no-index", path).start().waitFor() == 0
            assertTrue("$path must be gitignored", ignored)
        }
    }

    // #105: GitHub Pages publishes the privacy policy from docs/. The file has no front matter (the app ships the same file),
    // so the plugin that renders it must stay on, and the project files next to it must stay off the site.
    @Test fun onlyThePrivacyPolicyIsPublishedFromDocs() {
        val config = File("../docs/_config.yml").readText()
        assertTrue("the policy would not be rendered", config.contains("jekyll-optional-front-matter"))
        for (internal in listOf("PLAY_RELEASE.md", "TESTING.md")) {
            assertTrue("$internal would be published", config.contains("- $internal"))
        }
    }
}
