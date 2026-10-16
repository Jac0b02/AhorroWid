# GastoTrack Android — Proyecto Completo Listo para Compilar

Este paquete contiene la estructura completa del proyecto Android (`settings.gradle.kts`, `build.gradle.kts`, recursos XML, íconos adaptativos, código fuente Kotlin y flujo de compilación automática en la nube).

## Opción 1: Compilar el APK en la nube gratis (Sin instalar Android Studio)
1. Crea un repositorio nuevo en [GitHub](https://github.com/new).
2. Sube el contenido descomprimido de esta carpeta al repositorio.
3. Entra a la pestaña **Actions** de tu repositorio y ejecuta el flujo **Compilar GastoTrack APK**.
4. En ~2 minutos aparecerá el archivo descargable **`GastoTrack-debug-apk`** (`app-debug.apk`) listo para instalar en tu teléfono Android.

## Opción 2: Compilar en tu PC con Android Studio
1. Descomprime este archivo `.zip`.
2. Abre Android Studio y selecciona **Open** sobre la carpeta descomprimida.
3. Espera a que termine el *Gradle Sync* automático.
4. Ve al menú **Build > Build Bundle(s) / APK(s) > Build APK(s)** para obtener el archivo `app-debug.apk`.
