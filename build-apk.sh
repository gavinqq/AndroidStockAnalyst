#!/bin/bash
# 使用 Java 17 构建 APK（Android Gradle Plugin 要求 JDK 17）
# 若未安装：brew install openjdk@17

set -e
cd "$(dirname "$0")"

# 优先使用 JDK 17（Homebrew 或 Android Studio 自带）
JAVA17_HOME=""
if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
  JAVA17_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
  JAVA17_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  JAVA17_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

if [ -n "$JAVA17_HOME" ]; then
  export JAVA_HOME="$JAVA17_HOME"
  echo "Using JAVA_HOME=$JAVA_HOME (JDK 17)"
  "$JAVA_HOME/bin/java" -version
else
  echo "未检测到 JDK 17。Android Gradle Plugin 需要 JDK 17 才能构建。"
  echo "请安装后重试："
  echo "  brew install openjdk@17"
  echo "然后再次运行：./build-apk.sh"
  exit 1
fi

echo "Building debug APK..."
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
  echo ""
  echo "APK 已生成: $APK_PATH"
  echo "文件大小: $(du -h "$APK_PATH" | cut -f1)"
else
  echo "构建可能失败，未找到 $APK_PATH"
  exit 1
fi
