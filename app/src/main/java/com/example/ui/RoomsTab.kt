package com.example

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun RoomsTab(
  rooms: List<MobileRoom>,
  isFetchingRooms: Boolean = false,
  onJoinRoom: (MobileRoom) -> Unit,
  onExplorerClicked: (id: String, username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _, _ -> }
) {
  var joinedRoomId by remember { mutableStateOf<String?>(null) }
  val roomMessages = remember { mutableStateListOf<Pair<String, String>>() }
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
                  text = "Host: ${room.host}",
                  color = Color.Gray,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Normal,
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onExplorerClicked("exp_${room.host}", room.host, room.host, null) }
                    .padding(vertical = 2.dp, horizontal = 4.dp)
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
          Text(
            text = "Host: ${activeRoom.host} • ${activeRoom.membersCount} listening 📡",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
              .clickable { onExplorerClicked("exp_${activeRoom.host}", activeRoom.host, activeRoom.host, null) }
          )
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
              Text(
                text = sender,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    if (sender != "You") {
                      onExplorerClicked("exp_$sender", sender, sender, null)
                    }
                  }
                  .padding(vertical = 2.dp, horizontal = 4.dp)
              )
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
