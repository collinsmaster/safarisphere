package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun DiscoverTab(
  communities: List<MobileCommunity>,
  explorers: List<BackendExplorer>,
  posts: List<MobilePost>,
  onJoinCommunity: (MobileCommunity) -> Unit,
  onInitiateDM: (BackendExplorer) -> Unit,
  onCommentClicked: (MobilePost) -> Unit,
  onExplorerClicked: (id: String, username: String, displayName: String?, avatarUrl: String?) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }

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
                  .clickable { onExplorerClicked(exp.id, exp.username, exp.displayName, exp.avatarUrl) },
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
}
