import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ProgramLoader.java
 * ------------------
 * Responsible for reading a .vm source file from disk and handing back a
 * list of raw text lines ready to be parsed by the InstructionParser.
 *
 * Separation of concerns:
 *   ProgramLoader knows ONLY about file I/O.
 *   It does NOT interpret the contents — that is the parser's job.
 *
 * AI INTEGRATION HOOK:
 *   Swap or extend this class to accept programs from other sources,
 *   e.g., a string produced by a natural-language-to-VM compiler.
 */
public class ProgramLoader {

    /** Path to the .vm source file. */
    private final String filePath;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    /**
     * @param filePath absolute or relative path to the .vm file
     */
    public ProgramLoader(String filePath) {
        this.filePath = filePath;
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Read every line of the .vm file and return them as a list.
     *
     * Rules applied during loading:
     *   - Empty lines are skipped.
     *   - Lines whose first non-whitespace character is '#' are treated
     *     as comments and skipped (supports inline documentation in .vm files).
     *   - All other lines are trimmed and returned as-is.
     *
     * @return ordered list of source lines (comments and blanks excluded)
     * @throws IOException if the file cannot be opened or read
     */
    public List<String> load() throws IOException {
        List<String> lines = new ArrayList<>();

        System.out.println("[ProgramLoader] Loading program from: " + filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                // Skip blank lines
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Skip comment lines (lines starting with '#')
                if (trimmed.startsWith("#")) {
                    System.out.println("[ProgramLoader] Comment at line " + lineNumber + ": " + trimmed);
                    continue;
                }

                lines.add(trimmed);
                System.out.println("[ProgramLoader] Loaded line " + lineNumber + ": " + trimmed);
            }
        }

        System.out.println("[ProgramLoader] Total executable lines loaded: " + lines.size());
        return lines;
    }

    // ---------------------------------------------------------------
    // Convenience: load from a plain string (useful for AI integration)
    // ---------------------------------------------------------------

    /**
     * Parse a raw multi-line string as if it were a .vm file.
     * This allows the AI layer to generate VM code as a String and
     * pass it directly to the rest of the pipeline without writing a file.
     *
     * @param source the raw VM source code
     * @return ordered list of executable lines
     */
    public static List<String> loadFromString(String source) {
        List<String> lines = new ArrayList<>();
        for (String line : source.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
