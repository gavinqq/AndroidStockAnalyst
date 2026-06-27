# Android Stock Plan

An Android application that automatically monitors your download folder for stock holding images, analyzes them using ChatGPT, and generates execution plans for the next trading day.

## Features

- 📁 **Configurable Folder Monitoring**: Monitor any folder for new stock images
- 🤖 **ChatGPT Integration**: Automatic image analysis using GPT-4 Vision API
- 📊 **Execution Plans**: Generate detailed trading execution plans for multiple stock markets
- 🌍 **Multi-Market Support**: Configure countries for stock market analysis
- 🌐 **Multi-Language Support**: Generate execution plans in your preferred language
- 📱 **Background Service**: Continuous monitoring runs in the background
- 📜 **Plan History**: View and manage all generated execution plans
- 🔄 **Refresh Functionality**: Re-analyze the last detected image on demand

## Configuration

Before using the app, configure the following settings:

1. **Download Folder Path**: Set the folder to monitor for stock images
2. **Stock Market Countries**: Specify countries (e.g., "US,CN,HK,JP")
3. **ChatGPT API Key**: Your OpenAI API key with GPT-4 Vision access
4. **Execution Plan Language**: Language for generated plans (e.g., "English", "Chinese", "Japanese")

## Requirements

- Android 7.0 (API 24) or higher
- ChatGPT API key with GPT-4 Vision access
- Internet connection for API calls

## Permissions

- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES`: To monitor download folder
- `INTERNET`: For ChatGPT API calls
- `FOREGROUND_SERVICE`: For background monitoring

## How It Works

1. The app runs a background service that monitors your configured download folder
2. When a new image is detected, it checks if it looks like a stock holding image
3. If detected, the image is sent to ChatGPT for analysis
4. ChatGPT generates an execution plan based on the configured stock markets
5. The plan is saved with a datetime-based filename
6. Users can view the latest plan or browse history in the app

## Build

### 生成 APK

Android Gradle Plugin 需要 **JDK 17** 运行 Gradle。若系统只有 Java 25，请先安装 JDK 17：

```bash
# macOS (Homebrew)
brew install openjdk@17
```

**方式一：使用构建脚本（推荐）**

```bash
chmod +x build-apk.sh
./build-apk.sh
```

脚本会自动查找 JDK 17（Homebrew 或 Android Studio 自带），并执行 `./gradlew assembleDebug`。  
生成的 **Debug APK** 路径：`app/build/outputs/apk/debug/app-debug.apk`。

**方式二：手动指定 Java 17 后构建**

```bash
# 使用 Homebrew 安装的 JDK 17（Apple Silicon）
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

# 或 Intel Mac
# export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

./gradlew assembleDebug
```

**Release APK（签名需自行配置）：**

```bash
./gradlew assembleRelease
```

## License

This project is open source and available for personal use.
