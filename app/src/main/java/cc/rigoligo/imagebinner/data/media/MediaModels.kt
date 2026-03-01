package cc.rigoligo.imagebinner.data.media

import cc.rigoligo.imagebinner.domain.SortOrder

data class AlbumItem(
    val id: String,
    val name: String,
    val photoCount: Int,
    val coverPhotoId: String? = null
)

data class PhotoItem(
    val id: String,
    val albumId: String = "",
    val displayName: String = "",
    val capturedAt: Long
)

fun photoSortComparator(order: SortOrder): Comparator<PhotoItem> {
    return when (order) {
        SortOrder.OLDEST_FIRST -> compareBy<PhotoItem> { it.capturedAt }
        SortOrder.NEWEST_FIRST -> compareByDescending<PhotoItem> { it.capturedAt }
    }.thenBy { it.id }
}
