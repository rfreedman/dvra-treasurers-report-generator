import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ReportDataValidatorTest {

    private fun txn(
        date: LocalDate,
        amount: String = "10.00",
        category: String = "Dues"
    ) = Transaction(
        transactionDate = date,
        payee = "Payee",
        category = category,
        subCategory = "Other",
        amount = BigDecimal(amount),
        account = "Checking",
        notes = ""
    )

    @Test
    fun `empty transactions fail`() {
        val error = ReportDataValidator.validate(
            transactions = emptyList(),
            startingBalance = BigDecimal("100.00"),
            endingBalance = BigDecimal("100.00"),
            netTotal = BigDecimal.ZERO
        )
        assertEquals("No transactions found in CSV", error)
    }

    @Test
    fun `single month with matching balances passes`() {
        val transactions = listOf(
            txn(LocalDate.of(2026, 3, 1), "50.00"),
            txn(LocalDate.of(2026, 3, 15), "-20.00")
        )
        val error = ReportDataValidator.validate(
            transactions = transactions,
            startingBalance = BigDecimal("100.00"),
            endingBalance = BigDecimal("130.00"),
            netTotal = BigDecimal("30.00")
        )
        assertNull(error)
    }

    @Test
    fun `balance mismatch fails with diff`() {
        val transactions = listOf(txn(LocalDate.of(2026, 3, 1), "10.00"))
        val error = ReportDataValidator.validate(
            transactions = transactions,
            startingBalance = BigDecimal("100.00"),
            endingBalance = BigDecimal("111.00"),
            netTotal = BigDecimal("10.00")
        )
        assertNotNull(error)
        assertTrue(error!!.contains("Balances don't reconcile"))
        assertTrue(error.contains("diff 1.00"))
    }

    @Test
    fun `multiple months fail`() {
        val transactions = listOf(
            txn(LocalDate.of(2026, 3, 1)),
            txn(LocalDate.of(2026, 4, 1))
        )
        val error = ReportDataValidator.validate(
            transactions = transactions,
            startingBalance = BigDecimal("100.00"),
            endingBalance = BigDecimal("120.00"),
            netTotal = BigDecimal("20.00")
        )
        assertNotNull(error)
        assertTrue(error!!.startsWith("CSV spans multiple months:"))
        assertTrue(error.contains("2026-03"))
        assertTrue(error.contains("2026-04"))
    }

    @Test
    fun `net total mismatch vs transaction sum fails`() {
        val transactions = listOf(
            txn(LocalDate.of(2026, 3, 1), "50.00"),
            txn(LocalDate.of(2026, 3, 15), "-20.00")
        )
        val error = ReportDataValidator.validate(
            transactions = transactions,
            startingBalance = BigDecimal("100.00"),
            endingBalance = BigDecimal("125.00"),
            netTotal = BigDecimal("25.00")
        )
        assertNotNull(error)
        assertTrue(error!!.contains("Net total doesn't match sum of transactions"))
        assertTrue(error.contains("net = 25.00"))
        assertTrue(error.contains("sum = 30.00"))
    }
}
