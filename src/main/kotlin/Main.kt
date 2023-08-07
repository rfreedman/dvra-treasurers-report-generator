import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

@Composable
@Preview
fun app() {
    val numericRegex = Regex(pattern = "^[0-9]+$")
    val numericWithDecimalRegex = Regex(pattern = "^[0-9]+\\.$")
    val moneyRegex = Regex(pattern = "^[0-9]+\\.[0-9]?[0-9]?\$")

    var startingBalance by remember {mutableStateOf("")}
    var endingBalance by remember {mutableStateOf("")}
    var csvFileName by remember {mutableStateOf("")}
    var csvFile by remember { mutableStateOf<File?>(null) }

    var isCsvFileOpenChooserOpen by remember { mutableStateOf(false) }
    var isPdfFileSaveChooserOpen by remember { mutableStateOf(false) }

    val isCurrency : (String) -> Boolean = { value: String ->
         value.isEmpty()
            || value.matches(numericRegex)
            || value.matches(moneyRegex)
            || value.matches(numericWithDecimalRegex)
    }

    val haveAllInput : () -> Boolean = {
        startingBalance.isNotEmpty() && endingBalance.isNotEmpty() && csvFileName.isNotEmpty()
    }

    if (isCsvFileOpenChooserOpen) {
        csvFileOpenDialog(
            onCloseRequest =  { directory, file ->
                isCsvFileOpenChooserOpen = false
                if(file !== null) {
                    csvFileName = file
                    csvFile = File(directory, file)
                    // val exists = csvFile !== null && csvFile!!.exists()
                    // println("csvFile: $csvFileName exists: $exists")
                }
            }
        )
    }

    if(isPdfFileSaveChooserOpen) {
        pdfFileSaveDialog(
            fileName = getPdfFileNameFromCsvFilename(csvFileName),
            onCloseRequest =  { directoryName: String, pdfFileName: String? ->
                isPdfFileSaveChooserOpen = false
                if(pdfFileName !== null) {
                    ReportGenerator.generate(startingBalance, endingBalance, csvFile!!, File(directoryName, pdfFileName))
                    /*
                    val directory = File(directoryName)
                    val directoryExists = directory.exists()
                    println("directory: $directoryName exists: $directoryExists")
                    if(directoryExists) {
                        // val pdfFileName = file
                        val pdfFile = File(directory, fileName)
                        val fileExists = pdfFile.exists()
                        println("pdfFile: $fileName exists: $fileExists")
                    }
                     */
                }
            }
        )
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            TextField(
                value = startingBalance,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = { value ->
                    if(isCurrency(value)) {
                        startingBalance = value
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .then(Modifier.padding(top = 20.dp)),
                label = { Text(text = "Starting Balance: ") },
                // placeholder = { Text(text = "Starting Balance") }
            )

            TextField(
                value = endingBalance,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = { value ->
                    if(isCurrency(value))  {
                        endingBalance = value
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                label = { Text(text = "Ending Balance: ") },
                // placeholder = { Text(text = "Ending Balance") }
            )


            Button(
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                onClick = {
                    isCsvFileOpenChooserOpen = true
                }) {
                Text("Choose csv File")
            }

            Text(text ="" +csvFileName, modifier = Modifier.align(Alignment.CenterHorizontally))

            Button(
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                    .then(Modifier.padding(vertical = 10.dp)),
                enabled = haveAllInput(),
                onClick = {
                    isPdfFileSaveChooserOpen  = true
                }) {
                Text("Generate Report")
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DVRA Treasurer's Report Generator",
        // state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        app()
    }
}

private fun getPdfFileNameFromCsvFilename(csvFileName: String): String {
    return File(csvFileName).nameWithoutExtension.plus(".pdf")
}

@Composable
private fun csvFileOpenDialog(
    parent: Frame? = null,
    onCloseRequest: (directory: String, file: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {

            override fun setVisible(value: Boolean) {
                setFilenameFilter(FilenameFilter { _, name -> name.endsWith(".csv")  })
                super.setVisible(value)
                if (value) {
                    if(file !== null) {
                        onCloseRequest(this.directory, file)
                    } else {
                        onCloseRequest("", null)
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

@Composable
private fun pdfFileSaveDialog(
    parent: Frame? = null,
    fileName: String,
    onCloseRequest: (directory: String, file: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", SAVE) {

            override fun setVisible(value: Boolean) {
                setFilenameFilter(FilenameFilter { _, name -> name.endsWith(".pdf")  })
                setFile(fileName)
                super.setVisible(value)
                if (value) {
                    if(file !== null) {
                        onCloseRequest(this.directory, file)
                    } else {
                        onCloseRequest("", null)
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)