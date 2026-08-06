import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

data class Config(
    val author: String, // Treasurer's Name and Callsign for signature
    val pandocPath: String, // path to the "pandoc" executable
    val xelatexDir: String  // path to the xelatex directory, e.g. /Library/TeX/texbin on MAC OS
)

sealed class ConfigLoadResult {
    data class Ok(val config: Config) : ConfigLoadResult()
    data class Error(val message: String) : ConfigLoadResult()
}

object ConfigUtil {
    fun envPathSeparator(): String {
        val os = System.getProperty("os.name", "unknown").lowercase(Locale.ROOT)
        if (os.contains("win")) {
            return ";"
        }
        return ":"
    }

    fun defaultConfigFile(): File =
        File(System.getProperty("user.home"), ".dvratrg")
}

/**
 * Load and validate a `.dvratrg` properties file: required keys present,
 * pandoc executable exists, xelatexDir is an existing directory.
 */
fun loadValidatedConfig(
    configFile: File = ConfigUtil.defaultConfigFile()
): ConfigLoadResult {
    if (!configFile.exists()) {
        return ConfigLoadResult.Error("Config file not found: ${configFile.path}")
    }

    val prop = Properties()
    try {
        FileInputStream(configFile).use { prop.load(it) }
    } catch (ex: Exception) {
        return ConfigLoadResult.Error("Failed to read config file: ${ex.message}")
    }

    val author = prop.getProperty("author")?.trim().orEmpty()
    if (author.isEmpty()) {
        return ConfigLoadResult.Error("Config missing required 'author' entry")
    }

    val pandocPath = prop.getProperty("pandoc")?.trim().orEmpty()
    if (pandocPath.isEmpty()) {
        return ConfigLoadResult.Error("Config missing required 'pandoc' entry")
    }
    val pandocFile = File(pandocPath)
    if (!pandocFile.isFile || !pandocFile.canExecute()) {
        return ConfigLoadResult.Error("pandoc executable not found or not executable: $pandocPath")
    }

    val xelatexDir = prop.getProperty("xelatexDir")?.trim().orEmpty()
    if (xelatexDir.isEmpty()) {
        return ConfigLoadResult.Error("Config missing required 'xelatexDir' entry")
    }
    val xelatexDirFile = File(xelatexDir)
    if (!xelatexDirFile.isDirectory) {
        return ConfigLoadResult.Error("xelatexDir is not a directory: $xelatexDir")
    }

    return ConfigLoadResult.Ok(
        Config(author = author, pandocPath = pandocPath, xelatexDir = xelatexDir)
    )
}
