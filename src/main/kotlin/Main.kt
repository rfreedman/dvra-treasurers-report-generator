import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.AwtWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mikepenz.markdown.m2.Markdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

@Composable
@Preview
fun app() {
    val scope = rememberCoroutineScope()

    val numericRegex = Regex(pattern = "^[0-9]+$")
    val numericWithDecimalRegex = Regex(pattern = "^[0-9]+\\.$")
    val moneyRegex = Regex(pattern = "^[0-9]+\\.[0-9]?[0-9]?\$")

    val instructions = """
    * In Quicken, select "All Accounts" and then export the month's transactions to a CSV file.  
    ---
    *  Enter the starting and ending balance for the month
    ---     
    * Select the CSV file that you exported  
    ---
    * If necessary for debugging, select to keep the intermediate Markdown file  
    ---
    * Click the "Generate Report" button and select the directory where it should be written  
    """.trimIndent()

    var startingBalance by remember { mutableStateOf("") }
    var endingBalance by remember { mutableStateOf("") }
    var csvFileName by remember { mutableStateOf("") }
    var csvFile by remember { mutableStateOf<File?>(null) }

    var keepMarkdown by remember { mutableStateOf(false) }
    var instructionsExpanded by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

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
            initialDirectory = loadLastDirectory(DirectoryPrefs.KEY_CSV),
            onCloseRequest = { directoryPath, fileName ->
                isCsvFileOpenChooserOpen = false
                if (fileName != null) {
                    csvFileName = fileName
                    csvFile = File(directoryPath, fileName)
                    saveLastDirectory(DirectoryPrefs.KEY_CSV, File(directoryPath))
                }
            }
        )
    }

    if (isPdfFileSaveChooserOpen) {
        pdfFileSaveDialog(
            fileName = getPdfFileNameFromCsvFilename(csvFileName),
            initialDirectory = loadLastDirectory(DirectoryPrefs.KEY_PDF),
            onCloseRequest = { directoryName: String, pdfFileName: String? ->
                isPdfFileSaveChooserOpen = false

                if (pdfFileName == null) {
                    return@pdfFileSaveDialog
                }

                saveLastDirectory(DirectoryPrefs.KEY_PDF, File(directoryName))

                val selectedCsv = csvFile
                if (selectedCsv == null) {
                    status = "No CSV file selected"
                    return@pdfFileSaveDialog
                }

                val pdfOut = File(directoryName, pdfFileName)
                status = "Generating Report"
                isGenerating = true

                scope.launch {
                    try {
                        val channel = Channel<String>(capacity = 64)
                        val worker = launch(Dispatchers.IO) {
                            runReportGeneration(
                                ReportGenerationRequest(
                                    startingBalance = startingBalance,
                                    endingBalance = endingBalance,
                                    csvFile = selectedCsv,
                                    pdfFile = pdfOut,
                                    keepMarkdown = keepMarkdown
                                ),
                                channel
                            )
                        }

                        for (message in channel) {
                            when (val parsed = ReportStatus.parse(message)) {
                                is ReportStatus.Message.Done -> {
                                    status = "Report Generated in ${pdfOut.path}"
                                    if (Desktop.isDesktopSupported()) {
                                        withContext(Dispatchers.IO) {
                                            Desktop.getDesktop().open(pdfOut)
                                        }
                                    } else {
                                        println("Awt Desktop is not supported!")
                                    }
                                }
                                is ReportStatus.Message.Failed -> {
                                    status = parsed.text
                                }
                                is ReportStatus.Message.Progress -> {
                                    status = parsed.text
                                }
                            }
                        }
                        worker.join()
                    } finally {
                        isGenerating = false
                    }
                }
            }
        )
    }

    MaterialTheme {
        val scrollState = rememberScrollState()

        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 0.dp, vertical = 28.dp)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Image(classpathPainterResource("w2zq-transparent.png"), "DVRA", modifier = Modifier.align(alignment = Alignment.CenterHorizontally))

                TextButton(
                    onClick = { instructionsExpanded = !instructionsExpanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (instructionsExpanded) "Instructions ▼" else "Instructions ▶",
                        color = Color.Blue
                    )
                }

                if (instructionsExpanded) {
                    Box(
                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                            .padding(bottom = 12.dp)
                            .height(280.dp)
                    ) {
                        Markdown(
                            content = instructions
                        )
                    }
                }

                Divider(modifier = Modifier.width(200.dp).align(Alignment.CenterHorizontally).padding(bottom = 48.dp), color = Color.Blue, thickness = 1.dp)

                inputTextField(
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
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
                        .padding(vertical = 14.dp),
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
                        .padding(top = 14.dp, bottom = 7.dp),
                    onClick = {
                        isCsvFileOpenChooserOpen = true
                    }) {
                    Text("Choose csv File")
                }

                Text(text = csvFileName, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 7.dp))

                Divider(modifier = Modifier.width(200.dp).align(Alignment.CenterHorizontally), color = Color.Blue, thickness = 1.dp)

                labeledCheckbox(
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    label = "Keep Markdown  ",
                    value = keepMarkdown,
                    onValueChange = { newValue -> keepMarkdown = newValue }
                )

                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue, contentColor = Color.White),
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                        .padding(top = 4.dp, bottom = 4.dp),
                    enabled = haveAllInput() && !isGenerating,
                    onClick = {
                        status = ""
                        isPdfFileSaveChooserOpen = true
                    }) {
                    Text("Generate Report")
                }

                Text(
                    text = status,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            }

            PlatformVerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 28.dp, bottom = 8.dp, end = 2.dp)
            )

            Text(
                text = AppBuildInfo.displayLabel(),
                style = MaterialTheme.typography.caption,
                color = Color.Black,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 24.dp)
            )
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
            Row(
                Modifier.width(fieldWidth)
            ) {
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

@Composable
fun labeledCheckbox(modifier: Modifier, label: String, value: Boolean, onValueChange: (value: Boolean) -> Unit) {
    Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp).then(modifier)) {

        Checkbox(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .scale(0.75f),
            checked = value,
            onCheckedChange = onValueChange,
            enabled = true,
            colors = CheckboxDefaults.colors(Color.Blue)
        )

        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}


