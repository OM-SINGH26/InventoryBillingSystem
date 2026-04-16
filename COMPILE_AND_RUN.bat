@echo off
echo ==========================================
echo    INVENTORY BILLING SYSTEM - COMPILER
echo ==========================================

:: Create output folder
if not exist out mkdir out

echo.
echo Compiling all Java files...
echo.

javac -d out interfaces\*.java models\*.java exceptions\*.java filehandler\*.java threads\*.java console\ConsoleApp.java gui\GUIApp.java

if %errorlevel% == 0 (
    echo.
    echo ==========================================
    echo   Compilation SUCCESSFUL!
    echo ==========================================
    echo.
    echo Choose version to run:
    echo   1. Console Version
    echo   2. GUI Version
    echo.
    set /p choice="Enter 1 or 2: "

    if "%choice%"=="1" (
        echo Running Console Version...
        java -cp out console.ConsoleApp
    ) else (
        echo Running GUI Version...
        java -cp out gui.GUIApp
    )
) else (
    echo.
    echo ==========================================
    echo   Compilation FAILED! Check errors above.
    echo ==========================================
)

pause
