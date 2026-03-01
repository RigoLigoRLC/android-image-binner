package cc.rigoligo.imagebinner.data.media

import cc.rigoligo.imagebinner.domain.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {
    @Test
    fun oldestFirst_sortsByCapturedAtAscending() {
        val items = listOf(
            PhotoItem(id = "3", albumId = "a", displayName = "c.jpg", capturedAt = 3000L),
            PhotoItem(id = "1", albumId = "a", displayName = "a.jpg", capturedAt = 1000L),
            PhotoItem(id = "2", albumId = "a", displayName = "b.jpg", capturedAt = 2000L)
        )

        val sorted = items.sortedWith(photoSortComparator(SortOrder.OLDEST_FIRST))

        assertEquals(listOf("1", "2", "3"), sorted.map { it.id })
    }

    @Test
    fun newestFirst_sortsByCapturedAtDescending() {
        val items = listOf(
            PhotoItem(id = "3", albumId = "a", displayName = "c.jpg", capturedAt = 3000L),
            PhotoItem(id = "1", albumId = "a", displayName = "a.jpg", capturedAt = 1000L),
            PhotoItem(id = "2", albumId = "a", displayName = "b.jpg", capturedAt = 2000L)
        )

        val sorted = items.sortedWith(photoSortComparator(SortOrder.NEWEST_FIRST))

        assertEquals(listOf("3", "2", "1"), sorted.map { it.id })
    }
}