private fun getPdfFileNameFromCsvFilename(csvFileName: String): String {
    return File(csvFileName).nameWithoutExtension.plus(".pdf")
}

@Composable
private fun csvFileOpenDialog(
    parent: Frame? = null,
    initialDirectory: File? = null,
    onCloseRequest: (directory: String, file: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {

            override fun setVisible(value: Boolean) {
                setFilenameFilter(fileNameFilterByExtension(".csv"))
                if (initialDirectory != null) {
                    setDirectory(initialDirectory.absolutePath)
                }
                super.setVisible(value)
                if (value) {
                    if (file != null) {
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
    initialDirectory: File? = null,
    onCloseRequest: (directory: String, file: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", SAVE) {

            override fun setVisible(value: Boolean) {
                setFilenameFilter(fileNameFilterByExtension(".pdf"))
                if (initialDirectory != null) {
                    setDirectory(initialDirectory.absolutePath)
                }
                setFile(fileName)
                super.setVisible(value)
                if (value) {
                    if (file != null) {
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
    val initialPlacement = remember { loadClampedWindowPlacement() }
    val windowState = rememberWindowState(
        position = WindowPosition(initialPlacement.x.dp, initialPlacement.y.dp),
        width = initialPlacement.width.dp,
        height = initialPlacement.height.dp
    )

    fun persistWindowPlacement() {
        if (windowState.isMinimized) {
            return
        }
        val position = windowState.position
        if (position !is WindowPosition.Absolute) {
            return
        }
        val placement = clampWindowPlacement(
            WindowPlacement(
                x = position.x.value,
                y = position.y.value,
                width = windowState.size.width.value,
                height = windowState.size.height.value
            )
        )
        try {
            saveWindowPlacement(placement)
        } catch (ex: Exception) {
            println("Failed to save window placement: ${ex.message}")
        }
    }

    Window(
        onCloseRequest = {
            persistWindowPlacement()
            exitApplication()
        },
        title = "DVRA Treasurer's Report Generator",
        state = windowState
    ) {
        @OptIn(FlowPreview::class)
        LaunchedEffect(windowState) {
            snapshotFlow {
                val position = windowState.position
                WindowPlacementSnapshot(
                    minimized = windowState.isMinimized,
                    absolute = position is WindowPosition.Absolute,
                    x = (position as? WindowPosition.Absolute)?.x?.value,
                    y = (position as? WindowPosition.Absolute)?.y?.value,
                    width = windowState.size.width.value,
                    height = windowState.size.height.value
                )
            }
                .distinctUntilChanged()
                .debounce(300)
                .collect { snapshot ->
                    if (snapshot.minimized || !snapshot.absolute) {
                        return@collect
                    }
                    val x = snapshot.x ?: return@collect
                    val y = snapshot.y ?: return@collect
                    try {
                        saveWindowPlacement(
                            clampWindowPlacement(
                                WindowPlacement(
                                    x = x,
                                    y = y,
                                    width = snapshot.width,
                                    height = snapshot.height
                                )
                            )
                        )
                    } catch (ex: Exception) {
                        println("Failed to save window placement: ${ex.message}")
                    }
                }
        }

        app()
    }
}

private data class WindowPlacementSnapshot(
    val minimized: Boolean,
    val absolute: Boolean,
    val x: Float?,
    val y: Float?,
    val width: Float,
    val height: Float
)
