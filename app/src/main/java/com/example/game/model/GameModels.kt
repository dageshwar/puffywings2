package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class GameStatus {
    READY,
    PLAYING,
    GAME_OVER
}

enum class BirdSkin(
    val displayName: String,
    val primaryColor: Color,
    val bellyColor: Color,
    val wingColor: Color,
    val blushColor: Color,
    val beakColor: Color
) {
    GOLDEN(
        displayName = "Sunny",
        primaryColor = Color(0xFFFACC15),
        bellyColor = Color(0xFFFEF08A),
        wingColor = Color(0xFFEAB308),
        blushColor = Color(0xFFF472B6),
        beakColor = Color(0xFFF97316)
    ),
    BLOSSOM(
        displayName = "Blossom",
        primaryColor = Color(0xFFF472B6),
        bellyColor = Color(0xFFFCE7F3),
        wingColor = Color(0xFFEC4899),
        blushColor = Color(0xFFFB7185),
        beakColor = Color(0xFFFB923C)
    ),
    AQUA(
        displayName = "Kiwi",
        primaryColor = Color(0xFF2DD4BF),
        bellyColor = Color(0xFFCCFBF1),
        wingColor = Color(0xFF0D9488),
        blushColor = Color(0xFFF472B6),
        beakColor = Color(0xFFF97316)
    ),
    VIOLET(
        displayName = "Starlight",
        primaryColor = Color(0xFFA855F7),
        bellyColor = Color(0xFFF3E8FF),
        wingColor = Color(0xFF7E22CE),
        blushColor = Color(0xFFF472B6),
        beakColor = Color(0xFFFB923C)
    )
}

enum class ParticleType {
    FEATHER,
    STAR,
    SPARKLE,
    POP
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var alpha: Float,
    var life: Float,
    val maxLife: Float,
    val type: ParticleType,
    var rotation: Float = 0f,
    var rotSpeed: Float = 0f
)

data class Obstacle(
    val id: Long,
    var x: Float,
    val gapCenterY: Float,
    val gapHeight: Float,
    val width: Float,
    var scored: Boolean = false,
    val themeColor: Color,
    val capColor: Color
)

data class Cloud(
    var x: Float,
    val y: Float,
    val scale: Float,
    val speed: Float,
    val alpha: Float
)

data class Hill(
    val heightRatio: Float,
    val color: Color
)

enum class MedalTier(val title: String, val badgeColor: Color, val ribbonColor: Color, val minScore: Int) {
    NONE("Beginner", Color(0xFF94A3B8), Color(0xFF64748B), 0),
    BRONZE("Bronze Wing", Color(0xFFCD7F32), Color(0xFF9A3412), 5),
    SILVER("Silver Crest", Color(0xFFE2E8F0), Color(0xFF475569), 15),
    GOLD("Gold Master", Color(0xFFFBBF24), Color(0xFFD97706), 30),
    PLATINUM("Sky Legend", Color(0xFF38BDF8), Color(0xFF0284C7), 50);

    companion object {
        fun fromScore(score: Int): MedalTier {
            return when {
                score >= PLATINUM.minScore -> PLATINUM
                score >= GOLD.minScore -> GOLD
                score >= SILVER.minScore -> SILVER
                score >= BRONZE.minScore -> BRONZE
                else -> NONE
            }
        }
    }
}
