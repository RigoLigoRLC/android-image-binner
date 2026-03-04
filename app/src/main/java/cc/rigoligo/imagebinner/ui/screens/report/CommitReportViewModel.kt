package cc.rigoligo.imagebinner.ui.screens.report

import androidx.lifecycle.ViewModel
import cc.rigoligo.imagebinner.data.export.ReportExporter
import cc.rigoligo.imagebinner.data.export.ReportExportStrings
import cc.rigoligo.imagebinner.domain.commit.CommitItemResult
import cc.rigoligo.imagebinner.domain.commit.CommitRunResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommitReportViewModel(
    runResult: CommitRunResult,
    private val reportExporter: ReportExporter = ReportExporter(),
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommitReportUiState.fromRunResult(runResult))
    val uiState: StateFlow<CommitReportUiState> = _uiState.asStateFlow()

    fun setFailedOnlyFilter(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(showFailedOnly = enabled)
        }
    }

    fun requestExport(
        format: ReportExportFormat,
        strings: CommitReportExportStrings
    ) {
        val state = _uiState.value
        val content = when (format) {
            ReportExportFormat.CSV -> reportExporter.toCsv(
                itemResults = state.itemResults,
                strings = strings.exporterStrings
            )
            ReportExportFormat.JSON -> reportExporter.toJson(
                itemResults = state.itemResults,
                strings = strings.exporterStrings
            )
        }
        val timestamp = if (state.finishedAtEpochMillis > 0L) {
            state.finishedAtEpochMillis
        } else {
            nowEpochMillis()
        }

        _uiState.update { current ->
            current.copy(
                pendingExport = PreparedCommitReportExport(
                    format = format,
                    fileName = "${strings.fileNamePrefix}-${formatReportTimestamp(timestamp, strings.fileNameFallback)}.${format.fileExtension}",
                    mimeType = format.mimeType,
                    content = content,
                    createdAtEpochMillis = nowEpochMillis()
                )
            )
        }
    }

    fun onExportHandled() {
        _uiState.update { state ->
            state.copy(pendingExport = null)
        }
    }

    private fun formatReportTimestamp(
        timestamp: Long,
        fallback: String
    ): String {
        return runCatching {
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(timestamp))
        }.getOrElse { fallback }
    }
}

data class CommitReportExportStrings(
    val fileNamePrefix: String,
    val fileNameFallback: String,
    val exporterStrings: ReportExportStrings
)

data class CommitReportUiState(
    val startedAtEpochMillis: Long = 0L,
    val finishedAtEpochMillis: Long = 0L,
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val itemResults: List<CommitItemResult> = emptyList(),
    val showFailedOnly: Boolean = true,
    val pendingExport: PreparedCommitReportExport? = null
) {
    val failedItems: List<CommitItemResult>
        get() = itemResults.filterNot { it.success }

    val visibleItems: List<CommitItemResult>
        get() = if (showFailedOnly) failedItems else itemResults

    companion object {
        fun fromRunResult(runResult: CommitRunResult): CommitReportUiState {
            return CommitReportUiState(
                startedAtEpochMillis = runResult.startedAtEpochMillis,
                finishedAtEpochMillis = runResult.finishedAtEpochMillis,
                totalCount = runResult.itemResults.size,
                successCount = runResult.successCount,
                failureCount = runResult.failureCount,
                itemResults = runResult.itemResults
            )
        }
    }
}

enum class ReportExportFormat(
    val fileExtension: String,
    val mimeType: String
) {
    CSV(fileExtension = "csv", mimeType = "text/csv"),
    JSON(fileExtension = "json", mimeType = "application/json")
}

data class PreparedCommitReportExport(
    val format: ReportExportFormat,
    val fileName: String,
    val mimeType: String,
    val content: String,
    val createdAtEpochMillis: Long
)
