package com.example

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Response structures reflecting backend routes ---

data class AuthUser(
  val id: String,
  val username: String,
  val email: String,
  @Json(name = "displayName") val displayName: String?,
  @Json(name = "avatarUrl") val avatarUrl: String?,
  val xp: Int?,
  @Json(name = "streak_count") val streakCount: Int?
)

data class AuthResponse(
  val message: String? = null,
  val token: String? = null,
  @Json(name = "refreshToken") val refreshToken: String? = null,
  val user: AuthUser? = null,
  val requiresOtp: Boolean? = null
)

data class BackendPost(
  val id: String,
  @Json(name = "author_id") val authorId: String?,
  val content: String,
  @Json(name = "media_url") val mediaUrl: String? = null,
  @Json(name = "media_type") val mediaType: String? = null,
  @Json(name = "vibe_category") val vibeCategory: String? = null,
  @Json(name = "likes_count") val likesCount: Int? = null,
  @Json(name = "comments_count") val commentsCount: Int? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "display_name") val displayName: String? = null,
  val username: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null
)

data class CreatePostRequest(
  val content: String,
  @Json(name = "vibeCategory") val vibeCategory: String
)

data class BackendLikeResponse(
  val liked: Boolean,
  @Json(name = "likes_count") val likesCount: Any? = null
)

data class BackendExplorer(
  val id: String,
  val username: String,
  @Json(name = "display_name") val displayName: String?,
  @Json(name = "avatar_url") val avatarUrl: String?,
  @Json(name = "mood_state") val moodState: String?,
  @Json(name = "mood_emoji") val moodEmoji: String?,
  val xp: Int?
)

data class BackendChat(
  val id: String,
  @Json(name = "is_group") val isGroup: Boolean? = null,
  @Json(name = "group_name") val groupName: String? = null,
  @Json(name = "peer_id") val peerId: String? = null,
  @Json(name = "peer_name") val peerName: String? = null,
  @Json(name = "peer_avatar") val peerAvatar: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

data class InitiateChatRequest(
  val recipientId: String,
  val isGroup: Boolean = false,
  val groupName: String? = null
)

data class InitiateChatResponse(
  val message: String? = null,
  val chatId: String? = null,
  @Json(name = "peer_id") val peerId: String? = null
)

data class BackendMessage(
  val id: String,
  @Json(name = "chat_id") val chatId: String?,
  @Json(name = "sender_id") val senderId: String?,
  val content: String?,
  @Json(name = "created_at") val createdAt: String?,
  @Json(name = "sender_name") val senderName: String? = null,
  @Json(name = "sender_avatar") val senderAvatar: String? = null
)

data class SendMessageRequest(
  val content: String
)

data class BackendRoom(
  val id: String,
  @Json(name = "host_id") val hostId: String? = null,
  val title: String,
  val description: String? = null,
  val theme: String? = null,
  @Json(name = "active_members_count") val activeMembersCount: Int? = null,
  @Json(name = "max_members") val maxMembers: Int? = null
)

data class CreateRoomRequest(
  val title: String,
  val description: String,
  val theme: String
)

interface SafariSphereApi {
  @POST("auth/signup")
  suspend fun signup(@Body body: Map<String, String>): AuthResponse

  @POST("auth/login")
  suspend fun login(@Body body: Map<String, String>): AuthResponse

  @GET("auth/explorers")
  suspend fun getExplorers(): List<BackendExplorer>

  @POST("auth/profile/edit")
  suspend fun editProfile(@Body body: Map<String, String>): Map<String, Any>

  @GET("posts")
  suspend fun getPosts(@Query("category") category: String? = null): List<BackendPost>

  @POST("posts")
  suspend fun createPost(@Body request: CreatePostRequest): Map<String, Any>

  @POST("posts/{id}/like")
  suspend fun likePost(@Path("id") id: String): BackendLikeResponse

  @GET("chats")
  suspend fun getChats(): List<BackendChat>

  @POST("chats")
  suspend fun initiateChat(@Body request: InitiateChatRequest): InitiateChatResponse

  @GET("chats/{id}")
  suspend fun getChatMessages(@Path("id") id: String): List<BackendMessage>

  @POST("chats/{id}")
  suspend fun sendMessage(@Path("id") id: String, @Body body: SendMessageRequest): Map<String, Any>

  @GET("rooms")
  suspend fun getRooms(): List<BackendRoom>

  @POST("rooms")
  suspend fun createRoom(@Body request: CreateRoomRequest): Map<String, Any>
}

object NetworkService {
  private const val BASE_URL = "https://safarisphere.koyeb.app/api/v1/"
  
  private var authToken: String? = null

  fun setAuthToken(token: String?) {
    authToken = token
  }

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  }

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .addInterceptor { chain ->
      val original = chain.request()
      val builder = original.newBuilder()
      authToken?.let {
        builder.header("Authorization", "Bearer $it")
      }
      chain.proceed(builder.build())
    }
    .addInterceptor(loggingInterceptor)
    .build()

  val api: SafariSphereApi by lazy {
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(SafariSphereApi::class.java)
  }
}
