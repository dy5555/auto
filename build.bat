@echo off
setlocal

if not exist out mkdir out

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -source 1.8 -target 1.8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo [FAIL] Compile failed.
    pause
    exit /b 1
)

del sources.txt
jar cfe auto.jar com.dy5555.auto.App -C out .

echo.
echo [OK] auto.jar created.
pause
