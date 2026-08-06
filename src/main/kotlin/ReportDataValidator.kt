import java.math.BigDecimal
import java.time.YearMonth

/**
 * Sense-checks parsed report data before a PDF is written.
 * Returns an error message, or null when the data is consistent.
 */
object ReportDataValidator {

    fun validate(
        transactions: List<Transaction>,
        startingBalance: BigDecimal,
        endingBalance: BigDecimal,
        netTotal: BigDecimal
    ): String? {
        if (transactions.isEmpty()) {
            return "No transactions found in CSV"
        }

        val months = transactions.map { YearMonth.from(it.transactionDate) }.toSortedSet()
        if (months.size > 1) {
            return "CSV spans multiple months: ${months.joinToString(", ")}"
        }

        val amountSum = transactions.fold(BigDecimal.ZERO) { acc, txn -> acc.add(txn.amount) }
        if (amountSum.compareTo(netTotal) != 0) {
            return "Net total doesn't match sum of transactions: net = $netTotal, sum = $amountSum"
        }

        val expectedEnding = startingBalance.add(netTotal)
        if (expectedEnding.compareTo(endingBalance) != 0) {
            val diff = endingBalance.subtract(expectedEnding)
            return "Balances don't reconcile: start + net = $expectedEnding, but ending = $endingBalance (diff $diff)"
        }

        return null
    }
}
