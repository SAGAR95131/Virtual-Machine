@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM  docker_build.bat  —  Build Hypervisor VM (Multi-Language Linux Image)
REM ─────────────────────────────────────────────────────────────────────────────
echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║   Hypervisor VM  —  Building Linux Multi-Lang Image      ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
docker --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker not running. Start Docker Desktop first!
    pause & exit /b 1
)
echo [Building] hypervisor-vm (Ubuntu 22.04 + Java + Python + C + C++ + Node.js)
echo            This takes ~2-5 minutes on first run...
echo.
docker build -t hypervisor-vm .
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed. See above for details.
    pause & exit /b 1
)
echo.
echo ✔ Image built! Languages available inside Linux:
echo    Custom VM (.vm)  Python (.py)  C (.c)  C++ (.cpp)  JS (.js)
echo.
echo Run: docker_run.bat [filename]
pause
