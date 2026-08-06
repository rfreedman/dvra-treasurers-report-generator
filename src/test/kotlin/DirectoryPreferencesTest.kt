import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DirectoryPreferencesTest {

    @TempDir
    lateinit var tempDir: File

    private fun configFile(): File = File(tempDir, ".dvratrg")

    @Test
    fun `save and load csv and pdf directories independently`() {
        val csvDir = File(tempDir, "csv-exports").also { assertTrue(it.mkdir()) }
        val pdfDir = File(tempDir, "pdf-out").also { assertTrue(it.mkdir()) }
        val file = configFile()

        file.writeText("author=Test Author\n")

        saveLastDirectory(DirectoryPrefs.KEY_CSV, csvDir, configFile = file)
        saveLastDirectory(DirectoryPrefs.KEY_PDF, pdfDir, configFile = file)

        assertEquals(csvDir.absolutePath, loadLastDirectory(DirectoryPrefs.KEY_CSV, file)!!.absolutePath)
        assertEquals(pdfDir.absolutePath, loadLastDirectory(DirectoryPrefs.KEY_PDF, file)!!.absolutePath)

        val props = loadConfigProperties(file)
        assertEquals("Test Author", props.getProperty("author"))
    }

    @Test
    fun `saving csv does not clear pdf key`() {
        val csvDir = File(tempDir, "csv1").also { assertTrue(it.mkdir()) }
        val pdfDir = File(tempDir, "pdf1").also { assertTrue(it.mkdir()) }
        val csvDir2 = File(tempDir, "csv2").also { assertTrue(it.mkdir()) }
        val file = configFile()

        saveLastDirectory(DirectoryPrefs.KEY_CSV, csvDir, configFile = file)
        saveLastDirectory(DirectoryPrefs.KEY_PDF, pdfDir, configFile = file)
        saveLastDirectory(DirectoryPrefs.KEY_CSV, csvDir2, configFile = file)

        assertEquals(csvDir2.absolutePath, loadLastDirectory(DirectoryPrefs.KEY_CSV, file)!!.absolutePath)
        assertEquals(pdfDir.absolutePath, loadLastDirectory(DirectoryPrefs.KEY_PDF, file)!!.absolutePath)
    }

    @Test
    fun `missing key returns null`() {
        assertNull(loadLastDirectory(DirectoryPrefs.KEY_CSV, configFile()))
    }

    @Test
    fun `invalid path returns null on load`() {
        val file = configFile()
        saveMergedProperties(
            mapOf(DirectoryPrefs.KEY_CSV to File(tempDir, "gone").absolutePath),
            configFile = file
        )
        assertNull(loadLastDirectory(DirectoryPrefs.KEY_CSV, file))
    }

    @Test
    fun `non directory is ignored on save and null on load`() {
        val file = configFile()
        val notDir = File(tempDir, "file.txt").also { it.writeText("x") }

        saveLastDirectory(DirectoryPrefs.KEY_PDF, notDir, configFile = file)
        assertNull(loadConfigProperties(file).getProperty(DirectoryPrefs.KEY_PDF))

        saveMergedProperties(
            mapOf(DirectoryPrefs.KEY_PDF to notDir.absolutePath),
            configFile = file
        )
        assertNull(loadLastDirectory(DirectoryPrefs.KEY_PDF, file))
    }
}
