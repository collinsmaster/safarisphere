package com.example

import android.os.Bundle
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.MobilePost
import com.example.data.MobileRoom
import com.example.data.MobileMoment
import com.example.data.MobileChatMessage
import com.example.data.MobileCommunity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CornerSize

// ==============================================================================
// 🌟 HELPER METADATA MODELS, PARSERS, AND CUSTOM COLOR FILTERS FOR LOCAL MEDIA POSTS
// ==============================================================================
data class MediaMetadata(val ratio: String?, val rotation: Float, val filter: String?)

fun parsePostMetadata(content: String): Pair<String, MediaMetadata> {
  var cleanContent = content
  var ratio: String? = null
  var rotation = 0f
  var filter: String? = null
  
  val ratioRegex = "\\[RATIO:([^\\]]*)\\]".toRegex()
  val rotRegex = "\\[ROT:([^\\]]*)\\]".toRegex()
  val filterRegex = "\\[FILTER:([^\\]]*)\\]".toRegex()
  
  ratioRegex.find(content)?.let {
    ratio = it.groupValues[1]
    cleanContent = cleanContent.replace(it.value, "")
  }
  rotRegex.find(content)?.let {
    rotation = it.groupValues[1].toFloatOrNull() ?: 0f
    cleanContent = cleanContent.replace(it.value, "")
  }
  filterRegex.find(content)?.let {
    filter = it.groupValues[1]
    cleanContent = cleanContent.replace(it.value, "")
  }
  return cleanContent.trim() to MediaMetadata(ratio, rotation, filter)
}

fun getComposeColorFilter(filterName: String): androidx.compose.ui.graphics.ColorFilter? {
  return when (filterName) {
    "Cyberpunk" -> {
      androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
          floatArrayOf(
            1.2f, 0f, 0.4f, 0f, 30f,
            0f, 0.9f, 0.2f, 0f, 0f,
            0.2f, 0.4f, 1.4f, 0f, 40f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
    "Mono" -> {
      androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
          floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
    "Amber" -> {
      androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
          floatArrayOf(
            1.1f, 0f, 0f, 0f, 40f,
            0f, 0.9f, 0f, 0f, 10f,
            0f, 0f, 0.7f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
    "Forest" -> {
      androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
          floatArrayOf(
            0.8f, 0f, 0f, 0f, -10f,
            0f, 1.2f, 0.1f, 0f, 20f,
            0f, 0.1f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
          )
        )
      )
    }
    else -> null
  }
}

// ==============================================================================
// 🌟 SAFARI SPHERE MAIN ENTRYPOINT & UI ENGINE
// ==============================================================================
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SafariSphereApp()
      }
    }
  }
}

// Data models moved to /app/src/main/java/com/example/data/Models.kt

