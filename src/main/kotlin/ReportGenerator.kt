import java.io.File

class ReportGenerator {
    companion object {
        fun generate(startingBal: String, endingBal: String, csvFile: File, pdfFile: File) {
            println("start: $startingBal")
            println("end: $endingBal")
            println("csv: ${csvFile.path}")
            println("output: ${pdfFile.path}")
        }
    }
}