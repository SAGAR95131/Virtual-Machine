import java.util.regex.*;

/**
 * AITranslator.java
 * -----------------
 * Rule-based natural-language → VM instruction translator.
 *
 * Recognises common arithmetic descriptions and produces ready-to-run VM code.
 * No external API or ML model is required — pure regex pattern matching.
 *
 * AI INTEGRATION HOOK:
 * Replace or extend translate() to call an LLM API (e.g., Gemini, GPT-4)
 * that returns VM instructions as structured text, then return the result
 * as a TranslationResult for seamless GUI integration.
 *
 * Supported phrases (case-insensitive):
 * add X and Y → PUSH X / PUSH Y / ADD / PRINT
 * subtract X from Y → PUSH Y / PUSH X / SUB / PRINT (Y − X)
 * X minus Y → PUSH X / PUSH Y / SUB / PRINT
 * multiply X by Y → PUSH X / PUSH Y / MUL / PRINT
 * X times Y / X * Y → same as above
 * divide X by Y → PUSH X / PUSH Y / DIV / PRINT
 * square of X / X squared→ PUSH X / PUSH X / MUL / PRINT
 * cube of X / X cubed → (X²) then * X / PRINT
 * double X → PUSH X / PUSH 2 / MUL / PRINT
 * half of X / halve X → PUSH X / PUSH 2 / DIV / PRINT
 * push X → PUSH X
 * print / show result → PRINT
 * halt / stop → HALT
 */
public class AITranslator {

    // ── Result DTO ────────────────────────────────────────────────────────────
    public static class TranslationResult {
        public final String vmCode;
        public final String explanation;
        public final boolean success;

        public TranslationResult(String vmCode, String explanation, boolean success) {
            this.vmCode = vmCode;
            this.explanation = explanation;
            this.success = success;
        }
    }

    // Regex fragment that matches an optional-sign integer or decimal
    private static final String N = "(-?\\d+(?:\\.\\d+)?)";

    // ── Public entry point ────────────────────────────────────────────────────
    public TranslationResult translate(String input) {
        if (input == null || input.isBlank())
            return fail("Please enter a description.");

        String t = input.trim().toLowerCase().replaceAll("[!?]", "");

        TranslationResult r;

        if ((r = tryAdd(t)).success)
            return r;
        if ((r = trySubFrom(t)).success)
            return r;
        if ((r = trySub(t)).success)
            return r;
        if ((r = tryMul(t)).success)
            return r;
        if ((r = tryDiv(t)).success)
            return r;
        if ((r = trySquare(t)).success)
            return r;
        if ((r = tryCube(t)).success)
            return r;
        if ((r = tryDouble(t)).success)
            return r;
        if ((r = tryHalf(t)).success)
            return r;
        if ((r = tryPush(t)).success)
            return r;
        if ((r = tryPrint(t)).success)
            return r;
        if ((r = tryHalt(t)).success)
            return r;

        return fail("Could not understand: \"" + input + "\"\n\nTry phrases like:\n"
                + "  • add 5 and 10\n"
                + "  • subtract 3 from 20\n"
                + "  • multiply 6 by 7\n"
                + "  • divide 100 by 4\n"
                + "  • square of 8\n"
                + "  • double 15\n"
                + "  • half of 50");
    }

    // ── Pattern matchers ──────────────────────────────────────────────────────

    private TranslationResult tryAdd(String t) {
        Matcher m = first(t,
                "add " + N + " (?:and|to|\\+) " + N,
                "sum of " + N + " and " + N,
                N + " (?:plus|\\+) " + N);
        if (m == null)
            return fail(null);
        String a = m.group(1), b = m.group(2);
        return ok("PUSH " + a + "\nPUSH " + b + "\nADD\nPRINT",
                a + " + " + b + " = ?");
    }

