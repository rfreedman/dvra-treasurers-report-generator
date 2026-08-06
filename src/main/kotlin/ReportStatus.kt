/**
 * Status channel protocol for report generation.
 * Failures are a single terminal payload: [FAILED_PREFIX] + message (no dual-send).
 */
object ReportStatus {
    const val DONE = "Done!"
    const val FAILED_PREFIX = "Failed!:"

    sealed class Message {
        data class Progress(val text: String) : Message()
        data object Done : Message()
        data class Failed(val text: String) : Message()
    }

    fun failed(message: String): String = FAILED_PREFIX + message

    fun parse(raw: String): Message = when {
        raw == DONE -> Message.Done
        raw.startsWith(FAILED_PREFIX) -> Message.Failed(raw.removePrefix(FAILED_PREFIX))
        else -> Message.Progress(raw)
    }
}
