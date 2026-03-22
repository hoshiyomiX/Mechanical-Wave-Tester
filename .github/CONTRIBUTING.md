# Contributing to Mechanical Wave Tester

## Development Setup

1. **Prerequisites**
   - Android Studio Hedgehog (2023.1.1) or later
   - JDK 17
   - Android SDK 34
   - Gradle 8.5

2. **Clone the repository**
   ```bash
   git clone https://github.com/hoshiyomiX/Mechanical-Wave-Tester.git
   cd Mechanical-Wave-Tester
   ```

3. **Open in Android Studio**
   - File → Open → Select project directory
   - Wait for Gradle sync to complete

4. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

## Project Structure

```
app/src/main/java/com/transsion/rgbtester/
├── data/
│   ├── local/           # Local data sources (sysfs, shell)
│   ├── model/           # Data models
│   └── repository/      # Data repository
├── ui/                  # UI activities and fragments
├── viewmodel/           # ViewModels
└── RGBTesterApp.kt      # Application class
```

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names
- Add KDoc comments for public APIs

## Testing

Before submitting a PR, ensure:
- [ ] Code compiles without errors
- [ ] No lint warnings
- [ ] Manual testing on target device
- [ ] README updated if needed

## Device Compatibility

Target device: **Infinix X6873**
- MediaTek MT6897 platform
- HK32F0301 RGB LED controller
- AW20144 RGB driver IC

## Questions?

Open an issue for any questions or discussions.
