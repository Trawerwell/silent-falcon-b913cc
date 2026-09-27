# Primordial GUI reference from `primordial.jar`

This folder contains CFR-decompiled GUI classes and the font/shader resources used by the supplied JAR.

The JAR targets the legacy Minecraft `GuiScreen`/LWJGL API (Minecraft 1.8.9), while the main project targets Minecraft 1.21.4 and Fabric. These files are kept outside Gradle's source sets because the original classes cannot compile against the current game API or the current `aethereal` module/settings model. The active GUI is being ported into `src/main/java/aethereal/ui/screen` using the current APIs.

The decompiler is CFR 0.152. Decompiled code may need cleanup before reuse. See `src/primordial/ui` for the recovered Java sources and `resources/assets/minecraft/primordial` for the associated resources.
