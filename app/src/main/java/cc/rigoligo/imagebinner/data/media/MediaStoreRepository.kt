package cc.rigoligo.imagebinner.data.media

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import cc.rigoligo.imagebinner.domain.SortOrder

class MediaStoreRepository(
    private val contentResolver: ContentResolver
) {
    fun listAlbums(): List<AlbumItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val albumsById = linkedMapOf<String, MutableAlbumRow>()

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val photoIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val albumNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val albumId = cursor.getString(albumIdColumn) ?: continue
                val albumName = cursor.getString(albumNameColumn).orEmpty().ifBlank { UNKNOWN_ALBUM_NAME }
                val photoId = cursor.getLong(photoIdColumn).toString()
                val existing = albumsById[albumId]

                if (existing == null) {
                    albumsById[albumId] = MutableAlbumRow(
                        name = albumName,
                        photoCount = 1,
                        coverPhotoId = photoId
                    )
                } else {
                    existing.photoCount += 1
                }
            }
        }

        return albumsById
            .map { (id, row) ->
                AlbumItem(
                    id = id,
                    name = row.name,
                    photoCount = row.photoCount,
                    coverPhotoId = row.coverPhotoId
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun listPhotosByAlbum(albumId: String, order: SortOrder): List<PhotoItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
        )

        val photos = buildList {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.BUCKET_ID} = ?",
                arrayOf(albumId),
                null
            )?.use { cursor ->
                val photoIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    add(
                        PhotoItem(
                            id = cursor.getLong(photoIdColumn).toString(),
                            albumId = cursor.getString(albumIdColumn) ?: albumId,
                            displayName = cursor.getString(displayNameColumn).orEmpty(),
                            capturedAt = readCaptureDate(cursor, dateTakenColumn, dateAddedColumn)
                        )
                    )
                }
            }
        }

        return photos.sortedWith(photoSortComparator(order))
    }

    fun getPhotoCaptureDate(mediaId: String): Long? {
        val projection = arrayOf(
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
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

            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            return readCaptureDate(cursor, dateTakenColumn, dateAddedColumn)
        }

        return null
    }

    private fun readCaptureDate(cursor: Cursor, dateTakenColumn: Int, dateAddedColumn: Int): Long {
        val dateTaken = cursor.longOrNull(dateTakenColumn)
        if (dateTaken != null && dateTaken > 0L) {
            return dateTaken
        }

        val dateAddedSeconds = cursor.longOrNull(dateAddedColumn)
        return if (dateAddedSeconds != null && dateAddedSeconds > 0L) {
            dateAddedSeconds * MILLIS_PER_SECOND
        } else {
            0L
        }
    }

    private fun Cursor.longOrNull(columnIndex: Int): Long? {
        if (columnIndex == -1 || isNull(columnIndex)) {
            return null
        }
        return getLong(columnIndex)
    }

    private data class MutableAlbumRow(
        val name: String,
        var photoCount: Int,
        val coverPhotoId: String
    )

    private companion object {
        private const val MILLIS_PER_SECOND = 1000L
        private const val UNKNOWN_ALBUM_NAME = "Unknown"
    }
}
