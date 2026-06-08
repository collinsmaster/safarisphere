package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
  posts: List<MobilePost>,
  onMoodSelected: (String, String) -> Unit,
  onProfileUpdated: (nickname: String, bio: String, location: String, avatarUrl: String, handle: String, email: String, headline: String, interests: String) -> Unit,
  onLogOut: () -> Unit,
  forceShowSettings: Boolean = false,
  onDismissSettings: () -> Unit = {},
  isFetchingProfile: Boolean = false
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
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

  var selectedProfileTab by remember { mutableStateOf(0) } // 0 = Broadcasts, 1 = Stats & Achievements, 2 = Space Settings
  var selectedThemeMode by remember { mutableStateOf(prefs.getString("selected_orbit_theme", "Midnight Slate") ?: "Midnight Slate") }
  var beaconNotifsEnabled by remember { mutableStateOf(prefs.getBoolean("beacon_notifs_enabled", true)) }
  var frequencyChimesEnabled by remember { mutableStateOf(prefs.getBoolean("frequency_chimes_enabled", true)) }
  var cacheValue by remember { mutableStateOf("24.8 MB") }
  var isOptimizingCache by remember { mutableStateOf(false) }
  var optimizationStatusText by remember { mutableStateOf("") }
  val myPosts = remember(posts, handle) { posts.filter { it.username == handle } }

  var stealthModeEnabled by remember { mutableStateOf(prefs.getBoolean("stealth_mode_enabled", false)) }
  var frequencyBand by remember { mutableStateOf(prefs.getString("frequency_band", "5.8 GHz Hyperwave") ?: "5.8 GHz Hyperwave") }

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
    } else {
      // --- STREAMLINED BEAUTIFULLY ORGANIZED PROFILE HERO CARD ---
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
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Glowing dynamic avatar ring
            Box(
              modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(NeonCyan, BentoIndigo, SoftNeonMint)
                  )
                )
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = nickname,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            )
            Text(
              text = "@$handle",
              color = Color.Gray,
              fontSize = 13.sp
            )

            if (headline.isNotEmpty()) {
              Surface(
                color = NeonCyan.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).testTag("profile_headline_badge")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("📡", fontSize = 10.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = headline,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                  )
                }
              }
            }

            Text(
              text = bio.ifBlank { "Setting up communication matrix..." },
              color = Color.LightGray,
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 10.dp)
            ) {
              Icon(imageVector = Icons.Default.Place, contentDescription = "Place", tint = Color.Gray, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(location.ifBlank { "Deep Space Outer Edge" }, color = Color.Gray, fontSize = 12.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date", tint = Color.Gray, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(formatJoinedDateString(joinDate), color = Color.Gray, fontSize = 12.sp)
            }

            // Follower counts (Clean structured layout)
            Row(
              modifier = Modifier.padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("256", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Followers", color = Color.Gray, fontSize = 11.sp)
              }
              Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.1f)))
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("148", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Following", color = Color.Gray, fontSize = 11.sp)
              }
            }

            // Dynamic Tag Scroll view for interests
            val tags = remember(interests) {
              if (interests.trim().isEmpty()) emptyList<String>()
              else interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (tags.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
              ) {
                tags.forEach { tg ->
                  Surface(
                    color = BentoIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SoftNeonMint.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 3.dp)
                  ) {
                    Text(
                      text = tg,
                      color = SoftNeonMint,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inline interactive profile configs button row
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Mood Status Selector
              Surface(
                color = NeonCyan.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { showSelectMoodDialog = true }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(moodEmoji, fontSize = 12.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Status: $mood", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              // Edit details profile trigger button
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
                  containerColor = Color.White.copy(alpha = 0.06f),
                  contentColor = NeonCyan
                ),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(12.dp), tint = NeonCyan)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // HIGH-END COMPACT SEGMENTED NAVIGATION CONTROLS
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color.White.copy(alpha = 0.02f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            val tabsData = listOf(
              Triple(0, "📡 Transmissions", "broadcasts_tab"),
              Triple(1, "📈 Progression", "progression_tab"),
              Triple(2, "⚙️ Control Settings", "settings_tab")
            )
            tabsData.forEach { (tabId, tabName, testTag) ->
              val active = selectedProfileTab == tabId
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedProfileTab = tabId }
                  .testTag(testTag),
                color = if (active) NeonCyan.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (active) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = tabName,
                    color = if (active) NeonCyan else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      // SEGMENTED TAB DETAILS DISPLAY
      when (selectedProfileTab) {
        0 -> {
          // TAB 0: PERSONAL BROADCASTS
          if (myPosts.isEmpty()) {
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.01f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("📡", fontSize = 28.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("No transmissions active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  Text("Beam coordinates, astro snapshots or chimes from the feed tab!", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
              }
            }
          } else {
            items(myPosts) { post ->
              val (cleanContent, metadata) = remember(post.content) { parsePostMetadata(post.content) }
              Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GlassyCard.copy(alpha = 0.4f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                      contentAlignment = Alignment.Center
                    ) {
                      if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(model = avatarUrl, contentDescription = "avatar", modifier = Modifier.fillMaxSize().clip(CircleShape))
                      } else {
                        Text(moodEmoji, fontSize = 14.sp)
                      }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(nickname, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      Text(post.created, color = Color.Gray, fontSize = 9.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                      text = post.vibeCategory,
                      color = NeonCyan,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier
                        .background(NeonCyan.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(cleanContent, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                }
              }
            }
          }
        }
        1 -> {
          // TAB 1: PROGRESSION & STATS
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Star, contentDescription = "XP", tint = NeonCyan, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Orbital Vibe Level Indicator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Spacer(modifier = Modifier.weight(1f))
                  Text("Pioneer Rank #12", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Aggregated XP Gained:", color = Color.Gray, fontSize = 12.sp)
                  Text("$xp XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                  progress = (xp % 100) / 100f,
                  color = NeonCyan,
                  trackColor = Color.White.copy(alpha = 0.05f),
                  modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = "${100 - (xp % 100)} XP needed to escalate frequency levels",
                  color = Color.Gray,
                  fontSize = 10.sp,
                  textAlign = TextAlign.End,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }

          item {
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Verified Milestones", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

              Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GlassyCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text("Savannah Alpha Pathfinder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      Text("Initiated signal beacon on early trial phase", color = Color.Gray, fontSize = 10.sp)
                    }
                  }
                  HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎙️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text("Atmosphere Harmonizer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      Text("Disseminated high-vibe transmissions into the feed", color = Color.Gray, fontSize = 10.sp)
                    }
                  }
                }
              }
            }
          }
        }
        2 -> {
          // --- TAB 2: EXTREMELY MODERN CONTROL SETTINGS PAGE ---

          // SECTION 1: SYSTEM STYLING & CORE IDENTITY
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  "Cosmic Base Themes",
                  color = NeonCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  val modes = listOf("Obsidian Glow", "Cyber Horizontal", "Solar Savannah", "Bento Drift")
                  modes.forEach { md ->
                    val sel = selectedThemeMode == md
                    Surface(
                      modifier = Modifier.clickable {
                        selectedThemeMode = md
                        prefs.edit().putString("selected_orbit_theme", md).apply()
                        android.widget.Toast.makeText(context, "Theme balanced to: $md!", android.widget.Toast.LENGTH_SHORT).show()
                      },
                      shape = RoundedCornerShape(8.dp),
                      color = if (sel) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                      border = BorderStroke(1.dp, if (sel) NeonCyan else Color.White.copy(alpha = 0.1f))
                    ) {
                      Text(
                        text = md,
                        color = if (sel) NeonCyan else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          // SECTION 2: SIGNAL ENCRYPTION & QUANTUM BANDS (NEW HIGH-FIDELITY FEATURES!)
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                  "Signal Protocols",
                  color = NeonCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )

                // Stealth signal mode toggle
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Quantum Stealth Cloak", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Masks real-time coordinate broadcasts behind secure proxy blocks", color = Color.Gray, fontSize = 9.sp)
                  }

                  Switch(
                    checked = stealthModeEnabled,
                    onCheckedChange = {
                      stealthModeEnabled = it
                      prefs.edit().putBoolean("stealth_mode_enabled", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.Black,
                      checkedTrackColor = SoftNeonMint,
                      uncheckedThumbColor = Color.Gray,
                      uncheckedTrackColor = Color.White.copy(alpha = 0.05f)
                    )
                  )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.04f), thickness = 1.dp)

                // Quantum Communication frequency sector selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Quantum Frequency Tuning Band", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text("Calibrate current companion whisper response wavelengths", color = Color.Gray, fontSize = 9.sp)

                  Spacer(modifier = Modifier.height(4.dp))

                  val bands = listOf("2.4 GHz Quantum Flare", "5.8 GHz Hyperwave", "8.0 GHz Astro Link")
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    bands.forEach { bnd ->
                      val matched = frequencyBand == bnd
                      Surface(
                        modifier = Modifier
                          .weight(1f)
                          .clickable {
                            frequencyBand = bnd
                            prefs.edit().putString("frequency_band", bnd).apply()
                          },
                        shape = RoundedCornerShape(8.dp),
                        color = if (matched) BentoIndigo.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, if (matched) NeonCyan else Color.White.copy(alpha = 0.08f))
                      ) {
                        Text(
                          text = bnd.split(" ")[0] + " " + bnd.split(" ")[1],
                          textAlign = TextAlign.Center,
                          color = if (matched) NeonCyan else Color.Gray,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(vertical = 8.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // SECTION 3: IMMERSIVE TELEMETRY DIAGNOSTIC CORE
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  "Transceiver Telemetry Diagnostics",
                  color = NeonCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Signal strength: Strong (100%) 📡", color = Color.LightGray, fontSize = 11.sp)
                    Text("Connection Ping latency: 26ms", color = Color.LightGray, fontSize = 11.sp)
                    Text("Security Level: AstroCrypt v4.1", color = SoftNeonMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }

                  // Small circular meter diagram drawing
                  Box(
                    modifier = Modifier
                      .size(52.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.04f))
                      .border(1.dp, SoftNeonMint.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text("COMP", color = SoftNeonMint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                      Text("OK ✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          // SECTION 4: STORAGE OPTIMIZATION CORE
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  "Storage Telemetry Purge",
                  color = NeonCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("Disposable database blocks:", color = Color.Gray, fontSize = 10.sp)
                    Text(cacheValue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  }

                  if (isOptimizingCache) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                      Text(optimizationStatusText, color = Color.Gray, fontSize = 10.sp)
                    }
                  } else {
                    Button(
                      onClick = {
                        scope.launch {
                          isOptimizingCache = true
                          optimizationStatusText = "Sifting..."
                          delay(500)
                          optimizationStatusText = "Defragmenting..."
                          delay(400)
                          cacheValue = "0.0 KB"
                          isOptimizingCache = false
                          android.widget.Toast.makeText(context, "Orbital database defragmentation successful!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.height(32.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                      Text("Full Vacuum Purge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          // SECTION 5: REAL-TIME NOTIFICATIONS SWITCHES
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                  "Relay Alert Configurations",
                  color = NeonCyan,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Orbit Beacon Pings", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Receive notification on new incoming peer feeds", color = Color.Gray, fontSize = 9.sp)
                  }

                  Switch(
                    checked = beaconNotifsEnabled,
                    onCheckedChange = {
                      beaconNotifsEnabled = it
                      prefs.edit().putBoolean("beacon_notifs_enabled", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.Black,
                      checkedTrackColor = NeonCyan,
                      uncheckedThumbColor = Color.Gray,
                      uncheckedTrackColor = Color.White.copy(alpha = 0.05f)
                    )
                  )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.04f), thickness = 1.dp)

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Auditory Chime Vibrations", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Chimes acoustic frequencies when peer chat whisper lands", color = Color.Gray, fontSize = 9.sp)
                  }

                  Switch(
                    checked = frequencyChimesEnabled,
                    onCheckedChange = {
                      frequencyChimesEnabled = it
                      prefs.edit().putBoolean("frequency_chimes_enabled", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.Black,
                      checkedTrackColor = NeonCyan,
                      uncheckedThumbColor = Color.Gray,
                      uncheckedTrackColor = Color.White.copy(alpha = 0.05f)
                    )
                  )
                }
              }
            }
          }
        }
      }

      // INTEGRATE SECURE DEREGISTER EXCURSION BUTTON AT THE BOTTOM
      item {
        Button(
          onClick = onLogOut,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            contentColor = Color(0xFFE71D36)
          ),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, Color(0xFFE71D36).copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth().testTag("logout_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Log out", tint = Color(0xFFE71D36), modifier = Modifier.size(16.dp))
            Text("Deregister Signal & Quit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
  var isSavingProfile by remember { mutableStateOf(false) }
  var profileEditError by remember { mutableStateOf("") }
  var editOtpRequiredMode by remember { mutableStateOf(false) }
  var editOtpInput by remember { mutableStateOf("") }

  var isEditOtpChecking by remember { mutableStateOf(false) }
  var isEditOtpWrong by remember { mutableStateOf(false) }

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
                          fontSize = 11.sp,
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
