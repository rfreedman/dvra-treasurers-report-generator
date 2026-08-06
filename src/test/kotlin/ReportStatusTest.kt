import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReportStatusTest {

    @Test
    fun `done is terminal Done message`() {
        assertEquals(ReportStatus.Message.Done, ReportStatus.parse(ReportStatus.DONE))
        assertEquals(ReportStatus.Message.Done, ReportStatus.parse("Done!"))
    }

    @Test
    fun `failed prefix yields Failed with remainder`() {
        val parsed = ReportStatus.parse(ReportStatus.failed("Balances don't reconcile"))
        assertEquals(
            ReportStatus.Message.Failed("Balances don't reconcile"),
            parsed
        )
    }

    @Test
    fun `failed is a single payload not a separate Failed token`() {
        val payload = ReportStatus.failed("boom")
        assertTrue(payload.startsWith(ReportStatus.FAILED_PREFIX))
        assertEquals("Failed!:boom", payload)
        // Dual-send of "boom" then "Failed!" used to drop the message under CONFLATED.
        assertEquals(ReportStatus.Message.Failed("boom"), ReportStatus.parse(payload))
        assertEquals(
            ReportStatus.Message.Progress("Failed!"),
            ReportStatus.parse("Failed!")
        )
    }

    @Test
    fun `other strings are progress`() {
        assertEquals(
            ReportStatus.Message.Progress("Reading CSV File"),
            ReportStatus.parse("Reading CSV File")
        )
    }
}
