import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.apache.commons.text.WordUtils
import java.io.File
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.text.Charsets.UTF_8

object ReportGenerator {

    private const val BOM = "\uFEFF"

    private const val COL_DATE = 3
    private const val COL_PAYEE = 5
    private const val COL_CATEGORY = 6
    private const val COL_AMOUNT = 9
    private const val COL_ACCOUNT = 10
    private const val COL_NOTES = 11

    private var startingBalance: BigDecimal? = null
    private var endingBalance: BigDecimal? = null
    private var totalInflows: BigDecimal? = null
    private var totalOutflows: BigDecimal? = null
    private var netTotal: BigDecimal? = null

    private var transactions = ArrayList<Transaction>()
    private var creditCategories = HashMap<String, Category>()
    private var debitCategories = HashMap<String, Category>()

    private var processingRow = AtomicInteger(-1)

    // todo: these were command-line options, with default values, convert them to args or do away with them
    private var createPdf = true
    private var createDocx = false
    private var keepMarkdown = false

    fun generate(startingBal: String, endingBal: String, csvFile: File, pdfFile: File) {
        println("start: $startingBal")
        println("end: $endingBal")
        println("csv: ${csvFile.path}")
        println("output: ${pdfFile.path}")

        this.startingBalance = BigDecimal(startingBal)
        this.endingBalance = BigDecimal(endingBal)


        // get just the lines from the file that are csv data, ignore everything else
        val dataLines: String = extractCsvData(csvFile)

        // read the data lines as CSV
        val csvData : List<List<String>>  = csvReader().readAll(dataLines)

        csvData.forEach { row ->
            processRow(row)
        }

        createCategories()
        calculateTotals()
        writeMarkdown()

        if (createPdf) {
            convertMarkdownToPdf(pdfFile.path)
        }

        if (createDocx) {
            convertMarkdownToDocx()
        }

        if (!keepMarkdown) {
            Files.delete(File("report.md").toPath())
        }
    }

    private fun processRow(row: List<String>) {
        processingRow.getAndIncrement()

        do {
            if (row.size > COL_CATEGORY && row[COL_CATEGORY].contains("Transfer:")) {
                break // ignore transfers
            }

            if (row[0].contains("Total Inflows:")) {
                break // ignore - we'll calculate this
            }

            if (row[0].contains("Total Outflows:")) {
                break // ignore - we'll calculate this
            }

            if (row[0].contains("Net Total:")) {
                break // ignore - we'll calculate this
            }

            if (row.size > COL_DATE && isDate(row[COL_DATE])) {
                val categoryParts = row[COL_CATEGORY].split(":")

                val transaction = Transaction(
                    parseDate(row[COL_DATE].trim()),
                    row[COL_PAYEE].trim(),
                    categoryParts[0].trim(),
                    if (categoryParts.size > 1) categoryParts[1].trim() else "Other",
                    BigDecimal(row[COL_AMOUNT].trim().replace(",".toRegex(), "")),
                    row[COL_ACCOUNT].trim(),
                    row[COL_NOTES].trim()
                )
                transactions.add(transaction)
                break
            }
        } while (false)
    }

    private fun calculateTotals() {
        totalInflows = creditCategories.values.stream().map { category -> category.total }
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO)

