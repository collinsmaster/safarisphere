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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

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
  onBackToChatList: () -> Unit,
  onExplorerClicked: (id: String, username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _, _ -> }
) {
  // Modern chat-app states
  val messageReactions = remember { mutableStateMapOf<String, String>() }
  val activeReactionMenuId = remember { mutableStateOf<String?>(null) }
  val replyingToMessage = remember { mutableStateOf<BackendMessage?>(null) }
  var showAttachMenu by remember { mutableStateOf(false) }
  var chatSearchQuery by remember { mutableStateOf("") }
  var isSearchActive by remember { mutableStateOf(false) }

  // Simulated live typing feedback
  val isCompanionTyping = remember { mutableStateOf(false) }
  LaunchedEffect(activeChatId) {
    if (activeChatId != null) {
      isCompanionTyping.value = true
      delay(1500)
      isCompanionTyping.value = false
    }
  }

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
      val activeChat = chats.find { it.id == activeChatId }
      val chatName = activeChat?.peerName ?: "Secure Whisper Channel"

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
            .size(48.dp)
        ) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = chatName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.clickable {
              if (activeChat != null) {
                onExplorerClicked(activeChat.peerId ?: "", activeChat.peerName ?: "", activeChat.peerName, null)
              }
            }
          )
          Text("Quantum encrypted tunnel • Live", color = SoftNeonMint, fontSize = 11.sp)
        }

        // Search in Chat Toggle Button
        IconButton(
          onClick = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) chatSearchQuery = ""
          },
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search messages",
            tint = if (isSearchActive) NeonCyan else Color.White
          )
        }
      }

      // Expandable Search Bar inside the Conversation
      AnimatedVisibility(visible = isSearchActive) {
        OutlinedTextField(
          value = chatSearchQuery,
          onValueChange = { chatSearchQuery = it },
          placeholder = { Text("Filter conversation thread...", color = Color.Gray, fontSize = 12.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          shape = RoundedCornerShape(12.dp)
        )
      }

      val filteredMessages = remember(chatMessages, chatSearchQuery) {
        if (chatSearchQuery.isBlank()) {
          chatMessages
        } else {
          chatMessages.filter { it.content?.contains(chatSearchQuery, ignoreCase = true) == true }
        }
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredMessages) { msg ->
          val alignRight = msg.senderId == "me" || msg.senderId == "You"
          val messageId = msg.id ?: "msg_${System.currentTimeMillis()}"

          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start
          ) {
            Surface(
              modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable {
                  // Toggle active reaction menu
                  activeReactionMenuId.value = if (activeReactionMenuId.value == messageId) null else messageId
                },
              color = if (alignRight) BentoIndigoDeep else GlassyCard,
              shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (alignRight) 20.dp else 4.dp,
                bottomEnd = if (alignRight) 4.dp else 20.dp
              ),
              border = if (!alignRight) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                // Sender Label mapping directly to clinical clickable profile view
                if (!alignRight) {
                  Text(
                    text = msg.senderName ?: "Pioneer",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .clickable {
                        onExplorerClicked(msg.senderId ?: "", msg.senderName ?: "", msg.senderName, msg.senderAvatar)
                      }
                      .padding(bottom = 4.dp)
                  )
                }

                val textContent = msg.content ?: ""

                // 🌌 PARSING RICH MEDIA ATTACHMENTS FOR MODERN CHAT COGNITION 🌌
                if (textContent.contains("[Beacon Location:") || textContent.contains("live coordinates")) {
                  // Core Holographic Location Badge
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color.Black.copy(alpha = 0.4f))
                      .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                      .padding(10.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("🛰️", fontSize = 20.sp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        "LIVE TELEMETRY COORDS",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(textContent.replace("[Beacon Location:", "").replace("]", ""), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    // Simulated sweep
                    LinearProgressIndicator(
                       color = NeonCyan,
                       trackColor = Color.White.copy(alpha = 0.05f),
                       modifier = Modifier.fillMaxWidth().height(2.dp)
                    )
                  }
                } else if (textContent.contains("[Astro Capture:") || textContent.contains("Astro Flare")) {
                  // Beautiful deep gradient illustration to simulate astrophotography capture
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(
                        Brush.verticalGradient(
                          colors = listOf(BentoIndigoDeep, DeepObsidian)
                        )
                      )
                      .border(1.dp, SoftNeonMint.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                      .padding(12.dp)
                  ) {
                    Text("📸 ASTRO PHOTOGRAPHY", color = SoftNeonMint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                          Brush.sweepGradient(
                            colors = listOf(Color(0xFF8A2BE2), Color(0xFFDA70D6), Color(0xFF00FFFF), Color(0xFF8A2BE2))
                          )
                        )
                    ) {
                      Text(
                        "Solar Aurora Glow",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                      )
                    }
                  }
                } else if (textContent.contains("[Acoustic Tone:") || textContent.contains("Audio Frequency")) {
                  // Equalizer chimer with toggle playback trigger
                  var isPlaying by remember { mutableStateOf(false) }
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color.White.copy(alpha = 0.04f))
                      .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                      .padding(10.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                          .size(32.dp)
                          .clip(CircleShape)
                          .background(NeonCyan.copy(alpha = 0.15f))
                      ) {
                        Icon(
                          imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                          contentDescription = "Play/Stop frequency",
                          tint = NeonCyan,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text("ACOUSTIC CHIME WAVE", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                          if (isPlaying) "Streaming 432Hz Core... 🎵" else "Core Harmonic Resonance (432Hz)",
                          color = Color.White,
                          fontSize = 11.sp
                        )
                      }
                    }
                    if (isPlaying) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                      ) {
                        val bars = listOf(12.dp, 24.dp, 16.dp, 32.dp, 20.dp, 14.dp, 28.dp)
                        bars.forEach { h ->
                          Box(
                            modifier = Modifier
                              .width(3.dp)
                              .height(h)
                              .background(NeonCyan)
                          )
                        }
                      }
                    }
                  }
                } else {
                  // Plain string rendering
                  Text(
                    text = textContent,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Direct reply icon trigger
                  Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = "Reply",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                      .size(24.dp)
                      .clickable { replyingToMessage.value = msg }
                  )

                  Text(
                    text = if (msg.createdAt.isNullOrEmpty() || msg.createdAt.contains("Just now")) "Just now" else "17:40",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp
                  )
                }
              }
            }

            // Pin reaction badge if present
            if (messageReactions.containsKey(messageId)) {
              Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                modifier = Modifier.padding(horizontal = 12.dp).padding(top = 2.dp)
              ) {
                Text(
                  text = messageReactions[messageId]!!,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            // Action reaction bar below selected bubble card
            AnimatedVisibility(visible = activeReactionMenuId.value == messageId) {
              Row(
                modifier = Modifier
                  .padding(vertical = 4.dp, horizontal = 8.dp)
                  .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                  .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val emojis = listOf("❤️", "👍", "🔥", "😂", "🌌")
                emojis.forEach { emoji ->
                  Text(
                    text = emoji,
                    fontSize = 16.sp,
                    modifier = Modifier
                      .clickable {
                        messageReactions[messageId] = emoji
                        activeReactionMenuId.value = null
                      }
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Typing feedback simulation panel
      if (isCompanionTyping.value) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(SoftNeonMint)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "$chatName is transceiving frequency...",
            color = SoftNeonMint,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic
          )
        }
      }

      // Quoted Reply Context Banner
      if (replyingToMessage.value != null) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color.White.copy(alpha = 0.05f),
          shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                "REPLY QUOTE DIRECT",
                color = NeonCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = replyingToMessage.value!!.content?.take(48) ?: "",
                color = Color.LightGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            IconButton(
              onClick = { replyingToMessage.value = null },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
            }
          }
        }
      }

      // Rich Payload Attachments Sheet selector drawer
      AnimatedVisibility(visible = showAttachMenu) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          color = GlassyCard,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.18f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable {
                onUserInputChange("[Beacon Location: Savannah Base • 12.87°S, 33.43°E]")
                showAttachMenu = false
              }
            ) {
              Text("📍", fontSize = 24.sp)
              Text("GPS Beacon", color = Color.LightGray, fontSize = 10.sp)
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable {
                onUserInputChange("[Astro Capture: Nebula Core Flare]")
                showAttachMenu = false
              }
            ) {
              Text("🪐", fontSize = 24.sp)
              Text("Astro Star", color = Color.LightGray, fontSize = 10.sp)
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable {
                onUserInputChange("[Acoustic Tone: Core Heartbeat Frequencies (432Hz)]")
                showAttachMenu = false
              }
            ) {
              Text("📻", fontSize = 24.sp)
              Text("Acoustic 432", color = Color.LightGray, fontSize = 10.sp)
            }
          }
        }
      }

      // Conversation message bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // '+' toggle attachments
        IconButton(
          onClick = { showAttachMenu = !showAttachMenu },
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .size(48.dp)
        ) {
          Icon(
            imageVector = if (showAttachMenu) Icons.Default.Close else Icons.Default.Add,
            contentDescription = "Attach file",
            tint = if (showAttachMenu) Color.Red else NeonCyan
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
          value = userInput,
          onValueChange = onUserInputChange,
          placeholder = { Text("Write quantum whisper secure message...", color = Color.Gray, fontSize = 12.sp) },
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
          onClick = {
            if (userInput.isNotBlank()) {
              var finalInput = userInput
              if (replyingToMessage.value != null) {
                finalInput = "💬 (Replying directly to: \"${replyingToMessage.value!!.content?.take(32) ?: ""}\")\n$finalInput"
              }
              onUserInputChange(finalInput)
              onSendMessage()
              replyingToMessage.value = null
            }
          },
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
