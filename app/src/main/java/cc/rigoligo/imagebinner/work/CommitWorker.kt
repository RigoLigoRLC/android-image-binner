package cc.rigoligo.imagebinner.work

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.domain.TrashMode
import cc.rigoligo.imagebinner.domain.commit.CommitAssignment
import cc.rigoligo.imagebinner.domain.commit.CommitEngine
import cc.rigoligo.imagebinner.domain.commit.CommitMediaOperations
import cc.rigoligo.imagebinner.domain.commit.CommitRequest
import cc.rigoligo.imagebinner.domain.commit.CommitRunResult
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class CommitWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val commitConfig = parseConfig(inputData)
        val sessionDao = AppDatabase.getInstance(applicationContext).sessionDao()
        val savedSession = sessionDao.getGlobalSession() ?: return Result.failure(
            Data.Builder()
                .putString(KEY_ERROR, "No saved session exists for commit")
                .build()
        )
        val persistedAssignments = sessionDao.getAssignments()
        if (persistedAssignments.isEmpty()) {
            return Result.failure(
                Data.Builder()
                    .putString(KEY_ERROR, "No saved assignments exist for commit")
                    .build()
            )
        }

        val request = CommitRequest(
            assignments = persistedAssignments.map { assignment ->
                CommitAssignment(
                    mediaId = assignment.mediaId,
                    targetAlbumId = assignment.targetAlbumId
                )
            },
            sdkInt = commitConfig.sdkInt,
            trashMode = commitConfig.trashMode,
            trashAlbumId = commitConfig.trashAlbumId
        )

        val engine = CommitEngine(mediaOperationsFactory(applicationContext))
        val runResult = engine.commit(request)
        val detailsPath = persistRunResult(savedSession.profileId, runResult)

        return Result.success(runResult.toOutputData(detailsPath))
    }

    private fun parseConfig(data: Data): CommitConfig {
        return CommitConfig(
            sdkInt = data.getInt(KEY_SDK_INT, Build.VERSION.SDK_INT),
            trashMode = TrashMode.fromStorage(
                data.getString(KEY_TRASH_MODE) ?: TrashMode.SYSTEM_TRASH.storageValue
            ),
            trashAlbumId = data.getString(KEY_TRASH_ALBUM_ID)
        )
    }

    private fun persistRunResult(profileId: Long, runResult: CommitRunResult): String {
        val parentDirectory = File(applicationContext.filesDir, RESULT_DIRECTORY_NAME)
        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            error("Unable to create commit result directory")
        }

        val outputFile = File(
            parentDirectory,
            "commit-run-${runResult.startedAtEpochMillis}-$profileId.json"
        )
        outputFile.writeText(runResult.toJson(), Charsets.UTF_8)
        return outputFile.absolutePath
    }

    private fun CommitRunResult.toJson(): String {
        val itemArray = JSONArray()
        itemResults.forEach { item ->
            val itemJson = JSONObject()
                .put("mediaId", item.mediaId)
                .put("targetAlbumId", item.targetAlbumId)
                .put("action", item.action.name)
                .put("success", item.success)
            if (item.errorMessage != null) {
                itemJson.put("errorMessage", item.errorMessage)
            }
            itemArray.put(itemJson)
        }

        return JSONObject()
            .put("startedAtEpochMillis", startedAtEpochMillis)
            .put("finishedAtEpochMillis", finishedAtEpochMillis)
            .put("resolvedTrashStrategy", resolvedTrashStrategy.name)
            .put("successCount", successCount)
            .put("failureCount", failureCount)
            .put("itemResults", itemArray)
            .toString()
    }

    private fun CommitRunResult.toOutputData(detailsReference: String): Data {
        val firstFailure = itemResults.firstOrNull { !it.success }
        return Data.Builder()
            .putLong(KEY_RESULT_STARTED_AT, startedAtEpochMillis)
            .putLong(KEY_RESULT_FINISHED_AT, finishedAtEpochMillis)
            .putString(KEY_RESULT_TRASH_STRATEGY, resolvedTrashStrategy.name)
            .putInt(KEY_RESULT_TOTAL_COUNT, itemResults.size)
            .putInt(KEY_RESULT_SUCCESS_COUNT, successCount)
            .putInt(KEY_RESULT_FAILURE_COUNT, failureCount)
            .putString(KEY_RESULT_FIRST_FAILED_MEDIA_ID, firstFailure?.mediaId)
            .putString(KEY_RESULT_FIRST_ERROR_MESSAGE, firstFailure?.errorMessage)
            .putString(KEY_RESULT_DETAILS_REFERENCE, detailsReference)
            .build()
    }

    companion object {
        const val KEY_TRASH_MODE: String = "commit_trash_mode"
        const val KEY_TRASH_ALBUM_ID: String = "commit_trash_album_id"
        const val KEY_SDK_INT: String = "commit_sdk_int"

        const val KEY_RESULT_STARTED_AT: String = "commit_result_started_at"
        const val KEY_RESULT_FINISHED_AT: String = "commit_result_finished_at"
        const val KEY_RESULT_TRASH_STRATEGY: String = "commit_result_trash_strategy"
        const val KEY_RESULT_TOTAL_COUNT: String = "commit_result_total_count"
        const val KEY_RESULT_SUCCESS_COUNT: String = "commit_result_success_count"
        const val KEY_RESULT_FAILURE_COUNT: String = "commit_result_failure_count"
        const val KEY_RESULT_FIRST_FAILED_MEDIA_ID: String = "commit_result_first_failed_media_id"
        const val KEY_RESULT_FIRST_ERROR_MESSAGE: String = "commit_result_first_error_message"
        const val KEY_RESULT_DETAILS_REFERENCE: String = "commit_result_details_reference"
        const val KEY_ERROR: String = "commit_error"
        private const val RESULT_DIRECTORY_NAME: String = "commit-results"

        @Volatile
        var mediaOperationsFactory: (Context) -> CommitMediaOperations = {
            UnsupportedCommitMediaOperations()
        }

        fun createInputData(request: CommitRequest): Data {
            return Data.Builder()
                .putString(KEY_TRASH_MODE, request.trashMode.storageValue)
                .putString(KEY_TRASH_ALBUM_ID, request.trashAlbumId)
                .putInt(KEY_SDK_INT, request.sdkInt)
                .build()
        }
    }
}

private data class CommitConfig(
    val sdkInt: Int,
    val trashMode: TrashMode,
    val trashAlbumId: String?
)

private class UnsupportedCommitMediaOperations : CommitMediaOperations {
    override suspend fun moveToAlbum(mediaId: String, albumId: String) {
        error("Media commit operations are not wired yet")
    }

    override suspend fun moveToTrashAlbum(mediaId: String, trashAlbumId: String) {
        error("Media commit operations are not wired yet")
    }

    override suspend fun moveToSystemTrash(mediaId: String) {
        error("Media commit operations are not wired yet")
    }
}
