package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "posts")
data class PostEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val author: String,
  val username: String,
  val avatarUrl: String,
  val content: String,
  val vibeCategory: String,
  val likes: Int,
  val hasLiked: Boolean,
  val created: String,
  val mediaUrl: String?,
  val mediaType: String?,
  val commentsCount: Int
)
