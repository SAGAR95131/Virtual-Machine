@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM  docker_run.bat  —  Run the Custom VM inside a Linux Docker container
REM
REM  Usage:
REM    docker_run.bat              → runs default program.vm
REM    docker_run.bat myfile.vm   → runs a custom .vm file from this folder
REM ─────────────────────────────────────────────────────────────────────────────

echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║   Custom VM  —  Running on Linux (Docker)            ║
echo ╚══════════════════════════════════════════════════════╝
echo.

REM Check Docker is installed
docker --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not installed or not running.
    pause
    exit /b 1
)

REM Check the image exists
docker image inspect custom-vm >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker image 'custom-vm' not found.
    echo         Run docker_build.bat first!
    pause
    exit /b 1
)

REM If a custom .vm file is provided as argument, mount current dir and use it
IF "%~1"=="" (
    echo [INFO] Running default program.vm inside Linux container...
    echo.
    docker run --rm custom-vm
) ELSE (
    echo [INFO] Running %~1 inside Linux container...
    echo.
    REM Mount current directory as /app/programs so custom .vm files are accessible
    docker run --rm -v "%cd%:/app/programs" custom-vm programs/%~1
)

echo.
echo [Done] Linux container exited.
pause
