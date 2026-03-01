package cc.rigoligo.imagebinner.data.export

import cc.rigoligo.imagebinner.domain.commit.CommitAction
import cc.rigoligo.imagebinner.domain.commit.CommitItemResult
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {
    @Test
    fun exporter_writesCsvContainingFailedItems() {
        val csv = ReportExporter().toCsv(
            listOf(
                CommitItemResult(
                    mediaId = "1",
                    targetAlbumId = "bin-a",
                    action = CommitAction.MOVE_TO_ALBUM,
                    success = false,
                    errorMessage = "Permission denied"
                )
            )
        )

        assertTrue(csv.contains("media_id"))
        assertTrue(csv.contains("Permission denied"))
    }
}
