import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import org.apache.commons.text.WordUtils
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.nio.file.Files
import kotlin.text.Charsets.UTF_8

/**
 * Builds one treasurer's report. Create a fresh instance per run so mutable state
 * cannot leak across concurrent or sequential generations.
 */
class ReportGenerator {

    companion object {
        const val STATUS_DONE = ReportStatus.DONE
        const val STATUS_FAILED_PREFIX = ReportStatus.FAILED_PREFIX

        private const val TRANSACTION_START = ",,,\"" // as of 10/1/2024: first 3 cols empty
        private const val SPLIT_TRANSACTION_START = ",,\"S\",\"" // split txn: capital S in 3rd column

        /** Narrative fragment after the ending balance (includes leading "a net …" or "unchanged"). */
        fun netChangeNarrative(netTotal: BigDecimal): String =
            when {
                netTotal.compareTo(BigDecimal.ZERO) == 0 -> "unchanged"
                netTotal > BigDecimal.ZERO ->
                    "a net increase of $${netTotal.abs().toPlainString()}"
                else ->
                    "a net decrease of $${netTotal.abs().toPlainString()}"
            }
    }

    private var startingBalance: BigDecimal? = null
    private var endingBalance: BigDecimal? = null
    private var transactions: List<Transaction> = emptyList()
    private var categorized: CategorizedReport? = null

    suspend fun generate(
        author: String,
        startingBal: String,
        endingBal: String,
        pandocPath: String,
        xelatexDir: String,
        csvFile: File,
        pdfFile: File,
        keepMarkdown: Boolean,
        channel: Channel<String>
    ) {
        this.startingBalance = BigDecimal(startingBal)
        this.endingBalance = BigDecimal(endingBal)

        channel.send("Reading CSV File")

        val dataLines: String = withContext(Dispatchers.IO) {
            extractCsvData(csvFile)
        }

        val csvData: List<List<String>> = csvReader().readAll(dataLines)

        channel.send("Parsing CSV Rows")

        if (csvData.isEmpty()) {
            fail(channel, "No data read from file, report not generated")
            return
        }

        val parseResult = CsvTransactionParser.parseRows(csvData)
        transactions = parseResult.transactions

        if (parseResult.errors.isNotEmpty()) {
            val preview = parseResult.errors.take(5).joinToString("; ")
            val more = if (parseResult.errors.size > 5) " (+${parseResult.errors.size - 5} more)" else ""
            fail(channel, "CSV row errors: $preview$more")
            return
        }

        channel.send("Creating Transaction Categories")
        channel.send("Calculating Totals")
        val report = TransactionCategorizer.categorize(transactions)
        categorized = report

        ReportDataValidator.validate(
            transactions,
            startingBalance!!,
            endingBalance!!,
            report.netTotal
        )?.let { error ->
            fail(channel, error)
            return
        }

        channel.send("Writing Intermediate Markdown")

        val markdownPath = File(pdfFile.parentFile, "report.md").path
        val pdfPath = pdfFile.path

        try {
            withContext(Dispatchers.IO) {
                writeMarkdown(author, markdownPath)
            }
        } catch (ex: IOException) {
            fail(channel, "failed to write markdown file to disk: ${ex.message}")
            ex.printStackTrace()
            return
        }

        channel.send("Converting Markdown to PDF")
        val pandocReturnCode = withContext(Dispatchers.IO) {
            convertMarkdownToPdf(pandocPath, xelatexDir, markdownPath, pdfPath)
        }
        if (pandocReturnCode != 0) {
            fail(channel, "pandoc failed (code $pandocReturnCode): p: $pandocPath m: $markdownPath")
            return
        }

        if (!keepMarkdown) {
            channel.send("Deleting Intermediate Markdown")
            withContext(Dispatchers.IO) {
                try {
                    Files.delete(File(markdownPath).toPath())
                    println("markdown deleted")
                } catch (ex: IOException) {
                    channel.send("failed to delete markdown file")
                }
            }
        }

        channel.send(STATUS_DONE)
    }

    private suspend fun fail(channel: Channel<String>, message: String) {
        channel.send(ReportStatus.failed(message))
    }

