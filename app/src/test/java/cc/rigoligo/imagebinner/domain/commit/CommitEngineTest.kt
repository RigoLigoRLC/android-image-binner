package cc.rigoligo.imagebinner.domain.commit

import cc.rigoligo.imagebinner.domain.TrashMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitEngineTest {
    @Test
    fun commit_continuesAfterFailure_andCollectsPerItemResults() = runTest {
        val operations = FakeCommitMediaOperations(
            failingMediaIds = setOf("media-2")
        )
        val engine = CommitEngine(mediaOperations = operations)
        val request = CommitRequest(
            assignments = listOf(
                CommitAssignment(mediaId = "media-1", targetAlbumId = "bin-a"),
                CommitAssignment(mediaId = "media-2", targetAlbumId = CommitRequest.TRASH_TARGET_ID),
                CommitAssignment(mediaId = "media-3", targetAlbumId = "bin-b")
            ),
            sdkInt = 34,
            trashMode = TrashMode.SYSTEM_TRASH,
            trashAlbumId = "trash-bin"
        )

        val result = engine.commit(request)

        assertEquals(request.assignments.size, result.itemResults.size)
        assertTrue(result.itemResults.any { !it.success })
        assertTrue(result.itemResults.any { it.success })
        assertTrue(result.itemResults.last().success)
    }

    @Test
    fun commit_missingTrashAlbumId_marksItemFailed_andContinues() = runTest {
        val operations = FakeCommitMediaOperations(failingMediaIds = emptySet())
        val engine = CommitEngine(mediaOperations = operations)
        val request = CommitRequest(
            assignments = listOf(
                CommitAssignment(mediaId = "media-1", targetAlbumId = CommitRequest.TRASH_TARGET_ID),
                CommitAssignment(mediaId = "media-2", targetAlbumId = "bin-a")
            ),
            sdkInt = 29,
            trashMode = TrashMode.TRASH_ALBUM,
            trashAlbumId = null
        )

        val result = engine.commit(request)

        assertEquals(2, result.itemResults.size)
        assertEquals(false, result.itemResults.first().success)
        assertEquals(true, result.itemResults.last().success)
        assertTrue(result.itemResults.first().errorMessage?.contains("Trash album id is required") == true)
    }
}

private class FakeCommitMediaOperations(
    private val failingMediaIds: Set<String>
) : CommitMediaOperations {
    override suspend fun moveToAlbum(mediaId: String, albumId: String) {
        maybeThrow(mediaId)
    }

    override suspend fun moveToTrashAlbum(mediaId: String, trashAlbumId: String) {
        maybeThrow(mediaId)
    }

    override suspend fun moveToSystemTrash(mediaId: String) {
        maybeThrow(mediaId)
    }

    private fun maybeThrow(mediaId: String) {
        if (failingMediaIds.contains(mediaId)) {
            error("simulated failure for $mediaId")
        }
    }
}
