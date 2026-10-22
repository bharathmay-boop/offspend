package app.smsexpense

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

// #215: the wrapper only runs a Gradle download whose checksum matches, so a swapped zip can't build and sign the app.
class WrapperChecksumTest {
    @Test fun theGradleDownloadIsPinnedToItsChecksum() {
        // Gradle runs tests in app/, an IDE may run them from the repo root.
        val file = listOf("../gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties").map(::File).first { it.exists() }
        val props = Properties().apply { file.inputStream().use(::load) }
        val sum = props.getProperty("distributionSha256Sum").orEmpty()
        // Lowercase only: the wrapper compares it with its own lowercase hash, so an uppercase copy would fail the download.
        assertTrue("distributionSha256Sum must be the 64-character lowercase SHA-256 of the Gradle zip, was '$sum'", Regex("[0-9a-f]{64}").matches(sum))
    }
}
