import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * LanguageRunner.java
 * -------------------
 * Compiles and executes source code written in Python, C, or C++.
 *
 * Strategy:
 * Python → writes a .py file → runs: python <file>
 * C → writes a .c file → compiles with gcc, then runs the .exe
 * C++ → writes a .cpp file → compiles with g++, then runs the .exe
 *
 * All I/O is captured asynchronously and delivered via RunListener callbacks
 * so the GUI can update in real time without blocking the Event Dispatch
 * Thread.
 *
 * Prerequisites on PATH:
 * Python → python (or python3)
 * C → gcc
 * C++ → g++
 */
public class LanguageRunner {

    // ── Supported languages ───────────────────────────────────────────────
    public enum Language {
        PYTHON("Python", ".py"),
        C("C", ".c"),
        CPP("C++", ".cpp");

        public final String displayName;
        public final String extension;

        Language(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // ── Listener ──────────────────────────────────────────────────────────
    public interface RunListener {
        /** Emitted for each line of stdout/stderr output. */
        void onOutput(String line, boolean isError);

        /** Emitted when the process finishes (or fails to start). */
        void onFinished(int exitCode, long elapsedMs);

        /** Emitted if launching or compiling fails before the program even starts. */
        void onFatalError(String message);
    }

    // ── Temp directory (inside system temp, cleaned up after run) ─────────
    private static final Path TEMP_DIR;
    static {
        try {
            TEMP_DIR = Files.createTempDirectory("custom_vm_runner_");
            // Register shutdown hook to clean up temp files
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walk(TEMP_DIR)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException ignored) {
                }
            }));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp directory", e);
        }
    }

    // ── Main entry point ──────────────────────────────────────────────────
    /**
     * Runs the given source code in the given language asynchronously.
     * The listener receives output lines and a final finish event.
     *
     * @param language Target language
     * @param source   Source code text
     * @param listener Callback listener
     * @return the Thread running the process (daemon thread)
     */
    public Thread run(Language language, String source, RunListener listener) {
        Thread t = new Thread(() -> doRun(language, source, listener), "LangRunner-" + language);
        t.setDaemon(true);
        t.start();
        return t;
    }

    // ── Internal run logic ────────────────────────────────────────────────
    private void doRun(Language language, String source, RunListener listener) {
        long start = System.currentTimeMillis();

        try {
            // 1. Write source to temp file
            String baseName = "prog_" + System.currentTimeMillis();
            Path srcFile = TEMP_DIR.resolve(baseName + language.extension);
            Files.writeString(srcFile, source);

            // 2. Compile if needed (C / C++)
            if (language == Language.C || language == Language.CPP) {
                Path outExe = TEMP_DIR.resolve(baseName + ".exe");
                String compiler = (language == Language.C) ? "gcc" : "g++";

                List<String> compileCmd = Arrays.asList(
                        compiler,
                        srcFile.toString(),
                        "-o", outExe.toString(),
                        "-lm" // link math library (useful for C)
                );

                listener.onOutput("► Compiling with " + compiler + "...", false);
                int compileExit = runProcess(compileCmd, null, listener, true);

                if (compileExit != 0) {
                    long elapsed = System.currentTimeMillis() - start;
                    listener.onOutput("✘ Compilation failed (exit " + compileExit + ")", true);
                    listener.onFinished(compileExit, elapsed);
                    return;
                }
                listener.onOutput("✔ Compilation successful. Running...\n", false);

                // 3. Execute the compiled binary
                List<String> runCmd = Collections.singletonList(outExe.toString());
                int runExit = runProcess(runCmd, null, listener, false);
                long elapsed = System.currentTimeMillis() - start;
                listener.onFinished(runExit, elapsed);

            } else {
                // Python — run directly
                listener.onOutput("► Running Python...\n", false);
                // Try "python" first, fall back to "python3"
                String pythonCmd = findPython();
                List<String> runCmd = Arrays.asList(pythonCmd, srcFile.toString());
                int runExit = runProcess(runCmd, null, listener, false);
                long elapsed = System.currentTimeMillis() - start;
                listener.onFinished(runExit, elapsed);
            }

        } catch (IOException e) {
            listener.onFatalError("I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            listener.onFatalError("Interrupted.");
        }
    }

    /**
     * Runs a process, streams its stdout and stderr to the listener.
     *
     * @param cmd          Command + arguments
     * @param workDir      Working directory (null = inherit)
     * @param listener     Output receiver
     * @param compilePhase If true, stdout is treated as compile info, stderr as
     *                     error
     * @return process exit code
     */
    private int runProcess(List<String> cmd, Path workDir,
            RunListener listener, boolean compilePhase)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false); // keep stdout/stderr separate

        if (workDir != null)
            pb.directory(workDir.toFile());

        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            // Binary not found on PATH
            String prog = cmd.get(0);
            listener.onFatalError(
                    "Cannot launch \"" + prog + "\".\n" +
                            "Make sure it is installed and on your system PATH.\n" +
                            "  Python  → https://www.python.org/downloads/\n" +
                            "  GCC/G++ → https://www.mingw-w64.org/  (MinGW-w64 for Windows)\n\n" +
                            "Original error: " + e.getMessage());
            return -1;
        }

        // Stream stdout
        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null)
                    listener.onOutput(line, false);
            } catch (IOException ignored) {
            }
        }, "stdout-reader");
        stdoutThread.setDaemon(true);
        stdoutThread.start();

        // Stream stderr
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proc.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null)
                    listener.onOutput(line, true);
            } catch (IOException ignored) {
            }
        }, "stderr-reader");
        stderrThread.setDaemon(true);
        stderrThread.start();

        int exit = proc.waitFor();
        stdoutThread.join(2000);
        stderrThread.join(2000);
        return exit;
    }

    /** Detects whether 'python' or 'python3' is on the PATH. */
    private String findPython() {
        String[] candidates = { "python", "python3" };
        for (String py : candidates) {
            try {
                Process p = new ProcessBuilder(py, "--version")
                        .redirectErrorStream(true)
                        .start();
                p.waitFor();
                if (p.exitValue() == 0)
                    return py;
            } catch (Exception ignored) {
            }
        }
        return "python"; // best guess
    }

    // ── Starter templates ─────────────────────────────────────────────────
    public static String starterTemplate(Language lang) {
        switch (lang) {
            case PYTHON:
                return "# Python program\n" +
                        "\n" +
                        "def main():\n" +
                        "    a = 5\n" +
                        "    b = 10\n" +
                        "    print(f\"Sum of {a} and {b} = {a + b}\")\n" +
                        "    print(f\"Product = {a * b}\")\n" +
                        "\n" +
                        "if __name__ == '__main__':\n" +
                        "    main()\n";

            case C:
                return "#include <stdio.h>\n" +
                        "\n" +
                        "int main() {\n" +
                        "    int a = 5, b = 10;\n" +
                        "    printf(\"Sum of %d and %d = %d\\n\", a, b, a + b);\n" +
                        "    printf(\"Product = %d\\n\", a * b);\n" +
                        "    return 0;\n" +
                        "}\n";

            case CPP:
                return "#include <iostream>\n" +
                        "using namespace std;\n" +
                        "\n" +
                        "int main() {\n" +
                        "    int a = 5, b = 10;\n" +
                        "    cout << \"Sum of \" << a << \" and \" << b << \" = \" << (a + b) << endl;\n" +
                        "    cout << \"Product = \" << (a * b) << endl;\n" +
                        "    return 0;\n" +
                        "}\n";

            default:
                return "";
        }
    }
}
