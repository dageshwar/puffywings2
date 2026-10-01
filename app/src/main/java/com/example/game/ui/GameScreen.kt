package com.example.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ads.AdManager
import com.example.game.ads.AnchoredAdaptiveBanner
import com.example.game.engine.GameEngine
import com.example.game.model.BirdSkin
import com.example.game.model.GameStatus
import com.example.game.model.MedalTier
import com.example.game.sound.SoundManager
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    adManager: AdManager? = null
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val engine = remember { GameEngine(context, soundManager) }
    val currentAdManager = adManager ?: remember { AdManager(context).apply { initialize() } }

    // Recomposition trigger from game loop
    var frameTick by remember { mutableFloatStateOf(0f) }
    var isMuted by remember { mutableStateOf(soundManager.isMuted) }

    // High frequency 60 FPS game engine update loop
    LaunchedEffect(Unit) {
        var lastNanoTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { nowNanos ->
                val dt = ((nowNanos - lastNanoTime) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
                lastNanoTime = nowNanos
                engine.update(dt)
                frameTick = (frameTick + 1f) % 1000f
            }
        }
    }

    // Title float animation for start screen
    val infiniteTransition = rememberInfiniteTransition(label = "title_bob")
    val titleOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    engine.onScreenTap()
                }
            }
    ) {
        // 1. Interactive Canvas Game Layer
        Canvas(modifier = Modifier.fillMaxSize().testTag("game_canvas")) {
            engine.updateScreenSize(size.width, size.height)
            // Read frameTick to trigger redraw
            val _tick = frameTick
            GameCanvasRenderer.render(this, engine)
        }

        // 2. In-Game Top HUD (Score & Sound Toggle)
        TopHud(
            score = engine.score,
            bestScore = engine.bestScore,
            gameStatus = engine.gameStatus,
            isMuted = isMuted,
            onToggleMute = {
                isMuted = engine.soundManager.toggleMute()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Start Screen Overlay: ONLY visible on Main Menu, completely hidden during gameplay
        if (engine.gameStatus == GameStatus.READY) {
            StartScreenOverlay(
                bestScore = engine.bestScore,
                titleOffsetY = titleOffsetY,
                selectedSkin = engine.selectedSkin,
                onSelectSkin = { skin ->
                    engine.selectedSkin = skin
                    engine.soundManager.playClick()
                },
                onPlayClicked = {
                    engine.startGame()
                    engine.flap()
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 4. Game Over Screen Overlay: ONLY visible after Game Over, completely hidden during gameplay
        if (engine.gameStatus == GameStatus.GAME_OVER) {
            GameOverOverlay(
                score = engine.score,
                bestScore = engine.bestScore,
                isNewBest = engine.isNewBest,
                medal = engine.getMedal(),
                onPlayAgainClicked = {
                    val activity = context as? Activity
                    if (activity != null) {
                        currentAdManager.showInterstitial(activity) {
                            engine.restartGame()
                        }
                    } else {
                        engine.restartGame()
                    }
                },
                onHomeClicked = {
                    val activity = context as? Activity
                    if (activity != null) {
                        currentAdManager.showInterstitial(activity) {
                            engine.goToReadyMenu()
                        }
                    } else {
                        engine.goToReadyMenu()
                    }
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 5. Anchored Adaptive Banner Test Ad docked at the bottom of the screen
        AnchoredAdaptiveBanner(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopHud(
    score: Int,
    bestScore: Int,
    gameStatus: GameStatus,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High Score Chip (visible during game or start)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC0F172A),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Best Score Trophy",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BEST $bestScore",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Current Score Pill (highlighted during active play)
        if (gameStatus == GameStatus.PLAYING) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xEEFFFFFF),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "$score",
                    color = Color(0xFF0F172A),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .testTag("current_score_text")
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Sound Toggle Button
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xCC0F172A), CircleShape)
                .testTag("mute_toggle_button")
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute Sound" else "Mute Sound",
                tint = if (isMuted) Color(0xFFF87171) else Color(0xFF38BDF8),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun StartScreenOverlay(
    bestScore: Int,
    titleOffsetY: Float,
    selectedSkin: BirdSkin,
    onSelectSkin: (BirdSkin) -> Unit,
    onPlayClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .testTag("start_game_dialog")
    ) {
        Column(
            modifier = Modifier
                .padding(26.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Game Title
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF06B6D4))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "PUFFY WINGS",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tap to flap and fly through the towers!",
                color = Color(0xFF475569),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Character Skin Selector
            Text(
                text = "CHOOSE YOUR CHIRPER",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BirdSkin.values().forEach { skin ->
                    val isSelected = skin == selectedSkin
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(skin.primaryColor)
                            .border(
                                width = if (isSelected) 3.5.dp else 1.5.dp,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0x44000000),
                                shape = CircleShape
                            )
                            .clickable { onSelectSkin(skin) }
                            .testTag("skin_${skin.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cute inner eye & beak preview
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF0F172A), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(skin.beakColor, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Touch-Friendly Play Button
            Button(
                onClick = onPlayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .testTag("play_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981) // Crisp Emerald Play Green
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Icon",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Tap Hint
            Text(
                text = "✨ Or tap anywhere on screen to start! ✨",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    bestScore: Int,
    isNewBest: Boolean,
    medal: MedalTier,
    onPlayAgainClicked: () -> Unit,
    onHomeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF7FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .testTag("game_over_dialog")
    ) {
        Column(
            modifier = Modifier
                .padding(26.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "GAME OVER",
                color = Color(0xFFE11D48),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scoreboard Container
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Score
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SCORE",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$score",
                                color = Color(0xFF0F172A),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("game_over_score_text")
                            )
                        }

                        // Medal Badge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(medal.badgeColor, CircleShape)
                                    .border(2.dp, medal.ribbonColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Medal: ${medal.title}",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = medal.title,
                                color = medal.ribbonColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Best Score
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BEST",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$bestScore",
                                color = Color(0xFF0F172A),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("game_over_best_text")
                            )
                        }
                    }

                    // Celebratory High Score Badge
                    if (isNewBest && score > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF08A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFACC15))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "NEW HIGH SCORE!",
                                    color = Color(0xFFB45309),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Large Play Again Button
            Button(
                onClick = onPlayAgainClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .testTag("play_again_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Play Again Icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PLAY AGAIN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Home / Menu Button
            OutlinedButton(
                onClick = onHomeClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("home_button"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Main Menu Icon",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAIN MENU",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}
