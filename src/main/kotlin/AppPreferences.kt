import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object DirectoryPrefs {
    const val KEY_CSV = "lastCsvDirectory"
    const val KEY_PDF = "lastPdfDirectory"
}

/**
 * Load existing `~/.dvratrg` properties (empty if missing/unreadable).
 */
fun loadConfigProperties(configFile: File = ConfigUtil.defaultConfigFile()): Properties {
    val props = Properties()
    if (!configFile.exists()) {
        return props
    }
    try {
        FileInputStream(configFile).use { props.load(it) }
    } catch (_: Exception) {
        // return whatever was loaded / empty
    }
    return props
}

/**
 * Merge [updates] into [configFile], preserving other properties.
 */
fun saveMergedProperties(
    updates: Map<String, String>,
    configFile: File = ConfigUtil.defaultConfigFile()
) {
    val props = loadConfigProperties(configFile)
    updates.forEach { (key, value) -> props.setProperty(key, value) }
    configFile.parentFile?.mkdirs()
    FileOutputStream(configFile).use { out ->
        props.store(out, "DVRA Treasurer's Report Generator")
    }
}

/**
 * Return the stored directory for [key] only if it still exists as a directory.
 */
fun loadLastDirectory(
    key: String,
    configFile: File = ConfigUtil.defaultConfigFile()
): File? {
    val raw = loadConfigProperties(configFile).getProperty(key)?.trim().orEmpty()
    if (raw.isEmpty()) {
        return null
    }
    val dir = File(raw)
    return if (dir.isDirectory) dir else null
}

/**
 * Persist [directory] under [key] as an absolute path. No-op if not a directory.
 */
fun saveLastDirectory(
    key: String,
    directory: File,
    configFile: File = ConfigUtil.defaultConfigFile()
) {
    if (!directory.isDirectory) {
        return
    }
    saveMergedProperties(
        mapOf(key to directory.absolutePath),
        configFile = configFile
    )
}