        totalOutflows = debitCategories.values.stream().map { category -> category.total }
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO)

        netTotal = if(totalInflows !== null && totalOutflows !== null) {
            totalInflows!!.add(totalOutflows!!)
        } else {
            BigDecimal.ZERO
        }
    }

    private fun createCategories() {
        println("Categorizing Transactions")
        transactions.stream().filter { transaction -> transaction.amount >= BigDecimal.ZERO }
            .forEach { creditTransaction -> categorizeTransaction(creditCategories, creditTransaction) }

        transactions.stream().filter { transaction -> transaction.amount < BigDecimal.ZERO }
            .forEach { debitTransaction -> categorizeTransaction(debitCategories, debitTransaction) }
    }

    private fun categorizeTransaction(categoryMap: HashMap<String, Category>, transaction: Transaction) {
        if (!categoryMap.containsKey(transaction.category)) {
            val category = Category(transaction.category, BigDecimal.ZERO, HashMap())
            categoryMap[transaction.category] = category
        }

        val category = categoryMap[transaction.category]
        if (category !== null) {
            var subcategory = category.subcategories[transaction.subCategory]

            if (subcategory == null) {
                subcategory = Subcategory(transaction.subCategory, BigDecimal.ZERO, ArrayList())
                category.subcategories[subcategory.name] = subcategory
            }
            subcategory.transactions.add(transaction)
            subcategory.total = subcategory.total.add(transaction.amount)
            category.total = category.total.add(transaction.amount)
        }
    }

    private fun writeMarkdown() {
        println("Writing Intermediate Markdown file")
        val buf = StringBuilder()
        writeMarkdownYamlHeader(buf)

        val reportPeriodString = getReportPeriodString()

        buf
            .append("# DVRA Treasurer's Report for ")
            .append(reportPeriodString)
            .append("\n")
            .append("\n")
            .append("The beginning balance for ")
            .append(getReportPeriodString())
            .append(" was $")
            .append(startingBalance!!.toPlainString())
            .append("\n\n\n")
            .append("The ending balance for ")
            .append(getReportPeriodString())
            .append(" was $")
            .append(endingBalance!!.toPlainString())
            .append(", a net ")
            .append(if (netTotal!! > BigDecimal.ZERO) "increase" else "decrease")
            .append(" of $")
            .append(netTotal!!.abs())
            .append("\n<p>&nbsp;</p>\n")
            .append("| **Cash Flow for ").append(reportPeriodString).append("** | | \n")
            .append("| :--------------- | --------------: |\n")
            .append("| Starting Balance | ").append(startingBalance).append("|\n")
            .append("| Ending Balance | ").append(endingBalance).append("|\n")
            .append("| | |\n")
            .append("| Total Income | ").append(totalInflows).append("|\n")
            .append("| Total Expenses | ").append(totalOutflows).append("|\n")
            .append("| | |\n")
            .append("| Net Change | ").append(netTotal).append("|\n")
            .append("\n\n<p>&nbsp;</p>\n\n")


        appendCreditCategoriesMarkdown(buf)
        buf.append("\n\n<p>&nbsp;</p>\n\n")
        appendExpenseCategoriesMarkdown(buf)
        buf.append("\n\n<p>&nbsp;</p>\n\n")
        buf.append("\n\n<p>*Respectfully Submitted by Rich Freedman N2EHL, Treasurer*</p>\n\n")
        Files.write(File("report.md").toPath(), buf.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun writeMarkdownYamlHeader(sb: StringBuilder) {
        sb
            .append("---\n")
            .append("author: Rich Freedman N2EHL\n")
            .append("mainfont: Consolas\n")
            .append("geometry: margin=2cm\n")
            .append("header-includes:\n")
            .append("  - |\n")
            .append("    ```{=latex}\n")
            .append("    \\usepackage[margins=raggedright]{floatrow}\n")
            .append("    ```\n")
            .append("---\n")
    }

    private fun getReportPeriodString(): String {
        if (transactions.isEmpty()) {
            return "???"
        }

        val transactionDate = transactions[0].transactionDate
        return WordUtils.capitalizeFully(transactionDate.month.name) + " " + transactionDate.year
    }

    private fun appendCreditCategoriesMarkdown(buf: StringBuilder) {
        buf.append("**Income By Category**\n\n")
            .append("| **Category** | **Subcategory** | **Amount** | **Category Total** |\n")
            .append("| :--- | :--- | ---: | ---: |\n")

        creditCategories.forEach { (categoryName, category) ->
            buf.append("| ").append(categoryName).append(" | | | ").append(category.total).append(" |\n")

            category.subcategories.forEach { (subcategoryName, subcategory) ->
                buf.append("| | ").append(subcategoryName).append("  | ").append(subcategory.total).append(" | |\n")
            }
        }

        val totalCredits = creditCategories.values
            .stream()
            .map { category -> category.total }
            .reduce { x: BigDecimal, y: BigDecimal -> x.add(y) }.get()

        buf.append("| | | | |\n")
        buf.append("| ").append("**TOTAL**").append(" | | | **").append(totalCredits).append("** |\n")
    }

    private fun appendExpenseCategoriesMarkdown(buf: StringBuilder) {
        buf.append("**Expenses By Category**\n\n")
            .append("| **Category** | **Subcategory** | **Amount** | **Category Total** |\n")
            .append("| :--- | :--- | ---: | ---: |\n")

        debitCategories.forEach { (categoryName, category) ->
            buf.append("| ").append(categoryName).append(" | | | ").append(category.total).append(" |\n")

            category.subcategories.forEach { (subcategoryName, subcategory) ->
                buf.append("| | ").append(subcategoryName).append("  | ").append(subcategory.total).append(" | |\n")
            }

            buf.append("|||||\n|||||\n")
        }

        val totalDebits = debitCategories.values
            .stream()
            .map { category -> category.total }
            .reduce { x: BigDecimal, y: BigDecimal -> x.add(y) }.get()

        buf.append("| | | | |\n")
        buf.append("| ").append("**TOTAL**").append(" | | | **").append(totalDebits).append("** |\n")
    }

    private fun convertMarkdownToPdf(path: String) {
        println("Converting Markdown to PDF")
        val process = ProcessBuilder()
            .inheritIO()
            .command(
                "pandoc",
                "--pdf-engine",
                "xelatex",
                "-s",
                "-o",
                // getOutputFilenameRoot() + ".pdf",
                path,
                "report.md"
            )
            .start()
        process.waitFor()
        println("Done!")
    }

    private fun convertMarkdownToDocx() {
        TODO("Not yet implemented")
    }

    private fun isDate(str: String): Boolean {
        if (str.length < 8 || str.length > 10) {
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

    private fun parseDate(str: String): LocalDate {
        val dateParts = str.split("/".toRegex())
        return LocalDate.of(dateParts[2].toInt(), dateParts[0].toInt(), dateParts[1].toInt())
    }

    /**
     * Extract usable CSV data from the weird-ass Quicken file format.
     *
     * What's really weird is that the file doesn't start with a BOM,
     * and each of the actual data lines that we want start with a BOM,
     * but with a deprecated one, that indicates UTF_16 (U+FEFF), should be U+2060.
     *
     * Also, some lines that aren't really data lines (which we don't want)
     * also (seemingly randomly) start with the U+FEEF BOM, so we need to filter those out separately.
     *
     * And finally, even though the file contains UTF_16 BOMs, the file is actually UTF_8
     * (attempting to read it as UTF_16 fails)...
     */
    private fun extractCsvData(csvFile: File): String {
        val allLines = csvFile.readLines(charset = UTF_8)

        // filter to just lines starting with a BOM (all data lines do, but so do some others)
        val dataLines = allLines.filter { line ->
            line.startsWith(BOM)
        }
        .map {line -> line.substring(1)}  // remove the BOM from the lines
        .filter {line ->                            // remove the non-data lines (that are known at the moment)
            !line.startsWith("All Transactions")
                    && !line.startsWith("Filter Criteria")
                    && !line.startsWith(",\"Scheduled\"")
                    && !line.startsWith("Total Inflows")
        }

        // put it all back together so that it can be fed to the parser as a single string
        return dataLines.joinToString(separator = "\n")
    }
}

data class Category (
    var name: String,
    var total: BigDecimal,
    var subcategories: HashMap<String, Subcategory>
)

 data class Subcategory (
    var name: String,
    var total: BigDecimal,
    var transactions: ArrayList<Transaction>
)
 data class Transaction (
    var transactionDate: LocalDate,
    var payee: String,
    var category: String,
    var subCategory: String,
    var amount: BigDecimal,
    var account: String,
    var notes: String
)
