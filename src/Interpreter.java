import java.util.List;

/**
 * Interpreter.java — Fetch-Decode-Execute loop with full GUI integration.
 *
 * Runs on its own Thread. The GUI implements InterpreterListener and
 * receives real-time callbacks for every instruction, output value, error,
 * and halt event. All GUI updates are wrapped in SwingUtilities.invokeLater
 * on the listener side (not here), keeping this class UI-agnostic.
 *
 * Execution modes:
 * RUN – execute continuously with a configurable inter-instruction delay
 * STEP – execute one instruction then pause; GUI calls requestStep() to advance
 * PAUSED – waiting for resume() or requestStep()
 */
public class Interpreter implements Runnable {

    // ── Listener interface ────────────────────────────────────────────────────
    public interface InterpreterListener {
        /** Called after every instruction (including PUSH). */
        void onInstructionExecuted(int ip, Instruction instr, double[] stackState);

        /** Called by PRINT instruction. */
        void onOutput(String text);

        /** Called on any runtime error. */
        void onError(String error);

        /** Called when HALT is reached or program ends. */
        void onHalt();

        /** Called when the interpreter is reset to its initial state. */
        void onReset();
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private final StackMemory stack = new StackMemory();
    private List<Instruction> program;
    private InterpreterListener listener;

    private int ip = 0;
    private long stepDelayMs = 300; // milliseconds between instructions in RUN mode

    private volatile boolean halted = false;
    private volatile boolean paused = false;
    private volatile boolean stepRequested = false;

    private final Object pauseLock = new Object();

    // ── Configuration ─────────────────────────────────────────────────────────
    public void setProgram(List<Instruction> program) {
        this.program = program;
    }

    public void setListener(InterpreterListener listener) {
        this.listener = listener;
    }

    public void setStepDelay(long ms) {
        this.stepDelayMs = ms;
    }

    // ── Control ───────────────────────────────────────────────────────────────
    public void reset() {
        ip = 0;
        halted = false;
        paused = false;
        stepRequested = false;
        stack.clear();
        if (listener != null)
            listener.onReset();
    }

    public void halt() {
        halted = true;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public void requestStep() {
        stepRequested = true;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public boolean isHalted() {
        return halted;
    }

    public boolean isPaused() {
        return paused;
    }

    // ── Run loop ──────────────────────────────────────────────────────────────
    @Override
    public void run() {
        if (program == null || program.isEmpty()) {
            if (listener != null)
                listener.onError("No program loaded.");
            return;
        }

        while (!halted && ip < program.size()) {

            // Wait while paused (unless a step was requested)
            if (paused && !stepRequested) {
                synchronized (pauseLock) {
                    try {
                        while (paused && !stepRequested && !halted)
                            pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            if (halted)
                break;

            Instruction instr = program.get(ip);
            try {
                execute(instr);
                stepRequested = false;

                if (listener != null)
                    listener.onInstructionExecuted(ip, instr, stack.toArray());

                ip++;

                // After one step, re-pause in STEP mode
                if (paused) {
                    stepRequested = false;
                    continue; // loop back → will wait at top
                }

            } catch (Exception e) {
                if (listener != null)
                    listener.onError(e.getMessage());
                return;
            }

            // Configurable delay in continuous RUN mode
            if (!paused && stepDelayMs > 0 && !halted) {
                try {
                    Thread.sleep(stepDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (!halted && listener != null)
            listener.onHalt();
    }

    // ── Instruction dispatch ──────────────────────────────────────────────────
    private void execute(Instruction instr) {
        switch (instr.getOpCode()) {
            case PUSH:
                stack.push(instr.getOperand());
                break;

            case ADD: {
                double r = stack.pop(), l = stack.pop();
                stack.push(l + r);
                break;
            }
            case SUB: {
                double r = stack.pop(), l = stack.pop();
                stack.push(l - r);
                break;
            }
            case MUL: {
                double r = stack.pop(), l = stack.pop();
                stack.push(l * r);
                break;
            }

            case DIV: {
                double r = stack.pop();
                if (r == 0.0)
                    throw new ArithmeticException("Division by zero.");
                stack.push(stack.pop() / r);
                break;
            }

            case PRINT: {
                double v = stack.pop();
                String s = (v == Math.floor(v) && !Double.isInfinite(v))
                        ? String.valueOf((long) v)
                        : String.valueOf(v);
                if (listener != null)
                    listener.onOutput(s);
                break;
            }

            case HALT:
                halted = true;
                if (listener != null)
                    listener.onHalt();
                break;
        }
    }
}
