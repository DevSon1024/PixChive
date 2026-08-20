package com.devson.pixchive.core.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.devson.pixchive.core.data.models.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PagingSource for MediaStore images.
 *
 * @param repository  The data source.
 * @param bucketId    When non-null, only images in that bucket are paged.
 * @param sortOrder   MediaStore sort-order SQL string (e.g. "date_added DESC").
 *                    Defaults to date-added descending — the natural timeline order.
 */
class MediaStorePagingSource(
    private val repository: MediaStoreRepository,
    private val bucketId: String? = null,
    private val sortOrder: String = "date_added DESC"
) : PagingSource<Int, GalleryImage>() {

    override fun getRefreshKey(state: PagingState<Int, GalleryImage>): Int? {
        // Return the load position closest to the current anchor so that after
        // invalidation the grid jumps back to roughly where the user was.
        return state.anchorPosition?.let { anchor ->
            (anchor - state.config.pageSize / 2).coerceAtLeast(0)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryImage> =
        withContext(Dispatchers.IO) {
            val position = params.key ?: 0
            val limit = params.loadSize

            try {
                val images = if (bucketId != null) {
                    repository.getImagesForFolderPaged(bucketId, limit, position, sortOrder)
                } else {
                    repository.getAllImagesPaged(limit, position, sortOrder)
                }

                val nextKey = if (images.size < limit) null else position + images.size
                val prevKey = if (position == 0) null else maxOf(0, position - limit)

                LoadResult.Page(
                    data = images,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
}