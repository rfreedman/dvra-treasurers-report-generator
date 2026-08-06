import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(
    val transactionDate: LocalDate,
    val payee: String,
    val category: String,
    val subCategory: String,
    val amount: BigDecimal,
    val account: String,
    val notes: String
)

data class Category(
    val name: String,
    var total: BigDecimal,
    val subcategories: LinkedHashMap<String, Subcategory>
)

data class Subcategory(
    val name: String,
    var total: BigDecimal,
    val transactions: MutableList<Transaction>
)

data class CategorizedReport(
    val creditCategories: LinkedHashMap<String, Category>,
    val debitCategories: LinkedHashMap<String, Category>,
    val totalInflows: BigDecimal,
    val totalOutflows: BigDecimal,
    val netTotal: BigDecimal
)

/**
 * Groups transactions into credit/debit category trees and computes cash-flow totals.
 */
object TransactionCategorizer {

    fun categorize(transactions: List<Transaction>): CategorizedReport {
        val creditCategories = LinkedHashMap<String, Category>()
        val debitCategories = LinkedHashMap<String, Category>()

        for (transaction in transactions) {
            if (transaction.amount >= BigDecimal.ZERO) {
                addToCategory(creditCategories, transaction)
            } else {
                addToCategory(debitCategories, transaction)
            }
        }

        val totalInflows = sumCategoryTotals(creditCategories)
        val totalOutflows = sumCategoryTotals(debitCategories)
        val netTotal = totalInflows.add(totalOutflows)

        return CategorizedReport(
            creditCategories = creditCategories,
            debitCategories = debitCategories,
            totalInflows = totalInflows,
            totalOutflows = totalOutflows,
            netTotal = netTotal
        )
    }

    private fun sumCategoryTotals(categories: Map<String, Category>): BigDecimal =
        categories.values.fold(BigDecimal.ZERO) { acc, category -> acc.add(category.total) }

    private fun addToCategory(
        categoryMap: LinkedHashMap<String, Category>,
        transaction: Transaction
    ) {
        val category = categoryMap.getOrPut(transaction.category) {
            Category(transaction.category, BigDecimal.ZERO, LinkedHashMap())
        }

        val subcategory = category.subcategories.getOrPut(transaction.subCategory) {
            Subcategory(transaction.subCategory, BigDecimal.ZERO, ArrayList())
        }

        subcategory.transactions.add(transaction)
        subcategory.total = subcategory.total.add(transaction.amount)
        category.total = category.total.add(transaction.amount)
    }
}
