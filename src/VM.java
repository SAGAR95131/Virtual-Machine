import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.List;

/**
 * VM.java — Main entry point.
 *
 * If NO arguments are supplied → launches the Swing GUI.
 * If a .vm file path is supplied → runs in headless CLI mode (original
 * behaviour).
 *
 * This dual-mode design lets the same codebase power both interactive
 * development (GUI) and automated/scripted execution (CLI).
 */
public class VM {

    private static final String DEFAULT_PROGRAM = "program.vm";

    public static void main(String[] args) {

        if (args.length == 0) {
            // ── GUI mode ──────────────────────────────────────────────
            SwingUtilities.invokeLater(() -> new GUI());
            return;
        }

        // ── CLI mode ──────────────────────────────────────────────────
        String file = args[0];

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║      Custom Stack-Based Virtual Machine      ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        try {
            ProgramLoader loader = new ProgramLoader(file);
            List<String> lines = loader.load();
            InstructionParser parser = new InstructionParser();
            List<Instruction> program = parser.parse(lines);

            // Simple CLI listener: prints output and errors to stdout/stderr
            Interpreter interp = new Interpreter();
            interp.setProgram(program);
            interp.setStepDelay(0); // no delay in CLI mode
            interp.setListener(new Interpreter.InterpreterListener() {
                public void onInstructionExecuted(int ip, Instruction i, double[] s) {
                    System.out.println("[IP=" + ip + "] " + i);
                }

                public void onOutput(String text) {
                    System.out.println(">>> OUTPUT: " + text);
                }

                public void onError(String err) {
                    System.err.println("ERROR: " + err);
                }

                public void onHalt() {
                    System.out.println("[HALT]");
                }

                public void onReset() {
                }
            });

            interp.run(); // synchronous in CLI mode

        } catch (IOException e) {
            System.err.println("Cannot read file \"" + file + "\": " + e.getMessage());
            System.exit(1);
        } catch (InstructionParser.ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.exit(2);
        } catch (StackMemory.VMException e) {
            System.err.println("Runtime error: " + e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(99);
        }
    }
}
