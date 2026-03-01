package cc.rigoligo.imagebinner.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import cc.rigoligo.imagebinner.domain.commit.CommitMediaOperations

class MediaStoreCommitMediaOperations(
    private val contentResolver: ContentResolver
) : CommitMediaOperations {
    override suspend fun moveToAlbum(mediaId: String, albumId: String) {
        moveIntoBucket(mediaId = mediaId, bucketId = albumId)
    }

    override suspend fun moveToTrashAlbum(mediaId: String, trashAlbumId: String) {
        moveIntoBucket(mediaId = mediaId, bucketId = trashAlbumId)
    }

    override suspend fun moveToSystemTrash(mediaId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            error("System trash requires Android 11+.")
        }

        val mediaUri = mediaUriFromId(mediaId)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_TRASHED, 1)
        }
        val updatedRows = contentResolver.update(mediaUri, values, null, null)
        if (updatedRows <= 0) {
            error("Unable to move media $mediaId to system trash.")
        }
    }

    private fun moveIntoBucket(mediaId: String, bucketId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            error("Moving between albums requires Android 10+.")
        }

        val currentLocation = queryMediaLocation(mediaId)
            ?: error("Media $mediaId not found.")
        if (currentLocation.bucketId == bucketId) {
            return
        }

        val targetRelativePath = queryBucketRelativePath(bucketId)
            ?: error("Target album $bucketId is unavailable or empty.")
        if (currentLocation.relativePath == targetRelativePath) {
            return
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
        }
        val updatedRows = contentResolver.update(mediaUriFromId(mediaId), values, null, null)
        if (updatedRows <= 0) {
            error("Unable to move media $mediaId to target album.")
        }
    }

    private fun queryMediaLocation(mediaId: String): MediaLocation? {
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media._ID} = ?",
            arrayOf(mediaId),
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }

            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val relativePath = if (
                relativePathColumn >= 0 &&
                !cursor.isNull(relativePathColumn)
            ) {
                cursor.getString(relativePathColumn)
            } else {
                null
            }

            return MediaLocation(
                bucketId = cursor.getString(bucketIdColumn) ?: return null,
                relativePath = relativePath
            )
        }
        return null
    }

    private fun queryBucketRelativePath(bucketId: String): String? {
        val projection = arrayOf(MediaStore.MediaColumns.RELATIVE_PATH)
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.BUCKET_ID} = ?",
            arrayOf(bucketId),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }

            val relativePathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            while (true) {
                if (relativePathColumn >= 0 && !cursor.isNull(relativePathColumn)) {
                    val value = cursor.getString(relativePathColumn)
                    if (!value.isNullOrBlank()) {
                        return value
                    }
                }

                if (!cursor.moveToNext()) {
                    break
                }
            }
        }
        return null
    }

    private fun mediaUriFromId(mediaId: String) = ContentUris.withAppendedId(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        mediaId.toLongOrNull() ?: error("Invalid media id: $mediaId")
    )

    private data class MediaLocation(
        val bucketId: String,
        val relativePath: String?
    )
}
