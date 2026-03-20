import java.util.ArrayList;
import java.util.List;

/**
 * InstructionParser.java
 * ----------------------
 * Converts raw text lines (from ProgramLoader) into Instruction objects.
 *
 * Parser rules:
 *   - Tokens are separated by one or more whitespace characters.
 *   - The first token is the opcode  (case-insensitive).
 *   - The second token (if present) is the numeric operand.
 *   - Unrecognised opcodes or malformed operands throw a ParseException.
 *
 * AI INTEGRATION HOOK:
 *   A natural-language-to-VM translator can output lines like:
 *       "PUSH 5"
 *       "PUSH 10"
 *       "ADD"
 *       "PRINT"
 *   and feed them through this parser without any changes here.
 */
public class InstructionParser {

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Parse a list of raw source lines into a list of Instructions.
     *
     * @param lines the lines returned by ProgramLoader
     * @return an ordered list of Instructions ready for the Interpreter
     * @throws ParseException if a line cannot be parsed
     */
    public List<Instruction> parse(List<String> lines) {
        List<Instruction> program = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            try {
                Instruction instr = parseLine(line);
                program.add(instr);
            } catch (ParseException e) {
                // Enrich the exception with a line number before re-throwing
                throw new ParseException(
                        "Parse error on line " + (i + 1) + " (\"" + line + "\"): " + e.getMessage());
            }
        }

        System.out.println("[InstructionParser] Parsed " + program.size() + " instruction(s) successfully.");
        return program;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    /**
     * Parse a single source line into an Instruction.
     *
     * @param line one trimmed line of VM source code
     * @return the corresponding Instruction
     * @throws ParseException on any syntax error
     */
    private Instruction parseLine(String line) {
        // Split on whitespace; limit to 2 tokens (opcode + optional operand)
        String[] tokens = line.split("\\s+", 2);
        String opStr    = tokens[0].toUpperCase();

        Instruction.OpCode opCode;
        try {
            opCode = Instruction.OpCode.valueOf(opStr);
        } catch (IllegalArgumentException ex) {
            throw new ParseException("Unknown opcode \"" + tokens[0] + "\".");
        }

        // Opcodes that expect an operand
        if (opCode == Instruction.OpCode.PUSH) {
            if (tokens.length < 2 || tokens[1].trim().isEmpty()) {
                throw new ParseException("PUSH requires a numeric operand.");
            }
            double value;
            try {
                value = Double.parseDouble(tokens[1].trim());
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid numeric operand \"" + tokens[1].trim() + "\" for PUSH.");
            }
            return new Instruction(opCode, value);
        }

        // Opcodes that must NOT have an operand
        if (tokens.length > 1) {
            // Allow trailing spaces/comments – only warn; don't hard-fail
            System.out.println("[InstructionParser] Warning: opcode "
                    + opCode + " does not use an operand; ignoring \"" + tokens[1] + "\".");
        }

        return new Instruction(opCode);
    }

    // ---------------------------------------------------------------
    // Nested exception class
    // ---------------------------------------------------------------

    /** Thrown when the parser encounters invalid VM source syntax. */
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
}
