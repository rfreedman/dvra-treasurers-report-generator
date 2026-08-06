import java.math.BigDecimal
import java.time.LocalDate

data class CsvParseResult(
    val transactions: List<Transaction>,
    val errors: List<String>
)

/**
 * Parses Quicken-style CSV rows into [Transaction]s, collecting recoverable row errors
 * instead of throwing.
 */
object CsvTransactionParser {

    const val COL_DATE = 3
    const val COL_PAYEE = 5
    const val COL_CATEGORY = 6
    const val COL_AMOUNT = 9
    const val COL_ACCOUNT = 10
    const val COL_NOTES = 11

    fun parseRows(rows: List<List<String>>): CsvParseResult {
        val transactions = ArrayList<Transaction>()
        val errors = ArrayList<String>()

        rows.forEachIndexed { index, row ->
            val rowNumber = index + 1
            when (val outcome = parseRow(row, rowNumber)) {
                is RowOutcome.Skip -> Unit
                is RowOutcome.Error -> errors.add(outcome.message)
                is RowOutcome.Ok -> transactions.add(outcome.transaction)
            }
        }

        return CsvParseResult(transactions, errors)
    }

    private sealed class RowOutcome {
        data object Skip : RowOutcome()
        data class Error(val message: String) : RowOutcome()
        data class Ok(val transaction: Transaction) : RowOutcome()
    }

    private fun parseRow(row: List<String>, rowNumber: Int): RowOutcome {
        if (row.isNotEmpty() && row[0].contains("Total Inflows:")) {
            return RowOutcome.Skip
        }
        if (row.isNotEmpty() && row[0].contains("Total Outflows:")) {
            return RowOutcome.Skip
        }
        if (row.isNotEmpty() && row[0].contains("Net Total:")) {
            return RowOutcome.Skip
        }
        if (row.size > COL_CATEGORY && row[COL_CATEGORY].contains("Transfer:")) {
            return RowOutcome.Skip
        }

        if (row.size <= COL_DATE || !isDate(row[COL_DATE])) {
            return RowOutcome.Skip
        }

        if (row.size <= COL_NOTES) {
            return RowOutcome.Error(
                "row $rowNumber: expected at least ${COL_NOTES + 1} columns, found ${row.size}"
            )
        }

        val categoryParts = row[COL_CATEGORY].split(":")
        val category = categoryParts[0].trim()
        if (category.isEmpty()) {
            return RowOutcome.Error("row $rowNumber: blank category")
        }

        val amount = try {
            BigDecimal(row[COL_AMOUNT].trim().replace(",", ""))
        } catch (ex: NumberFormatException) {
            return RowOutcome.Error("row $rowNumber: invalid amount '${row[COL_AMOUNT]}'")
        }

        if (amount.scale() > 2) {
            return RowOutcome.Error(
                "row $rowNumber: amount has more than 2 decimal places ($amount)"
            )
        }

        val transactionDate = try {
            parseDate(row[COL_DATE].trim())
        } catch (ex: Exception) {
            return RowOutcome.Error("row $rowNumber: invalid date '${row[COL_DATE]}'")
        }

        return RowOutcome.Ok(
            Transaction(
                transactionDate,
                row[COL_PAYEE].trim(),
                category,
                if (categoryParts.size > 1) categoryParts[1].trim() else "Other",
                amount,
                row[COL_ACCOUNT].trim(),
                row[COL_NOTES].trim()
            )
        )
    }

    fun isDate(str: String): Boolean {
        if (str.length !in 8..10) {
            return false
        }
        val dateParts = str.split("/".toRegex())

        if (dateParts.size != 3) {
            return false
        }

        if (dateParts[0].isEmpty() || dateParts[0].length > 2) {
            return false
        }

        if (dateParts[1].isEmpty() || dateParts[1].length > 2) {
            return false
        }

        return dateParts[2].length == 4
    }

    fun parseDate(str: String): LocalDate {
        val dateParts = str.split("/".toRegex())
        return LocalDate.of(dateParts[2].toInt(), dateParts[0].toInt(), dateParts[1].toInt())
    }
}
