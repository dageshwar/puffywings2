package com.example.game.engine

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.game.model.BirdSkin
import com.example.game.model.Cloud
import com.example.game.model.GameStatus
import com.example.game.model.MedalTier
import com.example.game.model.Obstacle
import com.example.game.model.Particle
import com.example.game.model.ParticleType
import com.example.game.sound.SoundManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val context: Context,
    val soundManager: SoundManager
) {
    private val prefs = context.getSharedPreferences("puffy_wings_prefs", Context.MODE_PRIVATE)

    var gameStatus: GameStatus by mutableStateOf(GameStatus.READY)
        private set

    // Dimensions
    var screenWidth: Float = 1080f
        private set
    var screenHeight: Float = 1920f
        private set

    // Bird state
    var birdX: Float = 300f
        private set
    var birdY: Float = 800f
        private set
    var birdVy: Float = 0f
        private set
    var birdRotation: Float = 0f
        private set
    var birdRadius: Float = 42f
        private set
    var wingAngle: Float = 0f
        private set
    var eyeOpenRatio: Float = 1f
        private set
    var selectedSkin: BirdSkin by mutableStateOf(BirdSkin.GOLDEN)

    // Obstacles
    val obstacles = mutableListOf<Obstacle>()
    private var nextObstacleId = 1L

    // Clouds for parallax background
    val clouds = mutableListOf<Cloud>()

    // Ground scrolling offset
    var groundOffset: Float = 0f
        private set
    var groundHeight: Float = 220f
        private set

    // Particles
    val particles = mutableListOf<Particle>()

    // Score
    var score: Int by mutableIntStateOf(0)
        private set
    var bestScore: Int by mutableIntStateOf(prefs.getInt("best_score", 0))
        private set
    var isNewBest: Boolean by mutableStateOf(false)
        private set

    // Screen shake on hit
    var shakeDuration: Float = 0f
        private set
    var shakeIntensity: Float = 0f
        private set

    // Animation timers
    private var idleTime: Float = 0f
    private var flapAnimTimer: Float = 0f
    private var nextBlinkTimer: Float = 2.5f

    // Color palettes for obstacle pillars (candy, emerald, amethyst, sunset)
    private val pillarPalettes = listOf(
        Pair(Color(0xFF0284C7), Color(0xFF38BDF8)), // Sky Crystal
        Pair(Color(0xFF059669), Color(0xFF34D399)), // Emerald Spire
        Pair(Color(0xFF7C3AED), Color(0xFFA78BFA)), // Amethyst Column
        Pair(Color(0xFFEA580C), Color(0xFFFB923C)), // Coral Tower
        Pair(Color(0xFFDB2777), Color(0xFFF472B6))  // Berry Candy
    )

    init {
        // Initialize clouds
        initClouds()
    }

    fun updateScreenSize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        screenWidth = width
        screenHeight = height
        groundHeight = height * 0.13f
        birdRadius = (width * 0.056f).coerceIn(36f, 54f)
        birdX = width * 0.28f

        if (gameStatus == GameStatus.READY) {
            birdY = height * 0.46f
            birdVy = 0f
            birdRotation = 0f
        }
    }

    private fun initClouds() {
        clouds.clear()
        clouds.add(Cloud(x = 100f, y = 140f, scale = 1.0f, speed = 25f, alpha = 0.9f))
        clouds.add(Cloud(x = 550f, y = 240f, scale = 0.75f, speed = 18f, alpha = 0.75f))
        clouds.add(Cloud(x = 900f, y = 90f, scale = 1.2f, speed = 28f, alpha = 0.95f))
        clouds.add(Cloud(x = 350f, y = 380f, scale = 0.6f, speed = 14f, alpha = 0.65f))
    }

    fun onScreenTap() {
        when (gameStatus) {
            GameStatus.READY -> {
                startGame()
                flap()
            }
            GameStatus.PLAYING -> {
                flap()
            }
            GameStatus.GAME_OVER -> {
                // If game over, tap does not instantly restart to avoid accidental taps;
                // restarts via restartGame()
            }
        }
    }

    fun startGame() {
        gameStatus = GameStatus.PLAYING
        score = 0
        isNewBest = false
        obstacles.clear()
        particles.clear()
        birdVy = 0f
        birdRotation = 0f
        flapAnimTimer = 0.25f
        shakeDuration = 0f
    }

    fun restartGame() {
        soundManager.playClick()
        gameStatus = GameStatus.PLAYING
        score = 0
        isNewBest = false
        obstacles.clear()
        particles.clear()
        birdY = screenHeight * 0.45f
        birdVy = 0f
        birdRotation = 0f
        flapAnimTimer = 0.25f
        shakeDuration = 0f
        flap()
    }

    fun goToReadyMenu() {
        soundManager.playClick()
        gameStatus = GameStatus.READY
        birdY = screenHeight * 0.46f
        birdVy = 0f
        birdRotation = 0f
        obstacles.clear()
        particles.clear()
        shakeDuration = 0f
    }

    fun flap() {
        if (gameStatus != GameStatus.PLAYING && gameStatus != GameStatus.READY) return

        val scaleFactor = (screenHeight / 1920f).coerceIn(0.6f, 1.4f)
        birdVy = -820f * scaleFactor
        flapAnimTimer = 0.22f

        soundManager.playFlap()

        // Spawn cute feather & sparkle particles behind the bird
        val skin = selectedSkin
        repeat(5) {
            val angle = Random.nextFloat() * PI.toFloat() * 0.8f + PI.toFloat() * 0.6f
            val speed = Random.nextFloat() * 180f + 80f
            particles.add(
                Particle(
                    x = birdX - birdRadius * 0.7f + (Random.nextFloat() - 0.5f) * 16f,
                    y = birdY + (Random.nextFloat() - 0.5f) * 20f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (Random.nextBoolean()) skin.primaryColor else Color.White,
                    size = Random.nextFloat() * 10f + 6f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = 0.45f,
                    type = if (Random.nextBoolean()) ParticleType.FEATHER else ParticleType.SPARKLE,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() - 0.5f) * 400f
                )
            )
        }
    }

    fun update(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)

        // Parallax background clouds update
        updateClouds(dt)

        // Ground scrolling
        if (gameStatus != GameStatus.GAME_OVER) {
            val scrollSpeed = 220f + (score * 2.2f).coerceAtMost(100f)
            groundOffset = (groundOffset + scrollSpeed * dt) % (screenWidth * 0.2f)
        }

        // Screen shake decay
        if (shakeDuration > 0f) {
            shakeDuration -= dt
            if (shakeDuration < 0f) shakeDuration = 0f
        }

        // Update eye blink
        nextBlinkTimer -= dt
        if (nextBlinkTimer <= 0f) {
            eyeOpenRatio = 0.1f
            if (nextBlinkTimer < -0.16f) {
                eyeOpenRatio = 1f
                nextBlinkTimer = Random.nextFloat() * 3f + 2.5f
            }
        } else {
            eyeOpenRatio = 1f
        }

        when (gameStatus) {
            GameStatus.READY -> {
                // Bobbing idle animation
                idleTime += dt
                birdY = (screenHeight * 0.46f) + sin(idleTime * 4.5f) * (birdRadius * 0.45f)
                birdRotation = sin(idleTime * 4.5f) * 6f
                wingAngle = sin(idleTime * 8f) * 28f
            }
            GameStatus.PLAYING -> {
                updatePlayingPhysics(dt)
            }
            GameStatus.GAME_OVER -> {
                // If in air, fall to ground
                if (birdY < screenHeight - groundHeight - birdRadius) {
                    val scaleFactor = (screenHeight / 1920f).coerceIn(0.6f, 1.4f)
                    birdVy += 2400f * scaleFactor * dt
                    birdY += birdVy * dt
                    birdRotation = (birdRotation + 400f * dt).coerceAtMost(90f)
                } else {
                    birdY = screenHeight - groundHeight - birdRadius
                    birdVy = 0f
                }
                wingAngle = -10f
            }
        }

        // Update particles
        updateParticles(dt)
    }

    private fun updatePlayingPhysics(dt: Float) {
        val scaleFactor = (screenHeight / 1920f).coerceIn(0.6f, 1.4f)
        val gravity = 2250f * scaleFactor
        val maxFallSpeed = 1200f * scaleFactor

        // Gravity
        birdVy += gravity * dt
        if (birdVy > maxFallSpeed) birdVy = maxFallSpeed

        birdY += birdVy * dt

        // Ceiling clamp
        if (birdY < birdRadius + 10f) {
            birdY = birdRadius + 10f
            birdVy = 0f
        }

        // Rotation based on velocity
        if (birdVy < 0) {
            // Rising
            birdRotation = (birdRotation - 350f * dt).coerceAtLeast(-24f)
        } else {
            // Falling
            birdRotation = (birdRotation + 260f * dt).coerceAtMost(75f)
        }

        // Wing flapping animation
        if (flapAnimTimer > 0f) {
            flapAnimTimer -= dt
            wingAngle = sin((0.22f - flapAnimTimer) * 45f) * 35f
        } else {
            wingAngle = -12f + (birdVy / maxFallSpeed) * 10f
        }

        // Ground collision
        val floorY = screenHeight - groundHeight - birdRadius
        if (birdY >= floorY) {
            birdY = floorY
            triggerGameOver()
            return
        }

        // Update Obstacles
        updateObstacles(dt)

        // Check Obstacle Collisions
        checkObstacleCollisions()
    }

    private fun updateObstacles(dt: Float) {
        val baseSpeed = 260f * (screenWidth / 1080f).coerceIn(0.8f, 1.3f)
        val speed = baseSpeed + (score * 2.5f).coerceAtMost(90f)

        // Move existing obstacles
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.x -= speed * dt

            // Check if scored
            if (!obs.scored && obs.x + obs.width < birdX) {
                obs.scored = true
                score++
                soundManager.playScore()
                spawnScoreSparkles(obs.x + obs.width * 0.5f, obs.gapCenterY)

                if (score > bestScore) {
                    bestScore = score
                    if (!isNewBest) {
                        isNewBest = true
                        soundManager.playMedal()
                    }
                    prefs.edit().putInt("best_score", bestScore).apply()
                }
            }

            // Remove offscreen
            if (obs.x + obs.width < -50f) {
                iterator.remove()
            }
        }

        // Spawn new obstacle
        val obstacleSpacing = screenWidth * 0.62f
        val lastObs = obstacles.lastOrNull()
        if (lastObs == null || lastObs.x < screenWidth - obstacleSpacing) {
            spawnObstacle()
        }
    }

    private fun spawnObstacle() {
        val gapHeight = (screenHeight * 0.28f).coerceIn(380f, 520f)
        val minMargin = screenHeight * 0.16f
        val maxMargin = screenHeight - groundHeight - minMargin - gapHeight
        val gapCenterY = minMargin + gapHeight * 0.5f + Random.nextFloat() * (maxMargin.coerceAtLeast(10f))

        val palette = pillarPalettes[Random.nextInt(pillarPalettes.size)]
        val obsWidth = (screenWidth * 0.18f).coerceIn(120f, 175f)

        obstacles.add(
            Obstacle(
                id = nextObstacleId++,
                x = screenWidth + 20f,
                gapCenterY = gapCenterY,
                gapHeight = gapHeight,
                width = obsWidth,
                themeColor = palette.first,
                capColor = palette.second
            )
        )
    }

    private fun checkObstacleCollisions() {
        // Forgiving bird hitbox (circle radius slightly smaller than visual radius)
        val hitboxRadius = birdRadius * 0.76f

        for (obs in obstacles) {
            val obsLeft = obs.x
            val obsRight = obs.x + obs.width
            val gapTop = obs.gapCenterY - obs.gapHeight * 0.5f
            val gapBottom = obs.gapCenterY + obs.gapHeight * 0.5f

            // Check horizontal overlap with bird
            val closestX = birdX.coerceIn(obsLeft, obsRight)

            // Top obstacle check (from y = 0 to y = gapTop)
            if (birdY - hitboxRadius < gapTop) {
                val closestY = birdY.coerceIn(0f, gapTop)
                val dx = birdX - closestX
                val dy = birdY - closestY
                if (dx * dx + dy * dy < hitboxRadius * hitboxRadius) {
                    triggerGameOver()
                    return
                }
            }

            // Bottom obstacle check (from y = gapBottom to y = screenHeight - groundHeight)
            if (birdY + hitboxRadius > gapBottom) {
                val closestY = birdY.coerceIn(gapBottom, screenHeight - groundHeight)
                val dx = birdX - closestX
                val dy = birdY - closestY
                if (dx * dx + dy * dy < hitboxRadius * hitboxRadius) {
                    triggerGameOver()
                    return
                }
            }
        }
    }

    private fun triggerGameOver() {
        gameStatus = GameStatus.GAME_OVER
        soundManager.playHit()
        soundManager.playGameOver()
        shakeDuration = 0.35f
        shakeIntensity = 18f

        // Spawn collision stars and dust poofs
        repeat(16) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 320f + 90f
            particles.add(
                Particle(
                    x = birdX,
                    y = birdY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (Random.nextBoolean()) Color(0xFFFDE047) else Color(0xFFF97316),
                    size = Random.nextFloat() * 14f + 8f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = 0.55f,
                    type = ParticleType.STAR,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() - 0.5f) * 600f
                )
            )
        }
    }

    private fun spawnScoreSparkles(x: Float, y: Float) {
        repeat(10) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 220f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = Color(0xFFFDE047),
                    size = Random.nextFloat() * 12f + 6f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = 0.6f,
                    type = ParticleType.SPARKLE,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() - 0.5f) * 400f
                )
            )
        }
    }

    private fun updateClouds(dt: Float) {
        for (cloud in clouds) {
            cloud.x -= cloud.speed * dt
            if (cloud.x < -250f) {
                cloud.x = screenWidth + 100f + Random.nextFloat() * 150f
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life += dt
            if (p.life >= p.maxLife) {
                iter.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 300f * dt // slight gravity on particles
            p.rotation += p.rotSpeed * dt
            p.alpha = 1f - (p.life / p.maxLife)
        }
    }

    fun getMedal(): MedalTier = MedalTier.fromScore(score)
}
