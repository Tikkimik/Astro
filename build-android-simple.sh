#!/bin/bash

echo "🚀 Сборка Android APK с Java 17..."

# Экспортируем переменную для использования Java 17
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home

echo "✅ Используем Java 17 для Android сборки"
echo "JAVA_HOME: $JAVA_HOME"

# Проверяем наличие Java 17
if [ ! -d "$JAVA_HOME" ]; then
    echo "❌ Java 17 не найдена. Устанавливаем..."
    brew install openjdk@17
    export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/$(brew list --versions openjdk@17 | cut -d' ' -f2)/libexec/openjdk.jdk/Contents/Home
fi

echo "☕ Java версия: $($JAVA_HOME/bin/java -version 2>&1 | head -n 1)"

# Очищаем предыдущую сборку
echo "🧹 Очистка предыдущей сборки..."
gradle clean

# Собираем APK с системным Gradle
echo "🔨 Сборка APK..."
gradle :android:assembleDebug

# Проверяем результат
if [ $? -eq 0 ]; then
    echo "✅ APK успешно собран!"
    echo "📱 Файл APK находится в: android/build/outputs/apk/debug/"
    ls -la android/build/outputs/apk/debug/
else
    echo "❌ Ошибка при сборке APK"
    exit 1
fi


