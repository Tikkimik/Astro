#!/bin/bash

echo "🚀 Сборка Android APK с Java 17..."

# Устанавливаем Java 17 для Android сборки
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "✅ Используем Java 17 для Android сборки"
echo "JAVA_HOME: $JAVA_HOME"
echo "☕ Java версия: $(java -version 2>&1 | head -n 1)"

# Очищаем предыдущую сборку
echo "🧹 Очистка предыдущей сборки..."
./gradlew clean

# Собираем APK
echo "🔨 Сборка APK..."
./gradlew :android:assembleDebug

# Проверяем результат
if [ $? -eq 0 ]; then
    echo "✅ APK успешно собран!"
    echo "📱 Файл APK находится в: android/build/outputs/apk/debug/"
    ls -la android/build/outputs/apk/debug/
else
    echo "❌ Ошибка при сборке APK"
    exit 1
fi


