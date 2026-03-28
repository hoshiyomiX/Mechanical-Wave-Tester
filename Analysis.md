# Infinix X6873 RGB Back Cover LED Analysis

> **Device:** Infinix GT 30 Pro (X6873)
> **Platform:** MediaTek MT6897 (Dimensity 8350 Ultimate)
> **Android:** 15 (AP3A.240905.015.A2)
> **TranOS:** X6873-15.0.3.116SP01

---

## 📋 Project Overview

This repository contains comprehensive analysis of the Infinix X6873 RGB back cover lighting system, including:

1. **Hardware Architecture Documentation** - LED controller components and design
2. **Software Stack Analysis** - HAL, drivers, and framework details
3. **RGB vs White LED Variant Investigation** - Hardware vs Software limitation analysis
4. **Android Testing APK** - Tool for manual LED testing

---

## 🔬 Key Finding: RGB vs White LED Variant

> **CONCLUSION: The difference between RGB and White LED variants is a HARDWARE limitation, NOT a software lock.**

| Aspect | Hardware | Software | Notes |
|--------|----------|----------|-------|
| Firmware Support | ❌ | ✅ | RGB firmware exists in system |
| Driver Support | ❌ | ✅ | leds-hk32f0301.ko supports RGB |
| Device Tree Config | ❌ | ✅ | RGB config present in DTS |
| Physical LED Component | ✅ | ❌ | **Different LED chips** |
| MTP Program Memory | ✅ | ❌ | **Different MCU memory sizes** |
| PCB Traces | ✅ | ❌ | **RGB needs 3 channels** |

**It is NOT possible to convert a White LED variant to RGB through software modification.**

---

## 📁 Repository Contents

```
Infinix-X6873-RGB-Analysis/
├── README.md                    # This documentation
├── RGBBackCoverTester/          # Android APK for LED testing
│   ├── app/                     # Application source code
│   ├── .github/workflows/       # GitHub Actions CI/CD
│   └── README.md                # APK documentation
└── docs/                        # Additional documentation
    └── ANALYSIS_REPORT.md       # Detailed technical analysis
```

---

## 🔧 Hardware Components

### Primary LED Controller: HK32F0301 MCU

| Property | Value |
|----------|-------|
| I2C Address | 0x50 |
| Bus | I2C13 |
| Compatible | "suijing,hk32f0301_led" |
| Enable GPIO | GPIO 25 |
| Max Brightness | 255 |
| Max Current | 8mA per channel |

**Three Firmware Variants:**
- White LED only (fwversion: 0x0E)
- RGB variant 1 (fwversion: 0x0D)
- RGB variant 2 (fwversion: 0x08)

### Secondary Controller: AW20144 RGB LED Driver

Dedicated RGB LED driver IC from Awinic Technology.

---

## 📊 Software Stack

```
Android Framework (LightsManager)
        │
        ▼
MediaTek Light HAL (android.hardware.lights-service.mediatek)
        │
        ▼
Transsion TC LED Framework (tc_led.ko)
        │
        ▼
HK32F0301 Driver (leds-hk32f0301.ko)
        │
        ▼
Sysfs Interface (/sys/class/leds/*/)
```

---

## 🛠️ LED Control via Sysfs

```bash
# Set RGB color (requires root)
echo 255 > /sys/class/leds/backcover-red/brightness
echo 128 > /sys/class/leds/backcover-green/brightness
echo 64 > /sys/class/leds/backcover-blue/brightness

# Turn off
echo 0 > /sys/class/leds/*/brightness

# Check variant
cat /sys/bus/i2c/devices/13-0050/fwversion
```

---

## 📱 Testing APK

The `RGBBackCoverTester` APK provides:
- Device variant detection (RGB vs White)
- Color picker for RGB selection
- Brightness slider control
- Preset effects (Static, Breathing, Flash, Rainbow)
- Individual RGB channel testing

**Requirements:**
- Android 8.0+ (API 26)
- Root access
- X6873 device

See [RGBBackCoverTester/README.md](RGBBackCoverTester/README.md) for details.

---

## 📋 Quick Reference

### Kernel Modules

| Module | Function |
|--------|----------|
| leds-hk32f0301.ko | Main LED controller driver |
| tc_led.ko | Transsion LED framework |
| tc_led_class.ko | LED class interface |

### Init Files

```
/vendor/etc/init/lights-mtk-default.rc
```

### Firmware Files

```
/vendor/firmware/aw20144_all_rgb_on.bin
/vendor/firmware/aw20144_all_rgb_off.bin
```

---

## 🔗 Related Resources

- [HK32F0301 Datasheet](https://www.hsxp.com/) - Hangzhou Kelixin Technology
- [AW20144 Datasheet](https://www.awinic.com/) - Awinic Technology
- [MediaTek MT6897 Platform](https://www.mediatek.com/)

---

## 📄 License

This project is for educational and research purposes. Use at your own risk.

---

*Last Updated: 2026-03-22*
