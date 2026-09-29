package com.bhenx.finder

import android.app.Application
import com.bhenx.finder.bluetooth.BhenxAdvertiser
import com.bhenx.finder.bluetooth.BhenxGattClient
import com.bhenx.finder.bluetooth.BhenxGattServer
import com.bhenx.finder.bluetooth.BhenxRingManager
import com.bhenx.finder.bluetooth.BluetoothManager
import com.bhenx.finder.bluetooth.BluetoothScanner
import com.bhenx.finder.data.HistoryRepository
import com.bhenx.finder.data.SettingsRepository
import com.bhenx.finder.data.database.AppDatabase
import com.bhenx.finder.model.BhenxDeviceIdentity
import com.bhenx.finder.service.BhenxTargetService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BhenxFinderApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var bluetoothManager: BluetoothManager
        private set

    lateinit var bluetoothScanner: BluetoothScanner
        private set

    lateinit var bhenxAdvertiser: BhenxAdvertiser
        private set

    lateinit var bhenxRingManager: BhenxRingManager
        private set

    lateinit var bhenxGattServer: BhenxGattServer
        private set

    lateinit var bhenxGattClient: BhenxGattClient
        private set

    private val _isTargetModeActive = MutableStateFlow(false)
    val isTargetModeActive: StateFlow<Boolean> = _isTargetModeActive.asStateFlow()

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        historyRepository = HistoryRepository(database.searchHistoryDao())
        settingsRepository = SettingsRepository(this)
        bluetoothManager = BluetoothManager(this)
        bluetoothScanner = BluetoothScanner(this, bluetoothManager)

        bhenxAdvertiser = BhenxAdvertiser(this, bluetoothManager)
        bhenxRingManager = BhenxRingManager(this)
        bhenxGattServer = BhenxGattServer(this, bhenxRingManager) { getDeviceIdentity() }
        bhenxGattClient = BhenxGattClient(this, bluetoothManager)
    }

    fun getDeviceIdentity(): BhenxDeviceIdentity {
        return BhenxDeviceIdentity.getOrCreate(this, settingsRepository.deviceName.value)
    }

    fun setTargetMode(active: Boolean) {
        _isTargetModeActive.value = active
        if (active) {
            try {
                BhenxTargetService.start(this)
            } catch (_: Exception) {
                // Secours en processus local si le service d'arrière-plan échoue
                val identity = getDeviceIdentity()
                bhenxAdvertiser.startAdvertising(identity)
                bhenxGattServer.start()
            }
        } else {
            try {
                BhenxTargetService.stop(this)
            } catch (_: Exception) {
                // Ignorer
            }
            bhenxAdvertiser.stopAdvertising()
            bhenxGattServer.stop()
            bhenxRingManager.stopRinging()
        }
    }
}
