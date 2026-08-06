import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigLoaderTest {

    @TempDir
    lateinit var tempDir: File

    private fun writeConfig(body: String): File {
        val file = File(tempDir, ".dvratrg")
        file.writeText(body)
        return file
    }

    private fun executableStub(name: String): File {
        val file = File(tempDir, name)
        file.writeText("#!/bin/sh\n")
        assertTrue(file.setExecutable(true), "failed to mark $name executable")
        return file
    }

    @Test
    fun `missing file fails`() {
        val missing = File(tempDir, "no-such-config")
        val result = loadValidatedConfig(missing)
        assertTrue(result is ConfigLoadResult.Error)
        assertTrue((result as ConfigLoadResult.Error).message.contains("Config file not found"))
    }

    @Test
    fun `missing author fails`() {
        val pandoc = executableStub("pandoc")
        val xelatex = File(tempDir, "texbin").also { assertTrue(it.mkdir()) }
        val result = loadValidatedConfig(
            writeConfig(
                """
                pandoc=${pandoc.path}
                xelatexDir=${xelatex.path}
                """.trimIndent()
            )
        )
        assertTrue(result is ConfigLoadResult.Error)
        assertEquals(
            "Config missing required 'author' entry",
            (result as ConfigLoadResult.Error).message
        )
    }

    @Test
    fun `non executable pandoc fails`() {
        val pandoc = File(tempDir, "pandoc").also { it.writeText("not executable") }
        val xelatex = File(tempDir, "texbin").also { assertTrue(it.mkdir()) }
        val result = loadValidatedConfig(
            writeConfig(
                """
                author=Test Author
                pandoc=${pandoc.path}
                xelatexDir=${xelatex.path}
                """.trimIndent()
            )
        )
        assertTrue(result is ConfigLoadResult.Error)
        assertTrue((result as ConfigLoadResult.Error).message.contains("pandoc executable not found"))
    }

    @Test
    fun `xelatexDir must be a directory`() {
        val pandoc = executableStub("pandoc")
        val notDir = File(tempDir, "not-a-dir").also { it.writeText("x") }
        val result = loadValidatedConfig(
            writeConfig(
                """
                author=Test Author
                pandoc=${pandoc.path}
                xelatexDir=${notDir.path}
                """.trimIndent()
            )
        )
        assertTrue(result is ConfigLoadResult.Error)
        assertTrue((result as ConfigLoadResult.Error).message.contains("xelatexDir is not a directory"))
    }

    @Test
    fun `valid config loads`() {
        val pandoc = executableStub("pandoc")
        val xelatex = File(tempDir, "texbin").also { assertTrue(it.mkdir()) }
        val result = loadValidatedConfig(
            writeConfig(
                """
                author=Jane Doe W2ZQ
                pandoc=${pandoc.path}
                xelatexDir=${xelatex.path}
                """.trimIndent()
            )
        )
        assertTrue(result is ConfigLoadResult.Ok)
        val config = (result as ConfigLoadResult.Ok).config
        assertEquals("Jane Doe W2ZQ", config.author)
        assertEquals(pandoc.path, config.pandocPath)
        assertEquals(xelatex.path, config.xelatexDir)
    }
}
