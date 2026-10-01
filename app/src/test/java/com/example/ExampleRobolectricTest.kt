package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.model.MedalTier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Puffy Wings", appName)
    }

    @Test
    fun `verify medal tier awards`() {
        assertEquals(MedalTier.NONE, MedalTier.fromScore(2))
        assertEquals(MedalTier.BRONZE, MedalTier.fromScore(5))
        assertEquals(MedalTier.SILVER, MedalTier.fromScore(18))
        assertEquals(MedalTier.GOLD, MedalTier.fromScore(35))
        assertEquals(MedalTier.PLATINUM, MedalTier.fromScore(60))
    }
}
