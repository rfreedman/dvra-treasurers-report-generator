import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppBuildInfoTest {

    @Test
    fun `loads version and build date from generated properties`() {
        assertFalse(AppBuildInfo.version.isBlank())
        assertFalse(AppBuildInfo.buildDate.isBlank())
        assertTrue(AppBuildInfo.displayLabel().contains(AppBuildInfo.version))
        assertTrue(AppBuildInfo.displayLabel().contains(AppBuildInfo.buildDate))
    }
}
