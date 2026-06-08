package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import com.example.formatJoinedDateString
import com.example.parsePostMetadata

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserProfileViewer(
  userId: String,
  username: String,
  displayName: String?,
  avatarUrl: String?,
  posts: List<MobilePost>,
  onClose: () -> Unit,
  onStartDirectMessage: (id: String, name: String) -> Unit
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("safari_user_relationships", android.content.Context.MODE_PRIVATE) }
  
  // Follow State Management
  var isFollowing by remember(userId) { 
    mutableStateOf(prefs.getBoolean("follow_state_$userId", false)) 
  }
  var followersCount by remember(userId) {
    mutableStateOf(if (isFollowing) 124 else 123)
  }

  // Simulated User Profile Metadata
  val simulatedBio = remember(userId) {
    when (userId) {
      "companion" -> "Primary intelligence companion. Listening to Savannah frequencies and analyzing ecosystem metrics."
      "explorer_1" -> "Avid field biochemist tracking savanna plant species with drone-tech imagery."
      "explorer_2" -> "Astro photographer and signal analyst. Catch me camping under the nebula loops."
      else -> "De-centralized Pioneer at Safari Sphere. Vibing on custom acoustic tones."
    }
  }
  val simulatedLocation = remember(userId) {
    when (userId) {
      "companion" -> "Deep Core Server"
      "explorer_1" -> "Expedition Sector C"
      "explorer_2" -> "Savannah Base Shores"
      else -> "Sector Orbit Alpha"
    }
  }
  val simulatedJoined = remember(userId) { "2026-03-12" }
  val simulatedHeadline = remember(userId) { "🛰️ Hyperwave Signal Active" }
  val simulatedInterests = remember(userId) { "Nature, Space, Audio synthesis" }

  val myPosts = remember(posts, username) {
    posts.filter { it.username.equals(username, ignoreCase = true) || it.author.equals(displayName, ignoreCase = true) }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.85f))
      .clickable { onClose() }
      .padding(top = 40.dp)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .clickable(enabled = false) {}, // Prevent closing when tapping index bounds
      color = Color(0xFF0F0F14),
      shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Sticky Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close profile viewer", tint = Color.White)
          }
          Text(
            text = "Pioneer Coordinates",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          // Initiate whisper DM shortcut
          IconButton(
            onClick = {
              onStartDirectMessage(userId, displayName ?: username)
              onClose()
            },
            modifier = Modifier
              .clip(CircleShape)
              .background(NeonCyan.copy(alpha = 0.15f))
              .size(40.dp)
          ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Direct Whisper Command", tint = NeonCyan)
          }
        }

        // Profile details scroller
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Immersive Bento style Profile Card
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(24.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                // Colored status indicator ring
                Box(
                  modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                      Brush.linearGradient(
                        colors = if (isFollowing) listOf(NeonCyan, SoftNeonMint) else listOf(Color.DarkGray, Color.Gray)
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
                        contentDescription = "Pioneer avatar preview",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                      )
                    } else {
                      Text("👤", fontSize = 36.sp)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = displayName ?: username,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp
                )
                Text(
                  text = "@$username",
                  color = Color.Gray,
                  fontSize = 12.sp
                )

                // Inline headline
                Surface(
                  color = NeonCyan.copy(alpha = 0.06f),
                  border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                  Text(
                    text = simulatedHeadline,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }

                // Modern follow/unfollow button
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = {
                    val newState = !isFollowing
                    isFollowing = newState
                    prefs.edit().putBoolean("follow_state_$userId", newState).apply()
                    followersCount = if (newState) 124 else 123
                    
                    val toastMessage = if (newState) {
                      "Now following @$username!"
                    } else {
                      "Unfollowed @$username."
                    }
                    android.widget.Toast.makeText(context, toastMessage, android.widget.Toast.LENGTH_SHORT).show()
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) SoftNeonMint else NeonCyan
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(38.dp)
                    .testTag("follow_pioneer_button")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                      contentDescription = "Follow sign",
                      tint = Color.Black,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (isFollowing) "Following Pioneer" else "Follow Pioneer",
                      color = Color.Black,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                // Follower counts
                Row(
                  modifier = Modifier.padding(top = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$followersCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Followers", color = Color.Gray, fontSize = 10.sp)
                  }
                  Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.1f)))
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("98", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Following", color = Color.Gray, fontSize = 10.sp)
                  }
                }
              }
            }
          }

          // Description & Bio Cards
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = GlassyCard,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  "Pioneer Bio",
                  color = NeonCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                  text = simulatedBio,
                  color = Color.LightGray,
                  fontSize = 12.sp,
                  lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Place, contentDescription = "Place", tint = Color.Gray, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(simulatedLocation, color = Color.Gray, fontSize = 11.sp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Icon(imageVector = Icons.Default.DateRange, contentDescription = "Joined Date", tint = Color.Gray, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(formatJoinedDateString(simulatedJoined), color = Color.Gray, fontSize = 11.sp)
                }

                // Interests split
                val tags = simulatedInterests.split(",").map { it.trim() }
                Row(
                  modifier = Modifier.padding(top = 10.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  tags.forEach { tg ->
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BentoIndigo.copy(alpha = 0.15f))
                        .border(1.dp, SoftNeonMint.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                      Text(tg, color = SoftNeonMint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                  }
                }
              }
            }
          }

          // Active transmissions title
          item {
            Text(
              "Recent Transmissions (${myPosts.size})",
              color = Color.LightGray,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Transmissions List
          if (myPosts.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text("No recent posts made by this pioneer.", color = Color.Gray, fontSize = 11.sp)
              }
            }
          } else {
            items(myPosts) { post ->
              val (cleanContent, metadata) = remember(post.content) { parsePostMetadata(post.content) }
              Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GlassyCard.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                    ) {
                      if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(model = avatarUrl, contentDescription = "pfp", modifier = Modifier.fillMaxSize().clip(CircleShape))
                      } else {
                        Text("👤", fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                      }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(displayName ?: username, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      Text(post.created, color = Color.Gray, fontSize = 8.sp)
                    }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(cleanContent, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}
