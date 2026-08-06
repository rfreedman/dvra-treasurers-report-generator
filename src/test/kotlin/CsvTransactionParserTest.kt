import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class CsvTransactionParserTest {

    /**
     * Build a Quicken-like row with enough columns for [CsvTransactionParser].
     * Indices: 0..2 empty/prefix, 3 date, 5 payee, 6 category, 9 amount, 10 account, 11 notes.
     */
    private fun row(
        date: String = "03/15/2026",
        payee: String = "ACME",
        category: String = "Dues:Membership",
        amount: String = "25.00",
        account: String = "Checking",
        notes: String = "",
        col0: String = "",
        columnCount: Int = 12
    ): List<String> {
        val cells = MutableList(columnCount) { "" }
        if (columnCount > 0) cells[0] = col0
        if (columnCount > CsvTransactionParser.COL_DATE) cells[CsvTransactionParser.COL_DATE] = date
        if (columnCount > CsvTransactionParser.COL_PAYEE) cells[CsvTransactionParser.COL_PAYEE] = payee
        if (columnCount > CsvTransactionParser.COL_CATEGORY) cells[CsvTransactionParser.COL_CATEGORY] = category
        if (columnCount > CsvTransactionParser.COL_AMOUNT) cells[CsvTransactionParser.COL_AMOUNT] = amount
        if (columnCount > CsvTransactionParser.COL_ACCOUNT) cells[CsvTransactionParser.COL_ACCOUNT] = account
        if (columnCount > CsvTransactionParser.COL_NOTES) cells[CsvTransactionParser.COL_NOTES] = notes
        return cells
    }

    @Test
    fun `valid row produces transaction`() {
        val result = CsvTransactionParser.parseRows(listOf(row()))
        assertEquals(0, result.errors.size)
        assertEquals(1, result.transactions.size)
        val txn = result.transactions[0]
        assertEquals(LocalDate.of(2026, 3, 15), txn.transactionDate)
        assertEquals("ACME", txn.payee)
        assertEquals("Dues", txn.category)
        assertEquals("Membership", txn.subCategory)
        assertEquals(BigDecimal("25.00"), txn.amount)
    }

    @Test
    fun `transfer rows are skipped`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(category = "Transfer:Savings"))
        )
        assertTrue(result.transactions.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `total summary rows are skipped`() {
        val result = CsvTransactionParser.parseRows(
            listOf(
                row(col0 = "Total Inflows:"),
                row(col0 = "Total Outflows:"),
                row(col0 = "Net Total:")
            )
        )
        assertTrue(result.transactions.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `short row errors`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(columnCount = 8))
        )
        assertTrue(result.transactions.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("expected at least 12 columns"))
    }

    @Test
    fun `blank category errors`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(category = "  "))
        )
        assertTrue(result.transactions.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("blank category"))
    }

    @Test
    fun `invalid amount errors`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(amount = "not-a-number"))
        )
        assertTrue(result.transactions.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("invalid amount"))
    }

    @Test
    fun `amount with more than two decimals errors`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(amount = "10.123"))
        )
        assertTrue(result.transactions.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("more than 2 decimal places"))
    }

    @Test
    fun `invalid calendar date errors`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(date = "02/31/2026"))
        )
        assertTrue(result.transactions.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("invalid date"))
    }

    @Test
    fun `amount with commas is accepted`() {
        val result = CsvTransactionParser.parseRows(
            listOf(row(amount = "1,234.56"))
        )
        assertEquals(0, result.errors.size)
        assertEquals(BigDecimal("1234.56"), result.transactions[0].amount)
    }
}
