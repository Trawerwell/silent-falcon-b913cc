# Primordial Client

Исходный код клиентского Fabric-мода для Minecraft 1.21.4. Проект использует Java 21 и Gradle.

## Быстрый старт

```powershell
.\gradlew.bat build
```

Готовый мод появится в `build/libs/primordial-1.0.0.jar`. Файл `*-sources.jar` содержит исходники и не предназначен для установки в игру. Для запуска нужен Fabric Loader для Minecraft 1.21.4 и Fabric API; положите основной JAR в папку `mods` соответствующего профиля.

## Навигация по коду

- `src/main/java/aethereal/core` — запуск клиента, реестр модулей и система событий.
- `src/main/java/aethereal/module` — игровые функции, сгруппированные по категориям.
- `src/main/java/aethereal/setting` — типы настроек модулей.
- `src/main/java/aethereal/event` — события, используемые клиентом и mixin-кодом.
- `src/main/java/aethereal/handler` и `.../processor` — обработчики и фоновые/сервисные компоненты.
- `src/main/java/aethereal/ui` — экраны, виджеты и отрисовка интерфейса.
- `src/main/java/aethereal/config` — конфигурация и преобразование настроек.
- `src/main/java/aethereal/util` — общие вспомогательные классы.
- `src/main/java/platform/Initializer.java` — Fabric entrypoint.
- `src/main/java/platform/inject` — Mixin-классы, accessors и invokers для Minecraft.
- `src/main/resources` — метаданные мода, Mixin-конфигурация, текстуры и шейдеры.

Подробные рекомендации по навигации и безопасному редактированию см. в [руководстве по кодовой базе](docs/CODEBASE_GUIDE.md).

## Совместимость

Значения Minecraft, Fabric Loader, Fabric API и Java задаются в `gradle.properties` и `src/main/resources/fabric.mod.json`. При обновлении версии Minecraft проверьте Yarn mappings и сигнатуры Mixin-инъекций.
