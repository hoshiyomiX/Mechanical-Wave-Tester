# RGB Back Cover Tester

Android application for testing and controlling RGB back cover lighting on Infinix X6873 device.

## Features

- **Device Detection**: Automatically detects RGB LED devices via sysfs
- **Color Control**: Full RGB color picker with brightness adjustment
- **Preset Colors**: Quick access to common colors (Red, Green, Blue, etc.)
- **LED Effects**: Supports various effects:
  - Static (solid color)
  - Breathing (fade in/out)
  - Flash (blink)
  - Rainbow (color cycling)
- **RGB Testing**: Comprehensive testing for all RGB channels
- **Root Access**: Direct sysfs control via root shell commands

## Requirements

- **Android 8.0+** (API 26+)
- **Rooted device** (required for sysfs access)
- **Infinix X6873** or compatible device with RGB back cover LED

## Supported LED Controllers

Based on firmware analysis of Infinix X6873:

1. **HK32F0301** - Primary RGB back cover controller
   - I2C Address: 0x50
   - Compatible: `suijing,hk32f0301_led`

2. **AW20144** - RGB LED driver IC
   - Firmware: `aw20144_all_rgb_on.bin`, `aw20144_all_rgb_off.bin`

3. **TC LED Framework** - Transsion custom LED framework
   - Kernel modules: `tc_led.ko`, `tc_led_class.ko`

## Sysfs Paths

The app controls RGB LEDs through these sysfs paths:

```
# HK32F0301 RGB channels
/sys/class/leds/hk32f0301_red/brightness
/sys/class/leds/hk32f0301_green/brightness
/sys/class/leds/hk32f0301_blue/brightness

# Standard LED paths
/sys/class/leds/red/brightness
/sys/class/leds/green/brightness
/sys/class/leds/blue/brightness
```

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture:

```
app/src/main/java/com/transsion/rgbtester/
├── data/
│   ├── local/           # Local data sources
│   │   ├── RGBSysfsPaths.kt      # Sysfs path definitions
│   │   └── ShellCommandExecutor.kt  # Root shell command execution
│   ├── model/           # Data models
│   │   └── RGBModels.kt # RGB color, LED state models
│   └── repository/      # Data repository
│       └── RGBRepository.kt  # RGB operations repository
├── ui/
│   ├── main/            # Main screen
│   │   └── MainActivity.kt
│   ├── effects/         # Effects screen
│   └── settings/        # Settings screen
├── viewmodel/
│   └── RGBViewModel.kt  # Main ViewModel
└── RGBTesterApp.kt      # Application class
```

## Usage

### 1. Grant Root Access
When prompted, grant root access to the application.

### 2. Select Device
Choose the RGB device you want to control from the dropdown.

### 3. Set Color
- Tap the color preview to open color picker
- Or tap preset color buttons for quick selection

### 4. Adjust Brightness
Use the brightness slider to adjust LED intensity (0-100%).

### 5. Apply Effects
Select from available effects:
- **Static**: Solid color
- **Breathing**: Fade in/out effect
- **Flash**: Blink effect
- **Rainbow**: Color cycling

### 6. Run Tests
Use the "Run Test" button to verify RGB functionality:
- Tests each color channel (R, G, B)
- Tests white (all channels)
- Tests various effects

## Building

```bash
# Clone the project
cd RGBBackCoverTester

# Build with Gradle
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## Troubleshooting

### Root Access Denied
- Ensure your device is properly rooted
- Check if Magisk/SuperSU is installed correctly
- Grant root permission when prompted

### Device Not Detected
- Refresh device list from menu
- Check if LED modules are loaded:
  ```bash
  lsmod | grep -E "led|hk32|aw"
  ```
- Manually load modules:
  ```bash
  modprobe leds-hk32f0301
  ```

### Cannot Write to Sysfs
- Check SELinux status:
  ```bash
  getenforce
  ```
- Set to permissive if needed:
  ```bash
  setenforce 0
  ```

### Color Not Changing
- Verify device path exists:
  ```bash
  ls -la /sys/class/leds/
  ```
- Check permissions:
  ```bash
  ls -la /sys/class/leds/hk32f0301_red/
  ```

## Technical Notes

### HK32F0301 LED Controller

The HK32F0301 is an MCU-based RGB LED controller with:
- I2C interface (address 0x50)
- Programmable RGB color output
- MTP (Memory) for storing patterns
- Support for multiple firmware versions

Device tree compatible string: `suijing,hk32f0301_led`

### Kernel Modules

Required kernel modules for RGB control:
- `leds-hk32f0301.ko` - HK32F0301 driver
- `tc_led.ko` - Transsion LED framework
- `tc_led_class.ko` - LED class support
- `aw86224_light.ko` - Haptic/light driver

## License

This project is for educational and testing purposes.

## Device Info

- **Target Device**: Infinix X6873
- **Firmware**: X6873-15.0.3.116SP01
- **Platform**: MediaTek MT6897
- **Android Version**: 15
