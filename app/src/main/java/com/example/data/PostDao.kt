package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY created DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)
    
    @Query("UPDATE posts SET likes = :likes, hasLiked = :hasLiked WHERE id = :id")
    suspend fun updatePostStatus(id: String, likes: Int, hasLiked: Boolean)
}
