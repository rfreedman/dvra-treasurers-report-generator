import kotlinx.coroutines.channels.Channel
import java.io.File

data class ReportGenerationRequest(
    val startingBalance: String,
    val endingBalance: String,
    val csvFile: File,
    val pdfFile: File,
    val keepMarkdown: Boolean
)

/**
 * Loads config and runs [ReportGenerator] on the worker side of the status channel.
 * Always closes [channel] so the UI receiver cannot hang.
 */
suspend fun runReportGeneration(
    request: ReportGenerationRequest,
    channel: Channel<String>,
    configLoader: () -> ConfigLoadResult = { loadValidatedConfig() }
) {
    try {
        when (val configResult = configLoader()) {
            is ConfigLoadResult.Error -> {
                channel.send(ReportStatus.failed(configResult.message))
            }
            is ConfigLoadResult.Ok -> {
                ReportGenerator().generate(
                    author = configResult.config.author,
                    startingBal = request.startingBalance,
                    endingBal = request.endingBalance,
                    pandocPath = configResult.config.pandocPath,
                    xelatexDir = configResult.config.xelatexDir,
                    csvFile = request.csvFile,
                    pdfFile = request.pdfFile,
                    keepMarkdown = request.keepMarkdown,
                    channel = channel
                )
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        channel.send(ReportStatus.failed(t.message ?: t.toString()))
    } finally {
        channel.close()
    }
}
