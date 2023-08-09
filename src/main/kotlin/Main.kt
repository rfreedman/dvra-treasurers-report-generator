import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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

    var startingBalance by remember { mutableStateOf("") }
    var endingBalance by remember { mutableStateOf("") }
    var csvFileName by remember { mutableStateOf("") }
    var csvFile by remember { mutableStateOf<File?>(null) }
    var status by remember { mutableStateOf("Input Data") }

    var isCsvFileOpenChooserOpen by remember { mutableStateOf(false) }
    var isPdfFileSaveChooserOpen by remember { mutableStateOf(false) }

    val isCurrency: (String) -> Boolean = { value: String ->
        value.isEmpty()
                || value.matches(numericRegex)
                || value.matches(moneyRegex)
                || value.matches(numericWithDecimalRegex)
    }

    val haveAllInput: () -> Boolean = {
        startingBalance.isNotEmpty() && endingBalance.isNotEmpty() && csvFileName.isNotEmpty()
    }

    if (isCsvFileOpenChooserOpen) {
        csvFileOpenDialog(
            onCloseRequest = { directoryPath, fileName ->
                isCsvFileOpenChooserOpen = false
                if (fileName !== null) {
                    csvFileName = fileName
                    csvFile = File(directoryPath, fileName)
                }
            }
        )
    }

    if (isPdfFileSaveChooserOpen) {
        pdfFileSaveDialog(
            fileName = getPdfFileNameFromCsvFilename(csvFileName),
            onCloseRequest = { directoryName: String, pdfFileName: String? ->
                try {
                    isPdfFileSaveChooserOpen = false
                    if (pdfFileName !== null) {
                        status = "Generating Report"
                        ReportGenerator.generate(
                            startingBalance,
                            endingBalance,
                            csvFile!!,
                            File(directoryName, pdfFileName)
                        )
                        status = "Report Generated in " + File(directoryName, pdfFileName).path
                    }
                } catch(t: Throwable) {
                    status = "Error: " + t.message
                }
            }
        )
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(0.dp, 40.dp), Arrangement.spacedBy(5.dp)) {
                inputTextField(
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    //modifier = Modifier,
                    label = "Starting Balance",
                    value = startingBalance,
                    onValueChange = {
                        if (isCurrency(it)) {
                            startingBalance = it
                        }
                    }
                )

                inputTextField(
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                        .padding(0.dp, 20.dp, 0.dp, 20.dp),
                    label = "Ending Balance  ",
                    value = endingBalance,
                    onValueChange = {
                        if (isCurrency(it)) {
                            endingBalance = it
                        }
                    }
                )

                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue, contentColor = Color.White),
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                        .padding(0.dp, 20.dp, 0.dp, 20.dp),
                    onClick = {
                        isCsvFileOpenChooserOpen = true
                    }) {
                    Text("Choose csv File")
                }

                Text(text = csvFileName, modifier = Modifier.align(Alignment.CenterHorizontally))

                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue, contentColor = Color.White),
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                        .then(Modifier.padding(vertical = 10.dp)),
                    enabled = haveAllInput(),
                    onClick = {
                        isPdfFileSaveChooserOpen = true
                    }) {
                    Text("Generate Report")
                }

                Text(text = status, modifier = Modifier.align(Alignment.CenterHorizontally).offset(y = 225.dp))
        }
    }
}

/**
 * A Basic (non-Material) input field with a label
 */
@Suppress("SameParameterValue")
@Composable
private fun inputTextField(
    modifier: Modifier,
    fieldHeight: Dp = 20.dp,
    fieldWidth: Dp = 275.dp,
    label: String,
    value: String,
    onValueChange: (value: String) -> Unit
) {

    BasicTextField(
        modifier = modifier.then(Modifier.background(Color.White)),
        singleLine = true,
        value = value,
        onValueChange = onValueChange,
        decorationBox = { innerTextField ->
            // Because the decorationBox is used, the whole Row gets the same behaviour as the
            // internal input field would have otherwise. For example, there is no need to add a
            // Modifier.clickable to the Row anymore to bring the text field into focus when user
            // taps on a larger text field area which includes paddings and the icon areas.
            Row(
                Modifier.width(fieldWidth)
            ) {
                // Icon(Icons.Default.MailOutline, contentDescription = null)
                Text(
                    text = "$label:",
                    modifier = Modifier.height(fieldHeight).background(Color(0x55CCCCCC)) // ARGB
                )
                Spacer(Modifier.width(12.dp).height(fieldHeight).background(Color(0x55CCCCCC)))
                innerTextField()
            }
        }
    )
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
                // setFilenameFilter(FilenameFilter { _, name -> name.endsWith(".csv") })
                setFilenameFilter(fileNameFilterByExtension(".csv"))
                super.setVisible(value)
                if (value) {
                    if (file !== null) {
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

private fun fileNameFilterByExtension(extension: String) : FilenameFilter = FilenameFilter { _, name -> name.endsWith(extension) }


@Composable
private fun pdfFileSaveDialog(
    parent: Frame? = null,
    fileName: String,
    onCloseRequest: (directory: String, file: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", SAVE) {

            override fun setVisible(value: Boolean) {
                setFilenameFilter(fileNameFilterByExtension(".pdf"))
                setFile(fileName)
                super.setVisible(value)
                if (value) {
                    if (file !== null) {
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

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DVRA Treasurer's Report Generator",
        // state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        app()
    }
}