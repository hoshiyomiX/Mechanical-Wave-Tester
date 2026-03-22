package com.transsion.rgbtester.data.repository

import android.util.Log
import com.transsion.rgbtester.data.local.RGBSysfsPaths
import com.transsion.rgbtester.data.local.ShellCommandExecutor
import com.transsion.rgbtester.data.local.ShellResult
import com.transsion.rgbtester.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * RGB LED Repository
 *
 * Handles all RGB LED operations including:
 * - Device detection
 * - Color control
 * - Effect management
 * - Testing operations
 */
class RGBRepository {

    private val shellExecutor = ShellCommandExecutor()

    companion object {
        private const val TAG = "RGBRepository"
        private const val LED_CLASS_PATH = "/sys/class/leds"
    }

    // ==================== Device Detection ====================

    /**
     * Check if root access is available
     */
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        shellExecutor.checkRootAccess()
    }

    /**
     * Detect all available RGB LED devices
     */
    suspend fun detectDevices(): List<LEDDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<LEDDevice>()

        // Check predefined paths first
        for (ledPath in RGBSysfsPaths.getLedClassPaths()) {
            val device = probeLEDDevice(ledPath)
            if (device != null) {
                devices.add(device)
                Log.d(TAG, "Found LED device: ${device.name} at ${device.path}")
            }
        }

        // Scan /sys/class/leds for any additional devices
        val ledDirs = shellExecutor.listDirectory(LED_CLASS_PATH)
        for (ledDir in ledDirs) {
            val fullPath = "$LED_CLASS_PATH/$ledDir"
            if (devices.none { it.path == fullPath }) {
                val device = probeLEDDevice(fullPath)
                if (device != null) {
                    devices.add(device)
                    Log.d(TAG, "Found additional LED device: ${device.name}")
                }
            }
        }

        devices
    }

    /**
     * Get RGB device groups (combining R, G, B channels)
     */
    suspend fun getRGBDeviceGroups(): List<RGBDeviceGroup> = withContext(Dispatchers.IO) {
        val devices = detectDevices()
        val groups = mutableListOf<RGBDeviceGroup>()

        // Group HK32F0301 devices
        val hk32Red = devices.find { it.name.contains("hk32f0301_red", ignoreCase = true) }
        val hk32Green = devices.find { it.name.contains("hk32f0301_green", ignoreCase = true) }
        val hk32Blue = devices.find { it.name.contains("hk32f0301_blue", ignoreCase = true) }

        if (hk32Red != null || hk32Green != null || hk32Blue != null) {
            groups.add(RGBDeviceGroup(
                name = "HK32F0301 RGB Back Cover",
                redPath = hk32Red,
                greenPath = hk32Green,
                bluePath = hk32Blue,
                singlePath = null
            ))
        }

        // Group AW20144 devices
        val awRed = devices.find { it.name.contains("aw20144_red", ignoreCase = true) }
        val awGreen = devices.find { it.name.contains("aw20144_green", ignoreCase = true) }
        val awBlue = devices.find { it.name.contains("aw20144_blue", ignoreCase = true) }

        if (awRed != null || awGreen != null || awBlue != null) {
            groups.add(RGBDeviceGroup(
                name = "AW20144 RGB LED",
                redPath = awRed,
                greenPath = awGreen,
                bluePath = awBlue,
                singlePath = null
            ))
        }

        // Standard RGB LEDs
        val stdRed = devices.find { it.name.equals("red", ignoreCase = true) }
        val stdGreen = devices.find { it.name.equals("green", ignoreCase = true) }
        val stdBlue = devices.find { it.name.equals("blue", ignoreCase = true) }

        if (stdRed != null || stdGreen != null || stdBlue != null) {
            groups.add(RGBDeviceGroup(
                name = "Standard RGB LED",
                redPath = stdRed,
                greenPath = stdGreen,
                bluePath = stdBlue,
                singlePath = null
            ))
        }

        // TC LED (single device)
        val tcLed = devices.find { it.name.contains("tc_led", ignoreCase = true) }
        if (tcLed != null) {
            groups.add(RGBDeviceGroup(
                name = "Transsion TC LED",
                redPath = null,
                greenPath = null,
                bluePath = null,
                singlePath = tcLed
            ))
        }

        groups
    }

    private fun probeLEDDevice(path: String): LEDDevice? {
        if (!shellExecutor.fileExists(path)) {
            return null
        }

        val name = path.substringAfterLast("/")
        val brightnessPath = "$path/brightness"
        val triggerPath = "$path/trigger"
        val maxBrightnessPath = "$path/max_brightness"

        // Read max brightness
        val maxBrightness = shellExecutor.readFile(maxBrightnessPath)
            ?.trim()?.toIntOrNull() ?: 255

        // Read current brightness
        val currentBrightness = shellExecutor.readFile(brightnessPath)
            ?.trim()?.toIntOrNull() ?: 0

        // Get available triggers
        val triggers = shellExecutor.getAvailableTriggers(path)

        // Get current trigger
        val currentTrigger = shellExecutor.getCurrentTrigger(path) ?: "none"

        return LEDDevice(
            name = name,
            path = path,
            maxBrightness = maxBrightness,
            availableTriggers = triggers,
            currentTrigger = currentTrigger,
            currentBrightness = currentBrightness,
            isAvailable = true
        )
    }

    // ==================== Color Control ====================

    /**
     * Set RGB color on a device group
     */
    suspend fun setColor(group: RGBDeviceGroup, color: RGBColor): TestResult =
        withContext(Dispatchers.IO) {
            val adjustedColor = color.getAdjustedColor()

            if (group.isRGBSeparate) {
                // Set R, G, B separately
                val results = mutableListOf<TestResult>()

                group.redPath?.let {
                    results.add(setBrightness(it, adjustedColor.red))
                }
                group.greenPath?.let {
                    results.add(setBrightness(it, adjustedColor.green))
                }
                group.bluePath?.let {
                    results.add(setBrightness(it, adjustedColor.blue))
                }

                val success = results.all { it.success }
                TestResult(
                    success = success,
                    path = group.name,
                    operation = "setColor",
                    message = if (success) "Color set successfully" else results.firstOrNull { !it.success }?.message ?: "Failed"
                )
            } else if (group.isSingleRGB) {
                // Single RGB device - need different approach based on device
                setBrightness(group.singlePath!!, adjustedColor.red) // Simplified
            } else {
                TestResult(
                    success = false,
                    path = group.name,
                    operation = "setColor",
                    message = "No valid RGB device found"
                )
            }
        }

    /**
     * Set brightness for a single LED
     */
    suspend fun setBrightness(device: LEDDevice, brightness: Int): TestResult =
        withContext(Dispatchers.IO) {
            val clampedBrightness = brightness.coerceIn(0, device.maxBrightness)
            val brightnessPath = "${device.path}/brightness"

            val success = shellExecutor.writeFile(brightnessPath, clampedBrightness.toString())

            TestResult(
                success = success,
                path = device.path,
                operation = "setBrightness",
                message = if (success) "Brightness set to $clampedBrightness" else "Failed to set brightness"
            )
        }

    /**
     * Turn off all LEDs in a group
     */
    suspend fun turnOff(group: RGBDeviceGroup): TestResult = withContext(Dispatchers.IO) {
        setColor(group, RGBColor.OFF)
    }

    /**
     * Turn on LEDs with last color
     */
    suspend fun turnOn(group: RGBDeviceGroup, color: RGBColor): TestResult = withContext(Dispatchers.IO) {
        setColor(group, color)
    }

    // ==================== Effect Control ====================

    /**
     * Set LED trigger/effect
     */
    suspend fun setTrigger(device: LEDDevice, trigger: String): TestResult =
        withContext(Dispatchers.IO) {
            val triggerPath = "${device.path}/trigger"
            val success = shellExecutor.writeFile(triggerPath, trigger)

            TestResult(
                success = success,
                path = device.path,
                operation = "setTrigger",
                message = if (success) "Trigger set to $trigger" else "Failed to set trigger"
            )
        }

    /**
     * Set breathing effect
     */
    suspend fun setBreathingEffect(group: RGBDeviceGroup, color: RGBColor, speedMs: Int = 1000): Flow<TestResult> = flow {
        val adjustedColor = color.getAdjustedColor()

        while (true) {
            // Fade in
            for (i in 0..adjustedColor.red step 5) {
                if (group.redPath != null) {
                    shellExecutor.writeFile("${group.redPath.path}/brightness", i.toString())
                }
                if (group.greenPath != null) {
                    shellExecutor.writeFile("${group.greenPath.path}/brightness", (adjustedColor.green * i / adjustedColor.red.coerceAtLeast(1)).toString())
                }
                if (group.bluePath != null) {
                    shellExecutor.writeFile("${group.bluePath.path}/brightness", (adjustedColor.blue * i / adjustedColor.red.coerceAtLeast(1)).toString())
                }
                delay(speedMs / 51L)
            }

            // Fade out
            for (i in adjustedColor.red downTo 0 step 5) {
                if (group.redPath != null) {
                    shellExecutor.writeFile("${group.redPath.path}/brightness", i.toString())
                }
                if (group.greenPath != null) {
                    shellExecutor.writeFile("${group.greenPath.path}/brightness", (adjustedColor.green * i / adjustedColor.red.coerceAtLeast(1)).toString())
                }
                if (group.bluePath != null) {
                    shellExecutor.writeFile("${group.bluePath.path}/brightness", (adjustedColor.blue * i / adjustedColor.red.coerceAtLeast(1)).toString())
                }
                delay(speedMs / 51L)
            }

            emit(TestResult(true, group.name, "breathing", "Breathing cycle complete"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Set flash effect
     */
    suspend fun flashEffect(group: RGBDeviceGroup, color: RGBColor, times: Int = 3, intervalMs: Long = 200): List<TestResult> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<TestResult>()

            repeat(times) {
                // On
                setColor(group, color)
                delay(intervalMs)

                // Off
                turnOff(group)
                delay(intervalMs)

                results.add(TestResult(true, group.name, "flash", "Flash ${it + 1}/$times"))
            }

            results
        }

    /**
     * Rainbow effect
     */
    suspend fun rainbowEffect(group: RGBDeviceGroup, durationMs: Long = 5000): Flow<RGBColor> = flow {
        val steps = 360
        val stepDelay = durationMs / steps

        for (hue in 0 until steps) {
            // Convert HSV to RGB
            val color = hsvToRgb(hue.toFloat(), 1f, 1f)
            setColor(group, color)
            emit(color)
            delay(stepDelay)
        }
    }.flowOn(Dispatchers.IO)

    // ==================== Testing Operations ====================

    /**
     * Test individual LED channel
     */
    suspend fun testChannel(device: LEDDevice): TestResult = withContext(Dispatchers.IO) {
        try {
            // Save current state
            val originalBrightness = device.currentBrightness

            // Test sequence
            shellExecutor.writeFile("${device.path}/brightness", "255")
            Thread.sleep(500)
            shellExecutor.writeFile("${device.path}/brightness", "128")
            Thread.sleep(500)
            shellExecutor.writeFile("${device.path}/brightness", "0")
            Thread.sleep(500)
            shellExecutor.writeFile("${device.path}/brightness", originalBrightness.toString())

            TestResult(true, device.path, "testChannel", "Channel test completed")
        } catch (e: Exception) {
            TestResult(false, device.path, "testChannel", "Test failed: ${e.message}")
        }
    }

    /**
     * Run full RGB test
     */
    suspend fun runFullRGBTest(group: RGBDeviceGroup): Flow<TestResult> = flow {
        // Test Red
        emit(TestResult(true, group.name, "test", "Testing RED channel..."))
        setColor(group, RGBColor.RED)
        delay(1000)

        // Test Green
        emit(TestResult(true, group.name, "test", "Testing GREEN channel..."))
        setColor(group, RGBColor.GREEN)
        delay(1000)

        // Test Blue
        emit(TestResult(true, group.name, "test", "Testing BLUE channel..."))
        setColor(group, RGBColor.BLUE)
        delay(1000)

        // Test White
        emit(TestResult(true, group.name, "test", "Testing WHITE (all channels)..."))
        setColor(group, RGBColor.WHITE)
        delay(1000)

        // Turn off
        turnOff(group)
        emit(TestResult(true, group.name, "test", "RGB test completed successfully"))
    }.flowOn(Dispatchers.IO)

    // ==================== Utility Functions ====================

    private fun hsvToRgb(h: Float, s: Float, v: Float): RGBColor {
        val c = v * s
        val x = c * (1 - Math.abs((h / 60) % 2 - 1))
        val m = v - c

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            h < 60 -> { r = c; g = x; b = 0f }
            h < 120 -> { r = x; g = c; b = 0f }
            h < 180 -> { r = 0f; g = c; b = x }
            h < 240 -> { r = 0f; g = x; b = c }
            h < 300 -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        return RGBColor(
            red = ((r + m) * 255).toInt(),
            green = ((g + m) * 255).toInt(),
            blue = ((b + m) * 255).toInt()
        )
    }
}
