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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun FeedTab(
  moments: List<MobileMoment>,
  posts: List<MobilePost>,
  isFetchingPosts: Boolean = false,
  onScrollDirectionChanged: (Boolean) -> Unit,
  onLikeClicked: (MobilePost) -> Unit,
  onMomentClicked: (MobileMoment) -> Unit,
  onCommentClicked: (MobilePost) -> Unit,
  onExplorerClicked: (id: String, username: String, displayName: String?, avatarUrl: String?) -> Unit
) {
  val listState = rememberLazyListState()

  val activeVideoPostId = remember(listState) {
    derivedStateOf {
      val visibleItems = listState.layoutInfo.visibleItemsInfo
      if (visibleItems.isEmpty()) null
      else {
        val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
        val closestItem = visibleItems.minByOrNull { item ->
          val itemCenter = (item.offset + item.size / 2)
          kotlin.math.abs(itemCenter - viewportCenter)
        }
        closestItem?.let { item ->
          val postIndex = item.index - 4
          if (postIndex >= 0 && postIndex < posts.size) {
            posts[postIndex].id
          } else null
        }
      }
    }
  }

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
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onExplorerClicked("exp_" + post.username, post.username, post.author, post.avatarUrl)
                }
            ) {
              ExplorerAvatar(
                avatarUrl = post.avatarUrl,
                name = post.author,
                size = 42.dp,
                borderColor = NeonCyan.copy(alpha = 0.25f)
              )

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
                val isActivated = activeVideoPostId.value == post.id
                Box(
                  modifier = mediaModifier.background(Color.Black),
                  contentAlignment = Alignment.Center
                ) {
                  VideoPlayerView(
                    mediaUrl = post.mediaUrl,
                    isActivated = isActivated,
                    modifier = Modifier.fillMaxSize()
                  )

                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .border(
                        width = if (isActivated) 2.dp else 0.dp,
                        color = if (isActivated) NeonCyan.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                      )
                  )

                  Surface(
                    modifier = Modifier
                      .align(Alignment.TopStart)
                      .padding(12.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(6.dp)
                          .clip(CircleShape)
                          .background(if (isActivated) SoftNeonMint else Color.Gray)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (isActivated) "ACTIVE PLAYBACK 🔇" else "SCROLL TO CHIME 💡",
                        color = if (isActivated) NeonCyan else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              } else {
                // Image card load via AsyncImage with dynamic aspect ratio, rotation, and filter mapping
                AsyncImage(
                  model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(post.mediaUrl)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
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
              // Removed because reposts are not currently implemented
              // Row(verticalAlignment = Alignment.CenterVertically) {
              //   Icon(
              //     imageVector = Icons.Default.Share,
              //     contentDescription = "Repost",
              //     tint = Color(0xFF90A4AE),
              //     modifier = Modifier.size(18.dp).clickable { /* Repst simulation */ }
              //   )
              //   Spacer(modifier = Modifier.width(6.dp))
              //   Text("3", color = Color(0xFF90A4AE), fontSize = 12.sp)
              // }

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
