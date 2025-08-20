#!/bin/bash

echo "🚀 Сборка Android APK для Astro..."

# Проверяем наличие Java
if ! command -v java &> /dev/null; then
    echo "❌ Java не найдена. Установите Java 21 или выше."
    exit 1
fi

# Проверяем версию Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Требуется Java 21 или выше. Текущая версия: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java версия: $(java -version 2>&1 | head -n 1)"

# Очищаем предыдущую сборку
echo "🧹 Очистка предыдущей сборки..."
./gradlew clean

# Собираем APK
echo "🔨 Сборка APK..."
./gradlew :android:assembleDebug

# Проверяем результат
if [ $? -eq 0 ]; then
    echo "✅ APK успешно собран!"
    echo "📱 Файл APK находится в: android/build/outputs/apk/debug/android-debug.apk"
    echo ""
    echo "📋 Для установки на устройство:"
    echo "1. Включите режим разработчика на Android устройстве"
    echo "2. Включите отладку по USB"
    echo "3. Подключите устройство к компьютеру"
    echo "4. Выполните: adb install android/build/outputs/apk/debug/android-debug.apk"
else
    echo "❌ Ошибка при сборке APK"
    exit 1
fi


