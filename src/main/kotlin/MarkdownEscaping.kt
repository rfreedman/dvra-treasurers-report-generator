/**
 * Escapes values written into intermediate Markdown / YAML.
 */
object MarkdownEscaping {

    /** Escape pipe characters so table cells do not break column boundaries. */
    fun escapeTableCell(value: String): String =
        value.replace("|", "\\|").replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')

    /**
     * Quote a YAML scalar when it contains characters that would change parsing.
     * Always safe for plain author names without special characters.
     */
    fun escapeYamlScalar(value: String): String {
        val needsQuotes = value.isEmpty() ||
            value.any { it.isWhitespace() || it in ":#\"'{}[]|>&*!%@`" } ||
            value.startsWith('-') ||
            value.startsWith('?')

        if (!needsQuotes) {
            return value
        }

        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "\"$escaped\""
    }
}
