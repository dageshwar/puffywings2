package com.example.game.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.example.game.engine.GameEngine
import com.example.game.model.Cloud
import com.example.game.model.Obstacle
import com.example.game.model.Particle
import com.example.game.model.ParticleType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object GameCanvasRenderer {

    private val skyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0284C7), // Deep vibrant azure
            Color(0xFF38BDF8), // Bright sky
            Color(0xFF7DD3FC), // Soft sky
            Color(0xFFBAE6FD), // Horizon haze
            Color(0xFFE0F2FE)  // Warm glow
        )
    )

    private val distantHillColor = Color(0xFF6EE7B7) // Mint pastel green
    private val nearHillColor = Color(0xFF34D399)    // Emerald green
    private val grassColor = Color(0xFF22C55E)       // Fresh grass
    private val grassDarkColor = Color(0xFF16A34A)
    private val dirtColor = Color(0xFFD97706)        // Rich warm earth
    private val dirtDarkColor = Color(0xFFB45309)

    fun render(scope: DrawScope, engine: GameEngine) {
        val width = scope.size.width
        val height = scope.size.height

        // Apply screen shake if active
        val shakeX: Float
        val shakeY: Float
        if (engine.shakeDuration > 0f) {
            val factor = engine.shakeDuration / 0.35f
            shakeX = (Random.nextFloat() - 0.5f) * engine.shakeIntensity * factor
            shakeY = (Random.nextFloat() - 0.5f) * engine.shakeIntensity * factor
        } else {
            shakeX = 0f
            shakeY = 0f
        }

        scope.translate(shakeX, shakeY) {
            // 1. Sky & Sun
            drawSkyAndSun(width, height)

            // 2. Distant Hills (Parallax)
            drawHills(width, height, engine.groundHeight, engine.groundOffset)

            // 3. Clouds
            for (cloud in engine.clouds) {
                drawCloud(cloud)
            }

            // 4. Obstacles
            for (obstacle in engine.obstacles) {
                drawObstacle(obstacle, height, engine.groundHeight)
            }

            // 5. Ground
            drawGround(width, height, engine.groundHeight, engine.groundOffset)

            // 6. Particles
            for (particle in engine.particles) {
                drawParticle(particle)
            }

            // 7. Bird
            drawBird(engine)
        }
    }

    private fun DrawScope.drawSkyAndSun(width: Float, height: Float) {
        // Sky
        drawRect(brush = skyGradient, size = Size(width, height))

        // Sun with radial glow
        val sunCenter = Offset(width * 0.82f, height * 0.14f)
        val sunRadius = width * 0.11f

        drawCircle(
            color = Color(0x33FEF08A),
            radius = sunRadius * 2.2f,
            center = sunCenter
        )
        drawCircle(
            color = Color(0x66FEF08A),
            radius = sunRadius * 1.5f,
            center = sunCenter
        )
        drawCircle(
            color = Color(0xFFFFFBEB),
            radius = sunRadius,
            center = sunCenter
        )
    }

    private fun DrawScope.drawHills(width: Float, height: Float, groundHeight: Float, groundOffset: Float) {
        val groundTop = height - groundHeight

        // Far hills
        val hillPath1 = Path().apply {
            moveTo(0f, groundTop)
            val hillOffset1 = (groundOffset * 0.25f) % (width * 0.8f)
            lineTo(0f, groundTop - 90f)
            quadraticTo(width * 0.25f - hillOffset1, groundTop - 210f, width * 0.55f - hillOffset1, groundTop - 110f)
            quadraticTo(width * 0.85f - hillOffset1, groundTop - 240f, width * 1.25f - hillOffset1, groundTop - 90f)
            lineTo(width, groundTop - 90f)
            lineTo(width, groundTop)
            close()
        }
        drawPath(hillPath1, color = distantHillColor.copy(alpha = 0.65f))

        // Near hills
        val hillPath2 = Path().apply {
            moveTo(0f, groundTop)
            val hillOffset2 = (groundOffset * 0.5f) % (width * 0.6f)
            lineTo(0f, groundTop - 50f)
            quadraticTo(width * 0.35f - hillOffset2, groundTop - 140f, width * 0.7f - hillOffset2, groundTop - 60f)
            quadraticTo(width * 1.05f - hillOffset2, groundTop - 160f, width * 1.4f - hillOffset2, groundTop - 40f)
            lineTo(width, groundTop - 40f)
            lineTo(width, groundTop)
            close()
        }
        drawPath(hillPath2, color = nearHillColor.copy(alpha = 0.85f))
    }

    private fun DrawScope.drawCloud(cloud: Cloud) {
        val baseRadius = 38f * cloud.scale
        val color = Color.White.copy(alpha = cloud.alpha)
        val shadowColor = Color(0xFFE2E8F0).copy(alpha = cloud.alpha * 0.6f)

        // Subtle cloud puff shadow
        drawCircle(shadowColor, baseRadius * 1.25f, Offset(cloud.x, cloud.y + 6f * cloud.scale))
        drawCircle(shadowColor, baseRadius * 0.95f, Offset(cloud.x - baseRadius * 1.2f, cloud.y + 8f * cloud.scale))
        drawCircle(shadowColor, baseRadius * 1.05f, Offset(cloud.x + baseRadius * 1.2f, cloud.y + 7f * cloud.scale))

        // Cloud puffs
        drawCircle(color, baseRadius * 1.25f, Offset(cloud.x, cloud.y))
        drawCircle(color, baseRadius * 0.95f, Offset(cloud.x - baseRadius * 1.2f, cloud.y + 4f * cloud.scale))
        drawCircle(color, baseRadius * 1.05f, Offset(cloud.x + baseRadius * 1.2f, cloud.y + 3f * cloud.scale))
        drawCircle(color, baseRadius * 0.75f, Offset(cloud.x + baseRadius * 2.0f, cloud.y + 8f * cloud.scale))
    }

    private fun DrawScope.drawObstacle(obs: Obstacle, height: Float, groundHeight: Float) {
        val gapTop = obs.gapCenterY - obs.gapHeight * 0.5f
        val gapBottom = obs.gapCenterY + obs.gapHeight * 0.5f
        val groundTop = height - groundHeight
        val capHeight = 36f
        val capOverhang = 8f

        val bodyBrush = Brush.horizontalGradient(
            listOf(
                obs.themeColor,
                obs.capColor,
                obs.themeColor.copy(alpha = 0.9f)
            ),
            startX = obs.x,
            endX = obs.x + obs.width
        )

        val capBrush = Brush.horizontalGradient(
            listOf(
                obs.capColor,
                Color.White.copy(alpha = 0.8f),
                obs.capColor
            ),
            startX = obs.x - capOverhang,
            endX = obs.x + obs.width + capOverhang
        )

        // 1. TOP OBSTACLE (hanging down)
        if (gapTop > 0f) {
            // Main pillar body
            drawRoundRect(
                brush = bodyBrush,
                topLeft = Offset(obs.x, 0f),
                size = Size(obs.width, (gapTop - capHeight).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(0f, 0f)
            )

            // Inner gloss stripe on left
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(obs.x + 8f, 0f),
                size = Size(10f, (gapTop - capHeight).coerceAtLeast(0f))
            )

            // Cap at the bottom of the top pillar
            drawRoundRect(
                brush = capBrush,
                topLeft = Offset(obs.x - capOverhang, (gapTop - capHeight).coerceAtLeast(0f)),
                size = Size(obs.width + capOverhang * 2f, capHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Decorative gem / glowing crystal in the cap center
            val gemCenter = Offset(obs.x + obs.width * 0.5f, gapTop - capHeight * 0.5f)
            drawCircle(Color.White.copy(alpha = 0.9f), 6f, gemCenter)
            drawCircle(obs.capColor, 4f, gemCenter)
        }

        // 2. BOTTOM OBSTACLE (rising from ground)
        if (gapBottom < groundTop) {
            val bottomPillarHeight = groundTop - (gapBottom + capHeight)

            // Bottom pillar cap
            drawRoundRect(
                brush = capBrush,
                topLeft = Offset(obs.x - capOverhang, gapBottom),
                size = Size(obs.width + capOverhang * 2f, capHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Bottom decorative gem
            val gemCenterBottom = Offset(obs.x + obs.width * 0.5f, gapBottom + capHeight * 0.5f)
            drawCircle(Color.White.copy(alpha = 0.9f), 6f, gemCenterBottom)
            drawCircle(obs.capColor, 4f, gemCenterBottom)

            // Bottom main pillar body
            if (bottomPillarHeight > 0f) {
                drawRoundRect(
                    brush = bodyBrush,
                    topLeft = Offset(obs.x, gapBottom + capHeight),
                    size = Size(obs.width, bottomPillarHeight),
                    cornerRadius = CornerRadius(0f, 0f)
                )

                // Inner gloss stripe on left
                drawRect(
                    color = Color.White.copy(alpha = 0.25f),
                    topLeft = Offset(obs.x + 8f, gapBottom + capHeight),
                    size = Size(10f, bottomPillarHeight)
                )
            }
        }
    }

    private fun DrawScope.drawGround(width: Float, height: Float, groundHeight: Float, groundOffset: Float) {
        val groundTop = height - groundHeight

        // Dirt background
        drawRect(
            color = dirtColor,
            topLeft = Offset(0f, groundTop),
            size = Size(width, groundHeight)
        )

        // Diagonal stripes on dirt for scrolling effect
        val stripeWidth = 26f
        val totalStripes = ((width + groundHeight) / (stripeWidth * 2)).toInt() + 4
        for (i in -2..totalStripes) {
            val startX = i * stripeWidth * 2 - (groundOffset % (stripeWidth * 2))
            val stripePath = Path().apply {
                moveTo(startX, groundTop + 24f)
                lineTo(startX + stripeWidth, groundTop + 24f)
                lineTo(startX + stripeWidth - 28f, height)
                lineTo(startX - 28f, height)
                close()
            }
            drawPath(stripePath, color = dirtDarkColor.copy(alpha = 0.45f))
        }

        // Grass top layer
        drawRect(
            color = grassColor,
            topLeft = Offset(0f, groundTop),
            size = Size(width, 24f)
        )

        // Decorative grass scallops
        val scallopWidth = 32f
        val scallopCount = (width / scallopWidth).toInt() + 3
        for (i in -1..scallopCount) {
            val cx = i * scallopWidth - (groundOffset % scallopWidth) + scallopWidth * 0.5f
            drawCircle(
                color = grassColor,
                radius = scallopWidth * 0.45f,
                center = Offset(cx, groundTop + 22f)
            )
            drawCircle(
                color = grassDarkColor.copy(alpha = 0.25f),
                radius = scallopWidth * 0.25f,
                center = Offset(cx, groundTop + 22f)
            )
        }
    }

    private fun DrawScope.drawBird(engine: GameEngine) {
        val cx = engine.birdX
        val cy = engine.birdY
        val r = engine.birdRadius
        val skin = engine.selectedSkin

        rotate(degrees = engine.birdRotation, pivot = Offset(cx, cy)) {
            // Soft drop shadow
            drawCircle(
                color = Color(0x33000000),
                radius = r * 0.95f,
                center = Offset(cx + 4f, cy + 8f)
            )

            // Tail feathers
            val tailPath = Path().apply {
                moveTo(cx - r * 0.7f, cy - r * 0.15f)
                lineTo(cx - r * 1.35f, cy - r * 0.4f)
                lineTo(cx - r * 1.1f, cy + r * 0.1f)
                lineTo(cx - r * 1.4f, cy + r * 0.35f)
                lineTo(cx - r * 0.65f, cy + r * 0.4f)
                close()
            }
            drawPath(tailPath, color = skin.wingColor)

            // Chubby Main Body
            drawCircle(
                color = skin.primaryColor,
                radius = r,
                center = Offset(cx, cy)
            )

            // Belly highlight / soft underbelly
            val bellyPath = Path().apply {
                moveTo(cx - r * 0.4f, cy + r * 0.2f)
                quadraticTo(cx + r * 0.2f, cy - r * 0.1f, cx + r * 0.7f, cy + r * 0.3f)
                quadraticTo(cx + r * 0.4f, cy + r * 0.95f, cx - r * 0.2f, cy + r * 0.85f)
                close()
            }
            drawPath(bellyPath, color = skin.bellyColor)

            // Rosy Cheek
            drawCircle(
                color = skin.blushColor.copy(alpha = 0.85f),
                radius = r * 0.25f,
                center = Offset(cx + r * 0.28f, cy + r * 0.35f)
            )

            // Wing with flapping animation
            rotate(degrees = engine.wingAngle, pivot = Offset(cx - r * 0.25f, cy + r * 0.1f)) {
                val wingPath = Path().apply {
                    moveTo(cx - r * 0.5f, cy - r * 0.1f)
                    quadraticTo(cx + r * 0.1f, cy - r * 0.55f, cx + r * 0.25f, cy + r * 0.25f)
                    quadraticTo(cx - r * 0.2f, cy + r * 0.65f, cx - r * 0.65f, cy + r * 0.25f)
                    close()
                }
                drawPath(wingPath, color = skin.wingColor)

                // Wing inner feather accent
                drawCircle(
                    color = skin.primaryColor.copy(alpha = 0.9f),
                    radius = r * 0.24f,
                    center = Offset(cx - r * 0.2f, cy + r * 0.1f)
                )
            }

            // Big expressive cartoon eye
            val eyeCenter = Offset(cx + r * 0.36f, cy - r * 0.22f)
            val eyeRadius = r * 0.36f

            // Eye White
            drawCircle(color = Color.White, radius = eyeRadius, center = eyeCenter)

            // Eye pupil / Iris
            val pupilOffset = Offset(
                eyeCenter.x + eyeRadius * 0.25f,
                eyeCenter.y + (if (engine.birdVy > 300f) eyeRadius * 0.2f else -eyeRadius * 0.1f)
            )

            // Draw pupil if eye open
            if (engine.eyeOpenRatio > 0.3f) {
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = eyeRadius * 0.65f,
                    center = pupilOffset
                )

                // Specular eye sparkles
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.26f,
                    center = Offset(pupilOffset.x - eyeRadius * 0.18f, pupilOffset.y - eyeRadius * 0.18f)
                )
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.13f,
                    center = Offset(pupilOffset.x + eyeRadius * 0.18f, pupilOffset.y + eyeRadius * 0.18f)
                )
            } else {
                // Eye blink: cute curved arc line
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(eyeCenter.x - eyeRadius * 0.8f, eyeCenter.y),
                    end = Offset(eyeCenter.x + eyeRadius * 0.8f, eyeCenter.y),
                    strokeWidth = 6f
                )
            }

            // Cute Orange Beak
            val beakTop = Path().apply {
                moveTo(cx + r * 0.65f, cy - r * 0.12f)
                lineTo(cx + r * 1.35f, cy + r * 0.12f)
                lineTo(cx + r * 0.65f, cy + r * 0.18f)
                close()
            }
            drawPath(beakTop, color = skin.beakColor)

            val beakBottom = Path().apply {
                moveTo(cx + r * 0.65f, cy + r * 0.16f)
                lineTo(cx + r * 1.15f, cy + r * 0.26f)
                lineTo(cx + r * 0.62f, cy + r * 0.38f)
                close()
            }
            drawPath(beakBottom, color = Color(0xFFEA580C))
        }
    }

    private fun DrawScope.drawParticle(p: Particle) {
        val color = p.color.copy(alpha = p.alpha)
        when (p.type) {
            ParticleType.FEATHER -> {
                rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                    val halfSize = p.size * 0.5f
                    val path = Path().apply {
                        moveTo(p.x - halfSize * 0.4f, p.y - halfSize)
                        quadraticTo(p.x + halfSize, p.y, p.x, p.y + halfSize)
                        quadraticTo(p.x - halfSize, p.y, p.x - halfSize * 0.4f, p.y - halfSize)
                        close()
                    }
                    drawPath(path, color = color)
                }
            }
            ParticleType.STAR -> {
                rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                    val r = p.size
                    val innerR = r * 0.45f
                    val path = Path().apply {
                        for (i in 0 until 5) {
                            val outerAngle = i * (2 * PI / 5) - PI / 2
                            val innerAngle = outerAngle + PI / 5
                            val ox = p.x + (cos(outerAngle) * r).toFloat()
                            val oy = p.y + (sin(outerAngle) * r).toFloat()
                            val ix = p.x + (cos(innerAngle) * innerR).toFloat()
                            val iy = p.y + (sin(innerAngle) * innerR).toFloat()
                            if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                            lineTo(ix, iy)
                        }
                        close()
                    }
                    drawPath(path, color = color)
                }
            }
            ParticleType.SPARKLE -> {
                rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                    val s = p.size
                    // Diamond sparkle
                    val path = Path().apply {
                        moveTo(p.x, p.y - s)
                        lineTo(p.x + s * 0.35f, p.y)
                        lineTo(p.x, p.y + s)
                        lineTo(p.x - s * 0.35f, p.y)
                        close()
                    }
                    drawPath(path, color = color)
                    val crossPath = Path().apply {
                        moveTo(p.x - s, p.y)
                        lineTo(p.x, p.y + s * 0.35f)
                        lineTo(p.x + s, p.y)
                        lineTo(p.x, p.y - s * 0.35f)
                        close()
                    }
                    drawPath(crossPath, color = color)
                }
            }
            ParticleType.POP -> {
                drawCircle(color, p.size, Offset(p.x, p.y), style = Stroke(width = 3f))
            }
        }
    }
}
