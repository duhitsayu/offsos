package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.audio.TacticalAudioEngine
import com.example.media.TacticalMediaManager
import com.example.model.AppLanguage
import com.example.model.AppStrings
import com.example.model.MeshGroup
import com.example.model.MeshMessage
import com.example.model.MessageType
import com.example.ui.theme.TacticalAlertRed
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    currentGroup: MeshGroup?,
    messages: List<MeshMessage>,
    localNodeId: String,
    appLanguage: AppLanguage,
    audioEngine: TacticalAudioEngine,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceMessage: (filePath: String, durationSec: Int) -> Unit,
    onSendImageMessage: (filePath: String, caption: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var showAttachOptions by remember { mutableStateOf(false) }
    var selectedFullImage by remember { mutableStateOf<String?>(null) }

    val isRecordingVoice by audioEngine.isRecording.collectAsState()
    val recordingDurationSec by audioEngine.recordingDurationSec.collectAsState()
    val currentlyPlayingId by audioEngine.currentlyPlayingId.collectAsState()
    var liveSeconds by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    // Real Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = TacticalMediaManager.processAndSaveImage(context, uri)
            if (savedPath != null) {
                onSendImageMessage(savedPath, "Field Recon Photo // P2P Mesh")
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Track live seconds during active audio recording
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            liveSeconds = 0
            while (isRecordingVoice && liveSeconds < 60) {
                kotlinx.coroutines.delay(1000)
                liveSeconds++
            }
            if (isRecordingVoice) {
                val res = audioEngine.stopRecording()
                if (res != null) {
                    onSendVoiceMessage(res.filePath, res.durationSec)
                }
            }
        }
    }

    val groupName = currentGroup?.name ?: AppStrings.get("public_mesh", appLanguage)
    val memberCount = if (currentGroup != null) currentGroup.members.size else 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header with Group Name & Member Count
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentGroup != null) "$memberCount ACTIVE PEERS // ENCRYPTED" else "PUBLIC BROADCAST // NO RELAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "OFFLINE P2P MESH CHANNEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Send verified field photos, high-clarity voice notes, and tactical alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(messages) { msg ->
                val isSelf = msg.senderId == localNodeId
                ChatMessageBubble(
                    message = msg,
                    isSelf = isSelf,
                    isPlayingVoice = currentlyPlayingId == msg.id,
                    onToggleVoicePlayback = {
                        val path = msg.mediaUriOrData ?: ""
                        audioEngine.playAudio(msg.id, path)
                    },
                    onImageClick = { filePath ->
                        selectedFullImage = filePath
                    }
                )
            }
        }

        // Voice Recording Active Bar
        if (isRecordingVoice) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TacticalAlertRed.copy(alpha = 0.15f))
                    .border(1.dp, TacticalAlertRed, RoundedCornerShape(2.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(TacticalAlertRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECORDING VOICE NOTE (0:${if (liveSeconds < 10) "0" else ""}$liveSeconds)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TacticalAlertRed
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { audioEngine.cancelRecording() },
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("CANCEL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val res = audioEngine.stopRecording()
                            if (res != null) {
                                onSendVoiceMessage(res.filePath, res.durationSec)
                            }
                        },
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("TRANSMIT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Attach Options Row
        if (showAttachOptions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        showAttachOptions = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SELECT FIELD PHOTO", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                }

                OutlinedButton(
                    onClick = {
                        showAttachOptions = false
                        if (!hasAudioPermission) {
                            onRequestAudioPermission()
                        } else {
                            audioEngine.startRecording()
                        }
                    },
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RECORD VOICE NOTE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                }
            }
        }

        // Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Attach Button (Android Photo Picker)
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.size(38.dp).testTag("attach_photo_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Attach Field Photo",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Real Voice Note Record Button
            IconButton(
                onClick = {
                    if (!hasAudioPermission) {
                        onRequestAudioPermission()
                    } else {
                        if (isRecordingVoice) {
                            val res = audioEngine.stopRecording()
                            if (res != null) {
                                onSendVoiceMessage(res.filePath, res.durationSec)
                            }
                        } else {
                            audioEngine.startRecording()
                        }
                    }
                },
                modifier = Modifier.size(38.dp).testTag("record_voice_btn")
            ) {
                Icon(
                    imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Record Voice Note",
                    tint = if (isRecordingVoice) TacticalAlertRed else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Text Input Box
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = {
                    Text(
                        text = if (isRecordingVoice) "Recording voice note..." else "Tactical message...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                enabled = !isRecordingVoice,
                shape = RoundedCornerShape(2.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput)
                        textInput = ""
                    }
                },
                enabled = textInput.isNotBlank(),
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (textInput.isNotBlank()) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        RoundedCornerShape(2.dp)
                    )
                    .testTag("send_message_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (textInput.isNotBlank()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Full-Screen Image Dialog Preview
    selectedFullImage?.let { imagePath ->
        Dialog(onDismissRequest = { selectedFullImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RoundedCornerShape(4.dp))
                    .border(1.5.dp, Color(0xFFFFFFFF), RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIELD RECON PHOTO // HIGH RES",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFFFFF)
                        )
                        IconButton(onClick = { selectedFullImage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFFFFFFF))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Full Field Recon Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFF111111)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image file unavailable", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Stored internally at: ${imageFile.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: MeshMessage,
    isSelf: Boolean,
    isPlayingVoice: Boolean,
    onToggleVoicePlayback: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val timeStr = timeFormat.format(Date(message.timestamp))

    val bubbleAlignment = if (isSelf) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(2.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = bubbleAlignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(
                    if (message.isSos) TacticalAlertRed.copy(alpha = 0.15f)
                    else if (isSelf) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 1.dp,
                    color = if (message.isSos) TacticalAlertRed else MaterialTheme.colorScheme.outline,
                    shape = bubbleShape
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                // Header: Sender & Hops
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (message.isSos) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = "SOS",
                            tint = TacticalAlertRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isSelf) "YOU (${message.sender})" else message.sender,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = if (message.isSos) TacticalAlertRed else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${message.hops} hop",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content by Type
                when (message.type) {
                    MessageType.VOICE -> {
                        // Real Voice Note Player Bubble
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                                .clickable { onToggleVoicePlayback() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingVoice) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Voice Note",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Waveform simulation
                            Canvas(modifier = Modifier.weight(1f).height(18.dp)) {
                                val bars = 18
                                val barW = size.width / (bars * 1.6f)
                                for (i in 0 until bars) {
                                    val barH = if (isPlayingVoice) {
                                        (kotlin.math.sin(i * 0.8f + System.currentTimeMillis() * 0.005f) * 0.4f + 0.5f).toFloat() * size.height
                                    } else {
                                        ((i % 4 + 1) / 4f) * size.height * 0.7f
                                    }
                                    drawRect(
                                        color = if (isPlayingVoice) Color(0xFFFFFFFF) else Color.DarkGray,
                                        topLeft = Offset(i * barW * 1.6f, (size.height - barH) / 2f),
                                        size = Size(barW, barH)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingVoice) "PLAYING" else "0:${message.audioDurationSec ?: 4}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    MessageType.IMAGE -> {
                        // Real Image attachment card
                        Column {
                            val filePath = message.mediaUriOrData
                            val file = if (filePath != null) File(filePath) else null

                            if (file != null && file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Field Photo Attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 200.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                                        .clickable { onImageClick(file.absolutePath) }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(Color(0xFF1E2628), RoundedCornerShape(2.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "FIELD RECON PHOTO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }

                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
