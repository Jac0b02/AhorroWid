@echo off
echo ======================================================
echo    Compilando e instalando GastoTrack en tu celular
echo ======================================================
echo.

call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error durante la compilacion.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Instalando APK en el celular conectado...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error al instalar con adb. Asegurate de tener el celular conectado con depuracion USB.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Iniciando GastoTrack en tu pantalla...
adb shell am start -n com.gastotrack.app/.ui.MainActivity

echo.
echo ======================================================
echo    Listo! La app esta abierta en tu telefono.
echo ======================================================
timeout /t 3 >nul
