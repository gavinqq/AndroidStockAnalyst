#!/bin/bash
# Build a debug APK. Android Gradle Plugin requires Java 17 or newer.

set -e
cd "$(dirname "$0")"

# Prefer a known JDK 17, but allow any installed JDK >= 17.
JAVA_HOME_CANDIDATE=""
if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
  JAVA_HOME_CANDIDATE="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
  JAVA_HOME_CANDIDATE="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  JAVA_HOME_CANDIDATE="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_HOME_CANDIDATE="$(/usr/libexec/java_home 2>/dev/null || true)"
fi

if [ -n "$JAVA_HOME_CANDIDATE" ] && [ -x "$JAVA_HOME_CANDIDATE/bin/java" ]; then
  export JAVA_HOME="$JAVA_HOME_CANDIDATE"
elif command -v java >/dev/null 2>&1; then
  JAVA_HOME_CANDIDATE=""
else
  echo "未检测到 Java。Android Gradle Plugin 需要 JDK 17 或更高版本。"
  echo "请安装后重试："
  echo "  brew install openjdk@17"
  echo "然后再次运行：./build-apk.sh"
  exit 1
fi

JAVA_VERSION_OUTPUT="$(java -version 2>&1 | head -n 1)"
JAVA_MAJOR="$(printf "%s" "$JAVA_VERSION_OUTPUT" | sed -E 's/.*version "([0-9]+).*/\1/')"

if ! printf "%s" "$JAVA_MAJOR" | grep -Eq '^[0-9]+$' || [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "当前 Java 版本不支持构建：$JAVA_VERSION_OUTPUT"
  echo "Android Gradle Plugin 需要 JDK 17 或更高版本。"
  exit 1
fi

echo "Using Java: $JAVA_VERSION_OUTPUT"
if [ -n "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME=$JAVA_HOME"
fi

echo "Building debug APK..."
./gradlew --no-daemon assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
  echo ""
  echo "APK 已生成: $APK_PATH"
  echo "文件大小: $(du -h "$APK_PATH" | cut -f1)"
else
  echo "构建可能失败，未找到 $APK_PATH"
  exit 1
fi
