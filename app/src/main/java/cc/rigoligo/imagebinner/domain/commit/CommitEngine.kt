package cc.rigoligo.imagebinner.domain.commit

import cc.rigoligo.imagebinner.domain.TrashMode
import kotlinx.coroutines.CancellationException

class CommitEngine(
    private val mediaOperations: CommitMediaOperations,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun commit(request: CommitRequest): CommitRunResult {
        val startedAtEpochMillis = nowEpochMillis()
        val resolvedTrashStrategy = resolveTrashStrategy(
            apiLevel = request.sdkInt,
            trashMode = request.trashMode
        )
        val itemResults = request.assignments.map { assignment ->
            commitSingleItem(
                assignment = assignment,
                request = request,
                resolvedTrashStrategy = resolvedTrashStrategy
            )
        }
        val finishedAtEpochMillis = nowEpochMillis()

        return CommitRunResult(
            startedAtEpochMillis = startedAtEpochMillis,
            finishedAtEpochMillis = finishedAtEpochMillis,
            resolvedTrashStrategy = resolvedTrashStrategy,
            itemResults = itemResults
        )
    }

    private suspend fun commitSingleItem(
        assignment: CommitAssignment,
        request: CommitRequest,
        resolvedTrashStrategy: TrashStrategy
    ): CommitItemResult {
        return try {
            val operation = resolveOperation(
                assignment = assignment,
                request = request,
                resolvedTrashStrategy = resolvedTrashStrategy
            )
            operation.execute()
            CommitItemResult(
                mediaId = assignment.mediaId,
                targetAlbumId = assignment.targetAlbumId,
                action = operation.action,
                success = true
            )
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            CommitItemResult(
                mediaId = assignment.mediaId,
                targetAlbumId = assignment.targetAlbumId,
                action = actionForAssignment(assignment, resolvedTrashStrategy),
                success = false,
                errorMessage = throwable.message ?: throwable::class.java.simpleName
            )
        }
    }

    private fun actionForAssignment(
        assignment: CommitAssignment,
        resolvedTrashStrategy: TrashStrategy
    ): CommitAction {
        if (assignment.targetAlbumId != CommitRequest.TRASH_TARGET_ID) {
            return CommitAction.MOVE_TO_ALBUM
        }

        return if (resolvedTrashStrategy == TrashStrategy.SYSTEM_TRASH) {
            CommitAction.MOVE_TO_SYSTEM_TRASH
        } else {
            CommitAction.MOVE_TO_TRASH_ALBUM
        }
    }

    private fun resolveOperation(
        assignment: CommitAssignment,
        request: CommitRequest,
        resolvedTrashStrategy: TrashStrategy
    ): CommitOperation {
        if (assignment.targetAlbumId != CommitRequest.TRASH_TARGET_ID) {
            return CommitOperation(
                action = CommitAction.MOVE_TO_ALBUM
            ) {
                mediaOperations.moveToAlbum(assignment.mediaId, assignment.targetAlbumId)
            }
        }

        return if (resolvedTrashStrategy == TrashStrategy.SYSTEM_TRASH) {
            CommitOperation(
                action = CommitAction.MOVE_TO_SYSTEM_TRASH
            ) {
                mediaOperations.moveToSystemTrash(assignment.mediaId)
            }
        } else {
            val trashAlbumId = request.trashAlbumId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Trash album id is required when using trash album strategy")
            CommitOperation(
                action = CommitAction.MOVE_TO_TRASH_ALBUM
            ) {
                mediaOperations.moveToTrashAlbum(assignment.mediaId, trashAlbumId)
            }
        }
    }
}

interface CommitMediaOperations {
    suspend fun moveToAlbum(mediaId: String, albumId: String)

    suspend fun moveToTrashAlbum(mediaId: String, trashAlbumId: String)

    suspend fun moveToSystemTrash(mediaId: String)
}

data class CommitRequest(
    val assignments: List<CommitAssignment>,
    val sdkInt: Int,
    val trashMode: TrashMode,
    val trashAlbumId: String?
) {
    companion object {
        const val TRASH_TARGET_ID: String = "__TRASH__"
    }
}

data class CommitAssignment(
    val mediaId: String,
    val targetAlbumId: String
)

data class CommitRunResult(
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val resolvedTrashStrategy: TrashStrategy,
    val itemResults: List<CommitItemResult>
) {
    val successCount: Int
        get() = itemResults.count { it.success }

    val failureCount: Int
        get() = itemResults.size - successCount
}

data class CommitItemResult(
    val mediaId: String,
    val targetAlbumId: String,
    val action: CommitAction,
    val success: Boolean,
    val errorMessage: String? = null
)

enum class CommitAction {
    MOVE_TO_ALBUM,
    MOVE_TO_TRASH_ALBUM,
    MOVE_TO_SYSTEM_TRASH
}

private data class CommitOperation(
    val action: CommitAction,
    val execute: suspend () -> Unit
)
