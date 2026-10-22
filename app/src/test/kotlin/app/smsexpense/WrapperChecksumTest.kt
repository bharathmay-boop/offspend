package app.smsexpense

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

// #215: the wrapper only runs a Gradle download whose checksum matches, so a swapped zip can't build and sign the app.
class WrapperChecksumTest {
    @Test fun theGradleDownloadIsPinnedToItsChecksum() {
        // Tests run in app/, so the wrapper settings are one level up.
        val props = Properties().apply { File("../gradle/wrapper/gradle-wrapper.properties").inputStream().use(::load) }
        val sum = props.getProperty("distributionSha256Sum").orEmpty()
        assertTrue("distributionSha256Sum must be the 64-character SHA-256 of the Gradle zip, was '$sum'", Regex("[0-9a-f]{64}").matches(sum))
    }
}
