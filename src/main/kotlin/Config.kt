import java.util.*

data class Config(
    val signature: String, // Treasurer's Name and Callsign for signature
    val pandocPath: String, // path to the "pandoc" executable
    val xelatexDir: String  // path to the xelatex directory, e.g. /Library/TeX/texbin on MAC OS
) {
}

object ConfigUtil {
    fun envPathSeparator(): String {
        val os = System.getProperty("os.name", "unknown").lowercase(Locale.ROOT)
        if (os.contains("win")) {
            return ";"
        }
        return ":"
    }
}