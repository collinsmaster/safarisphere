package com.example

import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
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
import coil.compose.AsyncImage

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

// Data models reflecting backend entities
data class MobilePost(
  val id: String,
  val author: String,
  val username: String,
  val avatarUrl: String,
  val content: String,
  val vibeCategory: String,
  var likes: Int,
  var hasLiked: Boolean = false,
  val created: String
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

// ==============================================================================
// 📱 MAIN APPLICATION LAYOUT
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafariSphereApp() {
  var selectedTab by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()

  val context = LocalContext.current
  val prefs = remember(context) { context.getSharedPreferences("safari_sphere_prefs", Context.MODE_PRIVATE) }
  var isAuthenticated by remember { mutableStateOf(prefs.getBoolean("is_authenticated", false)) }
  var userNickname by remember { mutableStateOf(prefs.getString("user_nickname", "Pioneer Prime") ?: "Pioneer Prime") }
  var userHandle by remember { mutableStateOf(prefs.getString("user_handle", "explorer_prime") ?: "explorer_prime") }
  var userBio by remember { mutableStateOf(prefs.getString("user_bio", "A fresh pioneer in Safari Sphere!") ?: "A fresh pioneer in Safari Sphere!") }
  var userLocation by remember { mutableStateOf(prefs.getString("user_location", "Savannah Valley") ?: "Savannah Valley") }
  var userAvatarUrl by remember { mutableStateOf(prefs.getString("user_avatar_url", null)) }
  var userJoinDate by remember { mutableStateOf(prefs.getString("user_join_date", null)) }
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
    mutableStateListOf(
      MobilePost("p1", "Safari Explorer", "explorer_prime", "", "Spotted a family of cheetahs resting beneath a great baobab tree today. The wild cosmic energy is unmatched! 🐆 #savannah", "Adventurer", 45, false, "3m ago"),
      MobilePost("p2", "Sienna Dusk", "sienna_d", "", "Listening to celestial tribal drums loop live in Sector-B Vybe Room. Ambient synth magic. 🥁🌾", "Acoustics", 128, true, "15m ago"),
      MobilePost("p3", "Nova Quest", "quest_nova", "", "Completed the 'Savannah Pioneer' digital achievement! Earned 500 XP points. Next stop is the daily streak leaderboards! 🏆✨", "Gamification", 19, false, "32m ago")
    )
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
          
          prefs.edit()
            .putString("user_nickname", userNickname)
            .putString("user_handle", userHandle)
            .putString("user_bio", userBio)
            .putString("user_location", userLocation)
            .putString("user_avatar_url", userAvatarUrl)
            .putString("user_join_date", userJoinDate)
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
      try {
        val backendPosts = NetworkService.api.getPosts()
        if (backendPosts.isNotEmpty()) {
          postsList.clear()
          backendPosts.forEach { bp ->
            postsList.add(
              MobilePost(
                id = bp.id,
                author = bp.displayName ?: bp.username ?: "Anonymous Pioneer",
                username = bp.username ?: "anonymous_user",
                avatarUrl = bp.avatarUrl ?: "",
                content = bp.content,
                vibeCategory = bp.vibeCategory ?: "Adventurer",
                likes = bp.likesCount ?: 0,
                hasLiked = false,
                created = "Just now"
              )
            )
          }
        }
      } catch (e: Exception) {
        // Fallback or quiet keep existing
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
    }
  }

  LaunchedEffect(selectedTab) {
    if (selectedTab == 2) {
      loadExplorers()
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
    modifier = Modifier.fillMaxSize(),
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
          IconButton(onClick = { /* Simulated Settings */ }) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
          }
        }
      )
    },
    floatingActionButton = {
      if (selectedTab == 0) {
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
          Triple("Vybe Rooms", Icons.Default.Star, 1),
          Triple("Safari Chat", Icons.Default.Face, 2),
          Triple("Discover", Icons.Default.Search, 3),
          Triple("Me", Icons.Default.Person, 4)
        )

        navItems.forEach { (label, icon, index) ->
          val isSelected = selectedTab == index
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = index },
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
      // Swipeable or crossfade selection representing selected tabs
      Crossfade(targetState = selectedTab, label = "tab_navigation") { tab ->
        when (tab) {
          0 -> FeedTab(
            moments = momentsList,
            posts = postsList,
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
            }
          )
          1 -> RoomsTab(
            rooms = roomsList,
            onJoinRoom = { room ->
              val index = roomsList.indexOf(room)
              if (index != -1) {
                roomsList[index] = room.copy(membersCount = room.membersCount + 1)
                updateUserXp(25)
              }
            }
          )
          2 -> AICompanionTab(
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
          3 -> DiscoverTab(
            communities = communitiesList,
            explorers = explorersList,
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
            }
          )
          4 -> IdentityTab(
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
            onMoodSelected = { text, emo ->
              updateMood(text, emo)
            },
            onProfileUpdated = { newName, newBio, newLoc ->
              userNickname = newName
              userBio = newBio
              userLocation = newLoc
              prefs.edit()
                .putString("user_nickname", newName)
                .putString("user_bio", newBio)
                .putString("user_location", newLoc)
                .apply()
              scope.launch {
                try {
                  NetworkService.api.editProfile(mapOf(
                    "displayName" to newName,
                    "bio" to newBio,
                    "locationLabel" to newLoc
                  ))
                } catch (e: Exception) {
                  // Fallback quietly
                }
              }
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
              isAuthenticated = false
              selectedTab = 0
            }
          )
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

              Spacer(modifier = Modifier.height(20.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = {
                    // Quick SphereMate AI auto-caption recommendation helper based on active category
                    newPostContent = when (activeCategory) {
                      "Adventurer" -> "Stalking digital gold and neon sunsets on this cyber-expedition! 🐆🌾 #wilderness #adventure"
                      "Acoustics" -> "Vibing to the rhythmic tribal frequencies of midnight lo-fi synths. 🥁🎹 #synth #acoustic"
                      "Dreamer" -> "Floating in celestial digital clouds, dreaming of the solar savanna. ☁️☄️ #dreamer #neon"
                      else -> "Exploring the neon sun horizon under a clear $activeCategory vibe. 🌅✨ #wilderness"
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
                    if (newPostContent.isNotBlank()) {
                      val contentToSend = newPostContent
                      val categoryToSend = activeCategory
                      newPostContent = ""
                      showNewPostSheet = false

                      scope.launch {
                        try {
                          NetworkService.api.createPost(
                            CreatePostRequest(content = contentToSend, vibeCategory = categoryToSend)
                          )
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
                            created = "Just now"
                          )
                          postsList.add(0, p)
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
  onLikeClicked: (MobilePost) -> Unit,
  onMomentClicked: (MobileMoment) -> Unit
) {
  LazyColumn(
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
    items(posts) { post ->
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
            post.content,
            color = Color(0xFFECEFF1),
            fontSize = 14.sp,
            lineHeight = 20.sp
          )

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

            // Comments mockup
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Comments",
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("5", color = Color(0xFF90A4AE), fontSize = 12.sp)
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
                Text("Join Vybe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
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
  onJoinCommunity: (MobileCommunity) -> Unit,
  onInitiateDM: (BackendExplorer) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedExplorerForProfile by remember { mutableStateOf<BackendExplorer?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Large Header Search bar textfields
    item {
      Column {
        Text(
          "Discover the Cosmos",
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White
        )
        Text(
          "Uncover trending communities and digital landscapes of Safari Sphere",
          fontSize = 12.sp,
          color = Color.Gray,
          modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search hashtags, pioneers, communities...", color = Color.Gray) },
          leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon", tint = Color.Gray) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier.fillMaxWidth()
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
    items(communities) { comm ->
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
  onMoodSelected: (String, String) -> Unit,
  onProfileUpdated: (nickname: String, bio: String, location: String) -> Unit,
  onLogOut: () -> Unit
) {
  var showSelectMoodDialog by remember { mutableStateOf(false) }
  var showEditProfileDialog by remember { mutableStateOf(false) }

  var editNicknameInput by remember { mutableStateOf(nickname) }
  var editBioInput by remember { mutableStateOf(bio) }
  var editLocationInput by remember { mutableStateOf(location) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
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
              text = if (!joinDate.isNullOrEmpty()) {
                val formattedDate = try {
                  if (joinDate.contains("T")) {
                    joinDate.split("T")[0]
                  } else {
                    joinDate
                  }
                } catch (e: Exception) {
                  joinDate
                }
                "Joined: $formattedDate"
              } else {
                "Joined: Early Alpha"
              },
              color = Color.Gray,
              fontSize = 11.sp
            )
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
                editNicknameInput = nickname
                editBioInput = bio
                editLocationInput = location
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
  if (showEditProfileDialog) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.7f))
        .clickable { showEditProfileDialog = false },
      contentAlignment = Alignment.Center
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.9f)
          .wrapContentHeight()
          .clickable(enabled = false) {},
        color = GlassyCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            "Adapt Grid Identity Synthesis",
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 6.dp)
          )

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

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { showEditProfileDialog = false },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.05f),
                contentColor = Color.Gray
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                onProfileUpdated(editNicknameInput, editBioInput, editLocationInput)
                showEditProfileDialog = false
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Save & Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
      .padding(16.dp),
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
                  text = if (isSignUp) "Synthesize Profile" else "Safari Gateway",
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 20.sp,
                  letterSpacing = (-0.5).sp
                )
                Text(
                  text = if (isSignUp) "Configure secure credentials for Pioneer." else "Sign in to enter the social sphere.",
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

            if (!isSignUp) {
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
                  CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                } else {
                  Text("Verify Credentials & Enter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

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
                        Text("✗ Registered: $emailBackendError", color = DangerCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                  .testTag("signup_confirm_password_input"),
                singleLine = true
              )

              if (confirmPasswordInput.isNotEmpty()) {
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

              // OTP METHOD (Only showing after standard signup accepted by server)
              if (showOtpVerifyInput) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                  text = "6-Digit Secure Verification OTP",
                  color = NeonCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                  value = otpInput,
                  onValueChange = { otpInput = it },
                  placeholder = { Text("e.g. 123456", color = Color.Gray, fontSize = 13.sp) },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_otp_input"),
                  singleLine = true
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

                  if (showOtpVerifyInput) {
                    if (otpInput.trim().length != 6) {
                      authErrorMessage = "Proof of code must be exactly 6 digits."
                      return@Button
                    }
                    isLoading = true
                    scope.launch {
                      try {
                        val body = mutableMapOf(
                          "email" to emailTrimmed,
                          "password" to passwordInput,
                          "username" to userTrimmed,
                          "displayName" to nickTrimmed,
                          "otp" to otpInput.trim()
                        )
                        val response = NetworkService.api.signup(body)
                        val token = response.token ?: "mock_token"
                        onAuthenticated(nickTrimmed, userTrimmed, token)
                        android.widget.Toast.makeText(context, "Registration Approved! Welcome.", android.widget.Toast.LENGTH_SHORT).show()
                      } catch (e: Exception) {
                        authErrorMessage = "OTP Verification Failed: " + (e.localizedMessage ?: "Invalid verification code.")
                      } finally {
                        isLoading = false
                      }
                    }
                  } else {
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
                  }
                },
                enabled = if (showOtpVerifyInput) otpInput.trim().length == 6 else isFormValidForRegister,
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
                  CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                } else {
                  Text(
                    text = if (showOtpVerifyInput) "Submit Verification Code" else "Synthesize Explorer Profile",
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
