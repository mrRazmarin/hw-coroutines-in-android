package ru.netology.nmedia.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okio.IOException
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.net.ConnectException
import kotlin.coroutines.cancellation.CancellationException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data: Flow<List<Post>> = dao.getVisible().map(List<PostEntity>::toDto)
    override val hiddenCount: Flow<Int> = dao.getHiddenCount()

    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) throw ApiError(response.code(), "error_api")
            val body = response.body() ?: throw ApiError(response.code(), "error_api")
            dao.insert(body.toEntity(shown = true))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw AppError.from(e)
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.service.save(post)
            if (!response.isSuccessful) throw ApiError(response.code(), "error_api")
            val body = response.body() ?: throw ApiError(response.code(), "error_api")
            dao.insert(PostEntity.fromDto(body))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw AppError.from(e)
        }
    }

    override suspend fun removeById(id: Long) {
        val cachedPost = dao.getById(id) ?: throw UnknownError
        dao.removeById(id)

        try {
            val response = PostsApi.service.removeById(id)
            if (!response.isSuccessful) throw ApiError(response.code(), "error_api")
        } catch (e: CancellationException) {
            dao.insert(cachedPost)
            throw e
        } catch (e: Throwable) {
            dao.insert(cachedPost)
            throw AppError.from(e)
        }
    }

    override suspend fun likeById(id: Long) {
        val cachedPost = dao.getById(id) ?: throw UnknownError
        val willLike = !cachedPost.likedByMe
        val optimistic = cachedPost.copy(
            likedByMe = willLike,
            likes = cachedPost.likes + if (willLike) 1 else -1,
        )
        dao.insert(optimistic)

        try {
            val response = if (cachedPost.likedByMe) {
                PostsApi.service.dislikeById(id)
            } else {
                PostsApi.service.likeById(id)
            }
            if (!response.isSuccessful) throw ApiError(response.code(), "error_api")
            val body = response.body() ?: throw ApiError(response.code(), "error_api")
            dao.insert(PostEntity.fromDto(body))
        } catch (e: CancellationException) {
            dao.insert(cachedPost)
            throw e
        } catch (e: Throwable) {
            dao.insert(cachedPost)
            throw AppError.from(e)
        }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = PostsApi.service.getNewer(id = id)
            if (!response.isSuccessful) throw ApiError(response.code(), "error_api")
            val body = response.body() ?: throw ApiError(response.code(), "error_api")
            dao.insert(body.toEntity(shown = false))
            emit(body.size)
        }
    }.catch { e ->
        throw AppError.from(e)
    }

    override suspend fun showAll() {
        dao.markAllAsShown()
    }
}
