import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkdownEscapingTest {

    @Test
    fun `table cell escapes pipes and newlines`() {
        assertEquals("a\\|b", MarkdownEscaping.escapeTableCell("a|b"))
        assertEquals("a b", MarkdownEscaping.escapeTableCell("a\nb"))
        assertEquals("plain", MarkdownEscaping.escapeTableCell("plain"))
    }

    @Test
    fun `yaml scalar quotes when needed`() {
        assertEquals("Alice", MarkdownEscaping.escapeYamlScalar("Alice"))
        assertEquals("\"Jane Doe\"", MarkdownEscaping.escapeYamlScalar("Jane Doe"))
        assertEquals("\"Call: W2ZQ\"", MarkdownEscaping.escapeYamlScalar("Call: W2ZQ"))
        assertEquals("\"Say \\\"hi\\\"\"", MarkdownEscaping.escapeYamlScalar("Say \"hi\""))
    }
}
