@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM  docker_build.bat  —  Build the Custom VM Docker (Linux) image
REM  Run this from the VM\ project root directory.
REM ─────────────────────────────────────────────────────────────────────────────

echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║   Custom VM  —  Docker Linux Image Builder           ║
echo ╚══════════════════════════════════════════════════════╝
echo.

REM Check Docker is installed
docker --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not installed or not running.
    echo         Download Docker Desktop: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

echo [1/1] Building Docker image: custom-vm ...
echo       This may take a few minutes on first run (downloading base image).
echo.

docker build -t custom-vm .

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Docker build failed. See errors above.
    pause
    exit /b 1
)

echo.
echo ✔ Image built successfully!
echo.
echo   To run the VM with the default program.vm:
echo     docker_run.bat
echo.
echo   To run with a custom .vm file:
echo     docker run --rm -v "%cd%:/app/programs" custom-vm programs\yourfile.vm
echo.
pause
