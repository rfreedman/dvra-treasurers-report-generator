import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ReportGeneratorNarrativeTest {

    @Test
    fun `net zero is unchanged`() {
        assertEquals("unchanged", ReportGenerator.netChangeNarrative(BigDecimal.ZERO))
        assertEquals("unchanged", ReportGenerator.netChangeNarrative(BigDecimal("0.00")))
    }

    @Test
    fun `positive net is increase`() {
        assertEquals(
            "a net increase of $30.00",
            ReportGenerator.netChangeNarrative(BigDecimal("30.00"))
        )
    }

    @Test
    fun `negative net is decrease`() {
        assertEquals(
            "a net decrease of $12.50",
            ReportGenerator.netChangeNarrative(BigDecimal("-12.50"))
        )
    }
}
