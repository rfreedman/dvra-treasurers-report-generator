import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TransactionCategorizerTest {

    private fun txn(
        category: String,
        subCategory: String,
        amount: String,
        day: Int = 1
    ) = Transaction(
        transactionDate = LocalDate.of(2026, 3, day),
        payee = "Payee",
        category = category,
        subCategory = subCategory,
        amount = BigDecimal(amount),
        account = "Checking",
        notes = ""
    )

    @Test
    fun `credits and debits are totaled separately`() {
        val report = TransactionCategorizer.categorize(
            listOf(
                txn("Dues", "Regular", "50.00"),
                txn("Dues", "Regular", "25.00"),
                txn("Utilities", "Electric", "-30.00"),
                txn("Utilities", "Water", "-10.00")
            )
        )

        assertEquals(BigDecimal("75.00"), report.totalInflows)
        assertEquals(BigDecimal("-40.00"), report.totalOutflows)
        assertEquals(BigDecimal("35.00"), report.netTotal)

        assertEquals(BigDecimal("75.00"), report.creditCategories["Dues"]!!.total)
        assertEquals(BigDecimal("75.00"), report.creditCategories["Dues"]!!.subcategories["Regular"]!!.total)
        assertEquals(BigDecimal("-40.00"), report.debitCategories["Utilities"]!!.total)
        assertEquals(2, report.debitCategories["Utilities"]!!.subcategories.size)
    }

    @Test
    fun `preserves first-seen category insertion order`() {
        val report = TransactionCategorizer.categorize(
            listOf(
                txn("Zebra", "A", "1.00"),
                txn("Alpha", "B", "2.00"),
                txn("Zebra", "C", "3.00")
            )
        )

        assertEquals(listOf("Zebra", "Alpha"), report.creditCategories.keys.toList())
        assertEquals(
            listOf("A", "C"),
            report.creditCategories["Zebra"]!!.subcategories.keys.toList()
        )
    }

    @Test
    fun `empty list yields zero totals`() {
        val report = TransactionCategorizer.categorize(emptyList())
        assertEquals(BigDecimal.ZERO, report.totalInflows)
        assertEquals(BigDecimal.ZERO, report.totalOutflows)
        assertEquals(BigDecimal.ZERO, report.netTotal)
        assertTrue(report.creditCategories.isEmpty())
        assertTrue(report.debitCategories.isEmpty())
    }
}
