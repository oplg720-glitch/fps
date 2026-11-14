@echo off
setlocal

echo [FPS Stabilizer] Checking Java...
for /f "tokens=2 delims=\"" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%V
if not defined JAVA_VERSION (
  echo ERROR: Java was not found. Install JDK 25 and add it to PATH.
  exit /b 1
)
echo Detected Java %JAVA_VERSION%
echo %JAVA_VERSION% | findstr /b "25" >nul
if errorlevel 1 (
  echo ERROR: Minecraft 26.1.2 requires JDK 25. Detected %JAVA_VERSION%.
  exit /b 1
)

where gradle >nul 2>nul
if errorlevel 1 (
  echo ERROR: Gradle 9.4.1 is not on PATH.
  echo You can open this folder in IntelliJ IDEA and run the Gradle build task instead.
  exit /b 1
)

echo [FPS Stabilizer] Building mod...
gradle build
if errorlevel 1 exit /b 1

echo.
echo BUILD COMPLETE. Jar: build\libs\fps-stabilizer-1.1.0.jar
endlocal
