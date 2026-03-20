@echo off
REM ─────────────────────────────────────────────────────────────────────
REM  build_and_run.bat  —  Compile and launch the Custom VM IDE (GUI)
REM  Run this from the VM\ project root directory.
REM ─────────────────────────────────────────────────────────────────────

echo [Build] Compiling all Java sources...

if not exist out mkdir out

javac -d out ^
    src\Instruction.java ^
    src\StackMemory.java ^
    src\ProgramLoader.java ^
    src\InstructionParser.java ^
    src\Interpreter.java ^
    src\AITranslator.java ^
    src\LanguageRunner.java ^
    src\GUI.java ^
    src\VM.java

IF %ERRORLEVEL% NEQ 0 (
    echo [Build] *** COMPILATION FAILED ***
    pause
    exit /b 1
)

echo [Build] Compilation successful. Classes are in out\
echo.
echo [Run] Launching VM IDE...
java -cp out VM

REM ─── CLI mode (pass a .vm file) ───────────────────────────────────────
REM Uncomment the line below instead of the one above to run headless:
REM java -cp out VM program.vm