    private fun writeMarkdown(author: String, outputFilePath: String) {
        val report = categorized!!
        val buf = StringBuilder()
        writeMarkdownYamlHeader(author, buf)

        val reportPeriodString = getReportPeriodString()

        buf
            .append("# DVRA Treasurer's Report for ")
            .append(reportPeriodString)
            .append("\n")
            .append("\n")
            .append("The beginning balance for ")
            .append(reportPeriodString)
            .append(" was $")
            .append(startingBalance!!.toPlainString())
            .append("\n\n\n")
            .append("The ending balance for ")
            .append(reportPeriodString)
            .append(" was $")
            .append(endingBalance!!.toPlainString())
            .append(", ")
            .append(netChangeNarrative(report.netTotal))
            .append("\n<p>&nbsp;</p>\n")
            .append("| **Cash Flow for ").append(reportPeriodString).append("** | | \n")
            .append("| :--------------- | --------------: |\n")
            .append("| Starting Balance | ").append(startingBalance).append("|\n")
            .append("| Ending Balance | ").append(endingBalance).append("|\n")
            .append("| | |\n")
            .append("| Total Income | ").append(report.totalInflows).append("|\n")
            .append("| Total Expenses | ").append(report.totalOutflows).append("|\n")
            .append("| | |\n")
            .append("| Net Change | ").append(report.netTotal).append("|\n")
            .append("\n\n<p>&nbsp;</p>\n\n")

        if (report.creditCategories.isEmpty()) {
            appendNoCreditCategoriesMarkdown(buf)
        } else {
            appendCreditCategoriesMarkdown(buf, report)
        }

        buf.append("\n\n<p>&nbsp;</p>\n\n")

        if (report.debitCategories.isEmpty()) {
            appendNoExpenseCategoriesMarkdown(buf)
        } else {
            appendExpenseCategoriesMarkdown(buf, report)
        }

        buf.append("\n\n<p>&nbsp;</p>\n\n")
        buf.append("\n\n<p>*Respectfully Submitted by ${MarkdownEscaping.escapeTableCell(author)}, Treasurer*</p>\n\n")

        val file = File(outputFilePath)

        if (!file.exists()) {
            file.createNewFile()
        }

        if (file.canWrite()) {
            println("writing markdown file: ${file.path}")
            file.writeText(buf.toString())
        } else {
            println("can't write markdown file: ${file.path}")
            throw IOException("unable to write md file: ${file.toPath()}")
        }
    }

    private fun writeMarkdownYamlHeader(author: String, sb: StringBuilder) {
        sb
            .append("---\n")
            .append("author: ${MarkdownEscaping.escapeYamlScalar(author)}\n")
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

    private fun appendNoCreditCategoriesMarkdown(buf: StringBuilder) {
        buf.append("**Income By Category: No Income**\n\n")
    }

    private fun appendCreditCategoriesMarkdown(buf: StringBuilder, report: CategorizedReport) {
        buf.append("**Income By Category**\n\n")
            .append("| **Category** | **Subcategory** | **Amount** | **Category Total** |\n")
            .append("| :--- | :--- | ---: | ---: |\n")

        appendCategoryRows(buf, report.creditCategories)
        buf.append("| | | | |\n")
        buf.append("| ").append("**TOTAL**").append(" | | | **").append(report.totalInflows).append("** |\n")
    }

    private fun appendNoExpenseCategoriesMarkdown(buf: StringBuilder) {
        buf.append("**Expenses By Category: No Expenses**\n\n")
    }

    private fun appendExpenseCategoriesMarkdown(buf: StringBuilder, report: CategorizedReport) {
        buf.append("**Expenses By Category**\n\n")
            .append("| **Category** | **Subcategory** | **Amount** | **Category Total** |\n")
            .append("| :--- | :--- | ---: | ---: |\n")

        appendCategoryRows(buf, report.debitCategories, blankRowsBetweenCategories = true)
        buf.append("| | | | |\n")
        buf.append("| ").append("**TOTAL**").append(" | | | **").append(report.totalOutflows).append("** |\n")
    }

    private fun appendCategoryRows(
        buf: StringBuilder,
        categories: Map<String, Category>,
        blankRowsBetweenCategories: Boolean = false
    ) {
        categories.entries.sortedBy { it.key }.forEach { (categoryName, category) ->
            buf.append("| ")
                .append(MarkdownEscaping.escapeTableCell(categoryName))
                .append(" | | | ")
                .append(category.total)
                .append(" |\n")

            category.subcategories.entries.sortedBy { it.key }.forEach { (subcategoryName, subcategory) ->
                buf.append("| | ")
                    .append(MarkdownEscaping.escapeTableCell(subcategoryName))
                    .append("  | ")
                    .append(subcategory.total)
                    .append(" | |\n")
            }

            if (blankRowsBetweenCategories) {
                buf.append("|||||\n|||||\n")
            }
        }
    }

    private fun createProcessBuilder(xelatexDir: String): ProcessBuilder {
        val processBuilder = ProcessBuilder()
        val path = processBuilder.environment()["PATH"] ?: ""
        val pathSeparator = ConfigUtil.envPathSeparator()
        processBuilder.environment()["PATH"] = "$path$pathSeparator$xelatexDir"
        return processBuilder
    }

    private fun convertMarkdownToPdf(
        pandocPath: String,
        xelatexDir: String,
        markdownPath: String,
        pdfPath: String
    ): Int {
        val process = createProcessBuilder(xelatexDir)
            .inheritIO()
            .command(
                pandocPath,
                "--pdf-engine",
                "xelatex",
                "-s",
                "-o",
                pdfPath,
                markdownPath
            )
            .start()
        val processResult = process.waitFor()
        println("pandoc process result = $processResult")
        return processResult
    }

    /**
     * Extract usable CSV data from the Quicken export format.
     * Transaction rows start with empty leading columns (or an "S" split marker).
     */
    private fun extractCsvData(csvFile: File): String {
        val allLines = csvFile.readLines(charset = UTF_8)
        return allLines
            .filter { line ->
                line.startsWith(TRANSACTION_START) || line.startsWith(SPLIT_TRANSACTION_START)
            }
            .joinToString(separator = "\n")
    }
}
