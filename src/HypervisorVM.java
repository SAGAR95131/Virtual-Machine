import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * HypervisorVM.java — Linux Hypervisor Entry Point
 * --------------------------------------------------
 * A multi-language virtual machine runner designed to run ON Linux (Docker).
 *
 * Supported languages (auto-detected by file extension):
 *   .vm   → Custom Stack-Based VM (this project's own language)
 *   .py   → Python 3
 *   .c    → C  (compiled with gcc, then executed)
 *   .cpp  → C++ (compiled with g++, then executed)
 *   .java → Java (compiled with javac, then executed)
 *   .js   → JavaScript (run with Node.js)
 *
 * Usage (inside Docker Linux container):
 *   java -cp out HypervisorVM program.vm
 *   java -cp out HypervisorVM mycode.py
 *   java -cp out HypervisorVM hello.c
 *   java -cp out HypervisorVM Main.java
 *
 * Architecture:
 *   ┌──────────────────────────────────────┐
 *   │  HypervisorVM  (this class)          │  ← Language dispatcher
 *   ├──────────────────────────────────────┤
 *   │  JVM 17  (Ubuntu 22.04 Linux)        │  ← Java runtime
 *   ├──────────────────────────────────────┤
 *   │  Docker Linux Container              │  ← Isolated Linux OS
 *   ├──────────────────────────────────────┤
 *   │  Hyper-V / WSL2  ← HYPERVISOR ✅    │
 *   ├──────────────────────────────────────┤
 *   │  Windows Host / Physical Hardware    │
 *   └──────────────────────────────────────┘
 */
public class HypervisorVM {

    // ANSI colour codes for terminal output
    static final String RESET  = "\u001B[0m";
    static final String BOLD   = "\u001B[1m";
    static final String GREEN  = "\u001B[32m";
    static final String BLUE   = "\u001B[34m";
    static final String CYAN   = "\u001B[36m";
    static final String YELLOW = "\u001B[33m";
    static final String RED    = "\u001B[31m";
    static final String PURPLE = "\u001B[35m";
    static final String WHITE  = "\u001B[97m";

    public static void main(String[] args) throws Exception {
        printBanner();

        if (args.length == 0) {
            // Interactive mode — show menu
            runInteractiveMenu();
            return;
        }

        String filePath = args[0];
        runFile(filePath);
    }

    // ── Banner ───────────────────────────────────────────────────────────────
    static void printBanner() {
        System.out.println(BOLD + CYAN);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         HYPERVISOR VM  —  Multi-Language Runtime         ║");
        System.out.println("║              Host OS: Linux (Docker Container)           ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Languages: Custom VM | Python | C | C++ | Java | JS    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    // ── Interactive menu ─────────────────────────────────────────────────────
    static void runInteractiveMenu() throws Exception {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println(BOLD + WHITE + "\n┌─ Hypervisor VM Menu ─────────────────────┐" + RESET);
            System.out.println("│  " + BLUE   + "[1]" + RESET + " Run Custom VM program (.vm)       │");
            System.out.println("│  " + YELLOW + "[2]" + RESET + " Run Python script   (.py)         │");
            System.out.println("│  " + GREEN  + "[3]" + RESET + " Run C program       (.c)          │");
            System.out.println("│  " + GREEN  + "[4]" + RESET + " Run C++ program     (.cpp)        │");
            System.out.println("│  " + PURPLE + "[5]" + RESET + " Run Java program    (.java)       │");
            System.out.println("│  " + CYAN   + "[6]" + RESET + " Run JavaScript      (.js)         │");
            System.out.println("│  " + RED    + "[0]" + RESET + " Exit                              │");
            System.out.println("└────────────────────────────────────────┘");
            System.out.print(BOLD + "  Choose: " + RESET);

            String choice = sc.nextLine().trim();
            if (choice.equals("0")) {
                System.out.println(GREEN + "\n[HypervisorVM] Shutdown complete. Goodbye!" + RESET);
                break;
            }

            System.out.print(BOLD + "  Enter file path: " + RESET);
            String path = sc.nextLine().trim();
            if (path.isEmpty()) continue;
            runFile(path);
        }
    }

    // ── Dispatch by file extension ───────────────────────────────────────────
    static void runFile(String filePath) throws Exception {
        File f = new File(filePath);
        if (!f.exists()) {
            System.err.println(RED + "[Error] File not found: " + filePath + RESET);
            return;
        }

        String name = f.getName().toLowerCase();
        System.out.println(CYAN + "\n[HypervisorVM] Dispatching: " + f.getName() + RESET);

        long start = System.currentTimeMillis();

        if (name.endsWith(".vm")) {
            runCustomVM(filePath);
        } else if (name.endsWith(".py")) {
            runProcess("Python", detectPython(), filePath);
        } else if (name.endsWith(".c")) {
            compileAndRun("C", "gcc", filePath, "-lm");
        } else if (name.endsWith(".cpp")) {
            compileAndRun("C++", "g++", filePath, "-lstdc++");
        } else if (name.endsWith(".java")) {
            compileAndRunJava(filePath);
        } else if (name.endsWith(".js")) {
            runProcess("JavaScript", "node", filePath);
        } else {
            System.err.println(RED + "[Error] Unsupported file type: " + name + RESET);
            printSupportedTypes();
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println(CYAN + "\n[HypervisorVM] Finished in " + elapsed + "ms" + RESET);
    }

    // ── Custom VM runner ─────────────────────────────────────────────────────
    static void runCustomVM(String filePath) {
        System.out.println(BLUE + "► Running Custom Stack VM: " + filePath + RESET);
        System.out.println(BLUE + "──────────────────────────────────────────" + RESET);
        try {
            ProgramLoader loader = new ProgramLoader(filePath);
            List<String> lines = loader.load();
            InstructionParser parser = new InstructionParser();
            List<Instruction> program = parser.parse(lines);

            Interpreter interp = new Interpreter();
            interp.setProgram(program);
            interp.setStepDelay(0);
            interp.setListener(new Interpreter.InterpreterListener() {
                public void onInstructionExecuted(int ip, Instruction i, double[] s) {
                    System.out.println(BLUE + "  [IP=" + ip + "] " + i + RESET);
                }
                public void onOutput(String text) {
                    System.out.println(GREEN + "  >>> " + text + RESET);
                }
                public void onError(String err) {
                    System.err.println(RED + "  [VM ERROR] " + err + RESET);
                }
                public void onHalt() {
                    System.out.println(BLUE + "  [HALT]" + RESET);
                }
                public void onReset() {}
            });
            interp.run();
        } catch (Exception e) {
            System.err.println(RED + "[VM Error] " + e.getMessage() + RESET);
        }
    }

    // ── C / C++ compile + run ────────────────────────────────────────────────
    static void compileAndRun(String lang, String compiler, String srcFile, String flag)
            throws IOException, InterruptedException {
        Path tmp = Files.createTempDirectory("hvm_");
        String exe = tmp.resolve("prog").toString();

        System.out.println(GREEN + "► Compiling " + lang + " with " + compiler + "..." + RESET);

        List<String> compileCmd = Arrays.asList(compiler, srcFile, "-o", exe, flag);
        int compileExit = exec(compileCmd, null);
        if (compileExit != 0) {
            System.err.println(RED + "[Error] Compilation failed." + RESET);
            return;
        }
        System.out.println(GREEN + "✔ Compiled. Running..." + RESET);
        System.out.println(GREEN + "──────────────────────────────────────────" + RESET);
        exec(Collections.singletonList(exe), null);

        // Cleanup
        new File(exe).delete();
        tmp.toFile().delete();
    }

    // ── Java compile + run ───────────────────────────────────────────────────
    static void compileAndRunJava(String srcFile) throws IOException, InterruptedException {
        Path tmp = Files.createTempDirectory("hvm_java_");
        String className = new File(srcFile).getName().replace(".java", "");

        System.out.println(PURPLE + "► Compiling Java with javac..." + RESET);
        List<String> compileCmd = Arrays.asList("javac", "-d", tmp.toString(), srcFile);
        int exit = exec(compileCmd, null);
        if (exit != 0) {
            System.err.println(RED + "[Error] Java compilation failed." + RESET);
            return;
        }
        System.out.println(PURPLE + "✔ Compiled. Running..." + RESET);
        System.out.println(PURPLE + "──────────────────────────────────────────" + RESET);
        exec(Arrays.asList("java", "-cp", tmp.toString(), className), null);

        // Cleanup
        Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    // ── Generic process runner ───────────────────────────────────────────────
    static void runProcess(String lang, String cmd, String filePath)
            throws IOException, InterruptedException {
        System.out.println(YELLOW + "► Running " + lang + " with " + cmd + "..." + RESET);
        System.out.println(YELLOW + "──────────────────────────────────────────" + RESET);
        exec(Arrays.asList(cmd, filePath), null);
    }

    // ── Execute a process and stream output ──────────────────────────────────
    static int exec(List<String> cmd, File workDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        if (workDir != null) pb.directory(workDir);

        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            System.err.println(RED + "[Error] Cannot launch: " + cmd.get(0)
                    + "\n        Make sure it is installed and on PATH." + RESET);
            return -1;
        }

        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    System.out.println("  " + line);
            } catch (IOException ignored) {}
        });
        reader.setDaemon(true);
        reader.start();

        int exit = proc.waitFor();
        reader.join(3000);
        return exit;
    }

    // ── Detect python command ────────────────────────────────────────────────
    static String detectPython() {
        for (String py : new String[]{"python3", "python"}) {
            try {
                Process p = new ProcessBuilder(py, "--version")
                        .redirectErrorStream(true).start();
                p.waitFor();
                if (p.exitValue() == 0) return py;
            } catch (Exception ignored) {}
        }
        return "python3";
    }

    static void printSupportedTypes() {
        System.out.println(WHITE + "\nSupported file types:" + RESET);
        System.out.println("  .vm    → Custom Stack VM");
        System.out.println("  .py    → Python 3");
        System.out.println("  .c     → C (gcc)");
        System.out.println("  .cpp   → C++ (g++)");
        System.out.println("  .java  → Java (javac)");
        System.out.println("  .js    → JavaScript (node)");
    }
}
