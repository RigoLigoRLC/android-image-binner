package cc.rigoligo.imagebinner.data.export

import cc.rigoligo.imagebinner.domain.commit.CommitAction
import cc.rigoligo.imagebinner.domain.commit.CommitItemResult
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {
    @Test
    fun exporter_writesCsvWithLocalizedHeadersAndLabels() {
        val strings = ReportExportStrings(
            csvHeaderMediaId = "Media ID",
            csvHeaderTargetAlbumId = "Target Album ID",
            csvHeaderActionCode = "Action Code",
            csvHeaderActionLabel = "Action",
            csvHeaderStatusCode = "Status Code",
            csvHeaderStatusLabel = "Status",
            csvHeaderErrorMessage = "Error Message",
            actionMoveToAlbum = "Move to album",
            actionMoveToTrashAlbum = "Move to trash album",
            actionMoveToSystemTrash = "Move to system trash",
            statusSuccessCode = "SUCCESS",
            statusFailedCode = "FAILED",
            statusSuccessLabel = "Success",
            statusFailedLabel = "Failed"
        )

        val csv = ReportExporter().toCsv(
            listOf(
                CommitItemResult(
                    mediaId = "1",
                    targetAlbumId = "bin-a",
                    action = CommitAction.MOVE_TO_ALBUM,
                    success = false,
                    errorMessage = "Permission denied"
                )
            ),
            strings = strings
        )

        assertTrue(csv.contains("Media ID"))
        assertTrue(csv.contains("Action Code"))
        assertTrue(csv.contains("MOVE_TO_ALBUM"))
        assertTrue(csv.contains("Move to album"))
        assertTrue(csv.contains("FAILED"))
        assertTrue(csv.contains("Failed"))
        assertTrue(csv.contains("Permission denied"))
    }

    @Test
    fun exporter_writesJsonWithMachineCodesAndLocalizedLabels() {
        val strings = ReportExportStrings(
            csvHeaderMediaId = "media",
            csvHeaderTargetAlbumId = "target",
            csvHeaderActionCode = "actionCode",
            csvHeaderActionLabel = "actionLabel",
            csvHeaderStatusCode = "statusCode",
            csvHeaderStatusLabel = "statusLabel",
            csvHeaderErrorMessage = "error",
            actionMoveToAlbum = "Move to album",
            actionMoveToTrashAlbum = "Move to trash album",
            actionMoveToSystemTrash = "Move to system trash",
            statusSuccessCode = "SUCCESS",
            statusFailedCode = "FAILED",
            statusSuccessLabel = "Success",
            statusFailedLabel = "Failed"
        )

        val json = ReportExporter().toJson(
            listOf(
                CommitItemResult(
                    mediaId = "7",
                    targetAlbumId = "bin-z",
                    action = CommitAction.MOVE_TO_TRASH_ALBUM,
                    success = true,
                    errorMessage = null
                )
            ),
            strings = strings
        )

        assertTrue(json.contains("\"action\":\"MOVE_TO_TRASH_ALBUM\""))
        assertTrue(json.contains("\"actionLabel\":\"Move to trash album\""))
        assertTrue(json.contains("\"statusCode\":\"SUCCESS\""))
        assertTrue(json.contains("\"statusLabel\":\"Success\""))
    }
}
