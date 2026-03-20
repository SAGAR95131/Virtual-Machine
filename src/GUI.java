import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.List;

/**
 * GUI.java — Full Swing desktop IDE for the Custom Stack VM.
 *
 * Layout: JTabbedPane with two tabs:
 * Tab 1 (VM IDE):
 * NORTH → Gradient header + toolbar (Run/Pause/Step/Stop, speed slider)
 * CENTER → 3-column split: Code Editor | Stack Visualizer | Output Console
 * SOUTH → AI Translator panel + status bar
 * Tab 2 (Language Runner):
 * Supports writing and running Python, C, and C++ programs.
 *
 * The class implements Interpreter.InterpreterListener so it receives
 * real-time callbacks from the interpreter thread. All UI mutations are
 * dispatched via SwingUtilities.invokeLater for Swing thread safety.
 */
public class GUI extends JFrame implements Interpreter.InterpreterListener {

    // ── Colour palette (Catppuccin Mocha-inspired) ─────────────────────────
    static final Color C_BASE = new Color(30, 30, 46); // main bg
    static final Color C_MANTLE = new Color(24, 24, 37); // panel bg
    static final Color C_CRUST = new Color(17, 17, 27); // deepest bg
    static final Color C_SURFACE = new Color(49, 50, 68); // input / card
    static final Color C_OVERLAY = new Color(108, 112, 134); // dim text
    static final Color C_TEXT = new Color(205, 214, 244); // primary text
    static final Color C_BLUE = new Color(137, 180, 250); // accent / keywords
    static final Color C_GREEN = new Color(166, 227, 161); // success / output
    static final Color C_RED = new Color(243, 139, 168); // errors
    static final Color C_YELLOW = new Color(249, 226, 175); // numbers / warnings
    static final Color C_MAUVE = new Color(203, 166, 247); // AI / special
    static final Color C_TEAL = new Color(148, 226, 213); // PRINT instruction
    static final Color C_PINK = new Color(245, 194, 231); // HALT
    static final Color C_PEACH = new Color(250, 179, 135); // operators

    // ── Fonts ──────────────────────────────────────────────────────────────
    static final Font F_CODE = new Font("Consolas", Font.PLAIN, 14);
    static final Font F_UI = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font F_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font F_H1 = new Font("Segoe UI", Font.BOLD, 15);

    // ── VM components ──────────────────────────────────────────────────────
    private Interpreter interpreter;
    private Thread execThread;
    private List<Instruction> currentProgram;
    private final AITranslator ai = new AITranslator();

    // ── VM UI components ──────────────────────────────────────────────────
    private JTextPane codeEditor;
    private StyledDocument codeDoc;
    private StackViz stackViz;
    private JTextArea console;
    private JTextField aiInput;
    private JTextArea aiPreview;

    private JButton btnRun, btnPause, btnStep, btnStop, btnClear;
    private JButton btnLoad, btnSave;
    private JButton btnTranslate, btnInsert, btnRunAI;
    private JSlider speedSlider;

    private JLabel lblStatus, lblIP, lblDepth, lblMode;

    // ── Language Runner components ────────────────────────────────────────
    private final LanguageRunner langRunner = new LanguageRunner();
    private Thread langRunThread;
    private JComboBox<LanguageRunner.Language> langSelector;
    private JTextArea langCodeEditor;
    private JTextPane langConsole;
    private StyledDocument langConsoleDoc;
    private JButton btnLangRun, btnLangStop, btnLangClear, btnLangTemplate;
    private JLabel lblLangStatus;

    // current line highlight tag
    private Object currentHighlight;

