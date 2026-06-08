package com.example.data

data class MobilePost(
  val id: String,
  val author: String,
  val username: String,
  val avatarUrl: String,
  val content: String,
  val vibeCategory: String,
  var likes: Int,
  var hasLiked: Boolean = false,
  val created: String,
  val mediaUrl: String? = null,
  val mediaType: String? = "text",
  val commentsCount: Int = 0
)

data class MobileRoom(
  val id: String,
  val title: String,
  val host: String,
  val description: String,
  val theme: String,
  var membersCount: Int,
  val maxMembers: Int = 50
)

data class MobileMoment(
  val id: String,
  val author: String,
  val emoji: String,
  val isUnread: Boolean = true
)

data class MobileChatMessage(
  val sender: String,
  val content: String,
  val isFromMe: Boolean,
  val time: String
)

data class MobileCommunity(
  val id: String,
  val name: String,
  val handle: String,
  val members: Int,
  val vibe: String,
  val isJoined: Boolean = false
)
