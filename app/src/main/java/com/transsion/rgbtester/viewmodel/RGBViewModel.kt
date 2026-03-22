package com.transsion.rgbtester.viewmodel

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transsion.rgbtester.data.model.*
import com.transsion.rgbtester.data.repository.RGBRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * RGB Back Cover Tester ViewModel
 *
 * MVVM Architecture ViewModel for managing RGB LED state and operations
 */
class RGBViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RGBRepository()

    // ==================== State LiveData ====================

    private val _rootAvailable = MutableLiveData<Boolean>()
    val rootAvailable: LiveData<Boolean> = _rootAvailable

    private val _devices = MutableLiveData<List<LEDDevice>>()
    val devices: LiveData<List<LEDDevice>> = _devices

    private val _deviceGroups = MutableLiveData<List<RGBDeviceGroup>>()
    val deviceGroups: LiveData<List<RGBDeviceGroup>> = _deviceGroups

    private val _selectedGroup = MutableLiveData<RGBDeviceGroup?>()
    val selectedGroup: LiveData<RGBDeviceGroup?> = _selectedGroup

    private val _currentRGBColor = MutableLiveData<RGBColor>()
    val currentRGBColor: LiveData<RGBColor> = _currentRGBColor

    private val _brightness = MutableLiveData<Int>()
    val brightness: LiveData<Int> = _brightness

    private val _currentEffect = MutableLiveData<LEDEffect>()
    val currentEffect: LiveData<LEDEffect> = _currentEffect

    private val _isLedOn = MutableLiveData<Boolean>()
    val isLedOn: LiveData<Boolean> = _isLedOn

    private val _operationResult = MutableLiveData<TestResult>()
    val operationResult: LiveData<TestResult> = _operationResult

    private val _testProgress = MutableLiveData<String>()
    val testProgress: LiveData<String> = _testProgress

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Effect job for cancellation
    private var effectJob: Job? = null

    // Saved color when turning off
    private var savedColor: RGBColor = RGBColor.WHITE

    init {
        _currentRGBColor.value = RGBColor.WHITE
        _brightness.value = 100
        _currentEffect.value = LEDEffect.STATIC
        _isLedOn.value = false

        // Initialize device detection
        initialize()
    }

    // ==================== Initialization ====================

    fun initialize() {
        viewModelScope.launch {
            _isLoading.value = true

            // Check root access
            val hasRoot = repository.checkRootAccess()
            _rootAvailable.value = hasRoot

            if (!hasRoot) {
                _errorMessage.value = "Root access not available. Some features may not work."
            }

            // Detect devices
            detectDevices()

            _isLoading.value = false
        }
    }

    fun detectDevices() {
        viewModelScope.launch {
            _isLoading.value = true

            val deviceList = repository.detectDevices()
            _devices.value = deviceList

            val groups = repository.getRGBDeviceGroups()
            _deviceGroups.value = groups

            // Auto-select first available group
            if (groups.isNotEmpty() && _selectedGroup.value == null) {
                selectDeviceGroup(groups.first())
            }

            _isLoading.value = false
        }
    }

    // ==================== Device Selection ====================

    fun selectDeviceGroup(group: RGBDeviceGroup) {
        _selectedGroup.value = group
        _operationResult.value = TestResult(
            success = true,
            path = group.name,
            operation = "select",
            message = "Selected: ${group.name}"
        )
    }

    // ==================== Color Control ====================

    fun setColor(color: RGBColor) {
        val currentBrightness = _brightness.value ?: 100
        val colorWithBrightness = color.copy(brightness = currentBrightness)

        _currentRGBColor.value = colorWithBrightness
        savedColor = colorWithBrightness

        applyColor(colorWithBrightness)
    }

    fun setColorFromAndroidColor(color: Int) {
        val rgb = RGBColor(
            red = Color.red(color),
            green = Color.green(color),
            blue = Color.blue(color)
        )
        setColor(rgb)
    }

    fun setBrightness(value: Int) {
        _brightness.value = value

        val currentColor = _currentRGBColor.value ?: return
        val newColor = currentColor.copy(brightness = value)
        _currentRGBColor.value = newColor

        if (_isLedOn.value == true) {
            applyColor(newColor)
        }
    }

    private fun applyColor(color: RGBColor) {
        val group = _selectedGroup.value ?: return

        viewModelScope.launch {
            val result = repository.setColor(group, color)
            _operationResult.value = result

            if (result.success) {
                _isLedOn.value = color.red > 0 || color.green > 0 || color.blue > 0
            }
        }
    }

    // ==================== Power Control ====================

    fun turnOn() {
        val color = savedColor.takeIf { it.red > 0 || it.green > 0 || it.blue > 0 }
            ?: RGBColor.WHITE

        setColor(color)
        _isLedOn.value = true
    }

    fun turnOff() {
        val group = _selectedGroup.value ?: return

        viewModelScope.launch {
            val result = repository.turnOff(group)
            _operationResult.value = result
            _isLedOn.value = false
        }
    }

    fun togglePower() {
        if (_isLedOn.value == true) {
            turnOff()
        } else {
            turnOn()
        }
    }

    // ==================== Effect Control ====================

    fun setEffect(effect: LEDEffect) {
        _currentEffect.value = effect

        // Cancel any running effect
        effectJob?.cancel()

        when (effect) {
            LEDEffect.OFF -> turnOff()
            LEDEffect.STATIC -> {
                val color = _currentRGBColor.value ?: RGBColor.WHITE
                setColor(color)
            }
            LEDEffect.BREATHING -> startBreathingEffect()
            LEDEffect.FLASH -> startFlashEffect()
            LEDEffect.RAINBOW -> startRainbowEffect()
            else -> {
                // Try to use system trigger
                setSystemTrigger(effect.triggerValue)
            }
        }
    }

    private fun setSystemTrigger(trigger: String) {
        val group = _selectedGroup.value ?: return

        viewModelScope.launch {
            group.redPath?.let { repository.setTrigger(it, trigger) }
            group.greenPath?.let { repository.setTrigger(it, trigger) }
            group.bluePath?.let { repository.setTrigger(it, trigger) }
            group.singlePath?.let { repository.setTrigger(it, trigger) }

            _operationResult.value = TestResult(
                success = true,
                path = group.name,
                operation = "setTrigger",
                message = "Trigger set to $trigger"
            )
        }
    }

    private fun startBreathingEffect() {
        val group = _selectedGroup.value ?: return
        val color = _currentRGBColor.value ?: RGBColor.WHITE

        effectJob = viewModelScope.launch {
            repository.setBreathingEffect(group, color, 2000)
                .catch { e ->
                    _errorMessage.value = "Breathing effect error: ${e.message}"
                }
                .collect { result ->
                    _operationResult.value = result
                }
        }
    }

    private fun startFlashEffect() {
        val group = _selectedGroup.value ?: return
        val color = _currentRGBColor.value ?: RGBColor.WHITE

        effectJob = viewModelScope.launch {
            val results = repository.flashEffect(group, color, 5, 300)
            results.lastOrNull()?.let {
                _operationResult.value = it
                _isLedOn.value = false
            }
        }
    }

    private fun startRainbowEffect() {
        val group = _selectedGroup.value ?: return

        effectJob = viewModelScope.launch {
            repository.rainbowEffect(group, 10000)
                .catch { e ->
                    _errorMessage.value = "Rainbow effect error: ${e.message}"
                }
                .collect { color ->
                    _currentRGBColor.value = color
                }
        }
    }

    fun stopEffect() {
        effectJob?.cancel()
        effectJob = null
        setEffect(LEDEffect.STATIC)
    }

    // ==================== Testing ====================

    fun runRGBTest() {
        val group = _selectedGroup.value ?: return

        viewModelScope.launch {
            _testProgress.value = "Starting RGB test..."

            repository.runFullRGBTest(group)
                .catch { e ->
                    _testProgress.value = "Test failed: ${e.message}"
                    _errorMessage.value = e.message
                }
                .collect { result ->
                    _testProgress.value = result.message
                    _operationResult.value = result
                }

            _testProgress.value = "RGB test completed"
            _isLedOn.value = false
        }
    }

    fun testChannel(device: LEDDevice) {
        viewModelScope.launch {
            _testProgress.value = "Testing ${device.displayName}..."

            val result = repository.testChannel(device)
            _operationResult.value = result
            _testProgress.value = if (result.success) "Channel test passed" else "Channel test failed"
        }
    }

    // ==================== Presets ====================

    fun applyPreset(color: RGBColor) {
        setColor(color)
        setEffect(LEDEffect.STATIC)
    }

    fun getPresets(): List<Pair<String, RGBColor>> {
        return listOf(
            "Red" to RGBColor.RED,
            "Green" to RGBColor.GREEN,
            "Blue" to RGBColor.BLUE,
            "White" to RGBColor.WHITE,
            "Yellow" to RGBColor.YELLOW,
            "Cyan" to RGBColor.CYAN,
            "Magenta" to RGBColor.MAGENTA,
            "Orange" to RGBColor.ORANGE,
            "Purple" to RGBColor.PURPLE,
            "Pink" to RGBColor.PINK
        )
    }

    // ==================== Utility ====================

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        effectJob?.cancel()
    }
}