    /** subtract X from Y → Y - X */
    private TranslationResult trySubFrom(String t) {
        Matcher m = match(t, "subtract " + N + " from " + N);
        if (m == null)
            return fail(null);
        String sub = m.group(1), from = m.group(2);
        return ok("PUSH " + from + "\nPUSH " + sub + "\nSUB\nPRINT",
                from + " − " + sub + " = ?");
    }

    /** X minus Y or X - Y */
    private TranslationResult trySub(String t) {
        Matcher m = first(t, N + " minus " + N, N + " - " + N);
        if (m == null)
            return fail(null);
        String a = m.group(1), b = m.group(2);
        return ok("PUSH " + a + "\nPUSH " + b + "\nSUB\nPRINT",
                a + " − " + b + " = ?");
    }

    private TranslationResult tryMul(String t) {
        Matcher m = first(t,
                "multiply " + N + "(?: by)? " + N,
                "product of " + N + " and " + N,
                N + " (?:times|\\*|x) " + N);
        if (m == null)
            return fail(null);
        String a = m.group(1), b = m.group(2);
        return ok("PUSH " + a + "\nPUSH " + b + "\nMUL\nPRINT",
                a + " × " + b + " = ?");
    }

    private TranslationResult tryDiv(String t) {
        Matcher m = first(t,
                "divide " + N + "(?: by)? " + N,
                N + " divided by " + N,
                N + " / " + N,
                "quotient of " + N + " and " + N);
        if (m == null)
            return fail(null);
        String a = m.group(1), b = m.group(2);
        return ok("PUSH " + a + "\nPUSH " + b + "\nDIV\nPRINT",
                a + " ÷ " + b + " = ?");
    }

    private TranslationResult trySquare(String t) {
        Matcher m = first(t, "square of " + N, N + " squared");
        if (m == null)
            return fail(null);
        String a = m.group(1);
        return ok("PUSH " + a + "\nPUSH " + a + "\nMUL\nPRINT",
                a + "² = ?");
    }

    private TranslationResult tryCube(String t) {
        Matcher m = first(t, "cube of " + N, N + " cubed");
        if (m == null)
            return fail(null);
        String a = m.group(1);
        return ok("PUSH " + a + "\nPUSH " + a + "\nMUL\n# stack = " + a + "²\nPUSH " + a + "\nMUL\nPRINT",
                a + "³ = ?");
    }

    private TranslationResult tryDouble(String t) {
        Matcher m = match(t, "double (?:of )?" + N);
        if (m == null)
            return fail(null);
        String a = m.group(1);
        return ok("PUSH " + a + "\nPUSH 2\nMUL\nPRINT", "2 × " + a + " = ?");
    }

    private TranslationResult tryHalf(String t) {
        Matcher m = first(t, "half of " + N, "halve " + N);
        if (m == null)
            return fail(null);
        String a = m.group(1);
        return ok("PUSH " + a + "\nPUSH 2\nDIV\nPRINT", a + " / 2 = ?");
    }

    private TranslationResult tryPush(String t) {
        Matcher m = match(t, "push " + N);
        if (m == null)
            return fail(null);
        return ok("PUSH " + m.group(1), "Push " + m.group(1) + " onto the stack");
    }

    private TranslationResult tryPrint(String t) {
        if (t.matches("print|show(?: result)?|display(?: result)?"))
            return ok("PRINT", "Print top of stack");
        return fail(null);
    }

    private TranslationResult tryHalt(String t) {
        if (t.matches("halt|stop|end|quit|exit"))
            return ok("HALT", "Stop the VM");
        return fail(null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Try a single regex; return Matcher if found, null otherwise. */
    private Matcher match(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m : null;
    }

    /** Try multiple regexes in order; return first match or null. */
    private Matcher first(String text, String... regexes) {
        for (String r : regexes) {
            Matcher m = match(text, r);
            if (m != null)
                return m;
        }
        return null;
    }

    private TranslationResult ok(String code, String expl) {
        return new TranslationResult(code, expl, true);
    }

    private TranslationResult fail(String msg) {
        return new TranslationResult("", msg != null ? msg : "", false);
    }
}