    // ── Constructor ────────────────────────────────────────────────────────
    public GUI() {
        super("⚡  Custom VM IDE  —  Stack Machine  &  Language Runner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        applyGlobalLAF();
        buildFrame();
        setSize(1400, 880);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        loadDefaultProgram();
        setVisible(true);
    }

    // ── Look-and-feel ──────────────────────────────────────────────────────
    private void applyGlobalLAF() {
        UIManager.put("Panel.background", C_BASE);
        UIManager.put("ScrollPane.background", C_MANTLE);
        UIManager.put("Viewport.background", C_MANTLE);
        UIManager.put("TextArea.background", C_MANTLE);
        UIManager.put("TextArea.foreground", C_TEXT);
        UIManager.put("TextArea.caretForeground", C_BLUE);
        UIManager.put("TextField.background", C_SURFACE);
        UIManager.put("TextField.foreground", C_TEXT);
        UIManager.put("TextField.caretForeground", C_BLUE);
        UIManager.put("Label.foreground", C_TEXT);
        UIManager.put("Button.background", C_SURFACE);
        UIManager.put("Button.foreground", C_TEXT);
        UIManager.put("ScrollBar.thumb", C_SURFACE);
        UIManager.put("ScrollBar.track", C_MANTLE);
        UIManager.put("SplitPane.background", C_BASE);
        UIManager.put("SplitPane.dividerSize", 6);
    }

    // ── Frame assembly ─────────────────────────────────────────────────────
    private void buildFrame() {
        // Build the VM IDE panel
        JPanel vmPanel = new JPanel(new BorderLayout(0, 0));
        vmPanel.setBackground(C_BASE);
        vmPanel.add(buildHeader(), BorderLayout.NORTH);
        vmPanel.add(buildMainSplit(), BorderLayout.CENTER);
        vmPanel.add(buildBottom(), BorderLayout.SOUTH);

        // Tabbed pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(C_MANTLE);
        tabs.setForeground(C_TEXT);
        tabs.setFont(F_BOLD);
        tabs.addTab("⚡  VM IDE", vmPanel);
        tabs.addTab("🖥  Language Runner", buildLanguageRunnerTab());

        // Style tab area
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Slightly taller tab header appearance
        UIManager.put("TabbedPane.selected", C_SURFACE);
        UIManager.put("TabbedPane.background", C_MANTLE);
        UIManager.put("TabbedPane.foreground", C_TEXT);

        setContentPane(tabs);
    }

    // ── Header (gradient banner + toolbar) ────────────────────────────────
    private JPanel buildHeader() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(C_CRUST);

        // Gradient title bar
        JPanel titleBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(89, 85, 185),
                        getWidth(), 0, new Color(59, 130, 246));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        titleBar.setPreferredSize(new Dimension(0, 42));
        titleBar.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 10));
        JLabel title = new JLabel("⚡  Custom VM IDE  —  Stack Machine");
        title.setFont(F_H1);
        title.setForeground(Color.WHITE);
        titleBar.add(title);
        wrap.add(titleBar, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        toolbar.setBackground(C_CRUST);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_SURFACE));

        btnLoad = mkBtn("📂 Load", C_SURFACE);
        btnSave = mkBtn("💾 Save", C_SURFACE);
        btnRun = mkBtn("▶  Run", new Color(34, 197, 94));
        btnPause = mkBtn("⏸ Pause", new Color(234, 179, 8));
        btnStep = mkBtn("👣 Step", new Color(59, 130, 246));
        btnStop = mkBtn("⏹ Stop", new Color(239, 68, 68));
        btnClear = mkBtn("🗑 Clear", C_SURFACE);

        btnPause.setEnabled(false);
        btnStep.setEnabled(false);
        btnStop.setEnabled(false);

        btnLoad.addActionListener(e -> loadFile());
        btnSave.addActionListener(e -> saveFile());
        btnRun.addActionListener(e -> runProgram());
        btnPause.addActionListener(e -> togglePause());
        btnStep.addActionListener(e -> stepProgram());
        btnStop.addActionListener(e -> stopProgram());
        btnClear.addActionListener(e -> clearAll());

        toolbar.add(btnLoad);
        toolbar.add(btnSave);
        toolbar.add(new JSeparator(JSeparator.VERTICAL));
        toolbar.add(btnRun);
        toolbar.add(btnPause);
        toolbar.add(btnStep);
        toolbar.add(btnStop);
        toolbar.add(btnClear);

        // Speed slider
        JLabel spdLbl = lbl("Speed:");
        spdLbl.setForeground(C_OVERLAY);
        speedSlider = new JSlider(0, 900, 700); // 0=fast, 900=slow; actual delay = 1000-val
        speedSlider.setBackground(C_CRUST);
        speedSlider.setForeground(C_TEXT);
        speedSlider.setPreferredSize(new Dimension(130, 28));
        speedSlider.setToolTipText("Execution speed (right = faster)");
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(spdLbl);
        toolbar.add(speedSlider);

        wrap.add(toolbar, BorderLayout.SOUTH);
        return wrap;
    }

    // ── Main 3-column split ────────────────────────────────────────────────
    private JSplitPane buildMainSplit() {
        JSplitPane left = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildEditorPanel(), buildStackPanel());
        left.setDividerLocation(480);
        left.setBackground(C_BASE);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                left, buildConsolePanel());
        main.setDividerLocation(740);
        main.setBackground(C_BASE);
        return main;
    }

    // ── Code editor ───────────────────────────────────────────────────────
    private JPanel buildEditorPanel() {
        JPanel panel = card();
        panel.setLayout(new BorderLayout(0, 0));
        panel.setBorder(titledBorder("📝  Code Editor"));

        // Line-number gutter
        JTextArea lineNums = new JTextArea("1");
        lineNums.setFont(F_CODE);
        lineNums.setBackground(C_CRUST);
        lineNums.setForeground(C_OVERLAY);
        lineNums.setEditable(false);
        lineNums.setMargin(new Insets(4, 6, 4, 6));
        lineNums.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_SURFACE));

        codeEditor = new JTextPane();
        codeDoc = codeEditor.getStyledDocument();
        codeEditor.setFont(F_CODE);
        codeEditor.setBackground(C_MANTLE);
        codeEditor.setForeground(C_TEXT);
        codeEditor.setCaretColor(C_BLUE);
        codeEditor.setMargin(new Insets(4, 8, 4, 4));
        codeEditor.setEditable(true);

        // Sync line numbers and trigger syntax highlight on every change
        codeDoc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                syncLines(lineNums);
                scheduleSyntax();
            }

            public void removeUpdate(DocumentEvent e) {
                syncLines(lineNums);
                scheduleSyntax();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });

        JScrollPane scroll = new JScrollPane(codeEditor);
        scroll.setRowHeaderView(lineNums);
        scroll.getViewport().setBackground(C_MANTLE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // debounced syntax timer
    private Timer syntaxTimer;

    private void scheduleSyntax() {
        if (syntaxTimer != null)
            syntaxTimer.stop();
        syntaxTimer = new Timer(250, e -> highlightSyntax());
        syntaxTimer.setRepeats(false);
        syntaxTimer.start();
    }

    private void syncLines(JTextArea lnPanel) {
        SwingUtilities.invokeLater(() -> {
            String text = codeEditor.getText();
            int lines = text.split("\n", -1).length;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lines; i++)
                sb.append(i).append("\n");
            lnPanel.setText(sb.toString());
        });
    }

    /** Simple keyword-based syntax colouring for the code editor. */
    private void highlightSyntax() {
        SwingUtilities.invokeLater(() -> {
            try {
                String text = codeEditor.getText();
                // Reset all to default
                SimpleAttributeSet def = new SimpleAttributeSet();
                StyleConstants.setForeground(def, C_TEXT);
                codeDoc.setCharacterAttributes(0, text.length(), def, true);

                // Comments (#...)
                int i = 0;
                for (String line : text.split("\n", -1)) {
                    if (line.trim().startsWith("#")) {
                        SimpleAttributeSet a = new SimpleAttributeSet();
                        StyleConstants.setForeground(a, C_OVERLAY);
                        StyleConstants.setItalic(a, true);
                        codeDoc.setCharacterAttributes(i, line.length(), a, false);
                    }
                    i += line.length() + 1;
                }

                // Numbers
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\b\\d+(?:\\.\\d+)?\\b").matcher(text);
                while (m.find()) {
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    StyleConstants.setForeground(a, C_YELLOW);
                    codeDoc.setCharacterAttributes(m.start(), m.end() - m.start(), a, false);
                }

                // Keywords
                String[][] kw = {
                        { "\\bPUSH\\b", "#89b4fa" }, // blue
                        { "\\bADD\\b|\\bSUB\\b|\\bMUL\\b|\\bDIV\\b", "#cba6f7" }, // mauve
                        { "\\bPRINT\\b", "#94e2d5" }, // teal
                        { "\\bHALT\\b", "#f38ba8" } // red
                };
                for (String[] kv : kw) {
                    m = java.util.regex.Pattern.compile(kv[0]).matcher(text);
                    Color c = Color.decode(kv[1]);
                    while (m.find()) {
                        SimpleAttributeSet a = new SimpleAttributeSet();
                        StyleConstants.setForeground(a, c);
                        StyleConstants.setBold(a, true);
                        codeDoc.setCharacterAttributes(m.start(), m.end() - m.start(), a, false);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    // ── Stack visualizer panel ────────────────────────────────────────────
    private JPanel buildStackPanel() {
        JPanel panel = card();
        panel.setLayout(new BorderLayout());
        panel.setBorder(titledBorder("📚  Stack"));

        stackViz = new StackViz();
        panel.add(stackViz, BorderLayout.CENTER);

        // Depth label below the stack
        lblDepth = lbl("Depth: 0");
        lblDepth.setHorizontalAlignment(SwingConstants.CENTER);
        lblDepth.setForeground(C_OVERLAY);
        lblDepth.setFont(F_SMALL);
        panel.add(lblDepth, BorderLayout.SOUTH);
        return panel;
    }

    // ── Output console ────────────────────────────────────────────────────
    private JPanel buildConsolePanel() {
        JPanel panel = card();
        panel.setLayout(new BorderLayout());
        panel.setBorder(titledBorder("📤  Output Console"));

        console = new JTextArea();
        console.setFont(F_CODE);
        console.setBackground(C_CRUST);
        console.setForeground(C_GREEN);
        console.setCaretColor(C_GREEN);
        console.setEditable(false);
        console.setMargin(new Insets(6, 10, 6, 10));

        JScrollPane sp = new JScrollPane(console);
        sp.getViewport().setBackground(C_CRUST);
        sp.setBorder(BorderFactory.createEmptyBorder());
        panel.add(sp, BorderLayout.CENTER);

        JButton clrBtn = mkBtn("Clear Output", C_SURFACE);
        clrBtn.addActionListener(e -> console.setText(""));
        clrBtn.setFont(F_SMALL);
        panel.add(clrBtn, BorderLayout.SOUTH);
        return panel;
    }

    // ── Bottom = AI panel + status bar ────────────────────────────────────
    private JPanel buildBottom() {
        JPanel wrap = new JPanel(new BorderLayout(0, 0));
        wrap.setBackground(C_BASE);
        wrap.add(buildAIPanel(), BorderLayout.CENTER);
        wrap.add(buildStatusBar(), BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(C_MANTLE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_SURFACE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Left label
        JLabel aiLbl = lbl("🤖  AI Translator:");
        aiLbl.setFont(F_BOLD);
        aiLbl.setForeground(C_MAUVE);

        // Input field
        aiInput = new JTextField("e.g. add 5 and 10");
        aiInput.setFont(F_CODE);
        aiInput.setForeground(C_OVERLAY);
        aiInput.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (aiInput.getText().startsWith("e.g.")) {
                    aiInput.setText("");
                    aiInput.setForeground(C_TEXT);
                }
            }
        });
        aiInput.addActionListener(e -> translateAI());

        // Preview area (shows generated VM code)
        aiPreview = new JTextArea(3, 30);
        aiPreview.setFont(F_CODE);
        aiPreview.setBackground(C_CRUST);
        aiPreview.setForeground(C_TEAL);
        aiPreview.setEditable(false);
        aiPreview.setMargin(new Insets(4, 8, 4, 8));
        JScrollPane previewSP = new JScrollPane(aiPreview);
        previewSP.setBorder(BorderFactory.createLineBorder(C_SURFACE));

        btnTranslate = mkBtn("🔄 Translate", C_MAUVE);
        btnTranslate.setForeground(Color.WHITE);
        btnInsert = mkBtn("⊕ Insert", C_SURFACE);
        btnRunAI = mkBtn("▶ Run", new Color(34, 197, 94));
        btnInsert.setEnabled(false);
        btnRunAI.setEnabled(false);

        btnTranslate.addActionListener(e -> translateAI());
        btnInsert.addActionListener(e -> insertAICode());
        btnRunAI.addActionListener(e -> {
            insertAICode();
            runProgram();
        });

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(C_MANTLE);
        inputRow.add(aiLbl, BorderLayout.WEST);
        inputRow.add(aiInput, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnRow.setBackground(C_MANTLE);
        btnRow.add(btnTranslate);
        btnRow.add(btnInsert);
        btnRow.add(btnRunAI);

        JPanel leftCol = new JPanel(new BorderLayout(0, 4));
        leftCol.setBackground(C_MANTLE);
        leftCol.add(inputRow, BorderLayout.NORTH);
        leftCol.add(btnRow, BorderLayout.SOUTH);

        panel.add(leftCol, BorderLayout.WEST);
        panel.add(previewSP, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        bar.setBackground(C_CRUST);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_SURFACE));

        lblStatus = lbl("● Ready");
        lblStatus.setForeground(C_GREEN);
        lblIP = lbl("IP: —");
        lblIP.setForeground(C_OVERLAY);
        lblDepth = lbl("Depth: 0");
        lblDepth.setForeground(C_OVERLAY);
        lblMode = lbl("Mode: Idle");
        lblMode.setForeground(C_OVERLAY);

        for (JLabel l : new JLabel[] { lblStatus, lblIP, lblDepth, lblMode })
            l.setFont(F_SMALL);

        bar.add(lblStatus);
        bar.add(sep());
        bar.add(lblIP);
        bar.add(sep());
        bar.add(lblDepth);
        bar.add(sep());
        bar.add(lblMode);
        return bar;
    }

    // ── Actions ────────────────────────────────────────────────────────────
    private void loadFile() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Open .vm Program");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ProgramLoader loader = new ProgramLoader(fc.getSelectedFile().getAbsolutePath());
                List<String> lines = loader.load();
                codeEditor.setText(String.join("\n", lines));
                setStatus("Loaded: " + fc.getSelectedFile().getName(), C_GREEN);
            } catch (IOException ex) {
                setStatus("Load failed: " + ex.getMessage(), C_RED);
            }
        }
    }

    private void saveFile() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Save .vm Program");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                pw.print(codeEditor.getText());
                setStatus("Saved: " + fc.getSelectedFile().getName(), C_GREEN);
            } catch (IOException ex) {
                setStatus("Save failed: " + ex.getMessage(), C_RED);
            }
        }
    }

    private void runProgram() {
        stopProgram(); // ensure any previous run is halted

        try {
            List<String> raw = ProgramLoader.loadFromString(codeEditor.getText());
            currentProgram = new InstructionParser().parse(raw);
        } catch (Exception ex) {
            appendConsole("⚠  Parse error: " + ex.getMessage(), C_RED);
            setStatus("Parse error", C_RED);
            return;
        }

        console.setText("");
        stackViz.update(new double[0]);

        interpreter = new Interpreter();
        interpreter.setProgram(currentProgram);
        interpreter.setListener(this);
        int delay = 1000 - speedSlider.getValue(); // 100-1000 ms range
        interpreter.setStepDelay(Math.max(50, delay));

        execThread = new Thread(interpreter, "VM-Exec");
        execThread.setDaemon(true);
        execThread.start();

        setRunning(true);
        setStatus("● Running", C_GREEN);
        lblMode.setText("Mode: Run");
    }

    private void togglePause() {
        if (interpreter == null)
            return;
        if (interpreter.isPaused()) {
            interpreter.resume();
            btnPause.setText("⏸ Pause");
            setStatus("● Running", C_GREEN);
            lblMode.setText("Mode: Run");
        } else {
            interpreter.pause();
            btnPause.setText("▶ Resume");
            setStatus("⏸ Paused", C_YELLOW);
            lblMode.setText("Mode: Paused");
        }
    }

    private void stepProgram() {
        if (interpreter == null) {
            // Start in step mode: parse then pause immediately
            try {
                List<String> raw = ProgramLoader.loadFromString(codeEditor.getText());
                currentProgram = new InstructionParser().parse(raw);
            } catch (Exception ex) {
                appendConsole("⚠  Parse error: " + ex.getMessage(), C_RED);
                return;
            }
            console.setText("");
            stackViz.update(new double[0]);
            interpreter = new Interpreter();
            interpreter.setProgram(currentProgram);
            interpreter.setListener(this);
            interpreter.setStepDelay(0);
            interpreter.pause(); // start paused
            execThread = new Thread(interpreter, "VM-Step");
            execThread.setDaemon(true);
            execThread.start();
            setRunning(true);
        }
        interpreter.requestStep();
        setStatus("👣 Stepped", C_BLUE);
        lblMode.setText("Mode: Step");
    }

    private void stopProgram() {
        if (interpreter != null)
            interpreter.halt();
        if (execThread != null)
            execThread.interrupt();
        interpreter = null;
        execThread = null;
        setRunning(false);
        setStatus("⏹ Stopped", C_OVERLAY);
        lblMode.setText("Mode: Idle");
        clearHighlight();
    }

    private void clearAll() {
        stopProgram();
        codeEditor.setText("");
        console.setText("");
        stackViz.update(new double[0]);
        aiPreview.setText("");
        updateStatusBar(-1, 0);
    }

    // ── AI actions ─────────────────────────────────────────────────────────
    private void translateAI() {
        String input = aiInput.getText().trim();
        if (input.isEmpty() || input.startsWith("e.g."))
            return;

        AITranslator.TranslationResult r = ai.translate(input);
        if (r.success) {
            aiPreview.setForeground(C_TEAL);
            aiPreview.setText(r.vmCode + "\nHALT");
            btnInsert.setEnabled(true);
            btnRunAI.setEnabled(true);
            setStatus("AI: Translated — " + r.explanation, C_MAUVE);
        } else {
            aiPreview.setForeground(C_RED);
            aiPreview.setText(r.explanation);
            btnInsert.setEnabled(false);
            btnRunAI.setEnabled(false);
            setStatus("AI: Could not translate", C_RED);
        }
    }

    private void insertAICode() {
        String code = aiPreview.getText().trim();
        if (!code.isEmpty()) {
            codeEditor.setText(code);
            highlightSyntax();
        }
    }

    // ── InterpreterListener callbacks (called from VM thread) ─────────────
    @Override
    public void onInstructionExecuted(int ip, Instruction instr, double[] stackState) {
        SwingUtilities.invokeLater(() -> {
            stackViz.update(stackState);
            updateStatusBar(ip, stackState.length);
            highlightLine(ip);
            appendConsole("[IP=" + ip + "] " + instr, C_OVERLAY);
        });
    }

    @Override
    public void onOutput(String text) {
        SwingUtilities.invokeLater(() -> appendConsole(">>> " + text, C_GREEN));
    }

    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            appendConsole("⚠  ERROR: " + error, C_RED);
            setStatus("⚠ Error: " + error, C_RED);
            setRunning(false);
        });
    }

    @Override
    public void onHalt() {
        SwingUtilities.invokeLater(() -> {
            appendConsole("— HALT —", C_PINK);
            setStatus("✔ Halted", C_BLUE);
            setRunning(false);
            clearHighlight();
        });
    }

    @Override
    public void onReset() {
        SwingUtilities.invokeLater(() -> {
            stackViz.update(new double[0]);
            updateStatusBar(-1, 0);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void appendConsole(String text, Color color) {
        console.setForeground(color);
        console.append(text + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }

    private void setStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }

    private void updateStatusBar(int ip, int depth) {
        lblIP.setText("IP: " + (ip >= 0 ? ip : "—"));
        lblDepth.setText("Depth: " + depth);
        stackViz.lblDepth.setText("Depth: " + depth);
    }

    private void setRunning(boolean running) {
        btnRun.setEnabled(!running);
        btnPause.setEnabled(running);
        btnStep.setEnabled(true);
        btnStop.setEnabled(running);
        btnLoad.setEnabled(!running);
        btnSave.setEnabled(true);
    }

    /**
     * Highlight the line in the editor that corresponds to instruction index ip.
     */
    private void highlightLine(int ip) {
        try {
            String text = codeEditor.getText();
            String[] allLines = text.split("\n", -1);
            // Map ip → non-blank, non-comment line
            int instrIdx = 0, charPos = 0;
            for (String line : allLines) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    if (instrIdx == ip) {
                        clearHighlight();
                        int len = line.length();
                        // Use DefaultHighlighter
                        DefaultHighlighter.DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
                                new Color(88, 91, 112, 120));
                        currentHighlight = codeEditor.getHighlighter()
                                .addHighlight(charPos, charPos + len, painter);
                        // Scroll to visible
                        codeEditor.setCaretPosition(charPos);
                        return;
                    }
                    instrIdx++;
                }
                charPos += line.length() + 1;
            }
        } catch (Exception ignored) {
        }
    }

    private void clearHighlight() {
        if (currentHighlight != null) {
            codeEditor.getHighlighter().removeHighlight(currentHighlight);
            currentHighlight = null;
        }
    }

    private void loadDefaultProgram() {
        String sample = "# Sample VM program\n" +
                "# Example 1: add 5 + 10\n" +
                "PUSH 5\n" +
                "PUSH 10\n" +
                "ADD\n" +
                "PRINT\n" +
                "\n" +
                "# Example 2: 6 × 7\n" +
                "PUSH 6\n" +
                "PUSH 7\n" +
                "MUL\n" +
                "PRINT\n" +
                "\n" +
                "# Example 3: (3 + 4) * 2\n" +
                "PUSH 3\n" +
                "PUSH 4\n" +
                "ADD\n" +
                "PUSH 2\n" +
                "MUL\n" +
                "PRINT\n" +
                "\n" +
                "HALT\n";
        codeEditor.setText(sample);
        highlightSyntax();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── Language Runner Tab ───────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildLanguageRunnerTab() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_BASE);

        // ── Top header bar ────────────────────────────────────────────────
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(16, 132, 106),
                        getWidth(), 0, new Color(5, 150, 105));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 42));
        header.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 10));
        JLabel hTitle = new JLabel("🖥  Language Runner  —  Python · C · C++");
        hTitle.setFont(F_H1);
        hTitle.setForeground(Color.WHITE);
        header.add(hTitle);

        // ── Toolbar ───────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        toolbar.setBackground(C_CRUST);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_SURFACE));

        // Language selector
        JLabel langLbl = lbl("Language:");
        langLbl.setForeground(C_OVERLAY);
        langSelector = new JComboBox<>(LanguageRunner.Language.values());
        langSelector.setFont(F_BOLD);
        langSelector.setBackground(C_SURFACE);
        langSelector.setForeground(C_TEXT);
        langSelector.setFocusable(false);
        langSelector.addActionListener(e -> onLanguageChanged());

        btnLangTemplate = mkBtn("📋 Template", C_SURFACE);
        btnLangRun = mkBtn("▶  Run", new Color(34, 197, 94));
        btnLangStop = mkBtn("⏹ Stop", new Color(239, 68, 68));
        btnLangClear = mkBtn("🗑 Clear", C_SURFACE);

        btnLangStop.setEnabled(false);

        btnLangTemplate.addActionListener(e -> loadLangTemplate());
        btnLangRun.addActionListener(e -> runLangProgram());
        btnLangStop.addActionListener(e -> stopLangProgram());
        btnLangClear.addActionListener(e -> clearLangOutput());

        toolbar.add(langLbl);
        toolbar.add(langSelector);
        toolbar.add(new JSeparator(JSeparator.VERTICAL));
        toolbar.add(btnLangTemplate);
        toolbar.add(btnLangRun);
        toolbar.add(btnLangStop);
        toolbar.add(btnLangClear);

        // Status label on the right
        lblLangStatus = lbl("● Ready");
        lblLangStatus.setForeground(C_GREEN);
        lblLangStatus.setFont(F_SMALL);
        JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        statusRight.setBackground(C_CRUST);
        statusRight.add(lblLangStatus);

        JPanel toolbarWrap = new JPanel(new BorderLayout());
        toolbarWrap.setBackground(C_CRUST);
        toolbarWrap.add(toolbar, BorderLayout.WEST);
        toolbarWrap.add(statusRight, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(C_CRUST);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(toolbarWrap, BorderLayout.SOUTH);
        root.add(topPanel, BorderLayout.NORTH);

        // ── Code Editor (left) ────────────────────────────────────────────
        langCodeEditor = new JTextArea();
        langCodeEditor.setFont(F_CODE);
        langCodeEditor.setBackground(C_MANTLE);
        langCodeEditor.setForeground(C_TEXT);
        langCodeEditor.setCaretColor(C_BLUE);
        langCodeEditor.setMargin(new Insets(8, 12, 8, 8));
        langCodeEditor.setTabSize(4);

        // Line-number gutter for lang editor
        JTextArea langLineNums = new JTextArea("1");
        langLineNums.setFont(F_CODE);
        langLineNums.setBackground(C_CRUST);
        langLineNums.setForeground(C_OVERLAY);
        langLineNums.setEditable(false);
        langLineNums.setMargin(new Insets(8, 6, 8, 6));
        langLineNums.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_SURFACE));

        langCodeEditor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                syncLangLines(langLineNums);
            }

            public void removeUpdate(DocumentEvent e) {
                syncLangLines(langLineNums);
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });

        JScrollPane codeScroll = new JScrollPane(langCodeEditor);
        codeScroll.setRowHeaderView(langLineNums);
        codeScroll.getViewport().setBackground(C_MANTLE);
        codeScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(titledBorder("📝  Source Code"));
        editorPanel.setBackground(C_MANTLE);
        editorPanel.add(codeScroll, BorderLayout.CENTER);

        // ── Output Console (right) ────────────────────────────────────────
        langConsole = new JTextPane();
        langConsoleDoc = langConsole.getStyledDocument();
        langConsole.setFont(F_CODE);
        langConsole.setBackground(C_CRUST);
        langConsole.setEditable(false);
        langConsole.setMargin(new Insets(8, 12, 8, 8));

        JScrollPane consoleScroll = new JScrollPane(langConsole);
        consoleScroll.getViewport().setBackground(C_CRUST);
        consoleScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(titledBorder("📤  Output"));
        consolePanel.setBackground(C_CRUST);
        consolePanel.add(consoleScroll, BorderLayout.CENTER);

        // ── Split ─────────────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                editorPanel, consolePanel);
        split.setDividerLocation(700);
        split.setBackground(C_BASE);
        root.add(split, BorderLayout.CENTER);

        // ── Info bar at bottom ────────────────────────────────────────────
        JPanel infoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        infoBar.setBackground(C_CRUST);
        infoBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_SURFACE));

        JLabel reqLabel = lbl("Requirements: ");
        reqLabel.setForeground(C_OVERLAY);
        reqLabel.setFont(F_SMALL);

        JLabel reqDetail = lbl(
                "Python → python on PATH   |   C → gcc on PATH (MinGW-w64)   |   C++ → g++ on PATH (MinGW-w64)");
        reqDetail.setForeground(C_OVERLAY);
        reqDetail.setFont(F_SMALL);

        infoBar.add(reqLabel);
        infoBar.add(reqDetail);
        root.add(infoBar, BorderLayout.SOUTH);

        // Load default template
        langCodeEditor.setText(LanguageRunner.starterTemplate(LanguageRunner.Language.PYTHON));

        return root;
    }

    // ── Language Runner Actions ────────────────────────────────────────────

    private void onLanguageChanged() {
        // Optionally auto-load the template for newly selected language
        // Only if the editor is empty or has the previous template
        LanguageRunner.Language lang = (LanguageRunner.Language) langSelector.getSelectedItem();
        if (lang != null) {
            langCodeEditor.setText(LanguageRunner.starterTemplate(lang));
            setLangStatus("● Ready  |  " + lang.displayName + " selected", C_GREEN);
        }
    }

    private void loadLangTemplate() {
        LanguageRunner.Language lang = (LanguageRunner.Language) langSelector.getSelectedItem();
        if (lang != null)
            langCodeEditor.setText(LanguageRunner.starterTemplate(lang));
    }

    private void runLangProgram() {
        stopLangProgram();

        LanguageRunner.Language lang = (LanguageRunner.Language) langSelector.getSelectedItem();
        if (lang == null)
            return;

        String source = langCodeEditor.getText();
        if (source.isBlank()) {
            appendLangConsole("⚠ No code to run!", C_RED);
            return;
        }

        // Clear console
        langConsole.setText("");

        appendLangConsole("═══════════════════════════════════════", C_SURFACE);
        appendLangConsole(" Running: " + lang.displayName, C_BLUE);
        appendLangConsole("═══════════════════════════════════════", C_SURFACE);

        setLangStatus("● Running " + lang.displayName + "...", C_GREEN);
        btnLangRun.setEnabled(false);
        btnLangStop.setEnabled(true);

        langRunThread = langRunner.run(lang, source, new LanguageRunner.RunListener() {
            @Override
            public void onOutput(String line, boolean isError) {
                SwingUtilities.invokeLater(() -> appendLangConsole(line, isError ? C_RED : C_GREEN));
            }

            @Override
            public void onFinished(int exitCode, long elapsedMs) {
                SwingUtilities.invokeLater(() -> {
                    appendLangConsole("", C_TEXT);
                    appendLangConsole("═══════════════════════════════════════", C_SURFACE);
                    String msg = exitCode == 0
                            ? "✔ Finished successfully"
                            : "✘ Exited with code " + exitCode;
                    Color col = exitCode == 0 ? C_GREEN : C_RED;
                    appendLangConsole(" " + msg + "  (" + elapsedMs + " ms)", col);
                    appendLangConsole("═══════════════════════════════════════", C_SURFACE);
                    setLangStatus(msg, col);
                    btnLangRun.setEnabled(true);
                    btnLangStop.setEnabled(false);
                });
            }

            @Override
            public void onFatalError(String message) {
                SwingUtilities.invokeLater(() -> {
                    appendLangConsole("⚠ FATAL: " + message, C_RED);
                    setLangStatus("⚠ Error", C_RED);
                    btnLangRun.setEnabled(true);
                    btnLangStop.setEnabled(false);
                });
            }
        });
    }

    private void stopLangProgram() {
        if (langRunThread != null && langRunThread.isAlive()) {
            langRunThread.interrupt();
            appendLangConsole("⏹ Stopped by user.", C_YELLOW);
            setLangStatus("⏹ Stopped", C_OVERLAY);
        }
        langRunThread = null;
        btnLangRun.setEnabled(true);
        btnLangStop.setEnabled(false);
    }

    private void clearLangOutput() {
        langConsole.setText("");
        setLangStatus("● Ready", C_GREEN);
    }

    private void appendLangConsole(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, color);
                StyleConstants.setFontFamily(attr, "Consolas");
                StyleConstants.setFontSize(attr, 13);
                langConsoleDoc.insertString(langConsoleDoc.getLength(), text + "\n", attr);
                langConsole.setCaretPosition(langConsoleDoc.getLength());
            } catch (Exception ignored) {
            }
        });
    }

    private void setLangStatus(String msg, Color color) {
        lblLangStatus.setText(msg);
        lblLangStatus.setForeground(color);
    }

    private void syncLangLines(JTextArea lnPanel) {
        SwingUtilities.invokeLater(() -> {
            String text = langCodeEditor.getText();
            int lines = text.split("\n", -1).length;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lines; i++)
                sb.append(i).append("\n");
            lnPanel.setText(sb.toString());
        });
    }

    // ── Widget factories ──────────────────────────────────────────────────
    private JButton mkBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(F_BOLD);
        b.setBackground(bg);
        b.setForeground(C_TEXT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        b.addMouseListener(new MouseAdapter() {
            final Color orig = bg;

            public void mouseEntered(MouseEvent e) {
                b.setBackground(orig.brighter());
            }

            public void mouseExited(MouseEvent e) {
                b.setBackground(orig);
            }
        });
        return b;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(F_UI);
        l.setForeground(C_TEXT);
        return l;
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(C_MANTLE);
        return p;
    }

    private Border titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_SURFACE, 1, true), title);
        tb.setTitleFont(F_BOLD);
        tb.setTitleColor(C_BLUE);
        return BorderFactory.createCompoundBorder(
                tb, BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setPreferredSize(new Dimension(1, 14));
        s.setForeground(C_SURFACE);
        return s;
    }

    // ── Inner class: Stack Visualizer ─────────────────────────────────────
    class StackViz extends JPanel {
        private double[] data = new double[0];
        JLabel lblDepth;

        StackViz() {
            setBackground(C_CRUST);
            setPreferredSize(new Dimension(180, 0));
            lblDepth = new JLabel("Depth: 0");
            lblDepth.setFont(F_SMALL);
            lblDepth.setForeground(C_OVERLAY);
        }

        void update(double[] contents) {
            this.data = contents;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            g2.setColor(C_CRUST);
            g2.fillRect(0, 0, W, H);

            final int EH = 46; // element height
            final int EW = W - 40;
            final int X = 20;
            final int BASE = H - 30;
            final int GAP = 4;
            final int SHOW = Math.min(data.length, (H - 60) / (EH + GAP));

            // Base line
            g2.setColor(C_SURFACE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(X - 4, BASE, X + EW + 4, BASE);

            // "STACK BASE" label
            g2.setFont(F_SMALL);
            g2.setColor(C_OVERLAY);
            g2.drawString("STACK BASE", X, BASE + 16);

            if (data.length == 0) {
                g2.setFont(F_UI);
                g2.setColor(C_OVERLAY);
                String msg = "Empty";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                return;
            }

            for (int i = 0; i < SHOW; i++) {
                int y = BASE - (i + 1) * (EH + GAP);
                boolean isTop = (i == data.length - 1);

                // Box fill
                Color boxColor = isTop ? C_BLUE : new Color(49, 50, 68);
                GradientPaint gp = new GradientPaint(X, y, boxColor.brighter(),
                        X + EW, y + EH, boxColor.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(X, y, EW, EH, 10, 10);

                // Box border
                g2.setColor(isTop ? C_BLUE.brighter() : C_SURFACE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(X, y, EW, EH, 10, 10);

                // Index label on left
                g2.setColor(C_OVERLAY);
                g2.setFont(F_SMALL);
                g2.drawString("[" + i + "]", X - 14, y + EH / 2 + 4);

                // Value centered
                double val = data[i];
                String txt = (val == Math.floor(val) && !Double.isInfinite(val))
                        ? String.valueOf((long) val)
                        : String.format("%.3f", val);
                g2.setFont(new Font("Consolas", Font.BOLD, 14));
                g2.setColor(isTop ? Color.WHITE : C_TEXT);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(txt, X + (EW - fm.stringWidth(txt)) / 2, y + EH / 2 + 5);

                // "TOP ▲" tag on the top element
                if (isTop) {
                    g2.setColor(C_YELLOW);
                    g2.setFont(F_SMALL);
                    g2.drawString("▲ TOP", X + EW + 4, y + EH / 2 + 4);
                }
            }

            // Overflow indicator
            if (data.length > SHOW) {
                g2.setColor(C_OVERLAY);
                g2.setFont(F_SMALL);
                String more = "+" + (data.length - SHOW) + " more …";
                g2.drawString(more, X, BASE - SHOW * (EH + GAP) - 8);
            }
        }
    }
}
