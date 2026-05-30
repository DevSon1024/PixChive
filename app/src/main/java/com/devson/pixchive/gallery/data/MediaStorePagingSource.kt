package com.devson.pixchive.gallery.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.devson.pixchive.gallery.data.models.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStorePagingSource(
    private val repository: MediaStoreRepository,
    private val bucketId: String? = null
) : PagingSource<Int, GalleryImage>() {

    override fun getRefreshKey(state: PagingState<Int, GalleryImage>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(state.config.pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryImage> = withContext(Dispatchers.IO) {
        val position = params.key ?: 0
        val limit = params.loadSize

        try {
            val images = if (bucketId != null) {
                repository.getImagesForFolderPaged(bucketId, limit, position)
            } else {
                repository.getAllImagesPaged(limit, position)
            }

            val nextKey = if (images.isEmpty() || images.size < limit) null else position + images.size
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