// ==============================================================================
// 📱 MAIN APPLICATION LAYOUT
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafariSphereApp() {
  var selectedTab by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()
  val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 5 })

  val context = LocalContext.current
  val db = remember {
    Room.databaseBuilder(
      context,
      AppDatabase::class.java, "safari-sphere-db"
    ).build()
  }
  val prefs = remember(context) { context.getSharedPreferences("safari_sphere_prefs", Context.MODE_PRIVATE) }
  var isAuthenticated by remember { mutableStateOf(prefs.getBoolean("is_authenticated", false)) }
  var userNickname by remember { mutableStateOf(prefs.getString("user_nickname", "Pioneer Prime") ?: "Pioneer Prime") }
  var userHandle by remember { mutableStateOf(prefs.getString("user_handle", "explorer_prime") ?: "explorer_prime") }
  var userBio by remember { mutableStateOf(prefs.getString("user_bio", "A fresh pioneer in Safari Sphere!") ?: "A fresh pioneer in Safari Sphere!") }
  var userLocation by remember { mutableStateOf(prefs.getString("user_location", "Savannah Valley") ?: "Savannah Valley") }
  var userAvatarUrl by remember { mutableStateOf(prefs.getString("user_avatar_url", null)) }
  var userJoinDate by remember { mutableStateOf(prefs.getString("user_join_date", null)) }
  var userEmail by remember { mutableStateOf(prefs.getString("user_email", "pioneer@safari.com") ?: "pioneer@safari.com") }
  var userHeadline by remember { mutableStateOf(prefs.getString("user_headline", "Exploring uncharted digital frequencies...") ?: "Exploring uncharted digital frequencies...") }
  var userInterests by remember { mutableStateOf(prefs.getString("user_interests", "Wildlife Safari, Sound Design, Live Beats") ?: "Wildlife Safari, Sound Design, Live Beats") }
  var isFetchingProfile by remember { mutableStateOf(false) }

  // State Simulation / Local Fallback Store matching MongoDB / Postgres listings
  val momentsList = remember {
    mutableStateListOf(
      MobileMoment("m1", "Leo 🦁", "🦁", true),
      MobileMoment("m2", "Luna ✨", "🦄", true),
      MobileMoment("m3", "Sienna 🌴", "🐆", false),
      MobileMoment("m4", "Zach 🏕️", "🦊", false),
      MobileMoment("m5", "Zoe 🌌", "🐱", true)
    )
  }

  val postsList = remember {
    mutableStateListOf<MobilePost>()
  }

  val roomsList = remember {
    mutableStateListOf(
      MobileRoom("r1", "Savanna Beats & Visuals", "Sienna Dusk", "Chill electronic synthesis playing live under cosmic sky projections.", "neon-sunset", 18),
      MobileRoom("r2", "Night Safari Chat & Stories", "Zach Wild", "Sharing mysterious ranger stories deep from the savanna. Mic open!", "cyber-desert", 8),
      MobileRoom("r3", "Eco-Sphere & Preservation", "Green Heart", "Discussing green energy frameworks for digital-first spaces.", "emerald-deep", 4)
    )
  }

  val chatMessages = remember {
    mutableStateListOf(
      MobileChatMessage("SphereMate AI", "Welcome to Safari Sphere! I'm your digital companion. How can I help you vibing with the crew or writing captivating posts today? 🦁✨", false, "10:42")
    )
  }

  val communitiesList = remember {
    mutableStateListOf(
      MobileCommunity("c1", "Savannah Photography 📷", "nature_lenses", 1420, "Capture natural hues"),
      MobileCommunity("c2", "Ambient Synthesis 🎹", "vibe_frequencies", 894, "Electronic soundscapes"),
      MobileCommunity("c3", "Digital Rangers 🏕️", "sphere_rangers", 512, "Expeditions & logs")
    )
  }

  // Profile status references
  var userXp by remember { mutableStateOf(prefs.getInt("user_xp", 185)) }
  var userStreak by remember { mutableStateOf(prefs.getInt("user_streak", 3)) }
  var userMood by remember { mutableStateOf(prefs.getString("user_mood", "Chill") ?: "Chill") }
  var userMoodEmoji by remember { mutableStateOf(prefs.getString("user_mood_emoji", "🦁") ?: "🦁") }

  // New Post Sheets parameters
  var showNewPostSheet by remember { mutableStateOf(false) }
  var newPostContent by remember { mutableStateOf("") }
  var activeCategory by remember { mutableStateOf("Adventurer") }

  // Media & FAB state variables
  var isFabVisible by remember { mutableStateOf(true) }
  var showMediaEditor by remember { mutableStateOf(false) }
  var selectedMediaFilter by remember { mutableStateOf("Normal") }
  var mediaRotationZ by remember { mutableStateOf(0f) }
  var selectedAspectRatio by remember { mutableStateOf("Original") }

  // Chat conversation parameters
  var typingMessage by remember { mutableStateOf("") }
  var isAiTyping by remember { mutableStateOf(false) }

  // Interactive Live Moment Dialog overlay parameter
  var selectedMoment by remember { mutableStateOf<MobileMoment?>(null) }

  val updateUserXp: (Int) -> Unit = { xpToAdd ->
    val newXp = userXp + xpToAdd
    userXp = newXp
    prefs.edit().putInt("user_xp", newXp).apply()
  }

  // General Notification attributes
  var triggerProfileSettingsShow by remember { mutableStateOf(false) }
  var showNotificationsDialog by remember { mutableStateOf(false) }
  val notificationsList = remember { mutableStateListOf<BackendNotification>() }
  var isFetchingNotifications by remember { mutableStateOf(false) }

  // General Comments attributes
  var activePostForComments by remember { mutableStateOf<MobilePost?>(null) }
  val commentsListForActivePost = remember { mutableStateListOf<BackendComment>() }
  var isFetchingComments by remember { mutableStateOf(false) }
  var typingCommentText by remember { mutableStateOf("") }
  var isPostingComment by remember { mutableStateOf(false) }

  // Social feed, rooms, and uploading states
  var isFetchingPosts by remember { mutableStateOf(false) }
  var isFetchingRooms by remember { mutableStateOf(false) }
  var isUploadingPost by remember { mutableStateOf(false) }
  var uploadProgressAmount by remember { mutableStateOf(0f) }
  var uploadTelemetryText by remember { mutableStateOf("Initializing Upload...") }

  // Compose additions
  var postMediaUrl by remember { mutableStateOf("") }
  var postMediaType by remember { mutableStateOf("text") } // "text", "image", "video"

  val postMediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      postMediaUrl = uri.toString()
      selectedMediaFilter = "Normal"
      mediaRotationZ = 0f
      selectedAspectRatio = "Original"
    }
  }

  val loadCommentsForPost: (String) -> Unit = { postId ->
    scope.launch {
      isFetchingComments = true
      try {
        val comments = NetworkService.api.getComments(postId)
        commentsListForActivePost.clear()
        commentsListForActivePost.addAll(comments)
      } catch (e: Exception) {
        commentsListForActivePost.clear()
      } finally {
        isFetchingComments = false
      }
    }
  }

  val postComment: (String, String) -> Unit = { postId, text ->
    if (text.isNotBlank() && !isPostingComment) {
      isPostingComment = true
      scope.launch {
        try {
          NetworkService.api.createComment(postId, CreateCommentRequest(content = text))
          typingCommentText = ""
          loadCommentsForPost(postId)
          val idx = postsList.indexOfFirst { it.id == postId }
          if (idx != -1) {
            val post = postsList[idx]
            postsList[idx] = post.copy(commentsCount = post.commentsCount + 1)
          }
          updateUserXp(20)
        } catch (e: Exception) {
          val newC = BackendComment(
            id = "c_${System.currentTimeMillis()}",
            postId = postId,
            authorId = "current_user",
            parentId = null,
            content = text,
            createdAt = "Just now",
            displayName = userNickname,
            username = userHandle,
            avatarUrl = ""
          )
          commentsListForActivePost.add(newC)
          typingCommentText = ""
          val idx = postsList.indexOfFirst { it.id == postId }
          if (idx != -1) {
            val post = postsList[idx]
            postsList[idx] = post.copy(commentsCount = post.commentsCount + 1)
          }
        } finally {
          isPostingComment = false
        }
      }
    }
  }



  val loadNotificationsFromBackend = {
    scope.launch {
      isFetchingNotifications = true
      try {
        val notifications = NetworkService.api.getNotifications()
        notificationsList.clear()
        notificationsList.addAll(notifications)
      } catch (e: Exception) {
        notificationsList.clear()
        notificationsList.add(
          BackendNotification(
            id = "n_mock_1",
            receiverId = "current_user",
            senderId = "u_tester_2",
            type = "like",
            targetId = postsList.firstOrNull()?.id ?: "p1",
            isRead = false,
            createdAt = "5 mins ago",
            senderUsername = "ranger_zach",
            senderDisplayName = "Ranger Zach 🏕️",
            senderAvatarUrl = "",
            postContent = "Exploring neon sunsets in cyber-desert",
            postMediaUrl = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?auto=format&fit=crop&w=600&q=80",
            postMediaType = "image"
          )
        )
      } finally {
        isFetchingNotifications = false
      }
    }
  }

  val markNotificationsRead = {
    scope.launch {
      try {
        NetworkService.api.markNotificationsRead()
        loadNotificationsFromBackend()
      } catch (e: Exception) {}
    }
  }

  val updateMood: (String, String) -> Unit = { text, emo ->
    userMood = text
    userMoodEmoji = emo
    prefs.edit().putString("user_mood", text).putString("user_mood_emoji", emo).apply()
    updateUserXp(5)
  }

  // --- Real Safari Private Chat States ---
  val activeChatId = remember { mutableStateOf<String?>(null) }
  val activeChatMessages = remember { mutableStateListOf<BackendMessage>() }
  val explorersList = remember { mutableStateListOf<BackendExplorer>() }
  val activeChatsList = remember { mutableStateListOf<BackendChat>() }
  var isFetchingExplorers by remember { mutableStateOf(false) }
  var isFetchingChats by remember { mutableStateOf(false) }
  var authToken by remember { mutableStateOf(prefs.getString("auth_token", null)) }

  // Inject token dynamically on modification
  LaunchedEffect(authToken) {
    NetworkService.setAuthToken(authToken)
  }

  val loadUserProfile = {
    scope.launch {
      if (isAuthenticated) {
        try {
          isFetchingProfile = true
          val profile = NetworkService.api.getProfile()
          userNickname = profile.displayName ?: profile.username ?: userNickname
          userHandle = profile.username ?: userHandle
          userBio = profile.bio ?: userBio
          userLocation = profile.locationLabel ?: userLocation
          userAvatarUrl = profile.avatarUrl
          userJoinDate = profile.createdAt
          userEmail = profile.email ?: userEmail
          
          prefs.edit()
            .putString("user_nickname", userNickname)
            .putString("user_handle", userHandle)
            .putString("user_bio", userBio)
            .putString("user_location", userLocation)
            .putString("user_avatar_url", userAvatarUrl)
            .putString("user_join_date", userJoinDate)
            .putString("user_email", userEmail)
            .apply()
        } catch (e: Exception) {
          // Log or fallback quietly
        } finally {
          isFetchingProfile = false
        }
      }
    }
  }

  val loadPostsFromBackend = {
    scope.launch {
      isFetchingPosts = true
      // Load local posts initially for immediate, seamless rendering
      val localPosts = getLocalStoredPosts(context)
      if (localPosts.isNotEmpty() && postsList.isEmpty()) {
        postsList.addAll(localPosts)
      }
      try {
        val backendPosts = NetworkService.api.getPosts()
        val mergedPosts = mutableListOf<MobilePost>()
        backendPosts.forEach { bp ->
          mergedPosts.add(
            MobilePost(
              id = bp.id,
              author = bp.displayName ?: bp.username ?: "Anonymous Pioneer",
              username = bp.username ?: "anonymous_user",
              avatarUrl = bp.avatarUrl ?: "",
              content = bp.content,
              vibeCategory = bp.vibeCategory ?: "Adventurer",
              likes = bp.likesCount ?: 0,
              hasLiked = bp.hasLiked ?: false,
              created = "Just now",
              mediaUrl = bp.mediaUrl,
              mediaType = bp.mediaType ?: "text",
              commentsCount = bp.commentsCount ?: 0
            )
          )
        }
        
        // Merge in any local posts that are not yet on the backend feed
        val backendIdsSet = backendPosts.map { it.id }.toSet()
        localPosts.forEach { lp ->
          if (!backendIdsSet.contains(lp.id)) {
            mergedPosts.add(0, lp)
          }
        }
        
        postsList.clear()
        postsList.addAll(mergedPosts)
        saveAllPostsToLocalStorage(context, mergedPosts)
      } catch (e: Exception) {
        if (postsList.isEmpty()) {
          postsList.addAll(localPosts)
        }
      } finally {
        isFetchingPosts = false
      }
    }
  }

  val loadRoomsFromBackend = {
    scope.launch {
      isFetchingRooms = true
      try {
        val serverRooms = NetworkService.api.getRooms()
        if (serverRooms.isNotEmpty()) {
          roomsList.clear()
          serverRooms.forEach { r ->
            roomsList.add(
              MobileRoom(
                id = r.id,
                title = r.title.replace("vybe", "sphere", ignoreCase = true).replace("Vybe", "Sphere", ignoreCase = true),
                host = r.hostId ?: "Anonymous Host",
                description = (r.description ?: "").replace("vybe", "sphere", ignoreCase = true).replace("Vybe", "Sphere", ignoreCase = true),
                theme = r.theme ?: "neon-sunset",
                membersCount = r.activeMembersCount ?: 0,
                maxMembers = r.maxMembers ?: 50
              )
            )
          }
        }
      } catch (e: Exception) {
        // quiet fallback
      } finally {
        isFetchingRooms = false
      }
    }
  }

  val loadExplorers = {
    scope.launch {
      isFetchingExplorers = true
      try {
        val list = NetworkService.api.getExplorers()
        explorersList.clear()
        explorersList.addAll(list)
      } catch (e: Exception) {
        // Fallback list of pioneers in savanna directory
        explorersList.clear()
        explorersList.add(BackendExplorer("u_tester_1", "luna_quest", "Luna Wilde ✨", "", "Vibing", "✨", 450))
        explorersList.add(BackendExplorer("u_tester_2", "ranger_zach", "Ranger Zach 🏕️", "", "Creating", "🏕️", 210))
        explorersList.add(BackendExplorer("u_tester_3", "sound_sienna", "Sienna Dusk 🎹", "", "Chill", "🎹", 680))
      } finally {
        isFetchingExplorers = false
      }
    }
  }

  val loadChats = {
    scope.launch {
      isFetchingChats = true
      try {
        val list = NetworkService.api.getChats()
        activeChatsList.clear()
        activeChatsList.addAll(list)
      } catch (e: Exception) {
        activeChatsList.clear()
        activeChatsList.add(BackendChat("chat_mock_1", false, null, "u_tester_1", "Luna Wilde ✨", "", ""))
      } finally {
        isFetchingChats = false
      }
    }
  }

  val loadMessages = { id: String ->
    scope.launch {
      try {
        val list = NetworkService.api.getChatMessages(id)
        activeChatMessages.clear()
        activeChatMessages.addAll(list)
      } catch (e: Exception) {
        // Fallback/Seed message
      }
    }
  }

  val selectExplorer = { explorer: BackendExplorer ->
    scope.launch {
      try {
        val response = NetworkService.api.initiateChat(InitiateChatRequest(recipientId = explorer.id))
        val cid = response.chatId
        if (cid != null) {
          activeChatId.value = cid
          loadMessages(cid)
        }
      } catch (e: Exception) {
        activeChatId.value = "chat_mock_new"
        activeChatMessages.clear()
        activeChatMessages.add(
          BackendMessage(
            "m_welcome",
            "chat_mock_new",
            explorer.id,
            "Safari chat initialized with ${explorer.displayName ?: explorer.username}! Say hello! 👋",
            "",
            explorer.displayName ?: explorer.username,
            ""
          )
        )
      }
    }
  }

  // Reload feed posts and explorers on authentication/tab change
  LaunchedEffect(isAuthenticated, authToken) {
    if (isAuthenticated) {
      loadPostsFromBackend()
      loadUserProfile()
      loadRoomsFromBackend()
    }
  }

  LaunchedEffect(selectedTab) {
    if (selectedTab == 1) {
      loadRoomsFromBackend()
    }
    if (selectedTab == 2 || selectedTab == 3) {
      loadExplorers()
    }
    if (selectedTab == 2) {
      loadChats()
    } else if (selectedTab == 4) {
      loadUserProfile()
    }
  }

  // Poll chat messages periodically in active direct chat
  LaunchedEffect(activeChatId.value) {
    val id = activeChatId.value
    if (id != null) {
      while (activeChatId.value == id) {
        loadMessages(id)
        delay(3000)
      }
    }
  }

  if (!isAuthenticated) {
    BentoAuthScreen(
      onAuthenticated = { nick, handle, token ->
        prefs.edit()
          .putBoolean("is_authenticated", true)
          .putString("user_nickname", nick)
          .putString("user_handle", handle)
          .putString("auth_token", token)
          .apply()
        userNickname = nick
        userHandle = handle
        authToken = token
        isAuthenticated = true
      }
    )
  } else {
    Scaffold(
      modifier = Modifier.fillMaxSize().imePadding(),
    containerColor = DeepObsidian,
    topBar = {
      CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = DeepObsidian,
          titleContentColor = Color.White
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, BentoIndigoDeep))),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Safari Sphere Logo",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
            Text(
              "Safari Sphere",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp,
              color = Color.White
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              showNotificationsDialog = true
              loadNotificationsFromBackend()
            },
            modifier = Modifier.testTag("notification_bell_btn")
          ) {
            Box {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (notificationsList.any { !it.isRead }) NeonCyan else Color.LightGray,
                modifier = Modifier.size(24.dp)
              )
              val unreadCount = notificationsList.count { !it.isRead }
              if (unreadCount > 0) {
                Box(
                  modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE71D36))
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = unreadCount.toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
          if (selectedTab == 4) {
            IconButton(
              onClick = { triggerProfileSettingsShow = true },
              modifier = Modifier.testTag("top_bar_settings_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        }
      )
    },
    floatingActionButton = {
      if (selectedTab == 0) {
        androidx.compose.animation.AnimatedVisibility(
          visible = isFabVisible,
          enter = androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) + androidx.compose.animation.fadeIn(),
          exit = androidx.compose.animation.scaleOut(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy)) + androidx.compose.animation.fadeOut()
        ) {
          FloatingActionButton(
            onClick = { showNewPostSheet = true },
            containerColor = NeonCyan,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .testTag("fab_create_post")
              .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = NeonCyan.copy(alpha = 0.4f),
                spotColor = NeonCyan.copy(alpha = 0.4f)
              )
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Compose Post", modifier = Modifier.size(28.dp))
          }
        }
      }
    },
    bottomBar = {
      // Glassmorphic bottom navigation bar matching Bento design
      NavigationBar(
        containerColor = GlassyCard,
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .drawBehind {
            drawLine(
              color = Color.White.copy(alpha = 0.05f),
              start = Offset(0f, 0f),
              end = Offset(size.width, 0f),
              strokeWidth = 1.dp.toPx()
            )
          },
        tonalElevation = 0.dp
      ) {
        val navItems = listOf(
          Triple("Sphere", Icons.Default.Home, 0),
          Triple("Sphere Rooms", Icons.Default.Star, 1),
          Triple("Safari Chat", Icons.Default.Face, 2),
          Triple("Discover", Icons.Default.Search, 3),
          Triple("Me", Icons.Default.Person, 4)
        )

        navItems.forEach { (label, icon, index) ->
          val isSelected = selectedTab == index
          NavigationBarItem(
            selected = isSelected,
            onClick = { 
                scope.launch { pagerState.animateScrollToPage(index) }
              },
            icon = {
              Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.4f)
              )
            },
            label = {
              Text(
                label,
                fontSize = 10.sp,
                color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.4f),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
              )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
              indicatorColor = NeonCyan.copy(alpha = 0.15f)
            )
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(
          Brush.verticalGradient(
            colors = listOf(DeepObsidian, Color(0xFF101014), DeepObsidian)
          )
        )
    ) {
      LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
      }

      androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { page ->
        when (page) {
          0 -> {
            ModernPullToRefresh(
              isRefreshing = isFetchingPosts,
              onRefresh = { 
                loadPostsFromBackend()
                loadUserProfile()
              }
            ) {
              FeedTab(
                moments = momentsList,
                posts = postsList,
                isFetchingPosts = isFetchingPosts,
                onScrollDirectionChanged = { isVisible ->
                  isFabVisible = isVisible
                },
                onLikeClicked = { post ->
                  val index = postsList.indexOf(post)
                  if (index != -1) {
                    val updated = post.copy(
                      likes = if (post.hasLiked) post.likes - 1 else post.likes + 1,
                      hasLiked = !post.hasLiked
                    )
                    postsList[index] = updated
                    if (updated.hasLiked) {
                      updateUserXp(15)
                    }
                    scope.launch {
                      try {
                        NetworkService.api.likePost(post.id)
                      } catch (e: Exception) {
                        // Failover silently
                      }
                    }
                  }
                },
                onMomentClicked = { mom ->
                  val index = momentsList.indexOf(mom)
                  if (index != -1) {
                    momentsList[index] = mom.copy(isUnread = false)
                  }
                  selectedMoment = mom
                  updateUserXp(5)
                },
                onCommentClicked = { post ->
                  activePostForComments = post
                  loadCommentsForPost(post.id)
                }
              )
            }
          }
          1 -> {
            ModernPullToRefresh(
              isRefreshing = isFetchingRooms,
              onRefresh = { loadRoomsFromBackend() }
            ) {
              RoomsTab(
                rooms = roomsList,
                isFetchingRooms = isFetchingRooms,
                onJoinRoom = { room ->
                  val index = roomsList.indexOf(room)
                  if (index != -1) {
                    roomsList[index] = room.copy(membersCount = room.membersCount + 1)
                    updateUserXp(25)
                  }
                }
              )
            }
          }
          2 -> {
            ModernPullToRefresh(
              isRefreshing = isFetchingChats,
              onRefresh = { loadChats() }
            ) {
              AICompanionTab(
                explorers = explorersList,
                chats = activeChatsList,
                activeChatId = activeChatId.value,
                chatMessages = activeChatMessages,
                userInput = typingMessage,
                isFetchingExplorers = isFetchingExplorers,
                isFetchingChats = isFetchingChats,
                onUserInputChange = { typingMessage = it },
                onSelectExplorer = { explorer ->
                  selectExplorer(explorer)
                },
                onSelectChat = { chat ->
                  activeChatId.value = chat.id
                  loadMessages(chat.id)
                },
                onSendMessage = {
                  if (typingMessage.isNotBlank() && activeChatId.value != null) {
                    val contentToSend = typingMessage
                    typingMessage = ""
                    activeChatMessages.add(
                      BackendMessage(
                        id = "msg_opt_${System.currentTimeMillis()}",
                        chatId = activeChatId.value,
                        senderId = "me",
                        content = contentToSend,
                        createdAt = "Just now",
                        senderName = userNickname
                      )
                    )
                    scope.launch {
                      try {
                        NetworkService.api.sendMessage(activeChatId.value!!, SendMessageRequest(content = contentToSend))
                        loadMessages(activeChatId.value!!)
                      } catch (e: Exception) {
                        // Failover gracefully
                      }
                      updateUserXp(10)
                    }
                  }
                },
                onBackToChatList = {
                  activeChatId.value = null
                  activeChatMessages.clear()
                }
              )
            }
          }
          3 -> {
            ModernPullToRefresh(
              isRefreshing = isFetchingExplorers,
              onRefresh = { loadExplorers() }
            ) {
              DiscoverTab(
                communities = communitiesList,
                explorers = explorersList,
                posts = postsList,
                onJoinCommunity = { comm ->
                  val index = communitiesList.indexOf(comm)
                  if (index != -1) {
                    val updated = comm.copy(
                      members = if (comm.isJoined) comm.members - 1 else comm.members + 1,
                      isJoined = !comm.isJoined
                    )
                    communitiesList[index] = updated
                    if (updated.isJoined) {
                      updateUserXp(50)
                    }
                  }
                },
                onInitiateDM = { explorer ->
                  scope.launch {
                    try {
                      val result = NetworkService.api.initiateChat(InitiateChatRequest(recipientId = explorer.id))
                      val newChatId = result.chatId
                      if (newChatId != null) {
                        val alreadyIn = activeChatsList.any { it.id == newChatId }
                        if (!alreadyIn) {
                          activeChatsList.add(
                            BackendChat(
                              id = newChatId,
                              isGroup = false,
                              peerId = explorer.id,
                              peerName = explorer.displayName ?: explorer.username,
                              peerAvatar = "",
                              createdAt = ""
                            )
                          )
                        }
                        selectedTab = 2
                        android.widget.Toast.makeText(context, "Direct DM ready!", android.widget.Toast.LENGTH_SHORT).show()
                      }
                    } catch (e: Exception) {
                      val fakeChatId = "chat_${explorer.id}_fallback"
                      val alreadyIn = activeChatsList.any { it.id == fakeChatId }
                      if (!alreadyIn) {
                        activeChatsList.add(
                          BackendChat(
                            id = fakeChatId,
                            isGroup = false,
                            peerId = explorer.id,
                            peerName = explorer.displayName ?: explorer.username,
                            peerAvatar = "",
                            createdAt = ""
                          )
                        )
                      }
                      selectedTab = 2
                    }
                  }
                },
                onCommentClicked = { post ->
                  activePostForComments = post
                  loadCommentsForPost(post.id)
                }
              )
            }
          }
          4 -> {
            ModernPullToRefresh(
              isRefreshing = isFetchingProfile,
              onRefresh = { loadUserProfile() }
            ) {
              IdentityTab(
                xp = userXp,
                streak = userStreak,
                mood = userMood,
                moodEmoji = userMoodEmoji,
                nickname = userNickname,
                handle = userHandle,
                bio = userBio,
                location = userLocation,
                avatarUrl = userAvatarUrl,
                joinDate = userJoinDate,
                email = userEmail,
                headline = userHeadline,
                interests = userInterests,
                onMoodSelected = { text, emo ->
                  updateMood(text, emo)
                },
                onProfileUpdated = { newName, newBio, newLoc, newAvatar, newHandle, newEmail, newHeadline, newInterests ->
                  userNickname = newName
                  userBio = newBio
                  userLocation = newLoc
                  userAvatarUrl = if (newAvatar.isEmpty()) null else newAvatar
                  userHandle = newHandle
                  userEmail = newEmail
                  userHeadline = newHeadline
                  userInterests = newInterests
                  prefs.edit()
                    .putString("user_nickname", newName)
                    .putString("user_bio", newBio)
                    .putString("user_location", newLoc)
                    .putString("user_avatar_url", if (newAvatar.isEmpty()) null else newAvatar)
                    .putString("user_handle", newHandle)
                    .putString("user_email", newEmail)
                    .putString("user_headline", newHeadline)
                    .putString("user_interests", newInterests)
                    .apply()
                },
                onLogOut = {
                  prefs.edit().clear().apply()
                  userXp = 100
                  userStreak = 1
                  userMood = "Chill"
                  userMoodEmoji = "🦁"
                  userNickname = "Pioneer Prime"
                  userHandle = "explorer_prime"
                  userBio = "A fresh pioneer in Safari Sphere!"
                  userLocation = "Savannah Valley"
                  userAvatarUrl = null
                  userJoinDate = null
                  userHeadline = "Exploring uncharted digital frequencies..."
                  userInterests = "Wildlife Safari, Sound Design, Live Beats"
                  isAuthenticated = false
                  selectedTab = 0
                },
                forceShowSettings = triggerProfileSettingsShow,
                onDismissSettings = { triggerProfileSettingsShow = false },
                isFetchingProfile = isFetchingProfile
              )
            }
          }
        }
      }

      // ----------------------------------------------------------------------------
      // HOLOGRAPHIC AMBIENT UPLOADING PROGRESS OVERLAY
      // ----------------------------------------------------------------------------
      if (isUploadingPost) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {}, // absorb clicks to prevent spamming
          contentAlignment = Alignment.Center
        ) {
          Surface(
            modifier = Modifier
              .fillMaxWidth(0.85f)
              .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = GlassyCard,
            border = BorderStroke(2.dp, NeonCyan.copy(alpha = 0.35f))
          ) {
            Column(
              modifier = Modifier.padding(26.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              val infiniteTransition = rememberInfiniteTransition(label = "pulse")
              val glowScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                  animation = tween(1000, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse
                ),
                label = "g"
              )
              
              Box(
                modifier = Modifier
                  .size(90.dp)
                  .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                  }
                  .clip(CircleShape)
                  .background(NeonCyan.copy(alpha = 0.08f))
                  .border(BorderStroke(2.dp, NeonCyan.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text("🛰️", fontSize = 38.sp)
              }
              
              Spacer(modifier = Modifier.height(24.dp))
              
              Text(
                "Broadcasting Transmission",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
              )
              
              Spacer(modifier = Modifier.height(6.dp))
              
              Text(
                uploadTelemetryText,
                color = NeonCyan,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold
              )
              
              Spacer(modifier = Modifier.height(28.dp))
              
              // Custom Unique Cosmic Meter (Segmented Linear Gradient Progress)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color.White.copy(alpha = 0.08f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(uploadProgressAmount)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      Brush.horizontalGradient(
                        colors = listOf(NeonCyan, BentoIndigo, NeonCyan)
                      )
                    )
                )
              }
              
              Spacer(modifier = Modifier.height(14.dp))
              
              Text(
                "${(uploadProgressAmount * 100).toInt()}% Secure Connection",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // ----------------------------------------------------------------------------
      // NEW POST COMPOSE SHEET (MODAL POPUP)
      // ----------------------------------------------------------------------------
      if (showNewPostSheet) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { showNewPostSheet = false },
          contentAlignment = Alignment.Center
        ) {
          Surface(
            modifier = Modifier
              .fillMaxWidth(0.9f)
              .wrapContentHeight()
              .clickable(enabled = false) {}, // Prevent dismiss click inside card
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1B1B22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
          ) {
            Column(modifier = Modifier.padding(24.dp)) {
              Text(
                "Broadcast Cosmic Vibe",
                color = Color(0xFFFF9F1C),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
              )

              OutlinedTextField(
                value = newPostContent,
                onValueChange = { newPostContent = it },
                placeholder = { Text("What is vibing in the wilderness?", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Color(0xFFFF9F1C),
                  unfocusedBorderColor = Color.Gray,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(115.dp)
                  .testTag("post_text_input")
              )

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                "Vibe Class:",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Adventurer", "Acoustics", "Dreamer").forEach { cat ->
                  val isSel = activeCategory == cat
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(20.dp))
                      .background(if (isSel) Color(0xFFFF9F1C) else Color(0xFF24242F))
                      .clickable { activeCategory = cat }
                      .padding(horizontal = 14.dp, vertical = 6.dp)
                  ) {
                    Text(
                      cat,
                      color = if (isSel) Color.Black else Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                "Attach Cosmos Media:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )

              Spacer(modifier = Modifier.height(6.dp))

              // Row of media types
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf(
                  Triple("text", "📝 Text only", Color.Gray),
                  Triple("image", "📷 Add Photo", Color(0xFF4CAF50)),
                  Triple("video", "🎥 Add Video", Color(0xFF2196F3))
                ).forEach { (type, label, color) ->
                  Surface(
                    modifier = Modifier
                      .weight(1f)
                      .clickable { postMediaType = type },
                    shape = RoundedCornerShape(10.dp),
                    color = if (postMediaType == type) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(
                      width = 1.dp,
                      color = if (postMediaType == type) color else Color.Transparent
                    )
                  ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                      Text(label, color = if (postMediaType == type) Color.White else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }

              if (postMediaType != "text") {
                Spacer(modifier = Modifier.height(10.dp))

                if (postMediaUrl.isEmpty()) {
                  Button(
                    onClick = {
                      val mime = if (postMediaType == "image") "image/*" else "video/*"
                      postMediaPickerLauncher.launch(mime)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                  ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Pick", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Local Device File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                } else {
                  // Display preview of chosen local media with the edits!
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                      .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Text(
                      text = "📍 Local File Selected",
                      color = SoftNeonMint,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Render selected local media in small window in post creator sheet
                    Box(
                      modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(rotationZ = mediaRotationZ)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                      contentAlignment = Alignment.Center
                    ) {
                      AsyncImage(
                        model = postMediaUrl,
                        contentDescription = "Attachment Draft Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        colorFilter = getComposeColorFilter(selectedMediaFilter)
                      )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      // Edit Selected Media button
                      Button(
                        onClick = { showMediaEditor = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Text("🎨 Edit Selected Media", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }

                      // Change file button
                      Button(
                        onClick = {
                          val mime = if (postMediaType == "image") "image/*" else "video/*"
                          postMediaPickerLauncher.launch(mime)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Text("Change Selection", fontSize = 10.sp)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = {
                    // Quick SphereMate AI auto-caption recommendation helper based on active category
                    newPostContent = when (activeCategory) {
                      "Adventurer" -> "Stalking digital gold and neon sunsets on this cyber-expedition! 🐆🌾 *wilderness *adventure"
                      "Acoustics" -> "Vibing to the rhythmic tribal frequencies of midnight lo-fi synths. 🥁🎹 *synth *acoustic"
                      "Dreamer" -> "Floating in celestial digital clouds, dreaming of the solar savanna. ☁️☄️ *dreamer *neon"
                      else -> "Exploring the neon sun horizon under a clear $activeCategory vibe. 🌅✨ *wilderness"
                    }
                  },
                  colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF9C4FFF))
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = "AI Help", modifier = Modifier.size(16.dp))
                    Text(" SphereMate Caption", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                  }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { showNewPostSheet = false }) {
                  Text("Cancel", color = Color.Gray)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                  onClick = {
                    if (newPostContent.isNotBlank() || (postMediaType != "text" && postMediaUrl.isNotBlank())) {
                      var contentToSend = newPostContent
                      if (postMediaType != "text") {
                        if (selectedAspectRatio != "Original") {
                          contentToSend += " [RATIO:$selectedAspectRatio]"
                        }
                        if (mediaRotationZ != 0f) {
                          contentToSend += " [ROT:$mediaRotationZ]"
                        }
                        if (selectedMediaFilter != "Normal") {
                          contentToSend += " [FILTER:$selectedMediaFilter]"
                        }
                      }
                      val categoryToSend = activeCategory
                      val attachmentUrl = if (postMediaType != "text") postMediaUrl else null
                      val attachmentType = postMediaType

                      newPostContent = ""
                      showNewPostSheet = false

                      scope.launch {
                        isUploadingPost = true
                        uploadProgressAmount = 0.12f
                        uploadTelemetryText = "Sifting local coordinate payload..."
                        kotlinx.coroutines.delay(500)
                        uploadProgressAmount = 0.45f
                        uploadTelemetryText = "Aligning secure satellite links..."
                        kotlinx.coroutines.delay(600)
                        uploadProgressAmount = 0.78f
                        uploadTelemetryText = "Transmitting telemetry data frequencies..."
                        kotlinx.coroutines.delay(400)
                        try {
                          NetworkService.api.createPost(
                            CreatePostRequest(
                              content = contentToSend,
                              vibeCategory = categoryToSend,
                              mediaUrl = attachmentUrl,
                              mediaType = attachmentType
                            )
                          )
                          uploadProgressAmount = 1.0f
                          uploadTelemetryText = "Secure orbit established!"
                          kotlinx.coroutines.delay(400)
                          postMediaUrl = ""
                          postMediaType = "text"
                          selectedMediaFilter = "Normal"
                          mediaRotationZ = 0f
                          selectedAspectRatio = "Original"
                          loadPostsFromBackend()
                        } catch (e: Exception) {
                          // Local offline fallback
                          val p = MobilePost(
                            id = "p_${System.currentTimeMillis()}",
                            author = userNickname,
                            username = userHandle,
                            avatarUrl = "",
                            content = contentToSend,
                            vibeCategory = categoryToSend,
                            likes = 0,
                            hasLiked = false,
                            created = "Just now",
                            mediaUrl = attachmentUrl,
                            mediaType = attachmentType,
                            commentsCount = 0
                          )
                          uploadProgressAmount = 1.0f
                          uploadTelemetryText = "Stored locally in offline bank."
                          kotlinx.coroutines.delay(500)
                          postMediaUrl = ""
                          postMediaType = "text"
                          selectedMediaFilter = "Normal"
                          mediaRotationZ = 0f
                          selectedAspectRatio = "Original"
                          postsList.add(0, p)
                        } finally {
                          isUploadingPost = false
                        }
                        updateUserXp(40)
                      }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F1C), contentColor = Color.Black)
                ) {
                  Text("Post", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // ============================================================================
      // 🎨 INTERACTIVE MEDIA FINE-TUNER OVERLAY
      // ============================================================================
      if (showMediaEditor && postMediaUrl.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { showMediaEditor = false },
          contentAlignment = Alignment.Center
        ) {
          Surface(
            modifier = Modifier
              .fillMaxWidth(0.95f)
              .wrapContentHeight()
              .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141419),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
          ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                "🎨 Media Fine-Tuner",
                color = SoftNeonMint,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 14.dp)
              )

              // Interactive Preview Window
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(240.dp)
                  .background(Color.Black, RoundedCornerShape(16.dp))
                  .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                // Apply rotation, filter, and selected aspect ratio natively in preview!
                val previewModifier = Modifier
                  .align(Alignment.Center)
                  .then(
                    when (selectedAspectRatio) {
                      "1:1" -> Modifier.aspectRatio(1f)
                      "4:5" -> Modifier.aspectRatio(0.8f)
                      "5:7" -> Modifier.aspectRatio(5f / 7f)
                      "16:9" -> Modifier.aspectRatio(16f / 9f)
                      else -> Modifier.fillMaxSize()
                    }
                  )
                  .graphicsLayer(rotationZ = mediaRotationZ)
                  .clip(RoundedCornerShape(8.dp))

                AsyncImage(
                  model = postMediaUrl,
                  contentDescription = "Editing preview",
                  modifier = previewModifier,
                  contentScale = if (selectedAspectRatio == "Original") androidx.compose.ui.layout.ContentScale.Fit else androidx.compose.ui.layout.ContentScale.Crop,
                  colorFilter = getComposeColorFilter(selectedMediaFilter)
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Rotation Controller
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Rotate:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Button(
                  onClick = { mediaRotationZ = (mediaRotationZ + 90f) % 360f },
                  colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rotate icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate +90°", fontSize = 11.sp)
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Aspect Ratio Selector
              Column(modifier = Modifier.fillMaxWidth()) {
                Text("Aspect Ratio:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  listOf("Original", "1:1", "4:5", "5:7", "16:9").forEach { mode ->
                    val isSel = selectedAspectRatio == mode
                    Surface(
                      modifier = Modifier
                        .weight(1f)
                        .clickable { selectedAspectRatio = mode },
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSel) SoftNeonMint.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                      border = BorderStroke(1.dp, if (isSel) SoftNeonMint else Color.Transparent)
                    ) {
                      Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(mode, color = if (isSel) SoftNeonMint else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Color Filters
              Column(modifier = Modifier.fillMaxWidth()) {
                Text("Color Filters:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  listOf("Normal", "Cyberpunk", "Mono", "Amber", "Forest").forEach { filt ->
                    val isSel = selectedMediaFilter == filt
                    Surface(
                      modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMediaFilter = filt },
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                      border = BorderStroke(1.dp, if (isSel) NeonCyan else Color.Transparent)
                    ) {
                      Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(filt, color = if (isSel) NeonCyan else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              // Finish Button
              Button(
                onClick = { showMediaEditor = false },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
              ) {
                Text("Confirm & Save Edits", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }

      // ============================================================================
      // 🌟 DYNAMIC MODERN NOTIFICATIONS DRAWER OVERLAY
      // ============================================================================
      if (showNotificationsDialog) {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {},
          color = Color(0xFF13131A)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
              .padding(20.dp)
          ) {
            // Notification Header
            Row(
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  "Cosmic Activity",
                  color = Color.White,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "Tracks likes and comments on your vibes",
                  color = Color.Gray,
                  fontSize = 11.sp
                )
              }
              IconButton(onClick = { showNotificationsDialog = false }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
              }
            }

            // Divider
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(10.dp))

            // Action Bar
            Row(
              modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(
                onClick = { markNotificationsRead() },
                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
              ) {
                Text("Mark all as read", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            // Notification List Content
            if (isFetchingNotifications) {
              // Shimmer notifications list
              LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(4) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    ShimmerPlaceholder(modifier = Modifier.size(38.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp))
                      Spacer(modifier = Modifier.height(6.dp))
                      ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.8f).height(10.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ShimmerPlaceholder(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(6.dp))
                  }
                }
              }
            } else if (notificationsList.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("🌌", fontSize = 38.sp)
                  Spacer(modifier = Modifier.height(10.dp))
                  Text("Deep Space Serenity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Text("No social engagements caught in your orbit yet.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(notificationsList) { notif ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .background(if (notif.isRead) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.06f))
                      .border(
                        width = 1.dp,
                        color = if (notif.isRead) Color.Transparent else NeonCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                      )
                      .clickable {
                        showNotificationsDialog = false
                        selectedTab = 0
                        val matchingPost = postsList.find { it.id == notif.targetId }
                        if (matchingPost != null) {
                          activePostForComments = matchingPost
                          loadCommentsForPost(matchingPost.id)
                        }
                        scope.launch {
                          try {
                            if (!notif.isRead) {
                              NetworkService.api.markNotificationsRead()
                              loadNotificationsFromBackend()
                            }
                          } catch (e: Exception) {}
                        }
                      }
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(if (notif.type == "like") "❤️" else "💬", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = notif.senderDisplayName ?: "Someone",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                      )
                      val textAction = if (notif.type == "like") "liked your post:" else "commented on your post:"
                      Text(
                        text = "$textAction \"${notif.contentPreview ?: ""}\"",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!notif.postMediaUrl.isNullOrBlank()) {
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Color.Black)
                      ) {
                        AsyncImage(
                          model = notif.postMediaUrl,
                          contentDescription = "Post Thumbnail Preview",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                      }
                    } else {
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Color.White.copy(alpha = 0.04f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Text("📝", fontSize = 14.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // ============================================================================
      // 🌟 DIGITAL CORRIDORS RICH COMMENTS SHEET OVERLAY (THREADED & GALAXY SHIMMER)
      // ============================================================================
      if (activePostForComments != null) {
        val post = activePostForComments!!
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .testTag("full_comments_page"),
          color = Color(0xFF0F0F14)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
              .navigationBarsPadding()
          ) {
            // Full Screen Toolbar
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activePostForComments = null }) {
                  Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Feed",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  "Sphere Comments",
                  color = NeonCyan,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Text(
                    post.commentsCount.toString(),
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

            // Enhanced Context Header showing parent post being commented on
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(16.dp))
                .padding(14.dp),
              verticalAlignment = Alignment.Top
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(NeonCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
              ) {
                Text(if (post.author.contains("Lion") || post.author.contains("Prime")) "🦁" else "🐆", fontSize = 16.sp)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(post.author, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text("@${post.username}", color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val (postBodyText, _) = parsePostMetadata(post.content)
                Text(
                  postBodyText,
                  color = Color.LightGray,
                  fontSize = 13.sp,
                  lineHeight = 18.sp
                )
              }
            }

            Text(
              "All Chimes",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            // Scrollable Comments List
            if (isFetchingComments) {
              LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                items(3) {
                  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ShimmerPlaceholder(modifier = Modifier.size(32.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerPlaceholder(modifier = Modifier.width(80.dp).height(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        ShimmerPlaceholder(modifier = Modifier.width(40.dp).height(10.dp))
                      }
                      Spacer(modifier = Modifier.height(6.dp))
                      ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(14.dp))
                    }
                  }
                }
              }
            } else if (commentsListForActivePost.isEmpty()) {
              Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("📣", fontSize = 32.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("No frequencies detected yet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                  Text("Be the first to chime in!", color = Color.Gray, fontSize = 11.sp)
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                val parentComments = commentsListForActivePost.filter { it.parentId == null }
                items(parentComments) { pComm ->
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .background(Color.White.copy(alpha = 0.02f))
                      .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                      .padding(12.dp)
                  ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                      Box(
                        modifier = Modifier
                          .size(34.dp)
                          .clip(CircleShape)
                          .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                      ) {
                        if (!pComm.avatarUrl.isNullOrBlank()) {
                          AsyncImage(
                            model = pComm.avatarUrl,
                            contentDescription = "avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                          )
                        } else {
                          Text(if (pComm.displayName?.contains("Luna") == true) "✨" else "👽", fontSize = 14.sp)
                        }
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text(pComm.displayName ?: pComm.username ?: "Pioneer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                          Text(pComm.createdAt ?: "Just now", color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(pComm.content, color = Color(0xFFECEFF1), fontSize = 13.sp, lineHeight = 18.sp)
                      }
                    }

                    // Replies nested inside the parent card cleanly
                    val subComments = commentsListForActivePost.filter { it.parentId == pComm.id }
                    if (subComments.isNotEmpty()) {
                      Spacer(modifier = Modifier.height(8.dp))
                      subComments.forEach { subComm ->
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 6.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(26.dp)
                              .clip(CircleShape)
                              .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                          ) {
                            if (!subComm.avatarUrl.isNullOrBlank()) {
                              AsyncImage(
                                model = subComm.avatarUrl,
                                contentDescription = "avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                              )
                            } else {
                              Text("🐆", fontSize = 11.sp)
                            }
                          }
                          Spacer(modifier = Modifier.width(10.dp))
                          Column(modifier = Modifier.weight(1f)) {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.SpaceBetween,
                              modifier = Modifier.fillMaxWidth()
                            ) {
                              Text(subComm.displayName ?: subComm.username ?: "Explorer", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                              Text(subComm.createdAt ?: "Just now", color = Color.Gray, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(subComm.content, color = Color(0xFFECEFF1), fontSize = 12.sp, lineHeight = 16.sp)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message Composer Footer
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              listOf("🦁", "🔥", "✨").forEach { emoji ->
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { typingCommentText += emoji }
                    .padding(2.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(emoji, fontSize = 14.sp)
                }
              }

              Spacer(modifier = Modifier.width(6.dp))

              BasicTextField(
                value = typingCommentText,
                onValueChange = { typingCommentText = it },
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("comment_input_text")
                  .padding(vertical = 8.dp),
                decorationBox = { innerTextField ->
                  if (typingCommentText.isEmpty()) {
                    Text("Beam your thoughts...", color = Color.Gray, fontSize = 13.sp)
                  }
                  innerTextField()
                }
              )

              IconButton(
                onClick = {
                  if (typingCommentText.isNotBlank() && !isPostingComment) {
                    val textToPost = typingCommentText
                    typingCommentText = ""
                    postComment(post.id, textToPost)
                  }
                },
                enabled = !isPostingComment,
                modifier = Modifier.testTag("submit_comment_btn")
              ) {
                if (isPostingComment) {
                  CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                  Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Comment",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
}

// ==============================================================================
// 1. HOME SOCIAL FEED TAB
// ==============================================================================
@Composable
fun FeedTab(
  moments: List<MobileMoment>,
  posts: List<MobilePost>,
  isFetchingPosts: Boolean = false,
  onScrollDirectionChanged: (Boolean) -> Unit,
  onLikeClicked: (MobilePost) -> Unit,
  onMomentClicked: (MobileMoment) -> Unit,
  onCommentClicked: (MobilePost) -> Unit
) {
  val listState = rememberLazyListState()

  LaunchedEffect(listState) {
    var previousIndex = 0
    var previousScrollOffset = 0
    snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
      .collect { (currentIndex, currentScrollOffset) ->
        if (currentIndex > previousIndex) {
          // Scrolled down
          onScrollDirectionChanged(false)
        } else if (currentIndex < previousIndex) {
          // Scrolled up
          onScrollDirectionChanged(true)
        } else {
          // Same item index, inspect pixel offset with soft tolerance
          if (currentScrollOffset > previousScrollOffset + 5) {
            onScrollDirectionChanged(false)
          } else if (currentScrollOffset < previousScrollOffset - 5) {
            onScrollDirectionChanged(true)
          }
        }
        previousIndex = currentIndex
        previousScrollOffset = currentScrollOffset
      }
  }

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Moments Story Bar
    item {
      Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
          "Active Moments",
          color = Color.LightGray,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(moments) { mom ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable {
                onMomentClicked(mom)
              }
            ) {
              Box(
                modifier = Modifier
                  .size(62.dp)
                  .clip(CircleShape)
                  .background(
                    if (mom.isUnread) Brush.linearGradient(listOf(Color(0xFFFF9F1C), Color(0xFF9C4FFF)))
                    else Brush.linearGradient(listOf(Color.DarkGray, Color.Gray))
                  )
                  .padding(3.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF0D0D11)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(mom.emoji, fontSize = 26.sp)
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                mom.author,
                color = if (mom.isUnread) Color.White else Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    // Interactive Bento Grid Dashboard matching Design HTML
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          "Wilderness Hub Dashboard",
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 2.dp)
        )

        // Bento Tile 1: Live Vybe Room (Large)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Color(0xFF312E81).copy(alpha = 0.4f), // Indigo-900/40
                  Color(0xFF0F172A).copy(alpha = 0.4f)  // Slate-900/40
                )
              )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(18.dp)
        ) {
          Column {
            // Live pulsing badge header
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "VYBE ROOM",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              )

              // Pulsing Crimson LIVE tag
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFE71D36))
                  .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(5.dp)
                      .clip(CircleShape)
                      .background(Color.White)
                  )
                  Text(
                    "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              "Midnight Lo-fi &\nArchitecture",
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Avatars list matching design HTML
              Row(
                verticalAlignment = Alignment.CenterVertically
              ) {
                listOf("🦁", "🦄", "🐆").forEachIndexed { i, emoji ->
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .offset(x = (-i * 8).dp)
                      .clip(CircleShape)
                      .background(GlassyCard)
                      .border(1.5.dp, DeepObsidian, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(emoji, fontSize = 14.sp)
                  }
                }
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .offset(x = (-24).dp)
                    .clip(CircleShape)
                    .background(BentoIndigo)
                    .border(1.5.dp, DeepObsidian, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    "+42",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // Action button to join
              Button(
                onClick = { /* Simulated join */ },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color.White,
                  contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text(
                  "Join Room",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // Row of 2 smaller Bento Cards (SphereMate AI & Trending)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Bento Tile 2: SphereMate AI Guide (Small Card)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(24.dp))
              .background(NeonCyan.copy(alpha = 0.08f))
              .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
              .padding(16.dp)
          ) {
            Column(
              verticalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.height(100.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(NeonCyan),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Face,
                  contentDescription = "AI Symbol",
                  tint = Color.Black,
                  modifier = Modifier.size(18.dp)
                )
              }

              Column {
                Text(
                  "SphereMate",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "Found 3 communities for your vibe.",
                  color = NeonCyan.copy(alpha = 0.8f),
                  fontSize = 11.sp,
                  lineHeight = 14.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }

          // Bento Tile 3: Trending Topics (Small Card)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(24.dp))
              .background(Color.White.copy(alpha = 0.04f))
              .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
              .padding(16.dp)
          ) {
            Column(
              verticalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.height(100.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Trending",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }

              Column {
                Text(
                  "Trending",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "#CyberSavanna\n+14% today",
                  color = Color.White.copy(alpha = 0.5f),
                  fontSize = 11.sp,
                  lineHeight = 14.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }
        }
      }
    }

    // Divider Line separating Bento Dashboard from Feed
    item {
      HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
    }

    // Dynamic Social Post Feed List Header
    item {
      Text(
        "Community Feed",
        color = Color.LightGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
      )
    }

    // Dynamic Social Post Feed
    if (isFetchingPosts && posts.isEmpty()) {
      items(3) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          color = GlassyCard.copy(alpha = 0.5f),
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ShimmerPlaceholder(modifier = Modifier.size(42.dp), shape = CircleShape)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                ShimmerPlaceholder(modifier = Modifier.width(100.dp).height(14.dp))
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerPlaceholder(modifier = Modifier.width(60.dp).height(10.dp))
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
          }
        }
      }
    } else {
      items(posts) { post ->
        val (cleanContent, metadata) = remember(post.content) { parsePostMetadata(post.content) }
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Post Header
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
              contentAlignment = Alignment.Center
            ) {
              Text("🦁", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                post.author,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                "@${post.username} • ${post.created}",
                color = Color.Gray,
                fontSize = 12.sp
              )
            }

            // Vibe Category Tag badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(NeonCyan.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                post.vibeCategory,
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Body Content Text
          Text(
            cleanContent,
            color = Color(0xFFECEFF1),
            fontSize = 14.sp,
            lineHeight = 20.sp
          )

          if (!post.mediaUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            val mediaModifier = Modifier
              .fillMaxWidth()
              .then(
                when (metadata.ratio) {
                  "1:1" -> Modifier.aspectRatio(1f)
                  "4:5" -> Modifier.aspectRatio(0.8f)
                  "5:7" -> Modifier.aspectRatio(5f / 7f)
                  "16:9" -> Modifier.aspectRatio(16f / 9f)
                  else -> Modifier.wrapContentHeight().heightIn(max = 450.dp)
                }
              )
              .graphicsLayer(rotationZ = metadata.rotation)
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))

            if (post.mediaType == "video") {
              // Video Player Mock card with dynamic ratio modifier applied
              Box(
                modifier = mediaModifier.background(Color.Black),
                contentAlignment = Alignment.Center
              ) {
                val videoThumbnail = if (!post.mediaUrl.contains("mixkit")) {
                  post.mediaUrl
                } else {
                  "https://images.unsplash.com/photo-1547407139-3c921a66005c?auto=format&fit=crop&w=600&q=80"
                }
                AsyncImage(
                  model = videoThumbnail,
                  contentDescription = "Video Thumbnail",
                  modifier = Modifier.fillMaxSize().alpha(0.6f),
                  contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                  colorFilter = getComposeColorFilter(metadata.filter ?: "")
                )

                var isPlaying by remember { mutableStateOf(false) }
                Surface(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .clickable { isPlaying = !isPlaying }
                    .border(2.dp, NeonCyan, CircleShape),
                  color = Color.Black.copy(alpha = 0.6f)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                      contentDescription = if (isPlaying) "Pause" else "Play",
                      tint = NeonCyan,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }

                if (isPlaying) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
                      .border(2.dp, NeonCyan, RoundedCornerShape(16.dp))
                  ) {
                    Text(
                      "🛰️ FEED VIDEO LIVE CHIME",
                      color = NeonCyan,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                    LinearProgressIndicator(
                      color = NeonCyan,
                      trackColor = Color.White.copy(alpha = 0.1f),
                      modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter)
                    )
                  }
                }
              }
            } else {
              // Image card load via AsyncImage with dynamic aspect ratio, rotation, and filter mapping
              AsyncImage(
                model = post.mediaUrl,
                contentDescription = "Post Media Image Attachment",
                modifier = mediaModifier,
                contentScale = if (metadata.ratio == null || metadata.ratio == "Original") androidx.compose.ui.layout.ContentScale.Fit else androidx.compose.ui.layout.ContentScale.Crop,
                colorFilter = getComposeColorFilter(metadata.filter ?: "")
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Footer Metrics Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Like Button Metric Toggle
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { onLikeClicked(post) }
            ) {
              Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like Post",
                tint = if (post.hasLiked) NeonCyan else Color(0xFF90A4AE),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                post.likes.toString(),
                color = if (post.hasLiked) NeonCyan else Color(0xFF90A4AE),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Repost Mock Metric
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Repost",
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(18.dp).clickable { /* Repst simulation */ }
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("3", color = Color(0xFF90A4AE), fontSize = 12.sp)
            }

            // Fully Functional Comments button triggering comments detail corridors!
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { onCommentClicked(post) },
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "Comments",
                tint = NeonCyan,
                modifier = Modifier.size(18.dp).testTag("post_comment_btn_${post.id}")
              )
              Text(
                post.commentsCount.toString(),
                color = Color(0xFF90A4AE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
}

// ==============================================================================
// 2. LIVE VYBE ROOMS TAB
// ==============================================================================
@Composable
fun RoomsTab(
  rooms: List<MobileRoom>,
  isFetchingRooms: Boolean = false,
  onJoinRoom: (MobileRoom) -> Unit
) {
  var joinedRoomId by remember { mutableStateOf<String?>(null) }
  var roomMessages = remember { mutableStateListOf<Pair<String, String>>() }
  var roomInputMsg by remember { mutableStateOf("") }

  if (joinedRoomId == null) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Column {
          Text(
            "Live Atmosphere Rooms",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Text(
            "Join and hang out in live thematic spaces with real-time presence awareness",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
          )
        }
      }

      if (isFetchingRooms && rooms.isEmpty()) {
        items(3) {
          Surface(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            color = GlassyCard.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              ShimmerPlaceholder(modifier = Modifier.width(160.dp).height(16.dp))
              Spacer(modifier = Modifier.height(10.dp))
              ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(12.dp))
              Spacer(modifier = Modifier.height(6.dp))
              ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp))
              Spacer(modifier = Modifier.height(16.dp))
              Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                ShimmerPlaceholder(modifier = Modifier.width(80.dp).height(28.dp))
              }
            }
          }
        }
      } else {
        items(rooms) { room ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
          color = GlassyCard,
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              // Pulse/Live Red Dot Indicator
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFE71D36))
              )

              Spacer(modifier = Modifier.width(8.dp))

              Text(
                "LIVE ROOM • ${room.theme.uppercase()}",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )

              Spacer(modifier = Modifier.weight(1f))

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color.White.copy(alpha = 0.07f))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  "${room.membersCount}/${room.maxMembers} Members",
                  color = Color.LightGray,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              room.title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              room.description,
              color = Color(0xFFECEFF1),
              fontSize = 13.sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                "Host: ${room.host}",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
              )

              Spacer(modifier = Modifier.weight(1f))

              Button(
                onClick = {
                  joinedRoomId = room.id
                  onJoinRoom(room)
                  roomMessages.clear()
                  roomMessages.add("Ranger Sam" to "Welcome to the campfire! Share your stories here.")
                  roomMessages.add(room.host to "${room.title} channel initialized! Mic state: listening.")
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp)
              ) {
                Text("Join Sphere", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
    }
  } else {
    // ACTIVE JOINED VYBE ROOM CHAT INTERFACE
    val activeRoom = rooms.find { it.id == joinedRoomId }!!
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(activeRoom.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Host: ${activeRoom.host} • ${activeRoom.membersCount} listening", color = Color.Gray, fontSize = 12.sp)
        }

        IconButton(onClick = {
          joinedRoomId = null
          activeRoom.membersCount = Math.max(0, activeRoom.membersCount - 1)
        }) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Room", tint = Color.LightGray)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Audio / Synth Visual Waveform simulation in Bento style
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(55.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(NeonCyan.copy(alpha = 0.08f))
          .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Wave", tint = NeonCyan)
          Text("Real-time Savannah Synth Active 📻", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(roomMessages) { (sender, content) ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(GlassyCard)
              .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
              .padding(12.dp)
          ) {
            Column {
              Text(sender, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
              Text(content, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = roomInputMsg,
          onValueChange = { roomInputMsg = it },
          placeholder = { Text("Send vibe chat...", color = Color.Gray) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            if (roomInputMsg.isNotBlank()) {
              roomMessages.add("You" to roomInputMsg)
              roomInputMsg = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
        }
      }
    }
  }
}

// ==============================================================================
// 3. SPHEREMATE AI COMPANION CHAT TAB
// ==============================================================================
@Composable
fun AICompanionTab(
  explorers: List<BackendExplorer>,
  chats: List<BackendChat>,
  activeChatId: String?,
  chatMessages: List<BackendMessage>,
  userInput: String,
  isFetchingExplorers: Boolean,
  isFetchingChats: Boolean,
  onUserInputChange: (String) -> Unit,
  onSelectExplorer: (BackendExplorer) -> Unit,
  onSelectChat: (BackendChat) -> Unit,
  onSendMessage: () -> Unit,
  onBackToChatList: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    if (activeChatId == null) {
      // --- Chat Directory Overview ---
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        color = NeonCyan.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Text("💬", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Safari Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text("Real-time decentralized pioneer communication", color = Color.Gray, fontSize = 11.sp)
            }
          }
        }
      }

      Text(
        "Explorer Directory",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(vertical = 8.dp)
      )

      if (isFetchingExplorers) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          items(3) {
            Surface(
              modifier = Modifier
                .width(140.dp)
                .height(110.dp),
              color = GlassyCard.copy(alpha = 0.5f),
              shape = RoundedCornerShape(18.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                ShimmerPlaceholder(modifier = Modifier.size(38.dp), shape = CircleShape)
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerPlaceholder(modifier = Modifier.width(70.dp).height(12.dp))
              }
            }
          }
        }
      } else {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          items(explorers) { explorer ->
            Surface(
              modifier = Modifier
                .width(140.dp)
                .clickable { onSelectExplorer(explorer) },
              color = GlassyCard,
              shape = RoundedCornerShape(18.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(explorer.moodEmoji ?: "🧭", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = explorer.displayName ?: explorer.username,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "@${explorer.username}",
                  color = Color.LightGray.copy(alpha = 0.6f),
                  fontSize = 10.sp,
                  maxLines = 1,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${explorer.xp ?: 100} XP",
                  color = NeonCyan,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      Text(
        "Recent Channels",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      if (isFetchingChats) {
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          repeat(4) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ShimmerPlaceholder(modifier = Modifier.size(46.dp), shape = CircleShape)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                ShimmerPlaceholder(modifier = Modifier.width(130.dp).height(14.dp))
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.7f).height(10.dp))
              }
            }
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          if (chats.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 40.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No active channels. Click any explorer above to start a secure whisper session!",
                  color = Color.Gray,
                  fontSize = 12.sp,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            items(chats) { chat ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectChat(chat) },
                color = GlassyCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text("🌾", fontSize = 18.sp)
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = chat.peerName ?: chat.groupName ?: "Active Partner",
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp
                    )
                    Text(
                      text = "Tap to open secure whisper channel",
                      color = Color.Gray,
                      fontSize = 11.sp
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.Send,
                    tint = NeonCyan,
                    contentDescription = "Open Chat",
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // --- Active Whisper Conversation Thread ---
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBackToChatList,
          modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          val chatName = chats.find { it.id == activeChatId }?.peerName ?: "Secure Whisper Channel"
          Text(chatName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Quantum encrypted tunnel • Live", color = SoftNeonMint, fontSize = 11.sp)
        }
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(chatMessages) { msg ->
          val alignRight = msg.senderId == "me" || msg.senderId == "You"
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (alignRight) Arrangement.End else Arrangement.Start
          ) {
            Surface(
              modifier = Modifier.fillMaxWidth(0.82f),
              color = if (alignRight) BentoIndigoDeep else GlassyCard,
              shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (alignRight) 20.dp else 4.dp,
                bottomEnd = if (alignRight) 4.dp else 20.dp
              ),
              border = if (!alignRight) BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)) else null
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                if (!alignRight) {
                  Text(
                    text = msg.senderName ?: "Pioneer",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                  )
                }
                Text(
                  text = msg.content ?: "",
                  color = Color.White,
                  fontSize = 13.sp,
                  lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (msg.createdAt.isNullOrEmpty() || msg.createdAt.contains("Just now")) "Just now" else "17:40",
                  color = Color.White.copy(alpha = 0.4f),
                  fontSize = 9.sp,
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = userInput,
          onValueChange = onUserInputChange,
          placeholder = { Text("Write quantum whisper secure message...", color = Color.Gray, fontSize = 13.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("ai_chat_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = onSendMessage,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NeonCyan)
            .size(48.dp)
            .testTag("reply_submit_button")
        ) {
          Icon(imageVector = Icons.Default.Send, contentDescription = "Send Secure Message", tint = Color.Black)
        }
      }
    }
  }
}

// ==============================================================================
// 4. DISCOVER TAB (SEARCH & COMMUNITIES)
// ==============================================================================
@Composable
fun DiscoverTab(
  communities: List<MobileCommunity>,
  explorers: List<BackendExplorer>,
  posts: List<MobilePost>,
  onJoinCommunity: (MobileCommunity) -> Unit,
  onInitiateDM: (BackendExplorer) -> Unit,
  onCommentClicked: (MobilePost) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedExplorerForProfile by remember { mutableStateOf<BackendExplorer?>(null) }

  var isSearchingMockLoading by remember { mutableStateOf(false) }
  LaunchedEffect(searchQuery) {
    if (searchQuery.trim().isNotEmpty()) {
      isSearchingMockLoading = true
      kotlinx.coroutines.delay(400) // fast mock galaxy processing delay
      isSearchingMockLoading = false
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Large Header Search bar textfields
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White.copy(alpha = 0.02f))
          .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(24.dp)
          )
          .padding(18.dp)
      ) {
        Text(
          "Discover the Cosmos",
          fontSize = 20.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          letterSpacing = (-0.5).sp
        )
        Text(
          "Uncover trending communities and digital landscapes of Safari Sphere",
          fontSize = 11.sp,
          color = Color.Gray,
          modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search hashtags, pioneers, communities...", color = Color.Gray, fontSize = 13.sp) },
          leadingIcon = { 
            Icon(
              imageVector = Icons.Default.Search, 
              contentDescription = "Search icon", 
              tint = if (searchQuery.isNotEmpty()) NeonCyan else Color.Gray,
              modifier = Modifier.size(20.dp)
            ) 
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear, 
                  contentDescription = "Clear Search", 
                  tint = Color.Gray,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.15f)
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("discover_search_input"),
          singleLine = true
        )
      }
    }

    // Active Explorers horizontal row section
    val filteredExplorers = if (searchQuery.trim().isEmpty()) {
      explorers
    } else {
      explorers.filter {
        it.displayName?.contains(searchQuery, ignoreCase = true) == true ||
        it.username.contains(searchQuery, ignoreCase = true)
      }
    }

    val filteredCommunities = if (searchQuery.trim().isEmpty()) {
      communities
    } else {
      communities.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.handle.contains(searchQuery, ignoreCase = true) ||
        it.vibe.contains(searchQuery, ignoreCase = true)
      }
    }

    val filteredPosts = if (searchQuery.trim().isEmpty()) {
      emptyList()
    } else {
      posts.filter {
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.author.contains(searchQuery, ignoreCase = true) ||
        it.username.contains(searchQuery, ignoreCase = true) ||
        it.vibeCategory.contains(searchQuery, ignoreCase = true)
      }
    }

    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = if (searchQuery.trim().isEmpty()) "Savannah Explorers Directory" else "Matched Explorers (${filteredExplorers.size})",
          color = NeonCyan,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredExplorers.isEmpty()) {
          Text("No online explorers found matching search query.", color = Color.Gray, fontSize = 11.sp)
        } else {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(filteredExplorers) { exp ->
              Surface(
                modifier = Modifier
                  .width(135.dp)
                  .clickable { selectedExplorerForProfile = exp },
                color = GlassyCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Box(
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(exp.moodEmoji ?: "🦁", fontSize = 24.sp)
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    exp.displayName ?: exp.username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    "@${exp.username}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }

    if (searchQuery.trim().isNotEmpty()) {
      if (isSearchingMockLoading) {
        // Render 2 awesome post skeleton loaders
        items(2) {
          Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            color = GlassyCard,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerPlaceholder(modifier = Modifier.size(40.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  ShimmerPlaceholder(modifier = Modifier.width(120.dp).height(12.dp))
                  Spacer(modifier = Modifier.height(6.dp))
                  ShimmerPlaceholder(modifier = Modifier.width(80.dp).height(10.dp))
                }
              }
              Spacer(modifier = Modifier.height(14.dp))
              ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(16.dp))
              Spacer(modifier = Modifier.height(8.dp))
              ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
            }
          }
        }
      } else {
        // Matched Post section header
        item {
          Text(
            "Matched Cosmic Posts (${filteredPosts.size})",
            color = Color(0xFFFF9F1C),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        if (filteredPosts.isEmpty()) {
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = Color.White.copy(alpha = 0.02f),
              shape = RoundedCornerShape(16.dp)
            ) {
              Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No matching posts drifting in this sphere.", color = Color.Gray, fontSize = 11.sp)
              }
            }
          }
        } else {
          items(filteredPosts) { post ->
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(24.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text("🦁", fontSize = 16.sp)
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(post.author, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("@${post.username} • ${post.created}", color = Color.Gray, fontSize = 11.sp)
                  }
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(NeonCyan.copy(alpha = 0.15f))
                      .padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text(post.vibeCategory, color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(post.content, color = Color(0xFFECEFF1), fontSize = 13.sp)

                if (!post.mediaUrl.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(130.dp)
                      .clip(RoundedCornerShape(12.dp))
                  ) {
                    AsyncImage(
                      model = post.mediaUrl,
                      contentDescription = "Search Post Attachment Overlay",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("❤️ ${post.likes} Likes", color = Color.Gray, fontSize = 11.sp)

                  Surface(
                    modifier = Modifier.clickable { onCommentClicked(post) },
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.1f)
                  ) {
                    Text(
                      "💬 ${post.commentsCount} Comments",
                      color = NeonCyan,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Trending Categories Horizon in Bento style
    item {
      Column {
        Text("Trending Categories", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          val trends = listOf("⛺ Expedition", "🎨 Art & Vibes", "💡 Tech Savanna")
          trends.forEach { label ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GlassyCard)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
    }

    // Active Communities List Header
    item {
      Text("Recommended Communities", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    // Recommendations Items rendering in Bento grids
    items(filteredCommunities) { comm ->
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
          ) {
            Text("🏕️", fontSize = 20.sp)
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              comm.name,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            Text(
              "@${comm.handle} • ${comm.members} Pioneers",
              color = Color.Gray,
              fontSize = 12.sp
            )
          }

          Button(
            onClick = { onJoinCommunity(comm) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (comm.isJoined) NeonCyan.copy(alpha = 0.08f) else Color.Transparent
            ),
            border = BorderStroke(1.dp, if (comm.isJoined) NeonCyan.copy(alpha = 0.3f) else NeonCyan),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              if (comm.isJoined) "Joined ✓" else "Join",
              color = if (comm.isJoined) NeonCyan.copy(alpha = 0.7f) else NeonCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }

  // Selected Explorer Profile dialog overlay
  if (selectedExplorerForProfile != null) {
    val exp = selectedExplorerForProfile!!
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.73f))
        .clickable { selectedExplorerForProfile = null },
      contentAlignment = Alignment.Center
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .wrapContentHeight()
          .clickable(enabled = false) {},
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(NeonCyan, SoftNeonMint)))
              .padding(3.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(DeepObsidian),
              contentAlignment = Alignment.Center
            ) {
              Text(exp.moodEmoji ?: "📡", fontSize = 36.sp)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            exp.displayName ?: exp.username,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
          )

          Text(
            "@${exp.username}",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            "Status: ${exp.moodState ?: "Exploring Savannah shores"}",
            color = Color.LightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
          )

          Text(
            "Cosmic Level: ${(exp.xp ?: 100) / 100} • XP Balance: ${exp.xp ?: 100}",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { selectedExplorerForProfile = null },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.05f),
                contentColor = Color.Gray
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Close", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                onInitiateDM(exp)
                selectedExplorerForProfile = null
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black
              ),
              modifier = Modifier.weight(1.3f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send PM", modifier = Modifier.size(12.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Whisper DM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

// ==============================================================================
// 5. IDENTITY MY PROFILE TAB
// ==============================================================================
@Composable
fun IdentityTab(
  xp: Int,
  streak: Int,
  mood: String,
  moodEmoji: String,
  nickname: String,
  handle: String,
  bio: String,
  location: String,
  avatarUrl: String?,
  joinDate: String?,
  email: String,
  headline: String,
  interests: String,
  onMoodSelected: (String, String) -> Unit,
  onProfileUpdated: (nickname: String, bio: String, location: String, avatarUrl: String, handle: String, email: String, headline: String, interests: String) -> Unit,
  onLogOut: () -> Unit,
  forceShowSettings: Boolean = false,
  onDismissSettings: () -> Unit = {},
  isFetchingProfile: Boolean = false
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("safari_sphere_prefs", android.content.Context.MODE_PRIVATE) }
  var hasDraft by remember { mutableStateOf(prefs.getBoolean("profile_draft_exists", false)) }

  var editNicknameInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_nickname", nickname) ?: nickname else nickname) }
  var editBioInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_bio", bio) ?: bio else bio) }
  var editLocationInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_location", location) ?: location else location) }
  var editAvatarInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_avatar", avatarUrl ?: "") ?: "" else avatarUrl ?: "") }
  var editHandleInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_handle", handle) ?: handle else handle) }
  var editEmailInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_email", email) ?: email else email) }
  var editHeadlineInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_headline", headline) ?: headline else headline) }
  var editInterestsInput by remember { mutableStateOf(if (hasDraft) prefs.getString("draft_interests", interests) ?: interests else interests) }

  val avatarPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      editAvatarInput = uri.toString()
    }
  }

  var showSelectMoodDialog by remember { mutableStateOf(false) }
  var showEditProfileDialog by remember { mutableStateOf(false) }

  LaunchedEffect(forceShowSettings) {
    if (forceShowSettings) {
      showEditProfileDialog = true
      onDismissSettings()
    }
  }

  var lastAutoSaveTime by remember { mutableStateOf("") }

  LaunchedEffect(showEditProfileDialog) {
    if (showEditProfileDialog) {
      while (true) {
        delay(5000)
        prefs.edit()
          .putBoolean("profile_draft_exists", true)
          .putString("draft_nickname", editNicknameInput)
          .putString("draft_bio", editBioInput)
          .putString("draft_location", editLocationInput)
          .putString("draft_avatar", editAvatarInput)
          .putString("draft_handle", editHandleInput)
          .putString("draft_email", editEmailInput)
          .putString("draft_headline", editHeadlineInput)
          .putString("draft_interests", editInterestsInput)
          .apply()
        hasDraft = true
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        lastAutoSaveTime = sdf.format(java.util.Date())
      }
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (isFetchingProfile) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth().shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
          color = GlassyCard,
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            ShimmerPlaceholder(modifier = Modifier.size(92.dp), shape = CircleShape)
            Spacer(modifier = Modifier.height(14.dp))
            ShimmerPlaceholder(modifier = Modifier.width(150.dp).height(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerPlaceholder(modifier = Modifier.width(90.dp).height(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
              ShimmerPlaceholder(modifier = Modifier.width(70.dp).height(14.dp))
              ShimmerPlaceholder(modifier = Modifier.width(70.dp).height(14.dp))
            }
          }
        }
      }
      items(2) {
        Surface(
          modifier = Modifier.fillMaxWidth().height(80.dp),
          color = GlassyCard.copy(alpha = 0.5f),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            ShimmerPlaceholder(modifier = Modifier.width(110.dp).height(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.5f).height(10.dp))
          }
        }
      }
    } else {
      // Hero profile visual banner card in Bento Style
      item {
        Surface(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Glow Outer Circle Avatar border with Bento color gradients
          Box(
            modifier = Modifier
              .size(92.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(NeonCyan, BentoIndigo, SoftNeonMint)
                )
              )
              .padding(4.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(DeepObsidian),
              contentAlignment = Alignment.Center
            ) {
              if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                  model = avatarUrl,
                  contentDescription = "User Avatar",
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                )
              } else {
                Text(moodEmoji, fontSize = 42.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            nickname,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
          )
          Text(
            "@$handle",
            color = Color.Gray,
            fontSize = 13.sp
          )

          if (headline.isNotEmpty()) {
            Surface(
              color = NeonCyan.copy(alpha = 0.05f),
              border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.12f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).testTag("profile_headline_badge")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Text("💬", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = headline,
                  color = Color.White.copy(alpha = 0.92f),
                  fontSize = 11.sp,
                  fontStyle = FontStyle.Italic,
                  fontWeight = FontWeight.Medium,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Text(
            bio,
            color = Color.LightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Place,
              contentDescription = "Place",
              tint = Color.Gray,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              location,
              color = Color.Gray,
              fontSize = 11.sp
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DateRange,
              contentDescription = "Joined Date",
              tint = Color.Gray,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = formatJoinedDateString(joinDate),
              color = Color.Gray,
              fontSize = 11.sp
            )
          }

          // Dynamic Interests Tags Display section
          val parsedInterestsList = remember(interests) {
            if (interests.trim().isEmpty()) emptyList<String>()
            else interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          }
          
          if (parsedInterestsList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("profile_interests_scroll_row"),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              parsedInterestsList.forEach { tagVal ->
                Surface(
                  color = BentoIndigo.copy(alpha = 0.15f),
                  border = BorderStroke(1.dp, SoftNeonMint.copy(alpha = 0.25f)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = tagVal,
                    color = SoftNeonMint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Edit status button
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(NeonCyan.copy(alpha = 0.12f))
                .clickable { showSelectMoodDialog = true }
                .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(moodEmoji, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Status: $mood", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }

            // Adapt Identity button
            Button(
              onClick = {
                val draftExists = prefs.getBoolean("profile_draft_exists", false)
                if (draftExists) {
                  editNicknameInput = prefs.getString("draft_nickname", nickname) ?: nickname
                  editBioInput = prefs.getString("draft_bio", bio) ?: bio
                  editLocationInput = prefs.getString("draft_location", location) ?: location
                  editAvatarInput = prefs.getString("draft_avatar", avatarUrl ?: "") ?: ""
                  editHandleInput = prefs.getString("draft_handle", handle) ?: handle
                  editEmailInput = prefs.getString("draft_email", email) ?: email
                  editHeadlineInput = prefs.getString("draft_headline", headline) ?: headline
                  editInterestsInput = prefs.getString("draft_interests", interests) ?: interests
                  hasDraft = true
                } else {
                  editNicknameInput = nickname
                  editBioInput = bio
                  editLocationInput = location
                  editAvatarInput = avatarUrl ?: ""
                  editHandleInput = handle
                  editEmailInput = email
                  editHeadlineInput = headline
                  editInterestsInput = interests
                  hasDraft = false
                }
                showEditProfileDialog = true
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.08f),
                contentColor = NeonCyan
              ),
              border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.height(30.dp),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(10.dp), tint = NeonCyan)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Adapt Identity", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Gamification progress levels tracker (XP indicator) in Bento block style
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(imageVector = Icons.Default.Star, contentDescription = "XP Icon", tint = NeonCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Vibe Score & Progression", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Spacer(modifier = Modifier.weight(1f))

            Text("Rank #12", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Total XP Gained:", color = Color.Gray, fontSize = 13.sp)
            Text("$xp XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Level progress indicator
          LinearProgressIndicator(
            progress = (xp % 100) / 100f,
            color = NeonCyan,
            trackColor = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            "${100 - (xp % 100)} XP more to level up!",
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Daily Streak", tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Active Streak:", color = Color.Gray, fontSize = 13.sp)

            Spacer(modifier = Modifier.weight(1f))

            Text("$streak Continuous Days", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }

    // Display list of achievements unlocked inside Bento grid
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("Unlocked Achievements", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = GlassyCard,
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🏆", fontSize = 24.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Savannah Pioneer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Joined during early alpha state", color = Color.Gray, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }

    // Log Out button in Bento block style
    item {
      Button(
        onClick = onLogOut,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color.White.copy(alpha = 0.05f),
          contentColor = Color(0xFFE71D36)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE71D36).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("logout_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Log out",
            tint = Color(0xFFE71D36),
            modifier = Modifier.size(16.dp)
          )
          Text("Deregister Explorer / Sign Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

  // ----------------------------------------------------------------------------
  // MOOD SELECTOR DROPDOWN POPUP
  // ----------------------------------------------------------------------------
  if (showSelectMoodDialog) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.6f))
        .clickable { showSelectMoodDialog = false },
      contentAlignment = Alignment.Center
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.82f)
          .wrapContentHeight()
          .clickable(enabled = false) {},
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            "Select Mood State",
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
          )

          val moods = listOf(
            Triple("Vibing", "🦁", "Roaming on cosmic frequencies"),
            Triple("Chill", "🍉", "Relaxing on camp shores"),
            Triple("Creating", "🎹", "Sound design & caption synthesis"),
            Triple("Night Hunt", "🐆", "Sharing deep savanna mysteries")
          )

          moods.forEach { (text, emo, desc) ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onMoodSelected(text, emo)
                  showSelectMoodDialog = false
                }
                .padding(vertical = 10.dp)
            ) {
              Text(emo, fontSize = 20.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------------
  // PROFILE EDITING DIALOG POPUP
  // ----------------------------------------------------------------------------
  // Modern profile editing state variables
  val scope = rememberCoroutineScope()
  var isSavingProfile by remember { mutableStateOf(false) }
  var profileEditError by remember { mutableStateOf("") }
  var editOtpRequiredMode by remember { mutableStateOf(false) }
  var editOtpInput by remember { mutableStateOf("") }

  // OTP state tracking with real-time feedback for changing email address
  var isEditOtpChecking by remember { mutableStateOf(false) }
  var isEditOtpWrong by remember { mutableStateOf(false) }

  // Preset cybernetic avatars helper
  val avatarPresets = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=150",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=150",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=150",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=150"
  )

  if (showEditProfileDialog) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .testTag("full_edit_profile_page"),
      color = Color(0xFF0F0F14)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .navigationBarsPadding()
          .padding(16.dp)
      ) {
        // Full Screen Header
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
              text = "Adapt Identity Synthesis",
              color = NeonCyan,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 18.sp,
              letterSpacing = (-0.5).sp
            )
            
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.End
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(SoftNeonMint)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Security Shield Engaged",
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = "Fine-tune your explorer profile across the entire Safari ecosystem",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 14.dp)
          )

          // Scrollable inputs section
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            if (hasDraft) {
              item {
                Surface(
                  color = SoftNeonMint.copy(alpha = 0.08f),
                  border = BorderStroke(1.dp, SoftNeonMint.copy(alpha = 0.2f)),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("draft_restored_banner")
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        "RESTORED DRAFT DETECTED",
                        color = SoftNeonMint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        "Your unsaved details were recovered so you didn't lose any progress.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                      onClick = {
                        prefs.edit().putBoolean("profile_draft_exists", false).apply()
                        editNicknameInput = nickname
                        editBioInput = bio
                        editLocationInput = location
                        editAvatarInput = avatarUrl ?: ""
                        editHandleInput = handle
                        editEmailInput = email
                        editHeadlineInput = headline
                        editInterestsInput = interests
                        hasDraft = false
                        android.widget.Toast.makeText(context, "Draft cleared. Reset to profile standard.", android.widget.Toast.LENGTH_SHORT).show()
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = DangerCrimson.copy(alpha = 0.2f),
                        contentColor = DangerCrimson
                      ),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.testTag("clear_draft_button").height(28.dp)
                    ) {
                      Text("Discard", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            // Error Message Banner if exists
            if (profileEditError.isNotEmpty()) {
              item {
                Surface(
                  color = DangerCrimson.copy(alpha = 0.15f),
                  border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.3f)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                  Text(
                    text = profileEditError,
                    color = DangerCrimson,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              }
            }

            // OTP CHANGE EMAIL GATE
            if (editOtpRequiredMode) {
              item {
                Surface(
                  color = NeonCyan.copy(alpha = 0.08f),
                  border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                      "EMAIL VERIFICATION REQUISITION",
                      color = NeonCyan,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.ExtraBold,
                      letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      "To change your registered address, type the 6-digit confirmation proof dispatched to: ${editEmailInput.trim()}",
                      color = Color.LightGray,
                      fontSize = 12.sp
                    )
                  }
                }
              }

              item {
                Text("Verification Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                  value = editOtpInput,
                  onValueChange = { inputVal ->
                    if (inputVal.length <= 6 && inputVal.all { it.isDigit() }) {
                      editOtpInput = inputVal
                      
                      // Auto trigger validation if reaches 6 digits
                      if (inputVal.length == 6) {
                        isSavingProfile = true
                        isEditOtpChecking = true
                        isEditOtpWrong = false
                        profileEditError = ""
                        scope.launch {
                          try {
                            val body = mapOf(
                              "displayName" to editNicknameInput.trim(),
                              "bio" to editBioInput.trim(),
                              "locationLabel" to editLocationInput.trim(),
                              "avatarUrl" to editAvatarInput.trim(),
                              "username" to editHandleInput.trim(),
                              "email" to editEmailInput.trim().lowercase(),
                              "otp" to inputVal
                            )
                            val resp = NetworkService.api.editProfile(body)
                            android.widget.Toast.makeText(context, "Identity synthesization approved!", android.widget.Toast.LENGTH_SHORT).show()
                            onProfileUpdated(
                              editNicknameInput.trim(),
                              editBioInput.trim(),
                              editLocationInput.trim(),
                              editAvatarInput.trim(),
                              editHandleInput.trim(),
                              editEmailInput.trim().lowercase(),
                              editHeadlineInput.trim(),
                              editInterestsInput.trim()
                            )
                            prefs.edit().putBoolean("profile_draft_exists", false).apply()
                            hasDraft = false
                            showEditProfileDialog = false
                            editOtpRequiredMode = false
                            editOtpInput = ""
                          } catch (e: Exception) {
                            isEditOtpWrong = true
                            profileEditError = "✗ Verification Code incorrect: " + (e.localizedMessage ?: "Please confirm digits.")
                          } finally {
                            isSavingProfile = false
                            isEditOtpChecking = false
                          }
                        }
                      }
                    }
                  },
                  placeholder = { Text("Enter 6-digit OTP code", color = Color.Gray) },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isEditOtpWrong) DangerCrimson else NeonCyan,
                    unfocusedBorderColor = if (isEditOtpWrong) DangerCrimson.copy(alpha = 0.5f) else Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("edit_email_otp_input"),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  trailingIcon = {
                    if (isEditOtpChecking) {
                      CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (isEditOtpWrong) {
                      Icon(imageVector = Icons.Default.Warning, contentDescription = "Error icon", tint = DangerCrimson, modifier = Modifier.size(18.dp))
                    }
                  }
                )
                
                if (isEditOtpWrong) {
                  Text(
                    text = "The OTP code was incorrect. Please review the emails.",
                    color = DangerCrimson,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                  )
                }
              }
            } else {
              // ALL EDITABLE PROFILE FIELDS
              item {
                Text("Explorer Display Name (Nickname)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                  value = editNicknameInput,
                  onValueChange = { editNicknameInput = it },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("edit_nickname_input"),
                  singleLine = true
                )
              }

              item {
                Text("Unique Handle (Username)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                  value = editHandleInput,
                  onValueChange = { editHandleInput = it },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("edit_handle_input"),
                  singleLine = true
                )
              }

              item {
                Text("Registered Email Address", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                  value = editEmailInput,
                  onValueChange = { editEmailInput = it },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("edit_email_input"),
                  singleLine = true
                )
              }

              item {
                Text("Personal Bio Description", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                  value = editBioInput,
                  onValueChange = { editBioInput = it },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                )
              }

              item {
                Text("Sector / Location", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                  value = editLocationInput,
                  onValueChange = { editLocationInput = it },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("edit_location_input"),
                  singleLine = true
                )
              }

              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Short Headline Status", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${editHeadlineInput.length}/60",
                    color = if (editHeadlineInput.length > 55) DangerCrimson else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                
                OutlinedTextField(
                  value = editHeadlineInput,
                  onValueChange = { newVal ->
                    if (newVal.length <= 60) {
                      editHeadlineInput = newVal
                    }
                  },
                  placeholder = { Text("E.g. Listening to radio waves...", color = Color.Gray, fontSize = 12.sp) },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("edit_headline_input")
                )

                // Status Emoji Quick Picker Row
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  val emojis = listOf("💬", "🚀", "🪐", "🦁", "💻", "✨", "🔥", "🎧", "🎮", "🌟", "👾", "🏕️", "🛸", "🤖")
                  emojis.forEach { emoji ->
                    Box(
                      modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable {
                          if (editHeadlineInput.length + emoji.length <= 60) {
                            editHeadlineInput = editHeadlineInput + emoji
                          }
                        }
                        .testTag("emoji_picker_$emoji"),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(emoji, fontSize = 14.sp)
                    }
                  }
                }
              }

              item {
                Text("Interests & Orbit Tags", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                val currentInterestsList = remember(editInterestsInput) {
                  if (editInterestsInput.trim().isEmpty()) emptyList<String>()
                  else editInterestsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                var newInterestText by remember { mutableStateOf("") }
                
                OutlinedTextField(
                  value = newInterestText,
                  onValueChange = { newInterestText = it },
                  placeholder = { Text("E.g. Quantum Physics, Live Beats...", color = Color.Gray, fontSize = 12.sp) },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  singleLine = true,
                  trailingIcon = {
                    IconButton(
                      modifier = Modifier.testTag("add_interest_button"),
                      onClick = {
                        val trimmed = newInterestText.trim()
                        if (trimmed.isNotEmpty()) {
                          if (!currentInterestsList.any { it.equals(trimmed, ignoreCase = true) }) {
                            val newList = currentInterestsList + trimmed
                            editInterestsInput = newList.joinToString(", ")
                          }
                          newInterestText = ""
                        }
                      }
                    ) {
                      Icon(imageVector = Icons.Default.Add, contentDescription = "Add tag", tint = SoftNeonMint)
                    }
                  },
                  modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                // Current tags list with interactive delete action
                if (currentInterestsList.isNotEmpty()) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    currentInterestsList.forEach { tagStr ->
                      Surface(
                        color = BentoIndigo.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SoftNeonMint.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("interest_tag_$tagStr")
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text(
                            text = tagStr,
                            color = SoftNeonMint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove tag",
                            tint = DangerCrimson,
                            modifier = Modifier
                              .size(12.dp)
                              .clickable {
                                val newList = currentInterestsList - tagStr
                                editInterestsInput = newList.joinToString(", ")
                              }
                          )
                        }
                      }
                    }
                  }
                }

                // Preset Interests Row
                Text("Quick Add Orbit Presets:", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  val presets = listOf("AI", "Web3", "Gaming", "Beats", "Cosmos", "Design", "Biology", "Cyber-Punk")
                  presets.forEach { preset ->
                    val isAdded = currentInterestsList.any { it.equals(preset, ignoreCase = true) }
                    Surface(
                      color = if (isAdded) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                      border = BorderStroke(1.dp, if (isAdded) NeonCyan else Color.White.copy(alpha = 0.1f)),
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier
                        .clickable {
                          if (isAdded) {
                            val filtered = currentInterestsList.filter { !it.equals(preset, ignoreCase = true) }
                            editInterestsInput = filtered.joinToString(", ")
                          } else {
                            val newList = currentInterestsList + preset
                            editInterestsInput = newList.joinToString(", ")
                          }
                        }
                        .testTag("preset_interest_$preset")
                    ) {
                      Text(
                        text = if (isAdded) "✓ $preset" else "+ $preset",
                        color = if (isAdded) NeonCyan else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }
                }
              }

              item {
                Text("Avatar Icon Presets", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                  avatarPresets.forEach { url ->
                    val isSelected = editAvatarInput == url
                    Box(
                      modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(
                          width = if (isSelected) 3.dp else 1.dp,
                          color = if (isSelected) SoftNeonMint else Color.White.copy(alpha = 0.15f),
                          shape = CircleShape
                        )
                        .clickable { editAvatarInput = url }
                    ) {
                      AsyncImage(
                        model = url,
                        contentDescription = "Avatar preset option",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                      )
                    }
                  }
                }
              }

              item {
                Text("Custom Profile Picture", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = { avatarPickerLauncher.launch("image/*") },
                  colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                  Icon(imageVector = Icons.Default.AccountBox, contentDescription = "Pick Avatar Icon", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Pick Picture from local storage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                if (editAvatarInput.isNotEmpty() && !avatarPresets.contains(editAvatarInput)) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(8.dp)
                  ) {
                    Box(
                      modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, NeonCyan, CircleShape)
                    ) {
                      AsyncImage(
                        model = editAvatarInput,
                        contentDescription = "Selected Local Picture Preview",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                      )
                    }
                    Text("✓ Local Storage Chosen", color = SoftNeonMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Bottom Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { 
                showEditProfileDialog = false 
                editOtpRequiredMode = false
                editOtpInput = ""
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.05f),
                contentColor = Color.Gray
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (!editOtpRequiredMode) {
              Button(
                onClick = {
                  if (isSavingProfile) return@Button
                  isSavingProfile = true
                  profileEditError = ""
                  scope.launch {
                    try {
                      val body = mapOf(
                        "displayName" to editNicknameInput.trim(),
                        "bio" to editBioInput.trim(),
                        "locationLabel" to editLocationInput.trim(),
                        "avatarUrl" to editAvatarInput.trim(),
                        "username" to editHandleInput.trim(),
                        "email" to editEmailInput.trim().lowercase(),
                        "headline" to editHeadlineInput.trim(),
                        "interests" to editInterestsInput.trim()
                      )
                      val resp = NetworkService.api.editProfile(body)
                      if (resp.requiresOtp == true) {
                        editOtpRequiredMode = true
                      } else {
                        android.widget.Toast.makeText(context, "Identity credentials updated!", android.widget.Toast.LENGTH_SHORT).show()
                        onProfileUpdated(
                          editNicknameInput.trim(),
                          editBioInput.trim(),
                          editLocationInput.trim(),
                          editAvatarInput.trim(),
                          editHandleInput.trim(),
                          editEmailInput.trim().lowercase(),
                          editHeadlineInput.trim(),
                          editInterestsInput.trim()
                        )
                        prefs.edit().putBoolean("profile_draft_exists", false).apply()
                        hasDraft = false
                        showEditProfileDialog = false
                      }
                    } catch (e: Exception) {
                      profileEditError = e.localizedMessage ?: "Failed updating credentials."
                    } finally {
                      isSavingProfile = false
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = NeonCyan,
                  contentColor = Color.Black
                ),
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSavingProfile
              ) {
                if (isSavingProfile) {
                  CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save icon", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            } else {
              // When in OTP verifying mode, show a different direct verification submit button
              Button(
                onClick = {
                  if (editOtpInput.trim().length != 6) {
                    profileEditError = "Please input the 6-digit code register."
                    return@Button
                  }
                  isSavingProfile = true
                  profileEditError = ""
                  isEditOtpChecking = true
                  scope.launch {
                    try {
                      val body = mapOf(
                        "displayName" to editNicknameInput.trim(),
                        "bio" to editBioInput.trim(),
                        "locationLabel" to editLocationInput.trim(),
                        "avatarUrl" to editAvatarInput.trim(),
                        "username" to editHandleInput.trim(),
                        "email" to editEmailInput.trim().lowercase(),
                        "otp" to editOtpInput.trim(),
                        "headline" to editHeadlineInput.trim(),
                        "interests" to editInterestsInput.trim()
                      )
                      val resp = NetworkService.api.editProfile(body)
                      android.widget.Toast.makeText(context, "Email update successfully verified!", android.widget.Toast.LENGTH_SHORT).show()
                      onProfileUpdated(
                        editNicknameInput.trim(),
                        editBioInput.trim(),
                        editLocationInput.trim(),
                        editAvatarInput.trim(),
                        editHandleInput.trim(),
                        editEmailInput.trim().lowercase(),
                        editHeadlineInput.trim(),
                        editInterestsInput.trim()
                      )
                      prefs.edit().putBoolean("profile_draft_exists", false).apply()
                      hasDraft = false
                      showEditProfileDialog = false
                      editOtpRequiredMode = false
                      editOtpInput = ""
                    } catch (e: Exception) {
                      isEditOtpWrong = true
                      profileEditError = "✗ Verification Code is incorrect. Please check the digits."
                    } finally {
                      isSavingProfile = false
                      isEditOtpChecking = false
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = SoftNeonMint,
                  contentColor = Color.Black
                ),
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSavingProfile && editOtpInput.trim().length == 6
              ) {
                if (isSavingProfile) {
                  CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verify check", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify & Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

// ==============================================================================
// 🌟 STORY VIEWER OVERLAY COMPOSABLE
// ==============================================================================
@Composable
fun StoryViewerOverlay(
  moment: MobileMoment,
  onDismiss: () -> Unit
) {
  var progress by remember { mutableStateOf(0f) }
  LaunchedEffect(key1 = moment.id) {
    // Smoother countdown timer over 4 seconds
    val steps = 100
    for (i in 1..steps) {
      delay(40)
      progress = i / 100f
    }
    onDismiss()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.95f))
      .clickable { onDismiss() }
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.65f)
        .clickable(enabled = false) {}, // Prevent dismiss tap inside the story card
      color = DeepObsidian,
      shape = RoundedCornerShape(28.dp),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(Color(0xFF161525), DeepObsidian)
            )
          )
          .padding(24.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            // Live progress tracker at top of stories format
            LinearProgressIndicator(
              progress = progress,
              color = NeonCyan,
              trackColor = Color.White.copy(alpha = 0.08f),
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Author metadata header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
              ) {
                Text(moment.emoji, fontSize = 20.sp)
              }
              Column {
                Text(
                  moment.author,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                Text(
                  "Savannah Live Moments",
                  color = Color.LightGray,
                  fontSize = 12.sp
                )
              }
            }
          }

          // Giant glowing centered graphic representation
          Box(
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .size(150.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(moment.emoji, fontSize = 76.sp)
          }

          // Descriptive status block
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
          ) {
            val storiesPoetry = when (moment.id) {
              "m1" -> "Roaming beneath the glowing cyber canopy. Solar winds feel warm tonight! 🦁🌾"
              "m2" -> "A quick lo-fi ambient synthesis session by the neon lakes. Celestial energy in the air! ✨🎹"
              "m3" -> "Spotted some electronic lights flickering deep near the emerald sector! 🐆🌌"
              "m4" -> "Constructing a fresh virtual campfire deck for the late-night chat logs. ⛺🦊"
              "m5" -> "The starry sky projection from sector-3 looks completely mesmerizing tonight! 🌌🐱"
              else -> "Enjoying fresh cosmic vibes on the digital savanna expedition. 🏕️"
            }

            Text(
              storiesPoetry,
              color = Color.White,
              fontWeight = FontWeight.Medium,
              fontSize = 14.sp,
              textAlign = TextAlign.Center,
              lineHeight = 22.sp,
              modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
              onClick = onDismiss,
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(40.dp)
            ) {
              Text("Dismiss Expedition", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

// ==============================================================================
// 🌟 SECURE BENTO AUTHENTICATION COMPOSABLE
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BentoAuthScreen(
  onAuthenticated: (nickname: String, handle: String, token: String) -> Unit
) {
  val scope = rememberCoroutineScope()
  val context = androidx.compose.ui.platform.LocalContext.current
  var isLoading by remember { mutableStateOf(false) }

  // Forgot password flow states
  var showForgotPassword by remember { mutableStateOf(false) }
  var forgotPasswordStep by remember { mutableStateOf(1) } // 1: Enter identity, 2: Verify OTP, 3: Set new password
  var forgotIdentityInput by remember { mutableStateOf("") }
  var forgotOtpInput by remember { mutableStateOf("") }
  var forgotNewPasswordInput by remember { mutableStateOf("") }
  var forgotConfirmPasswordInput by remember { mutableStateOf("") }
  var forgotErrorMessage by remember { mutableStateOf("") }
  var forgotSentEmailAddress by remember { mutableStateOf("") }
  var forgotDebugOtp by remember { mutableStateOf<String?>(null) }

  // Shared state
  var isSignUp by remember { mutableStateOf(false) }
  var authErrorMessage by remember { mutableStateOf("") }
  var otpInput by remember { mutableStateOf("") }
  var showOtpVerifyInput by remember { mutableStateOf(false) }

  // Login inputs
  var loginCredentialInput by remember { mutableStateOf("") }
  var loginPasswordInput by remember { mutableStateOf("") }

  // Signup inputs
  var nicknameInput by remember { mutableStateOf("") }
  var usernameInput by remember { mutableStateOf("") }
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var confirmPasswordInput by remember { mutableStateOf("") }

  var isUsernameFocused by remember { mutableStateOf(false) }
  var isEmailFocused by remember { mutableStateOf(false) }
  var isPasswordFocused by remember { mutableStateOf(false) }
  var isConfirmPasswordFocused by remember { mutableStateOf(false) }

  // Signup live backend validation states
  var isUsernameValidating by remember { mutableStateOf(false) }
  var isUsernameAvailable by remember { mutableStateOf(false) }
  var usernameBackendError by remember { mutableStateOf<String?>(null) }

  var isEmailValidating by remember { mutableStateOf(false) }
  var isEmailAvailable by remember { mutableStateOf(false) }
  var emailBackendError by remember { mutableStateOf<String?>(null) }

  // Live rule checking: Username constraints
  val usernameLengthOk = usernameInput.length >= 3 && usernameInput.length <= 16
  val usernameNoSpaces = !usernameInput.contains(" ")
  val usernameAllowedChars = usernameInput.all { it.isLetterOrDigit() || it == '_' || it == '.' }
  val usernameNotStartWithPeriod = !usernameInput.startsWith(".")
  val usernameNotEndWithPeriod = !usernameInput.endsWith(".")
  val usernameNoConsecutivePeriods = !usernameInput.contains("..")

  val isUsernameLocallyValid = usernameInput.isNotEmpty() &&
      usernameLengthOk &&
      usernameNoSpaces &&
      usernameAllowedChars &&
      usernameNotStartWithPeriod &&
      usernameNotEndWithPeriod &&
      usernameNoConsecutivePeriods

  // Live rule checking: Email constraints
  val emailContainsAt = emailInput.contains("@")
  val emailIsValidFormat = android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()
  val isGmailcomValid = if (emailInput.contains("@gmail.")) {
    emailInput.endsWith("@gmail.com") || (emailInput.substringAfter("@").endsWith(".com") && emailInput.contains("gmail"))
  } else {
    true
  }
  val isEmailLocallyValid = emailInput.isNotEmpty() && emailContainsAt && emailIsValidFormat && isGmailcomValid

  // Live rule checking: Password constraints
  val passLengthOk = passwordInput.length >= 6
  val passHasLetter = passwordInput.any { it.isLetter() }
  val passHasDigit = passwordInput.any { it.isDigit() }
  val passHasSymbol = passwordInput.any { !it.isLetterOrDigit() }
  val isPasswordStrong = passLengthOk && passHasLetter && passHasDigit && passHasSymbol

  // Live rule checking: Confirm password
  val isConfirmPasswordMatch = confirmPasswordInput == passwordInput

  // Live database check: Username uniqueness
  LaunchedEffect(usernameInput) {
    if (usernameInput.isEmpty()) {
      isUsernameAvailable = false
      usernameBackendError = null
      return@LaunchedEffect
    }
    if (!isUsernameLocallyValid) {
      isUsernameAvailable = false
      usernameBackendError = "Follow the username rules."
      return@LaunchedEffect
    }
    delay(400)
    isUsernameValidating = true
    try {
      val res = NetworkService.api.checkUsername(usernameInput.trim())
      isUsernameAvailable = res.available
      usernameBackendError = res.error
    } catch (e: Exception) {
      isUsernameAvailable = true
      usernameBackendError = null
    } finally {
      isUsernameValidating = false
    }
  }

  // Live database check: Email existence
  LaunchedEffect(emailInput) {
    if (emailInput.isEmpty()) {
      isEmailAvailable = false
      emailBackendError = null
      return@LaunchedEffect
    }
    if (!isEmailLocallyValid) {
      isEmailAvailable = false
      emailBackendError = "Enter a valid email address structure."
      return@LaunchedEffect
    }
    delay(400)
    isEmailValidating = true
    try {
      val res = NetworkService.api.checkEmail(emailInput.trim().lowercase())
      isEmailAvailable = res.available
      emailBackendError = res.error
    } catch (e: Exception) {
      isEmailAvailable = true
      emailBackendError = null
    } finally {
      isEmailValidating = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(DeepObsidian, Color(0xFF101014), DeepObsidian)
        )
      )
      .padding(16.dp)
      .imePadding(),
    contentAlignment = Alignment.Center
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 480.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(vertical = 24.dp)
    ) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = GlassyCard,
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
          Column(modifier = Modifier.padding(24.dp)) {
            // INTEGRATED PREMIUM HEADER SECTION
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(listOf(NeonCyan, BentoIndigo, SoftNeonMint)))
                  .padding(2.5.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(DeepObsidian),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isSignUp) Icons.Default.Person else Icons.Default.Lock,
                    contentDescription = "Auth Icon",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = when {
                    showForgotPassword -> "Restore Passphrase"
                    isSignUp -> "Synthesize Profile"
                    else -> "Safari Gateway"
                  },
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 20.sp,
                  letterSpacing = (-0.5).sp
                )
                Text(
                  text = when {
                    showForgotPassword -> "Dispatching secure recovery frequencies."
                    isSignUp -> "Configure secure credentials for Pioneer."
                    else -> "Sign in to enter the social sphere."
                  },
                  color = Color.Gray,
                  fontSize = 12.sp
                )
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (showForgotPassword) {
              // ================= FORGOT PASSWORD LAYOUT =================
              when (forgotPasswordStep) {
                1 -> {
                  // Step 1: Input Identity (Username or Email)
                  Text(
                    text = "Enter Explorer Username or Email",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                  )
                  OutlinedTextField(
                    value = forgotIdentityInput,
                    onValueChange = { forgotIdentityInput = it },
                    placeholder = { Text("e.g. pioneer_prime or princemaster@gmail.com", color = Color.Gray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = NeonCyan,
                      unfocusedBorderColor = Color.DarkGray,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_identity_input"),
                    singleLine = true
                  )

                  if (forgotErrorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = forgotErrorMessage,
                      color = DangerCrimson,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                    
                    if (forgotErrorMessage.contains("not registered")) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Button(
                        onClick = {
                          isSignUp = true
                          showForgotPassword = false
                          authErrorMessage = ""
                          if (forgotIdentityInput.contains("@")) {
                            emailInput = forgotIdentityInput
                          } else {
                            usernameInput = forgotIdentityInput
                          }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f), contentColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                      ) {
                        Text("Create a new Pioneer Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(20.dp))

                  Button(
                    onClick = {
                      if (isLoading) return@Button
                      if (forgotIdentityInput.isEmpty()) {
                        forgotErrorMessage = "Please type your account username or email."
                        return@Button
                      }
                      forgotErrorMessage = ""
                      isLoading = true
                      scope.launch {
                        try {
                          val response = NetworkService.api.forgotPassword(mapOf("usernameOrEmail" to forgotIdentityInput.trim()))
                          forgotSentEmailAddress = response["email"] as? String ?: forgotIdentityInput.trim()
                          forgotDebugOtp = response["debugOtp"] as? String
                          forgotPasswordStep = 2
                        } catch (e: Exception) {
                          val msg = e.message ?: ""
                          if (msg.contains("404")) {
                            forgotErrorMessage = "This account username or email is not registered. Would you like to create an account?"
                          } else {
                            forgotErrorMessage = "Database connect failure. Retrying frequency attunement..."
                          }
                        } finally {
                          isLoading = false
                        }
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forgot_submit_identity_btn")
                  ) {
                    if (isLoading) {
                      CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatch Verification Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(14.dp))

                  TextButton(
                    onClick = { showForgotPassword = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                  ) {
                    Text("Return to Sign In", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  }
                }
                2 -> {
                  // Step 2: Verification Input
                  Text(
                    text = "COSMIC SECURITY VERIFICATION",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "A secure verification code has been dispatched to: $forgotSentEmailAddress",
                    color = Color.LightGray,
                    fontSize = 12.sp
                  )

                  if (forgotDebugOtp != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "(Telemetry Debug Code: $forgotDebugOtp)",
                      color = SoftNeonMint,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                  OutlinedTextField(
                    value = forgotOtpInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) forgotOtpInput = it },
                    placeholder = { Text("Enter 6-digit OTP code", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = NeonCyan,
                      unfocusedBorderColor = Color.DarkGray,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_otp_input"),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                      keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                  )

                  if (forgotErrorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = forgotErrorMessage,
                      color = DangerCrimson,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Spacer(modifier = Modifier.height(20.dp))

                  val isOtpLengthCompleted = forgotOtpInput.trim().length == 6
                  Button(
                    onClick = {
                      if (!isOtpLengthCompleted) return@Button
                      forgotErrorMessage = ""
                      forgotPasswordStep = 3
                    },
                    enabled = isOtpLengthCompleted,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = SoftNeonMint,
                      contentColor = Color.Black,
                      disabledContainerColor = SoftNeonMint.copy(alpha = 0.15f),
                      disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forgot_otp_verify_btn")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center
                    ) {
                      Icon(imageVector = Icons.Default.Check, contentDescription = "Verify", modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Verify Cosmic Recovery Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                  }

                  Spacer(modifier = Modifier.height(20.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Incorrect address? Change email",
                      color = NeonCyan,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      style = TextStyle(textDecoration = TextDecoration.Underline),
                      modifier = Modifier.clickable {
                        forgotPasswordStep = 1
                        forgotErrorMessage = ""
                      }
                    )
                  }
                }
                3 -> {
                  // Step 3: Change Password
                  val passLengthOk = forgotNewPasswordInput.length >= 6
                  val passHasLetter = forgotNewPasswordInput.any { it.isLetter() }
                  val passHasDigit = forgotNewPasswordInput.any { it.isDigit() }
                  val passHasSymbol = forgotNewPasswordInput.any { !it.isLetterOrDigit() }
                  val isPasswordStrong = passLengthOk && passHasLetter && passHasDigit && passHasSymbol
                  val isConfirmMatch = forgotConfirmPasswordInput == forgotNewPasswordInput && forgotConfirmPasswordInput.isNotEmpty()

                  Text(
                    text = "ESTABLISH NEW PASSPHRASE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                  )
                  Text(
                    text = "Input a brand-new strong passphrase below to synthesize security credentials.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                  )

                  Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                  ) {
                    OutlinedTextField(
                      value = forgotNewPasswordInput,
                      onValueChange = { forgotNewPasswordInput = it },
                      placeholder = { Text("New Passphrase", color = Color.Gray) },
                      visualTransformation = PasswordVisualTransformation(),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.fillMaxWidth().testTag("forgot_new_password_input"),
                      singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                      value = forgotConfirmPasswordInput,
                      onValueChange = { forgotConfirmPasswordInput = it },
                      placeholder = { Text("Confirm New Passphrase", color = Color.Gray) },
                      visualTransformation = PasswordVisualTransformation(),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.fillMaxWidth().testTag("forgot_confirm_password_input"),
                      singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("COSMIC SECURITY CHECKS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      val rules = listOf(
                        Triple(passLengthOk, "At least 6 characters in length", "1"),
                        Triple(passHasLetter, "Contains alphanumeric alphabetic letters", "2"),
                        Triple(passHasDigit, "Contains digital numerical parameters", "3"),
                        Triple(passHasSymbol, "Contains special characters/symbols", "4"),
                        Triple(true, "NEW: Must not match previous old password", "5")
                      )
                      rules.forEach { (ok, ruleLabel, code) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (ok) SoftNeonMint else Color.Gray,
                            modifier = Modifier.size(13.dp)
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(ruleLabel, color = if (ok) Color.LightGray else Color.Gray, fontSize = 11.sp)
                        }
                      }
                    }
                  }

                  if (forgotErrorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      text = forgotErrorMessage,
                      color = DangerCrimson,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Spacer(modifier = Modifier.height(20.dp))

                  Button(
                    onClick = {
                      if (!isPasswordStrong || !isConfirmMatch || isLoading) return@Button
                      forgotErrorMessage = ""
                      isLoading = true
                      scope.launch {
                        try {
                          val body = mapOf(
                            "usernameOrEmail" to forgotIdentityInput.trim(),
                            "otp" to forgotOtpInput.trim(),
                            "newPassword" to forgotNewPasswordInput
                          )
                          val response = NetworkService.api.resetPassword(body)
                          android.widget.Toast.makeText(context, response["message"] as? String ?: "Passphrase reset successful!", android.widget.Toast.LENGTH_LONG).show()
                          
                          showForgotPassword = false
                          forgotPasswordStep = 1
                          loginCredentialInput = forgotIdentityInput
                          loginPasswordInput = ""
                        } catch (e: Exception) {
                          val msg = e.message ?: ""
                          if (msg.contains("same as your old") || msg.contains("400")) {
                            forgotErrorMessage = "✗ Security Violation: Your new password cannot be identical to your old password."
                          } else {
                            forgotErrorMessage = e.localizedMessage ?: "✗ Reset failed. Please check parameters."
                          }
                        } finally {
                          isLoading = false
                        }
                      }
                    },
                    enabled = isPasswordStrong && isConfirmMatch && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = NeonCyan,
                      contentColor = Color.Black,
                      disabledContainerColor = NeonCyan.copy(alpha = 0.15f),
                      disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forgot_finalize_btn")
                  ) {
                    if (isLoading) {
                      CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesize & Apply Passphrase", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      }
                    }
                  }
                }
              }
            } else if (!isSignUp) {
              // ================= SIGN IN LAYOUT =================
              Text(
                text = "Explorer Handle or Email",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = loginCredentialInput,
                onValueChange = { loginCredentialInput = it },
                placeholder = { Text("e.g. pioneer_prime or princemaster@gmail.com", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = NeonCyan,
                  unfocusedBorderColor = Color.DarkGray,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("login_credential_input"),
                singleLine = true
              )

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Secure Passphrase",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = loginPasswordInput,
                onValueChange = { loginPasswordInput = it },
                placeholder = { Text("••••••••", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = NeonCyan,
                  unfocusedBorderColor = Color.DarkGray,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("login_password_input"),
                singleLine = true
              )

              if (authErrorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = authErrorMessage,
                  color = DangerCrimson,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = {
                  if (isLoading) return@Button
                  if (loginCredentialInput.isEmpty() || loginPasswordInput.isEmpty()) {
                    authErrorMessage = "All credentials must be populated."
                    return@Button
                  }
                  authErrorMessage = ""
                  isLoading = true
                  scope.launch {
                    try {
                      val body = mapOf(
                        "username" to loginCredentialInput.trim(),
                        "password" to loginPasswordInput
                      )
                      val response = NetworkService.api.login(body)
                      val token = response.token ?: "mock_token"
                      val displayName = response.user?.displayName ?: response.user?.username ?: loginCredentialInput.trim().substringBefore("@")
                      val realHandle = response.user?.username ?: loginCredentialInput.trim()
                      onAuthenticated(displayName, realHandle, token)
                    } catch (e: Exception) {
                      val rawMessage = e.message ?: ""
                      if (rawMessage.contains("401")) {
                        authErrorMessage = "Invalid credentials. Verify your passphrase and try again."
                      } else {
                        authErrorMessage = "Connection error: Unable to contact Safari Sphere database server. Please try again."
                      }
                    } finally {
                      isLoading = false
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = NeonCyan,
                  contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("login_submit_button")
              ) {
                if (isLoading) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "login_loading")
                    val alpha1 by infiniteTransition.animateFloat(
                      initialValue = 0.2f,
                      targetValue = 1f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "d1"
                    )
                    val alpha2 by infiniteTransition.animateFloat(
                      initialValue = 0.2f,
                      targetValue = 1f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "d2"
                    )
                    val alpha3 by infiniteTransition.animateFloat(
                      initialValue = 0.2f,
                      targetValue = 1f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "d3"
                    )
                    Text("Authenticating Frequencies  ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = alpha1)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = alpha2)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = alpha3)))
                  }
                } else {
                  Text("Verify Credentials & Enter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Forgot your passphrase? Recover it here.",
                color = Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier
                  .align(Alignment.CenterHorizontally)
                  .clickable {
                    showForgotPassword = true
                    forgotPasswordStep = 1
                    forgotIdentityInput = ""
                    forgotErrorMessage = ""
                  }
                  .padding(vertical = 4.dp)
              )

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Don't have an account? ",
                  color = Color.White,
                  fontSize = 14.sp
                )
                Text(
                  text = "Sign up.",
                  color = NeonCyan,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  style = TextStyle(textDecoration = TextDecoration.Underline),
                  modifier = Modifier
                    .clickable {
                      isSignUp = true
                      authErrorMessage = ""
                      showOtpVerifyInput = false
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                )
              }

            } else {
              // ================= SIGN UP LAYOUT =================
              if (showOtpVerifyInput) {
                // DEDICATED PRESTIGE VERIFICATION SCREEN
                Text(
                  text = "Quantum Node Authenticator",
                  color = NeonCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                  text = "Enter Verification Code",
                  color = Color.White,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-0.5).sp,
                  modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Show the email destination beautifully
                Surface(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                  color = Color.White.copy(alpha = 0.04f),
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Email,
                      contentDescription = "Destination Email",
                      tint = NeonCyan,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = "We sent a 6-digit cosmic proof to:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                      )
                      Text(
                        text = emailInput.trim(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                      )
                    }
                  }
                }

                 // OTP state tracking with real-time feedback
                var isOtpChecking by remember { mutableStateOf(false) }
                var isOtpCorrect by remember { mutableStateOf(false) }
                var isOtpWrong by remember { mutableStateOf(false) }

                LaunchedEffect(otpInput) {
                  val trimmed = otpInput.trim()
                  if (trimmed.length < 6) {
                    isOtpCorrect = false
                    isOtpWrong = false
                    if (authErrorMessage.contains("incorrect", ignoreCase = true) || authErrorMessage.contains("Failed", ignoreCase = true)) {
                      authErrorMessage = ""
                    }
                  } else if (trimmed.length == 6) {
                    isOtpChecking = true
                    authErrorMessage = ""
                    try {
                      val body = mutableMapOf(
                        "email" to emailInput.trim(),
                        "password" to passwordInput,
                        "username" to usernameInput.trim(),
                        "displayName" to nicknameInput.trim(),
                        "otp" to trimmed
                      )
                      val response = NetworkService.api.signup(body)
                      val token = response.token ?: "mock_token"
                      isOtpCorrect = true
                      isOtpWrong = false
                      delay(800) // Brief beautiful glow pause
                      onAuthenticated(nicknameInput.trim(), usernameInput.trim(), token)
                      android.widget.Toast.makeText(context, "Identity Verified! Welcome to the sphere.", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                      isOtpCorrect = false
                      isOtpWrong = true
                      authErrorMessage = "✗ Verification Code is incorrect. Please check the digits."
                    } finally {
                      isOtpChecking = false
                    }
                  }
                }

                // OTP Countdown state tracking
                var resendCountdown by remember { mutableIntStateOf(40) }
                LaunchedEffect(showOtpVerifyInput) {
                  resendCountdown = 40
                }
                LaunchedEffect(showOtpVerifyInput, resendCountdown) {
                  if (showOtpVerifyInput && resendCountdown > 0) {
                    delay(1000L)
                    resendCountdown -= 1
                  }
                }

                // Input Guidance Banner
                Text(
                  text = if (isOtpChecking) "Attuning validation wave..." else "Type the digits below to establish full user identity.",
                  color = if (isOtpChecking) NeonCyan else Color.Gray,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(bottom = 8.dp)
                )

                // Beautiful, custom Slot-Based 6-digit OTP Field
                // A hidden BasicTextField overlays a row of 6 gorgeous modern cards!
                Box(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                  contentAlignment = Alignment.Center
                ) {
                  BasicTextField(
                    value = otpInput,
                    onValueChange = { inputVal ->
                      if (inputVal.length <= 6 && inputVal.all { it.isDigit() }) {
                        otpInput = inputVal
                      }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.Transparent), // hide text inside the main input
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(58.dp)
                      .testTag("signup_otp_input"),
                    decorationBox = {
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        repeat(6) { index ->
                          val char = otpInput.getOrNull(index)?.toString() ?: ""
                          val isFocusedSlot = otpInput.length == index
                          val hasChar = char.isNotEmpty()
                          val borderStrokeColor = when {
                            isOtpCorrect -> SoftNeonMint
                            isOtpWrong -> DangerCrimson
                            isFocusedSlot -> NeonCyan
                            hasChar -> SoftNeonMint
                            else -> Color.White.copy(alpha = 0.12f)
                          }
                          val backgroundBoxColor = when {
                            isOtpCorrect -> SoftNeonMint.copy(alpha = 0.12f)
                            isOtpWrong -> DangerCrimson.copy(alpha = 0.12f)
                            isFocusedSlot -> NeonCyan.copy(alpha = 0.08f)
                            hasChar -> SoftNeonMint.copy(alpha = 0.04f)
                            else -> Color.White.copy(alpha = 0.02f)
                          }

                          Box(
                            modifier = Modifier
                              .weight(1f)
                              .height(54.dp)
                              .clip(RoundedCornerShape(12.dp))
                              .background(backgroundBoxColor)
                              .border(
                                width = if (isOtpCorrect || isOtpWrong) 2.dp else if (isFocusedSlot) 2.dp else 1.dp,
                                color = borderStrokeColor,
                                shape = RoundedCornerShape(12.dp)
                              ),
                            contentAlignment = Alignment.Center
                          ) {
                            if (isFocusedSlot && !isOtpChecking) {
                              Box(
                                modifier = Modifier
                                  .width(2.dp)
                                  .height(18.dp)
                                  .background(NeonCyan)
                              )
                            } else if (isOtpChecking && isFocusedSlot) {
                              CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonCyan, strokeWidth = 2.dp)
                            } else {
                              Text(
                                text = char,
                                color = if (isOtpCorrect) SoftNeonMint else if (isOtpWrong) DangerCrimson else if (hasChar) Color.White else Color.Gray.copy(alpha = 0.4f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                              )
                            }
                          }
                        }
                      }
                    }
                  )
                }

                // Error message
                if (authErrorMessage.isNotEmpty()) {
                  Text(
                    text = authErrorMessage,
                    color = DangerCrimson,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                  )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Verify Button
                val isOtpLengthCompleted = otpInput.trim().length == 6
                Button(
                  onClick = {
                    if (isLoading || !isOtpLengthCompleted) return@Button
                    authErrorMessage = ""
                    isLoading = true
                    scope.launch {
                      try {
                        val body = mutableMapOf(
                          "email" to emailInput.trim(),
                          "password" to passwordInput,
                          "username" to usernameInput.trim(),
                          "displayName" to nicknameInput.trim(),
                          "otp" to otpInput.trim()
                        )
                        val response = NetworkService.api.signup(body)
                        val token = response.token ?: "mock_token"
                        onAuthenticated(nicknameInput.trim(), usernameInput.trim(), token)
                        android.widget.Toast.makeText(context, "Identity Verified! Welcome to the sphere.", android.widget.Toast.LENGTH_SHORT).show()
                      } catch (e: Exception) {
                        authErrorMessage = "Verification Failed: " + (e.localizedMessage ?: "Invalid verification code.")
                      } finally {
                        isLoading = false
                      }
                    }
                  },
                  enabled = isOtpLengthCompleted && !isLoading,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = SoftNeonMint,
                    contentColor = Color.Black,
                    disabledContainerColor = SoftNeonMint.copy(alpha = 0.15f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("signup_otp_verify_button")
                ) {
                  if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                  } else {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center
                    ) {
                      Icon(imageVector = Icons.Default.Check, contentDescription = "Verify", modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Verify Cosmic Identity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resend Code Trigger with Countdown Helper
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (resendCountdown > 0) {
                    Text(
                      text = "Resend code in ${resendCountdown}s",
                      color = Color.Gray,
                      fontSize = 12.sp
                    )
                  } else {
                    Text(
                      text = "Did not receive code?",
                      color = Color.LightGray,
                      fontSize = 12.sp
                    )
                    Text(
                      text = "Resend Code",
                      color = NeonCyan,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      style = TextStyle(textDecoration = TextDecoration.Underline),
                      modifier = Modifier.clickable {
                        if (isLoading) return@clickable
                        isLoading = true
                        authErrorMessage = ""
                        scope.launch {
                          try {
                            val body = mapOf(
                              "email" to emailInput.trim(),
                              "password" to passwordInput,
                              "username" to usernameInput.trim(),
                              "displayName" to nicknameInput.trim()
                            )
                            val response = NetworkService.api.signup(body)
                            resendCountdown = 40
                            otpInput = ""
                            authErrorMessage = response.message ?: "A new validation code was dispatched."
                          } catch (e: Exception) {
                            authErrorMessage = "Dispatch failed: " + (e.localizedMessage ?: "Try again.")
                          } finally {
                            isLoading = false
                          }
                        }
                      }
                    )
                  }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Incorrect Email / Go Back Button
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back icon",
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Incorrect details? Change details",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable {
                      showOtpVerifyInput = false
                      authErrorMessage = ""
                    }
                  )
                }
              } else {
                // 1. NICKNAME INPUT
              Text(
                text = "Display Name",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = nicknameInput,
                onValueChange = { nicknameInput = it },
                placeholder = { Text("e.g. Ranger Sam", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = if (nicknameInput.trim().length >= 2) SoftNeonMint else Color.DarkGray,
                  unfocusedBorderColor = if (nicknameInput.trim().length >= 2) SoftNeonMint else Color.DarkGray,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("signup_nickname_input"),
                singleLine = true
              )

              Spacer(modifier = Modifier.height(14.dp))

              // 2. SMART USERNAME INPUT WITH GLOW EFFECT
              val usernameBorderColor = when {
                usernameInput.isEmpty() -> Color.DarkGray
                isUsernameLocallyValid && isUsernameAvailable -> SoftNeonMint
                else -> DangerCrimson
              }

              Text(
                text = "Unique Username Handle",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                placeholder = { Text("e.g. ranger_sam", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = usernameBorderColor,
                  unfocusedBorderColor = usernameBorderColor,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isUsernameFocused = it.isFocused }
                  .testTag("signup_username_input"),
                singleLine = true
              )

              // SMART REAL-TIME FEEDBACK FOR USERNAME
              if (isUsernameFocused && usernameInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  color = Color.White.copy(alpha = 0.02f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = "Username Requirements",
                      color = Color.LightGray,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        ValidationChip("3 - 16 characters", usernameLengthOk)
                        ValidationChip("Only letters, numbers, _, .", usernameAllowedChars)
                        ValidationChip("No double dots (..)", usernameNoConsecutivePeriods)
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        ValidationChip("No spacing allowed", usernameNoSpaces)
                        ValidationChip("Doesn't start with dot", usernameNotStartWithPeriod)
                        ValidationChip("Doesn't end with dot", usernameNotEndWithPeriod)
                      }
                    }

                    if (isUsernameLocallyValid) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(1.dp)
                          .background(Color.White.copy(alpha = 0.05f))
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      if (isUsernameValidating) {
                        Text("Checking availability...", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      } else if (usernameBackendError != null) {
                        Text("✗ Taken: $usernameBackendError", color = DangerCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      } else if (isUsernameAvailable) {
                        Text("✓ Handle is available!", color = SoftNeonMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // 3. SMART EMAIL INPUT WITH GLOW EFFECT
              val emailBorderColor = when {
                emailInput.isEmpty() -> Color.DarkGray
                isEmailLocallyValid && isEmailAvailable -> SoftNeonMint
                else -> DangerCrimson
              }

              Text(
                text = "Explorer Email Address",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                placeholder = { Text("e.g. sam@ranger.org", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = emailBorderColor,
                  unfocusedBorderColor = emailBorderColor,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isEmailFocused = it.isFocused }
                  .testTag("signup_email_input"),
                singleLine = true
              )

              // SMART REAL-TIME FEEDBACK FOR EMAIL
              if (isEmailFocused && emailInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  color = Color.White.copy(alpha = 0.02f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = "Email Constraints",
                      color = Color.LightGray,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        ValidationChip("Contains '@' symbol", emailContainsAt)
                        ValidationChip("Valid email format structure", emailIsValidFormat)
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        if (emailInput.contains("@gmail.")) {
                          ValidationChip("Gmail must end with '.com'", isGmailcomValid)
                        } else {
                          ValidationChip("Format validation passed", true)
                        }
                      }
                    }

                    if (isEmailLocallyValid) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(1.dp)
                          .background(Color.White.copy(alpha = 0.05f))
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      if (isEmailValidating) {
                        Text("Sifting archive registries...", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      } else if (emailBackendError != null) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Start
                        ) {
                          Text("✗ ", color = DangerCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                          Text(
                            text = "login?",
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(textDecoration = TextDecoration.Underline),
                            modifier = Modifier
                              .clickable {
                                isSignUp = false
                                authErrorMessage = ""
                                showOtpVerifyInput = false
                              }
                              .padding(end = 4.dp)
                          )
                          Text(
                            text = "email is registered. Login",
                            color = DangerCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      } else if (isEmailAvailable) {
                        Text("✓ Email address available!", color = SoftNeonMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // 4. SMART PASSWORD INPUT WITH LIVE FEEDBACK AND STRICT GLOW STATES
              val passwordBorderColor = when {
                passwordInput.isEmpty() -> Color.DarkGray
                isPasswordStrong -> SoftNeonMint
                else -> DangerCrimson
              }

              Text(
                text = "Account Blueprint Passphrase",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                placeholder = { Text("••••••••", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = passwordBorderColor,
                  unfocusedBorderColor = passwordBorderColor,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isPasswordFocused = it.isFocused }
                  .testTag("signup_password_input"),
                singleLine = true
              )

              // SMART PASSWORD RULES FEEDBACK
              if (isPasswordFocused && passwordInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  color = Color.White.copy(alpha = 0.02f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = "Passphrase Strength Blueprint",
                      color = Color.LightGray,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        ValidationChip("Min 6 characters", passLengthOk)
                        ValidationChip("Includes numbers", passHasDigit)
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        ValidationChip("Includes letters", passHasLetter)
                        ValidationChip("Includes symbols", passHasSymbol)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // 5. SMART CONFIRM PASSWORD WITH GLOW FEEDBACK
              val confirmPasswordBorderColor = when {
                confirmPasswordInput.isEmpty() -> Color.DarkGray
                isConfirmPasswordMatch -> SoftNeonMint
                else -> DangerCrimson
              }

              Text(
                text = "Repeat Blueprint Passphrase",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = { confirmPasswordInput = it },
                placeholder = { Text("••••••••", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = confirmPasswordBorderColor,
                  unfocusedBorderColor = confirmPasswordBorderColor,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isConfirmPasswordFocused = it.isFocused }
                  .testTag("signup_confirm_password_input"),
                singleLine = true
              )

              if (isConfirmPasswordFocused && confirmPasswordInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val matchColor = if (isConfirmPasswordMatch) SoftNeonMint else DangerCrimson
                Text(
                  text = if (isConfirmPasswordMatch) "✓ Passphrases match!" else "✗ Passphrases do not match.",
                  color = matchColor,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp)
                )
              }

              if (authErrorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = authErrorMessage,
                  color = DangerCrimson,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Spacer(modifier = Modifier.height(20.dp))

              // ENABLE BUTTON ONLY WHEN VALID - STRICT SPAM/OTP PRESERVATION PROVISIONS
              val isFormValidForRegister = nicknameInput.trim().length >= 2 &&
                  isUsernameLocallyValid &&
                  isUsernameAvailable &&
                  isEmailLocallyValid &&
                  isEmailAvailable &&
                  isPasswordStrong &&
                  isConfirmPasswordMatch

              Button(
                onClick = {
                  if (isLoading) return@Button
                  authErrorMessage = ""

                  val nickTrimmed = nicknameInput.trim()
                  val userTrimmed = usernameInput.trim()
                  val emailTrimmed = emailInput.trim()

                  // Send standard signup details to backend which emits the OTP
                  isLoading = true
                  scope.launch {
                    try {
                      val body = mapOf(
                        "email" to emailTrimmed,
                        "password" to passwordInput,
                        "username" to userTrimmed,
                        "displayName" to nickTrimmed
                      )
                      val response = NetworkService.api.signup(body)
                      if (response.requiresOtp == true) {
                        showOtpVerifyInput = true
                        authErrorMessage = response.message ?: "Verification code emitted successfully!"
                      } else {
                        val token = response.token ?: "mock_token"
                        onAuthenticated(nickTrimmed, userTrimmed, token)
                      }
                    } catch (e: Exception) {
                      authErrorMessage = "Registration sifting failed: " + (e.localizedMessage ?: "Please try again.")
                    } finally {
                      isLoading = false
                    }
                  }
                },
                enabled = isFormValidForRegister && !isLoading,
                colors = ButtonDefaults.buttonColors(
                  containerColor = NeonCyan,
                  contentColor = Color.Black,
                  disabledContainerColor = NeonCyan.copy(alpha = 0.15f),
                  disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("signup_submit_button")
              ) {
                if (isLoading) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "signup_loading")
                    val alpha1 by infiniteTransition.animateFloat(
                      initialValue = 0.2f,
                      targetValue = 1f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "s1"
                    )
                    val alpha2 by infiniteTransition.animateFloat(
                      initialValue = 0.2f,
                      targetValue = 1f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "s2"
                    )
                    val alpha3 by infiniteTransition.animateFloat(
                      initialValue = 0.2f,
                      targetValue = 1f,
                      animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "s3"
                    )
                    Text("Synthesizing Profile DNA  ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = alpha1)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = alpha2)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = alpha3)))
                  }
                } else {
                  Text(
                    text = "Synthesize Explorer Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Already have an account? ",
                  color = Color.White,
                  fontSize = 14.sp
                )
                Text(
                  text = "Login.",
                  color = NeonCyan,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  style = TextStyle(textDecoration = TextDecoration.Underline),
                  modifier = Modifier
                    .clickable {
                      isSignUp = false
                      authErrorMessage = ""
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                )
              }
              }
            }
          }
        }
      }

      // CARD 3: QUANTUM SECURITY NOTICE
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = GlassyCard.copy(alpha = 0.5f),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Verified Status",
              tint = SoftNeonMint,
              modifier = Modifier.size(18.dp)
            )
            Text(
              "Safari Sphere Quantum decentralised core. Authentications, assets, and communications are secured via dual-layer client-server cryptographic encryption protocols.",
              color = Color.Gray,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun ValidationChip(
  label: String,
  isValid: Boolean
) {
  val textColor = if (isValid) SoftNeonMint else Color.Gray
  val icon = if (isValid) "✓" else "•"

  Row(
    modifier = Modifier
      .padding(vertical = 1.dp)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = icon,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      lineHeight = 12.sp
    )
  }
}

// Shimmer skeleton loader helper
@Composable
fun ShimmerPlaceholder(
  modifier: Modifier = Modifier,
  shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
  val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
  val alphaState = transition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.6f,
    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
      animation = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
      repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
    ),
    label = "shimmer_alpha"
  )
  val alpha = alphaState.value

  Box(
    modifier = modifier
      .clip(shape)
      .background(Color.White.copy(alpha = alpha))
  )
}

// ----------------------------------------------------------------------------
// 🌀 MODERN NEON ORBIT PULL-TO-REFRESH COMPONENT
// ----------------------------------------------------------------------------
@Composable
fun ModernPullToRefresh(
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  content: @Composable () -> Unit
) {
  var pullOffset by remember { mutableStateOf(0f) }
  val maxPullOffset = 260f
  val coroutineScope = rememberCoroutineScope()
  
  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(isRefreshing) {
        if (isRefreshing) return@pointerInput
        detectVerticalDragGestures(
          onDragStart = {},
          onDragEnd = {
            if (pullOffset > 140f) {
              onRefresh()
            }
            coroutineScope.launch {
              androidx.compose.animation.core.animate(
                initialValue = pullOffset,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
              ) { value, _ ->
                pullOffset = value
              }
            }
          },
          onDragCancel = {
            coroutineScope.launch {
              androidx.compose.animation.core.animate(
                initialValue = pullOffset,
                targetValue = 0f
              ) { value, _ ->
                pullOffset = value
              }
            }
          },
          onVerticalDrag = { change, dragAmount ->
            change.consume()
            val newOffset = (pullOffset + dragAmount * 0.45f).coerceIn(0f, maxPullOffset)
            pullOffset = newOffset
          }
        )
      }
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          translationY = if (isRefreshing) 110f else pullOffset
        }
    ) {
      content()
    }
     
    if (pullOffset > 8f || isRefreshing) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 16.dp)
          .graphicsLayer {
            translationY = if (isRefreshing) 0f else (pullOffset - 80f).coerceAtLeast(0f)
            alpha = (pullOffset / 90f).coerceIn(0f, 1f)
            scaleX = (pullOffset / 140f).coerceIn(0.8f, 1.15f)
            scaleY = (pullOffset / 140f).coerceIn(0.8f, 1.15f)
          }
          .background(Color(0xFF1E1E28), RoundedCornerShape(28.dp))
          .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
          .shadow(16.dp, RoundedCornerShape(28.dp))
          .padding(horizontal = 20.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val transition = rememberInfiniteTransition(label = "pull_ref")
          val rotationAngle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
              animation = tween(800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart
            ),
            label = "spin"
          )
          
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Sync",
            tint = NeonCyan,
            modifier = Modifier
              .size(20.dp)
              .graphicsLayer { rotationZ = rotationAngle }
          )
          Text(
            text = if (isRefreshing) "Refreshing..." else "Release to refresh",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

// FORMAT JOINED TIME ON PROFILE
// Includes name of the month (letters) and the year, e.g. "June 2026"
fun formatJoinedDateString(joinDate: String?): String {
  if (joinDate.isNullOrEmpty()) return "Joined: Early Alpha"
  return try {
    val cleanDate = if (joinDate.contains("T")) joinDate.split("T")[0] else joinDate
    val parts = cleanDate.split("-")
    if (parts.size >= 2) {
      val year = parts[0]
      val monthNum = parts[1].toIntOrNull() ?: 1
      val monthName = when (monthNum) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "January"
      }
      "Joined: $monthName $year"
    } else {
      "Joined: $joinDate"
    }
  } catch (e: Exception) {
    "Joined: $joinDate"
  }
}

// LOCAL CREDENTIAL STORAGE FOR PASSWORD RECOVERY
fun saveLocalUserCredential(context: Context, handleOrEmail: String, pass: String) {
  val prefs = context.getSharedPreferences("safari_sphere_auth_db", Context.MODE_PRIVATE)
  prefs.edit()
    .putString("pass_${handleOrEmail.trim().lowercase()}", pass)
    .apply()
}

fun getLocalUserCredential(context: Context, handleOrEmail: String): String? {
  val prefs = context.getSharedPreferences("safari_sphere_auth_db", Context.MODE_PRIVATE)
  return prefs.getString("pass_${handleOrEmail.trim().lowercase()}", null)
}

// LOCAL POST PERSISTENCE
fun savePostToLocalStorage(context: Context, post: MobilePost) {
  val prefs = context.getSharedPreferences("safari_sphere_local_db", Context.MODE_PRIVATE)
  val postsJsonStr = prefs.getString("local_posts", "[]") ?: "[]"
  try {
    val array = org.json.JSONArray(postsJsonStr)
    val obj = org.json.JSONObject()
    obj.put("id", post.id)
    obj.put("author", post.author)
    obj.put("username", post.username)
    obj.put("avatarUrl", post.avatarUrl)
    obj.put("content", post.content)
    obj.put("vibeCategory", post.vibeCategory)
    obj.put("likes", post.likes)
    obj.put("hasLiked", post.hasLiked)
    obj.put("created", post.created)
    obj.put("mediaUrl", post.mediaUrl ?: "")
    obj.put("mediaType", post.mediaType)
    obj.put("commentsCount", post.commentsCount)
    
    val newArray = org.json.JSONArray()
    newArray.put(obj)
    for (i in 0 until array.length()) {
      newArray.put(array.getJSONObject(i))
    }
    prefs.edit().putString("local_posts", newArray.toString()).apply()
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

fun saveAllPostsToLocalStorage(context: Context, posts: List<MobilePost>) {
  val prefs = context.getSharedPreferences("safari_sphere_local_db", Context.MODE_PRIVATE)
  try {
    val newArray = org.json.JSONArray()
    posts.take(50).forEach { post ->
      val obj = org.json.JSONObject()
      obj.put("id", post.id)
      obj.put("author", post.author)
      obj.put("username", post.username)
      obj.put("avatarUrl", post.avatarUrl)
      obj.put("content", post.content)
      obj.put("vibeCategory", post.vibeCategory)
      obj.put("likes", post.likes)
      obj.put("hasLiked", post.hasLiked)
      obj.put("created", post.created)
      obj.put("mediaUrl", post.mediaUrl ?: "")
      obj.put("mediaType", post.mediaType)
      obj.put("commentsCount", post.commentsCount)
      newArray.put(obj)
    }
    prefs.edit().putString("local_posts", newArray.toString()).apply()
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

fun getLocalStoredPosts(context: Context): List<MobilePost> {
  val prefs = context.getSharedPreferences("safari_sphere_local_db", Context.MODE_PRIVATE)
  val postsJsonStr = prefs.getString("local_posts", "[]") ?: "[]"
  val list = mutableListOf<MobilePost>()
  try {
    val array = org.json.JSONArray(postsJsonStr)
    for (i in 0 until array.length()) {
      val obj = array.getJSONObject(i)
      list.add(
        MobilePost(
          id = obj.getString("id"),
          author = obj.getString("author"),
          username = obj.getString("username"),
          avatarUrl = obj.getString("avatarUrl"),
          content = obj.getString("content"),
          vibeCategory = obj.getString("vibeCategory"),
          likes = obj.getInt("likes"),
          hasLiked = obj.getBoolean("hasLiked"),
          created = obj.getString("created"),
          mediaUrl = obj.optString("mediaUrl", "").takeIf { it.isNotEmpty() },
          mediaType = obj.getString("mediaType"),
          commentsCount = obj.getInt("commentsCount")
        )
      )
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return list
}

