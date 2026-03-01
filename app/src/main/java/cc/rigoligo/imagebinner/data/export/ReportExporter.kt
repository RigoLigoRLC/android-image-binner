package cc.rigoligo.imagebinner.data.export

import cc.rigoligo.imagebinner.domain.commit.CommitItemResult

class ReportExporter {
    fun toCsv(itemResults: List<CommitItemResult>): String {
        val header = "media_id,target_album_id,action,success,error_message"
        if (itemResults.isEmpty()) {
            return header
        }

        val rows = itemResults.joinToString(separator = "\n") { result ->
            listOf(
                result.mediaId,
                result.targetAlbumId,
                result.action.name,
                result.success.toString(),
                result.errorMessage.orEmpty()
            ).joinToString(separator = ",") { value ->
                value.escapeCsv()
            }
        }
        return "$header\n$rows"
    }

    fun toJson(itemResults: List<CommitItemResult>): String {
        val successCount = itemResults.count { it.success }
        val failureCount = itemResults.size - successCount
        val itemsJson = itemResults.joinToString(separator = ",") { result ->
            "{" +
                "\"mediaId\":\"${result.mediaId.escapeJson()}\"," +
                "\"targetAlbumId\":\"${result.targetAlbumId.escapeJson()}\"," +
                "\"action\":\"${result.action.name}\"," +
                "\"success\":${result.success}," +
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
