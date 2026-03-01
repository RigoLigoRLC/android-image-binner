package cc.rigoligo.imagebinner.domain.commit

import cc.rigoligo.imagebinner.domain.TrashMode

enum class TrashStrategy {
    SYSTEM_TRASH,
    TRASH_ALBUM
}

fun resolveTrashStrategy(apiLevel: Int, trashMode: TrashMode): TrashStrategy {
    return if (apiLevel >= API_LEVEL_SYSTEM_TRASH && trashMode == TrashMode.SYSTEM_TRASH) {
        TrashStrategy.SYSTEM_TRASH
    } else {
        TrashStrategy.TRASH_ALBUM
    }
}

private const val API_LEVEL_SYSTEM_TRASH: Int = 30
