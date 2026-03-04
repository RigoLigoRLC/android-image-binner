package cc.rigoligo.imagebinner.data.export

import cc.rigoligo.imagebinner.domain.commit.CommitAction
import cc.rigoligo.imagebinner.domain.commit.CommitItemResult

class ReportExporter {
    fun toCsv(
        itemResults: List<CommitItemResult>,
        strings: ReportExportStrings
    ): String {
        val header = listOf(
            strings.csvHeaderMediaId,
            strings.csvHeaderTargetAlbumId,
            strings.csvHeaderActionCode,
            strings.csvHeaderActionLabel,
            strings.csvHeaderStatusCode,
            strings.csvHeaderStatusLabel,
            strings.csvHeaderErrorMessage
        ).joinToString(separator = ",") { it.escapeCsv() }
        if (itemResults.isEmpty()) {
            return header
        }

        val rows = itemResults.joinToString(separator = "\n") { result ->
            val statusCode = if (result.success) {
                strings.statusSuccessCode
            } else {
                strings.statusFailedCode
            }
            val statusLabel = if (result.success) {
                strings.statusSuccessLabel
            } else {
                strings.statusFailedLabel
            }
            listOf(
                result.mediaId,
                result.targetAlbumId,
                result.action.name,
                strings.actionLabel(result.action),
                statusCode,
                statusLabel,
                result.errorMessage.orEmpty()
            ).joinToString(separator = ",") { value ->
                value.escapeCsv()
            }
        }
        return "$header\n$rows"
    }

    fun toJson(
        itemResults: List<CommitItemResult>,
        strings: ReportExportStrings
    ): String {
        val successCount = itemResults.count { it.success }
        val failureCount = itemResults.size - successCount
        val itemsJson = itemResults.joinToString(separator = ",") { result ->
            val statusCode = if (result.success) {
                strings.statusSuccessCode
            } else {
                strings.statusFailedCode
            }
            val statusLabel = if (result.success) {
                strings.statusSuccessLabel
            } else {
                strings.statusFailedLabel
            }
            "{" +
                "\"mediaId\":\"${result.mediaId.escapeJson()}\"," +
                "\"targetAlbumId\":\"${result.targetAlbumId.escapeJson()}\"," +
                "\"action\":\"${result.action.name}\"," +
                "\"actionLabel\":\"${strings.actionLabel(result.action).escapeJson()}\"," +
                "\"success\":${result.success}," +
                "\"statusCode\":\"${statusCode.escapeJson()}\"," +
                "\"statusLabel\":\"${statusLabel.escapeJson()}\"," +
                "\"errorMessage\":\"${result.errorMessage.orEmpty().escapeJson()}\"" +
                "}"
        }

        return "{" +
            "\"totalCount\":${itemResults.size}," +
            "\"successCount\":$successCount," +
            "\"failureCount\":$failureCount," +
            "\"itemResults\":[${itemsJson}]" +
            "}"
    }

    private fun String.escapeCsv(): String {
        val escaped = replace("\"", "\"\"")
        return if (contains(',') || contains('"') || contains('\n') || contains('\r')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun String.escapeJson(): String {
        return buildString(length) {
            this@escapeJson.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}

data class ReportExportStrings(
    val csvHeaderMediaId: String,
    val csvHeaderTargetAlbumId: String,
    val csvHeaderActionCode: String,
    val csvHeaderActionLabel: String,
    val csvHeaderStatusCode: String,
    val csvHeaderStatusLabel: String,
    val csvHeaderErrorMessage: String,
    val actionMoveToAlbum: String,
    val actionMoveToTrashAlbum: String,
    val actionMoveToSystemTrash: String,
    val statusSuccessCode: String,
    val statusFailedCode: String,
    val statusSuccessLabel: String,
    val statusFailedLabel: String
) {
    fun actionLabel(action: CommitAction): String {
        return when (action) {
            CommitAction.MOVE_TO_ALBUM -> actionMoveToAlbum
            CommitAction.MOVE_TO_TRASH_ALBUM -> actionMoveToTrashAlbum
            CommitAction.MOVE_TO_SYSTEM_TRASH -> actionMoveToSystemTrash
        }
    }
}
