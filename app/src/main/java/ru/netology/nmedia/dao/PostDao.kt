package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity WHERE shown = 1 ORDER BY id DESC")
    suspend fun getVisibleOnce(): List<PostEntity>
    @Query("SELECT COUNT(*) FROM PostEntity WHERE shown = 0")
    fun getHiddenCount(): Flow<Int>
    @Query("SELECT MAX(id) FROM PostEntity")
    suspend fun getMaxId(): Long?
    @Query("UPDATE PostEntity SET shown = 1 WHERE shown = 0")
    suspend fun markAllAsShown()

    @Query("SELECT * FROM PostEntity WHERE shown = 1 ORDER BY id DESC")
    fun getVisible(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) == 0 FROM PostEntity")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("SELECT * FROM PostEntity WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?
}
