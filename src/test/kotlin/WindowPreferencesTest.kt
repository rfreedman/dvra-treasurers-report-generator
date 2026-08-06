import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Rectangle
import java.io.File

class WindowPreferencesTest {

    @TempDir
    lateinit var tempDir: File

    private val singleScreen = listOf(Rectangle(0, 0, 1920, 1080))

    @Test
    fun `clamp keeps window fully on screen`() {
        val clamped = clampWindowPlacement(
            WindowPlacement(x = 5000f, y = 5000f, width = 800f, height = 600f),
            screens = singleScreen
        )
        assertTrue(clamped.x >= 0f)
        assertTrue(clamped.y >= 0f)
        assertTrue(clamped.x + clamped.width <= 1920f + 0.01f)
        assertTrue(clamped.y + clamped.height <= 1080f + 0.01f)
        assertEquals(800f, clamped.width)
        assertEquals(600f, clamped.height)
    }

    @Test
    fun `clamp shrinks oversized window to screen`() {
        val clamped = clampWindowPlacement(
            WindowPlacement(x = 0f, y = 0f, width = 5000f, height = 4000f),
            screens = singleScreen
        )
        assertEquals(1920f, clamped.width)
        assertEquals(1080f, clamped.height)
        assertEquals(0f, clamped.x)
        assertEquals(0f, clamped.y)
    }

    @Test
    fun `clamp prefers screen containing window center`() {
        val screens = listOf(
            Rectangle(0, 0, 1920, 1080),
            Rectangle(1920, 0, 1280, 800)
        )
        val clamped = clampWindowPlacement(
            WindowPlacement(x = 2000f, y = 100f, width = 600f, height = 500f),
            screens = screens
        )
        assertTrue(clamped.x >= 1920f)
        assertTrue(clamped.x + clamped.width <= 1920f + 1280f + 0.01f)
        assertTrue(clamped.y + clamped.height <= 800f + 0.01f)
    }

    @Test
    fun `save and load round trip preserves values and other keys`() {
        val file = File(tempDir, ".dvratrg")
        file.writeText("author=Test Author\npandoc=/bin/sh\n")

        saveWindowPlacement(
            WindowPlacement(x = 12.5f, y = 40f, width = 820f, height = 900f),
            configFile = file
        )

        val loaded = loadWindowPlacement(file)
        assertEquals(12.5f, loaded.x)
        assertEquals(40f, loaded.y)
        assertEquals(820f, loaded.width)
        assertEquals(900f, loaded.height)

        val text = file.readText()
        assertTrue(text.contains("author=Test Author") || text.contains("author"))
        // Properties may escape or reorder; ensure author survived via Properties load
        val props = java.util.Properties()
        file.inputStream().use { props.load(it) }
        assertEquals("Test Author", props.getProperty("author"))
    }

    @Test
    fun `missing file uses defaults`() {
        val missing = File(tempDir, "nope")
        assertEquals(WindowPlacement.DEFAULT, loadWindowPlacement(missing))
    }
}
