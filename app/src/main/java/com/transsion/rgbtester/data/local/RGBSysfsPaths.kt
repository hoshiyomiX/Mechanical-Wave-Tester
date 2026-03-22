package com.transsion.rgbtester.data.local

/**
 * RGB Back Cover Sysfs Paths for Infinix X6873
 *
 * Based on firmware analysis:
 * - HK32F0301 LED Controller at I2C 0x50
 * - AW20144 RGB LED Driver
 * - TC LED Framework (Transsion Custom)
 */
object RGBSysfsPaths {

    // ==================== HK32F0301 LED Controller ====================
    // Primary RGB back cover controller (I2C address 0x50)

    /**
     * Main HK32F0301 LED sysfs base path
     * Compatible: "suijing,hk32f0301_led"
     */
    const val HK32F0301_BASE = "/sys/class/leds/hk32f0301"

    /**
     * RGB brightness controls
     */
    const val HK32_RED_BRIGHTNESS = "/sys/class/leds/hk32f0301_red/brightness"
    const val HK32_GREEN_BRIGHTNESS = "/sys/class/leds/hk32f0301_green/brightness"
    const val HK32_BLUE_BRIGHTNESS = "/sys/class/leds/hk32f0301_blue/brightness"

    /**
     * RGB trigger (effect mode)
     */
    const val HK32_RED_TRIGGER = "/sys/class/leds/hk32f0301_red/trigger"
    const val HK32_GREEN_TRIGGER = "/sys/class/leds/hk32f0301_green/trigger"
    const val HK32_BLUE_TRIGGER = "/sys/class/leds/hk32f0301_blue/trigger"

    /**
     * Max brightness
     */
    const val HK32_MAX_BRIGHTNESS = "/sys/class/leds/hk32f0301/max_brightness"

    // ==================== TC LED Framework ====================
    // Transsion Custom LED Framework

    const val TC_LED_BASE = "/sys/class/leds/tc_led"
    const val TC_LED_BRIGHTNESS = "/sys/class/leds/tc_led/brightness"
    const val TC_LED_TRIGGER = "/sys/class/leds/tc_led/trigger"

    // ==================== Standard LED Paths ====================

    const val RED_LED_BRIGHTNESS = "/sys/class/leds/red/brightness"
    const val GREEN_LED_BRIGHTNESS = "/sys/class/leds/green/brightness"
    const val BLUE_LED_BRIGHTNESS = "/sys/class/leds/blue/brightness"

    const val RED_LED_TRIGGER = "/sys/class/leds/red/trigger"
    const val GREEN_LED_TRIGGER = "/sys/class/leds/green/trigger"
    const val BLUE_LED_TRIGGER = "/sys/class/leds/blue/trigger"

    // ==================== PDLC (Smart Back Cover) ====================

    const val PDLC_ENABLE = "/sys/class/leds/tc_pdlc/brightness"
    const val PDLC_MODE = "/sys/class/leds/tc_pdlc/mode"

    // ==================== AW20144 RGB LED ====================

    const val AW20144_BASE = "/sys/class/leds/aw20144"
    const val AW20144_RED = "/sys/class/leds/aw20144_red/brightness"
    const val AW20144_GREEN = "/sys/class/leds/aw20144_green/brightness"
    const val AW20144_BLUE = "/sys/class/leds/aw20144_blue/brightness"

    // ==================== Alternative Paths ====================
    // Different firmware versions may use different paths

    val ALTERNATIVE_RGB_PATHS = listOf(
        // Transsion custom paths
        "/sys/class/leds/rgb_led",
        "/sys/class/leds/backcover_rgb",
        "/sys/class/leds/back_cover_led",
        // Standard MTK paths
        "/sys/class/leds/lcd-backlight",
        "/sys/class/leds/lcd-backlight1",
        // I2C device paths
        "/sys/bus/i2c/devices/0-0050/leds",
        "/sys/bus/i2c/drivers/hk32f0301_led"
    )

    // ==================== Effect Modes ====================

    object TriggerModes {
        const val NONE = "none"
        const val TIMER = "timer"
        const val HEARTBEAT = "heartbeat"
        const val DEFAULT_ON = "default-on"
        const val TRANSIENT = "transient"
        const val BREATHING = "breathing"
        const val RAINBOW = "rainbow"
        const val FLASH = "flash"
        const val MUSIC = "music"
    }

    /**
     * Get all available LED paths on the device
     */
    fun getLedClassPaths(): List<String> = listOf(
        "/sys/class/leds/hk32f0301_red",
        "/sys/class/leds/hk32f0301_green",
        "/sys/class/leds/hk32f0301_blue",
        "/sys/class/leds/tc_led",
        "/sys/class/leds/red",
        "/sys/class/leds/green",
        "/sys/class/leds/blue",
        "/sys/class/leds/aw20144_red",
        "/sys/class/leds/aw20144_green",
        "/sys/class/leds/aw20144_blue"
    )
}
