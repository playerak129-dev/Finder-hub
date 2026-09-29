package com.bhenx.finder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bhenx.finder.bluetooth.BhenxBleProtocol
import com.bhenx.finder.bluetooth.RssiSmoother
import com.bhenx.finder.data.LanguagePreference
import com.bhenx.finder.data.SettingsRepository
import com.bhenx.finder.data.ThemePreference
import com.bhenx.finder.model.BhenxDeviceIdentity
import com.bhenx.finder.model.DeviceInfo
import com.bhenx.finder.model.ProximityLevel
import com.bhenx.finder.model.SearchHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BhenxFinderTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun testDefaultSettings() {
        assertEquals("BHENX FINDER", settingsRepository.deviceName.value)
        assertEquals(ThemePreference.DARK, settingsRepository.theme.value)
        assertEquals(LanguagePreference.FRENCH, settingsRepository.language.value)
        assertTrue(settingsRepository.notificationsEnabled.value)
    }

    @Test
    fun testUpdateDeviceName() {
        settingsRepository.setDeviceName("Mon Téléphone Pro")
        assertEquals("Mon Téléphone Pro", settingsRepository.deviceName.value)

        // Blank should revert to default
        settingsRepository.setDeviceName("   ")
        assertEquals(SettingsRepository.DEFAULT_DEVICE_NAME, settingsRepository.deviceName.value)
    }

    @Test
    fun testUpdateTheme() {
        settingsRepository.setTheme(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, settingsRepository.theme.value)

        settingsRepository.setTheme(ThemePreference.SYSTEM)
        assertEquals(ThemePreference.SYSTEM, settingsRepository.theme.value)
    }

    @Test
    fun testUpdateLanguage() {
        settingsRepository.setLanguage(LanguagePreference.ENGLISH)
        assertEquals(LanguagePreference.ENGLISH, settingsRepository.language.value)

        settingsRepository.setLanguage(LanguagePreference.CREOLE)
        assertEquals(LanguagePreference.CREOLE, settingsRepository.language.value)
    }

    @Test
    fun testUpdateNotifications() {
        settingsRepository.setNotificationsEnabled(false)
        assertFalse(settingsRepository.notificationsEnabled.value)
    }

    @Test
    fun testRssiSmoother() {
        val smoother = RssiSmoother(windowSize = 4)
        assertEquals(null, smoother.getSmoothed())

        // Premier échantillon : moyenne = -70
        val s1 = smoother.addSample(-70)
        assertEquals(-70, s1)

        // Deuxième échantillon : (-70 + -60) / 2 = -65
        val s2 = smoother.addSample(-60)
        assertEquals(-65, s2)

        // Troisième & quatrième échantillons
        smoother.addSample(-62)
        val s4 = smoother.addSample(-64)
        // (-70 + -60 + -62 + -64) / 4 = -256 / 4 = -64
        assertEquals(-64, s4)

        // Reset
        smoother.reset()
        assertEquals(null, smoother.getSmoothed())
    }

    @Test
    fun testProximityLevels() {
        assertEquals(ProximityLevel.VERY_CLOSE, ProximityLevel.fromRssi(-48))
        assertEquals(ProximityLevel.VERY_CLOSE, ProximityLevel.fromRssi(-55))
        assertEquals(ProximityLevel.NEAR, ProximityLevel.fromRssi(-56))
        assertEquals(ProximityLevel.NEAR, ProximityLevel.fromRssi(-68))
        assertEquals(ProximityLevel.MEDIUM, ProximityLevel.fromRssi(-69))
        assertEquals(ProximityLevel.MEDIUM, ProximityLevel.fromRssi(-78))
        assertEquals(ProximityLevel.FAR, ProximityLevel.fromRssi(-79))
        assertEquals(ProximityLevel.FAR, ProximityLevel.fromRssi(-88))
        assertEquals(ProximityLevel.VERY_WEAK, ProximityLevel.fromRssi(-89))
        assertEquals(ProximityLevel.VERY_WEAK, ProximityLevel.fromRssi(-100))

        // Vérification des durées d'animation : plus proche = intervalle plus court (pulsation plus rapide)
        assertTrue(ProximityLevel.VERY_CLOSE.pulseDurationMs < ProximityLevel.NEAR.pulseDurationMs)
        assertTrue(ProximityLevel.NEAR.pulseDurationMs < ProximityLevel.MEDIUM.pulseDurationMs)
        assertTrue(ProximityLevel.MEDIUM.pulseDurationMs < ProximityLevel.FAR.pulseDurationMs)
        assertTrue(ProximityLevel.FAR.pulseDurationMs < ProximityLevel.VERY_WEAK.pulseDurationMs)
    }

    @Test
    fun testBhenxBleProtocolAdvEncodeDecode() {
        val identity = BhenxDeviceIdentity(
            id = "BHX-ABC123",
            name = "Mon Phone"
        )
        val encoded = BhenxBleProtocol.encodeAdvData(identity)
        assertTrue(encoded.isNotEmpty())

        val decoded = BhenxBleProtocol.decodeAdvData(encoded)
        assertNotNull(decoded)
        assertEquals("BHX-ABC123", decoded?.first)
        assertEquals("Mon Phone", decoded?.second)
    }

    @Test
    fun testBhenxBleProtocolCommands() {
        val ringCmd = BhenxBleProtocol.createRingCommand("sess1234")
        val ringStr = String(ringCmd, StandardCharsets.UTF_8)
        assertTrue(ringStr.startsWith(BhenxBleProtocol.CMD_RING_PREFIX))
        assertTrue(ringStr.contains("sess1234"))

        val stopCmd = BhenxBleProtocol.createStopCommand("sess1234")
        val stopStr = String(stopCmd, StandardCharsets.UTF_8)
        assertTrue(stopStr.startsWith(BhenxBleProtocol.CMD_STOP_RING_PREFIX))
        assertTrue(stopStr.contains("sess1234"))

        // Test format ciblé avec identifiant
        val targetedRing = BhenxBleProtocol.createRingCommand("BHX-9988", "token987")
        val targetedRingStr = String(targetedRing, StandardCharsets.UTF_8)
        assertEquals("RING:BHX-9988:token987", targetedRingStr)

        val parsedRing = BhenxBleProtocol.parseRingCommand(targetedRingStr)
        assertNotNull(parsedRing)
        assertEquals("BHX-9988", parsedRing?.first)
        assertEquals("token987", parsedRing?.second)

        val targetedStop = BhenxBleProtocol.createStopCommand("BHX-9988", "token987")
        val targetedStopStr = String(targetedStop, StandardCharsets.UTF_8)
        assertEquals("STOP:BHX-9988:token987", targetedStopStr)

        val parsedStop = BhenxBleProtocol.parseStopCommand(targetedStopStr)
        assertNotNull(parsedStop)
        assertEquals("BHX-9988", parsedStop?.first)
        assertEquals("token987", parsedStop?.second)
    }

    @Test
    fun testBhenxDeviceIdentity() {
        val identity = BhenxDeviceIdentity.getOrCreate(context, "Téléphone Test")
        assertNotNull(identity.id)
        assertTrue(identity.id.startsWith("BHX-"))
        assertEquals("Téléphone Test", identity.name)

        // Doit rester persistant entre deux appels
        val identity2 = BhenxDeviceIdentity.getOrCreate(context, "Téléphone Test")
        assertEquals(identity.id, identity2.id)
    }

    @Test
    fun testDeviceInfoModel() {
        val device = DeviceInfo(
            name = "Pixel 8",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -65,
            isBonded = true,
            deviceType = "BLE",
            isBhenxDevice = true,
            bhenxId = "BHX-112233",
            proximityLevel = ProximityLevel.NEAR
        )
        assertEquals("Pixel 8", device.name)
        assertEquals("AA:BB:CC:DD:EE:FF", device.address)
        assertEquals(-65, device.rssi)
        assertTrue(device.isBonded)
        assertTrue(device.isBhenxDevice)
        assertEquals("BHX-112233", device.bhenxId)
        assertEquals(ProximityLevel.NEAR, device.proximityLevel)
    }

    @Test
    fun testSearchHistoryModel() {
        val timestamp = System.currentTimeMillis()
        val history = SearchHistory(
            id = 1,
            deviceName = "Mon téléphone",
            deviceAddress = "11:22:33:44:55:66",
            timestamp = timestamp,
            result = "Trouvé (fait sonner)"
        )
        assertEquals(1L, history.id)
        assertEquals("Mon téléphone", history.deviceName)
        assertEquals("11:22:33:44:55:66", history.deviceAddress)
        assertEquals("Trouvé (fait sonner)", history.result)
    }
}
