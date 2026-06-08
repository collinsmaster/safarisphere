package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val postId: String,
    val authorId: String,
    val content: String,
    val createdAt: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String
)
