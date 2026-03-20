/**
 * Instruction.java
 * -----------------
 * Defines the instruction set architecture (ISA) for the custom VM.
 *
 * Each instruction represents an opcode that the Interpreter will execute.
 * Having a separate Instruction type makes it easy to add new instructions
 * later (e.g., JMP, LOAD, STORE) without touching other parts of the VM.
 *
 * AI INTEGRATION HOOK:
 *   A natural-language-to-VM compiler can produce InstructionToken objects
 *   from parsed sentences and feed them directly into the Interpreter.
 */
public class Instruction {

    /**
     * Supported opcodes in this VM.
     *
     * PUSH  – push a numeric value onto the stack
     * ADD   – pop top two values, push their sum
     * SUB   – pop top two values, push (second - top)
     * MUL   – pop top two values, push their product
     * DIV   – pop top two values, push (second / top)
     * PRINT – pop the top value and print it
     * HALT  – stop the execution engine
     */
    public enum OpCode {
        PUSH,
        ADD,
        SUB,
        MUL,
        DIV,
        PRINT,
        HALT
    }

    // ---------------------------------------------------------------
    // Instance fields
    // ---------------------------------------------------------------

    /** The operation this instruction represents. */
    private final OpCode opCode;

    /**
     * Optional operand (only meaningful for PUSH).
     * For all other instructions this is Double.NaN.
     */
    private final double operand;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /**
     * Create an instruction that carries an operand (e.g., PUSH 42).
     *
     * @param opCode  the operation
     * @param operand the numeric value associated with the operation
     */
    public Instruction(OpCode opCode, double operand) {
        this.opCode   = opCode;
        this.operand  = operand;
    }

    /**
     * Create an instruction that does NOT carry an operand
     * (e.g., ADD, SUB, PRINT, HALT).
     *
     * @param opCode the operation
     */
    public Instruction(OpCode opCode) {
        this(opCode, Double.NaN);
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    /** @return the opcode of this instruction */
    public OpCode getOpCode() {
        return opCode;
    }

    /**
     * @return the numeric operand, or Double.NaN if not applicable
     */
    public double getOperand() {
        return operand;
    }

    /** @return true when this instruction carries a valid operand */
    public boolean hasOperand() {
        return !Double.isNaN(operand);
    }

    @Override
    public String toString() {
        return hasOperand()
                ? opCode + " " + operand
                : opCode.toString();
    }
}
