@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM  docker_run.bat — Run any language file on the Linux Hypervisor VM
REM
REM  Usage:
REM    docker_run.bat                  → interactive menu (inside Linux)
REM    docker_run.bat program.vm       → run Custom VM file
REM    docker_run.bat examples\hello.py  → run Python
REM    docker_run.bat examples\hello.c   → run C
REM    docker_run.bat examples\hello.cpp → run C++
REM    docker_run.bat examples\hello.js  → run JavaScript
REM ─────────────────────────────────────────────────────────────────────────────
echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║   Hypervisor VM  —  Running on Linux (Docker)            ║
echo ║   Host OS: Ubuntu 22.04  ^|  Hypervisor: Hyper-V/WSL2   ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

docker --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker not running. Start Docker Desktop first!
    pause & exit /b 1
)

docker image inspect hypervisor-vm >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Image 'hypervisor-vm' not found. Run docker_build.bat first!
    pause & exit /b 1
)

IF "%~1"=="" (
    echo [Linux] Launching interactive multi-language menu...
    echo.
    docker run --rm -it -v "%cd%:/app/code" hypervisor-vm
) ELSE (
    REM Resolve the file — if it's in a subdirectory, mount appropriately
    echo [Linux] Running: %~1
    echo.
    docker run --rm -v "%cd%:/app/code" hypervisor-vm code/%~1
)

echo.
echo [Done] Linux container exited.
pause
