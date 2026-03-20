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
 * GUI.java — VMware Workstation-style IDE
 *
 * Layout mirrors VMware Workstation:
 *  ┌─ Menu Bar ──────────────────────────────────────────┐
 *  ├─ Power Toolbar ─────────────────────────────────────┤
 *  ├─ VM Library (left) ─┬─ VM Console / Editor (center)─┤
 *  │  • Custom VM         │  [OS Badge]  [Status]         │
 *  │  • Python            │  Terminal-style output        │
 *  │  • C                 ├───────────────────────────────┤
 *  │  • C++               │  Code Editor                  │
 *  │  • Java              │                               │
 *  │  • JavaScript        │                               │
 *  ├─ Virtual HW ────────┤                               │
 *  │  CPU / RAM / OS/NET  │                               │
 *  └─────────────────────┴───────────────────────────────┘
 *  └─ Status Bar ────────────────────────────────────────┘
 */
public class GUI extends JFrame implements Interpreter.InterpreterListener {

    // ── VMware colour palette ───────────────────────────────────────────────
    static final Color C_BG       = new Color(28,  28,  28);
    static final Color C_SIDEBAR  = new Color(37,  37,  38);
    static final Color C_TOOLBAR  = new Color(45,  45,  48);
    static final Color C_CONSOLE  = new Color(13,  17,  23);
    static final Color C_EDITOR   = new Color(22,  22,  30);
    static final Color C_TEXT     = new Color(212, 212, 212);
    static final Color C_DIM      = new Color(110, 110, 120);
    static final Color C_ACCENT   = new Color(0,   120, 212);   // VMware blue
    static final Color C_GREEN    = new Color(78,  201, 176);
    static final Color C_RED      = new Color(244,  71,  71);
    static final Color C_YELLOW   = new Color(220, 170,  50);
    static final Color C_SELECTED = new Color(  9,  71, 113);
    static final Color C_HOVER    = new Color( 55,  55,  60);
    static final Color C_BORDER   = new Color( 60,  60,  65);
    static final Color C_HEADER   = new Color( 18,  18,  20);
    static final Color C_PURPLE   = new Color(180, 130, 255);

    // ── Fonts ───────────────────────────────────────────────────────────────
    static final Font F_MONO  = new Font("Consolas",  Font.PLAIN,  13);
    static final Font F_UI    = new Font("Segoe UI",  Font.PLAIN,  12);
    static final Font F_BOLD  = new Font("Segoe UI",  Font.BOLD,   12);
    static final Font F_SMALL = new Font("Segoe UI",  Font.PLAIN,  11);
    static final Font F_H1    = new Font("Segoe UI",  Font.BOLD,   14);
    static final Font F_H2    = new Font("Segoe UI",  Font.BOLD,   11);

    // ── VM Environments ────────────────────────────────────────────────────
    enum VMEnv {
        CUSTOM_VM  ("⚡  Custom Stack VM",  "Ubuntu 22.04 LTS",   "Stack-Based Language VM", "🟢"),
        PYTHON     ("🐍  Python 3",         "Python 3.11 Runtime", "Interpreted / Dynamic",   "🟡"),
        C_LANG     ("⚙   C Language",       "GCC 11 Compiler",     "Compiled / Systems",      "🔵"),
        CPP        ("⚙   C++ Language",     "G++ 11 Compiler",     "Compiled / OOP",          "🔵"),
        JAVA       ("☕  Java",             "JVM 17 Runtime",      "Bytecode / OOP",          "🟠"),
        JAVASCRIPT ("🌐  JavaScript",       "Node.js 18 Runtime",  "Interpreted / Async",     "🟢");

        final String label, osInfo, type, dot;
        VMEnv(String l, String o, String t, String d) { label=l; osInfo=o; type=t; dot=d; }
    }

    // ── State ───────────────────────────────────────────────────────────────
    private VMEnv activeEnv = VMEnv.CUSTOM_VM;
    private Interpreter    interpreter;
    private Thread         execThread;
    private List<Instruction> currentProgram;
    private final AITranslator  ai         = new AITranslator();
    private final LanguageRunner langRunner = new LanguageRunner();
    private Thread langRunThread;

    // ── UI refs ─────────────────────────────────────────────────────────────
    private JTextArea codeEditor;
    private JTextPane vmConsole;
    private StyledDocument consoleDoc;
    private JLabel lblVMName, lblOSInfo, lblVMType, lblPowerDot;
    private JLabel lblStatusMsg, lblIP, lblDepth, lblHypervisor;
    private JButton btnPowerOn, btnSuspend, btnPowerOff, btnRestart, btnSnapshot;
    private JSlider speedSlider;
    private JPanel  libraryPanel;
    private VMEnv[] envOrder = VMEnv.values();
    private VMDesktop vmDesktop;
    private JTabbedPane centerTabs;
    private JPanel[] libCards;
    private JLabel hwCPU, hwRAM, hwOS, hwNet, hwDisk;
    private StackViz stackViz;

    // ════════════════════ CONSTRUCTOR ══════════════════════════════════════
    public GUI() {
        super("VMware-Style VM — Hypervisor Multi-Language Runtime");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        globalLAF();
        buildMenuBar();
        buildFrame();
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        loadDefaultCode();
        setVisible(true);
    }

    // ── Global Look & Feel ──────────────────────────────────────────────────
    private void globalLAF() {
        UIManager.put("Panel.background",           C_BG);
        UIManager.put("ScrollPane.background",      C_SIDEBAR);
        UIManager.put("Viewport.background",        C_EDITOR);
        UIManager.put("TextArea.background",        C_EDITOR);
        UIManager.put("TextArea.foreground",        C_TEXT);
        UIManager.put("TextArea.caretForeground",   C_ACCENT);
        UIManager.put("Label.foreground",           C_TEXT);
        UIManager.put("Button.background",          C_TOOLBAR);
        UIManager.put("Button.foreground",          C_TEXT);
        UIManager.put("ScrollBar.thumb",            C_SIDEBAR);
        UIManager.put("ScrollBar.track",            C_BG);
        UIManager.put("SplitPane.background",       C_BG);
        UIManager.put("SplitPane.dividerSize",      5);
        UIManager.put("OptionPane.background",      C_SIDEBAR);
        UIManager.put("OptionPane.messageForeground", C_TEXT);
    }

    // ── Menu Bar ────────────────────────────────────────────────────────────
    private void buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(C_HEADER);
        mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        String[][] menus = {
            {"File",  "New VM...", "Open VM...", "Close VM", "---", "Exit"},
            {"View",  "Full Screen", "---", "Zoom In", "Zoom Out"},
            {"VM",    "Power On", "Shut Down", "Suspend", "Restart", "---", "Settings..."},
            {"Snapshot", "Take Snapshot...", "Manage Snapshots..."},
            {"Help",  "Documentation", "---", "About Hypervisor VM"}
        };
        for (String[] group : menus) {
            JMenu m = new JMenu(group[0]);
            m.setFont(F_UI);
            m.setForeground(C_TEXT);
            for (int i = 1; i < group.length; i++) {
                if (group[i].equals("---")) m.addSeparator();
                else { JMenuItem mi = new JMenuItem(group[i]); mi.setFont(F_UI); m.add(mi); }
            }
            mb.add(m);
        }
        setJMenuBar(mb);
    }

    // ── Main Frame ──────────────────────────────────────────────────────────
    private void buildFrame() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_BG);

        root.add(buildPowerToolbar(), BorderLayout.NORTH);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildCenterPanel());
        main.setDividerLocation(230);
        main.setBackground(C_BG);
        root.add(main, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Power Toolbar (VMware-style) ────────────────────────────────────────
    private JPanel buildPowerToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        bar.setBackground(C_HEADER);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        // VM logo area
        JLabel logo = new JLabel("  ⬡  HypervisorVM");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logo.setForeground(C_ACCENT);
        bar.add(logo);
        bar.add(makeSep());

        // Power buttons
        btnPowerOn  = powerBtn("▶  Power On",   new Color(34, 160, 90));
        btnSuspend  = powerBtn("⏸  Suspend",    new Color(180, 140, 0));
        btnPowerOff = powerBtn("⏹  Power Off",  new Color(180, 40, 40));
        btnRestart  = powerBtn("↺  Restart",    C_TOOLBAR);
        btnSnapshot = powerBtn("📷  Snapshot",  C_TOOLBAR);

        btnPowerOn .addActionListener(e -> runProgram());
        btnSuspend .addActionListener(e -> togglePause());
        btnPowerOff.addActionListener(e -> stopProgram());
        btnRestart .addActionListener(e -> { stopProgram(); runProgram(); });

        bar.add(btnPowerOn);
        bar.add(btnSuspend);
        bar.add(btnPowerOff);
        bar.add(btnRestart);
        bar.add(makeSep());
        bar.add(btnSnapshot);
        bar.add(makeSep());

        // Speed
        JLabel spdLbl = new JLabel("Speed:");
        spdLbl.setFont(F_SMALL); spdLbl.setForeground(C_DIM);
        speedSlider = new JSlider(0, 900, 700);
        speedSlider.setBackground(C_HEADER);
        speedSlider.setPreferredSize(new Dimension(120, 24));
        speedSlider.setToolTipText("Execution speed");
        bar.add(spdLbl);
        bar.add(speedSlider);
        bar.add(makeSep());

        // Step / Load / Save
        JButton btnStep = toolBtn("👣 Step");
        JButton btnLoad = toolBtn("📂 Load");
        JButton btnSave = toolBtn("💾 Save");
        JButton btnClear= toolBtn("🗑 Clear");
        btnStep.addActionListener(e -> stepProgram());
        btnLoad.addActionListener(e -> loadFile());
        btnSave.addActionListener(e -> saveFile());
        btnClear.addActionListener(e -> clearAll());
        for (JButton b : new JButton[]{btnStep,btnLoad,btnSave,btnClear}) bar.add(b);

        return bar;
    }

    // ── Sidebar: VM Library + Virtual Hardware ──────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setBackground(C_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        // VM Library header
        JPanel libHdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));
        libHdr.setBackground(C_HEADER);
        libHdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        JLabel libTitle = new JLabel("VM LIBRARY");
        libTitle.setFont(F_H2); libTitle.setForeground(C_DIM);
        libHdr.add(libTitle);

        // VM list
        libraryPanel = new JPanel();
        libraryPanel.setLayout(new BoxLayout(libraryPanel, BoxLayout.Y_AXIS));
        libraryPanel.setBackground(C_SIDEBAR);

        libCards = new JPanel[envOrder.length];
        for (int i = 0; i < envOrder.length; i++) {
            libCards[i] = makeEnvCard(envOrder[i]);
            libraryPanel.add(libCards[i]);
        }
        refreshLibrarySelection();

        JScrollPane libScroll = new JScrollPane(libraryPanel);
        libScroll.setBorder(null);
        libScroll.getViewport().setBackground(C_SIDEBAR);
        libScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel libPanel = new JPanel(new BorderLayout());
        libPanel.setBackground(C_SIDEBAR);
        libPanel.add(libHdr, BorderLayout.NORTH);
        libPanel.add(libScroll, BorderLayout.CENTER);

        // Virtual Hardware panel
        JPanel hwPanel = buildHardwarePanel();

        sidebar.add(libPanel,  BorderLayout.CENTER);
        sidebar.add(hwPanel,   BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel makeEnvCard(VMEnv env) {
        JPanel card = new JPanel(new BorderLayout(6, 0));
        card.setBackground(C_SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel dot = new JLabel(env.dot);
        dot.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));

        JLabel name = new JLabel(env.label);
        name.setFont(F_BOLD); name.setForeground(C_TEXT);

        JLabel sub = new JLabel(env.osInfo);
        sub.setFont(F_SMALL); sub.setForeground(C_DIM);

        JPanel text = new JPanel(new BorderLayout(0, 1));
        text.setBackground(C_SIDEBAR);
        text.add(name, BorderLayout.CENTER);
        text.add(sub,  BorderLayout.SOUTH);

        card.add(dot,  BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { switchVM(env); }
            public void mouseEntered(MouseEvent e) {
                if (activeEnv != env) card.setBackground(C_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                if (activeEnv != env) card.setBackground(C_SIDEBAR);
            }
        });
        return card;
    }

    private void switchVM(VMEnv env) {
        stopProgram();
        activeEnv = env;
        refreshLibrarySelection();
        lblVMName.setText(env.label);
        lblOSInfo.setText(env.osInfo);
        lblVMType.setText(env.type);
        lblPowerDot.setText("⬤ Powered Off");
        lblPowerDot.setForeground(C_DIM);
        hwOS.setText("OS:        " + env.osInfo);
        loadDefaultCode();
        appendConsole("─────────────────────────────────────────", C_BORDER);
        appendConsole("  Switched to: " + env.label, C_ACCENT);
        appendConsole("  Runtime:     " + env.osInfo, C_DIM);
        appendConsole("─────────────────────────────────────────", C_BORDER);
    }

    private void refreshLibrarySelection() {
        for (int i = 0; i < envOrder.length; i++) {
            Color bg = (envOrder[i] == activeEnv) ? C_SELECTED : C_SIDEBAR;
            libCards[i].setBackground(bg);
            for (Component c : libCards[i].getComponents()) {
                c.setBackground(bg);
                if (c instanceof JPanel)
                    for (Component cc : ((JPanel)c).getComponents()) cc.setBackground(bg);
            }
        }
    }

    // ── Virtual Hardware Info Panel ─────────────────────────────────────────
    private JPanel buildHardwarePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(C_HEADER);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel title = new JLabel("VIRTUAL HARDWARE");
        title.setFont(F_H2); title.setForeground(C_DIM);
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(6));

        hwCPU  = hwRow("CPU:       4 cores (virtual)");
        hwRAM  = hwRow("RAM:       512 MB");
        hwOS   = hwRow("OS:        Ubuntu 22.04 LTS");
        hwNet  = hwRow("Network:   NAT (Bridged)");
        hwDisk = hwRow("Storage:   /app (Docker vol)");
        hwRow("Hypervisor: Hyper-V / WSL2");

        for (JLabel lbl : new JLabel[]{hwCPU, hwRAM, hwOS, hwNet, hwDisk}) {
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            p.add(lbl);
            p.add(Box.createVerticalStrut(3));
        }
        return p;
    }

    private JLabel hwRow(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_SMALL); l.setForeground(C_DIM);
        return l;
    }

    // ── Center: VM Header + Tabs (Desktop | IDE) ──────────────────────────────
    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(C_BG);

        center.add(buildVMHeader(), BorderLayout.NORTH);

        // Build desktop panel
        vmDesktop = new VMDesktop(this);

        // Build IDE panel (console + stack + editor)
        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.setBackground(C_BG);
        upperPanel.add(buildVMConsole(), BorderLayout.CENTER);
        upperPanel.add(buildStackPanel(), BorderLayout.EAST);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                upperPanel, buildEditorPanel());
        vertical.setDividerLocation(340);
        vertical.setBackground(C_BG);

        // Tabs: Desktop | IDE
        centerTabs = new JTabbedPane();
        centerTabs.setBackground(C_HEADER);
        centerTabs.setForeground(C_TEXT);
        centerTabs.setFont(F_BOLD);
        UIManager.put("TabbedPane.selected",    C_SELECTED);
        UIManager.put("TabbedPane.background",  C_HEADER);
        UIManager.put("TabbedPane.foreground",  C_TEXT);
        centerTabs.addTab("🖥  VM Desktop", vmDesktop);
        centerTabs.addTab("⚡  IDE / Console", vertical);
        centerTabs.setSelectedIndex(0);

        center.add(centerTabs, BorderLayout.CENTER);
        return center;
    }

    // ── VM Header (OS badge + status) ───────────────────────────────────────
    private JPanel buildVMHeader() {
        JPanel hdr = new JPanel(new BorderLayout(0, 0));
        hdr.setBackground(C_HEADER);
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        hdr.setPreferredSize(new Dimension(0, 52));

        // Left: VM name + OS
        JPanel left = new JPanel(new GridLayout(2, 1, 0, 0));
        left.setBackground(C_HEADER);
        left.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        lblVMName = new JLabel(activeEnv.label);
        lblVMName.setFont(F_H1); lblVMName.setForeground(C_TEXT);

        lblOSInfo = new JLabel(activeEnv.osInfo);
        lblOSInfo.setFont(F_SMALL); lblOSInfo.setForeground(C_DIM);

        left.add(lblVMName);
        left.add(lblOSInfo);

        // Right: type + power status
        JPanel right = new JPanel(new GridLayout(2, 1, 0, 0));
        right.setBackground(C_HEADER);
        right.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        lblVMType = new JLabel(activeEnv.type);
        lblVMType.setFont(F_SMALL); lblVMType.setForeground(C_DIM);
        lblVMType.setHorizontalAlignment(SwingConstants.RIGHT);

        lblPowerDot = new JLabel("⬤ Powered Off");
        lblPowerDot.setFont(F_SMALL); lblPowerDot.setForeground(C_DIM);
        lblPowerDot.setHorizontalAlignment(SwingConstants.RIGHT);

        right.add(lblVMType);
        right.add(lblPowerDot);

        hdr.add(left,  BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── VM Console ──────────────────────────────────────────────────────────
    private JPanel buildVMConsole() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_CONSOLE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        // Console title bar
        JPanel cbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        cbar.setBackground(new Color(20, 20, 24));
        JLabel ctitle = new JLabel("  🖥  VM CONSOLE");
        ctitle.setFont(F_H2); ctitle.setForeground(C_DIM);
        JButton clrBtn = toolBtn("Clear");
        clrBtn.addActionListener(e -> { try { consoleDoc.remove(0, consoleDoc.getLength()); } catch(Exception ex){} });
        cbar.add(ctitle);
        cbar.add(clrBtn);

        vmConsole = new JTextPane();
        consoleDoc = vmConsole.getStyledDocument();
        vmConsole.setFont(F_MONO);
        vmConsole.setBackground(C_CONSOLE);
        vmConsole.setForeground(C_GREEN);
        vmConsole.setCaretColor(C_GREEN);
        vmConsole.setEditable(false);
        vmConsole.setMargin(new Insets(8, 12, 8, 8));

        JScrollPane sp = new JScrollPane(vmConsole);
        sp.setBorder(null);
        sp.getViewport().setBackground(C_CONSOLE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(cbar, BorderLayout.NORTH);
        panel.add(sp,   BorderLayout.CENTER);

        // Boot message
        SwingUtilities.invokeLater(() -> {
            appendConsole("  HypervisorVM — Linux Multi-Language Runtime", C_ACCENT);
            appendConsole("  Host OS : Ubuntu 22.04 LTS (Docker Container)", C_DIM);
            appendConsole("  Hypervisor: Hyper-V / WSL2", C_DIM);
            appendConsole("  Java:     17 (Eclipse Temurin)", C_DIM);
            appendConsole("  ─────────────────────────────────────────", C_BORDER);
            appendConsole("  Select a VM from the library and press ▶  Power On", C_TEXT);
        });

        return panel;
    }

    // ── Stack Visualizer ────────────────────────────────────────────────────
    private JPanel buildStackPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_HEADER);
        p.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, C_BORDER));
        p.setPreferredSize(new Dimension(160, 0));

        JLabel title = new JLabel("  STACK", SwingConstants.CENTER);
        title.setFont(F_H2); title.setForeground(C_DIM);
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        stackViz = new StackViz();

        p.add(title,   BorderLayout.NORTH);
        p.add(stackViz, BorderLayout.CENTER);
        return p;
    }

    // ── Code Editor ─────────────────────────────────────────────────────────
    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(C_EDITOR);

        // Editor title bar
        JPanel ebar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        ebar.setBackground(new Color(20, 20, 24));
        ebar.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, C_BORDER));
        JLabel etitle = new JLabel("  📝  CODE EDITOR");
        etitle.setFont(F_H2); etitle.setForeground(C_DIM);

        // AI translator row
        JTextField aiField = new JTextField("e.g. add 5 and 10");
        aiField.setFont(F_MONO);
        aiField.setBackground(new Color(30, 30, 38));
        aiField.setForeground(C_DIM);
        aiField.setCaretColor(C_ACCENT);
        aiField.setPreferredSize(new Dimension(240, 24));
        aiField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (aiField.getText().startsWith("e.g.")) { aiField.setText(""); aiField.setForeground(C_TEXT); }
            }
        });

        JButton aiBtn = toolBtn("🤖 AI Translate");
        aiBtn.addActionListener(e -> {
            String input = aiField.getText().trim();
            if (!input.isEmpty() && !input.startsWith("e.g.")) {
                AITranslator.TranslationResult r = ai.translate(input);
                if (r.success) { codeEditor.setText(r.vmCode + "\nHALT"); appendConsole("AI → " + r.explanation, C_PURPLE); }
                else           { appendConsole("AI: " + r.explanation, C_RED); }
            }
        });

        ebar.add(etitle);
        ebar.add(Box.createHorizontalStrut(20));
        ebar.add(new JLabel("🤖") {{ setFont(F_SMALL); setForeground(C_DIM); }});
        ebar.add(aiField);
        ebar.add(aiBtn);

        // Line-number gutter
        JTextArea lineNums = new JTextArea("1");
        lineNums.setFont(F_MONO);
        lineNums.setBackground(new Color(20, 20, 26));
        lineNums.setForeground(C_DIM);
        lineNums.setEditable(false);
        lineNums.setMargin(new Insets(8, 8, 8, 8));
        lineNums.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        codeEditor = new JTextArea();
        codeEditor.setFont(F_MONO);
        codeEditor.setBackground(C_EDITOR);
        codeEditor.setForeground(C_TEXT);
        codeEditor.setCaretColor(C_ACCENT);
        codeEditor.setTabSize(4);
        codeEditor.setMargin(new Insets(8, 12, 8, 8));
        codeEditor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { syncLines(lineNums); }
            public void removeUpdate(DocumentEvent e) { syncLines(lineNums); }
            public void changedUpdate(DocumentEvent e) {}
        });

        JScrollPane edScroll = new JScrollPane(codeEditor);
        edScroll.setRowHeaderView(lineNums);
        edScroll.setBorder(null);
        edScroll.getViewport().setBackground(C_EDITOR);

        panel.add(ebar,     BorderLayout.NORTH);
        panel.add(edScroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Status Bar ──────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 3));
        bar.setBackground(C_HEADER);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        lblStatusMsg  = statusLbl("⬤ Ready");    lblStatusMsg.setForeground(C_GREEN);
        lblIP         = statusLbl("IP: —");
        lblDepth      = statusLbl("Stack: 0");
        lblHypervisor = statusLbl("Hypervisor: Hyper-V/WSL2  |  Host: Ubuntu 22.04");

        bar.add(lblStatusMsg);  bar.add(makeSep());
        bar.add(lblIP);         bar.add(makeSep());
        bar.add(lblDepth);      bar.add(makeSep());
        bar.add(lblHypervisor);
        return bar;
    }

    // ════════════════════ ACTIONS ═════════════════════════════════════════

    private void runProgram() {
        stopProgram();

        if (activeEnv == VMEnv.CUSTOM_VM) {
            runCustomVM();
        } else {
            runLanguage();
        }

        lblPowerDot.setText("⬤ Running");
        lblPowerDot.setForeground(C_GREEN);
        lblStatusMsg.setText("⬤ Running");
        lblStatusMsg.setForeground(C_GREEN);
    }

    private void runCustomVM() {
        try {
            List<String> raw = ProgramLoader.loadFromString(codeEditor.getText());
            currentProgram = new InstructionParser().parse(raw);
        } catch (Exception ex) {
            appendConsole("⚠  Parse error: " + ex.getMessage(), C_RED);
            return;
        }

        stackViz.update(new double[0]);
        if (centerTabs != null) centerTabs.setSelectedIndex(1); // switch to IDE tab for VM
        interpreter = new Interpreter();
        interpreter.setProgram(currentProgram);
        interpreter.setListener(this);
        int delay = 1000 - speedSlider.getValue();
        interpreter.setStepDelay(Math.max(50, delay));

        execThread = new Thread(interpreter, "VM-Exec");
        execThread.setDaemon(true);
        execThread.start();

        appendConsole("  ▶  Booting Custom Stack VM...", C_ACCENT);
        setButtons(true);
    }

    private void runLanguage() {
        LanguageRunner.Language lang = toRunnerLang(activeEnv);
        if (lang == null) { appendConsole("⚠ Unsupported environment.", C_RED); return; }

        String src = codeEditor.getText();
        if (src.isBlank()) { appendConsole("⚠ No code to run.", C_RED); return; }

        if (centerTabs != null) centerTabs.setSelectedIndex(1); // switch to IDE tab
        appendConsole("  ▶  Launching " + activeEnv.label + "...", C_ACCENT);
        appendConsole("  Runtime: " + activeEnv.osInfo, C_DIM);

        btnPowerOn.setEnabled(false);
        btnPowerOff.setEnabled(true);

        langRunThread = langRunner.run(lang, src, new LanguageRunner.RunListener() {
            public void onOutput(String line, boolean isError) {
                SwingUtilities.invokeLater(() -> appendConsole("  " + line, isError ? C_RED : C_GREEN));
            }
            public void onFinished(int code, long ms) {
                SwingUtilities.invokeLater(() -> {
                    String msg = code == 0 ? "✔ Exited normally" : "✘ Exit code " + code;
                    appendConsole("  " + msg + "  (" + ms + " ms)", code == 0 ? C_GREEN : C_RED);
                    lblPowerDot.setText("⬤ Powered Off");
                    lblPowerDot.setForeground(C_DIM);
                    lblStatusMsg.setText("⬤ Finished");
                    lblStatusMsg.setForeground(code == 0 ? C_GREEN : C_RED);
                    btnPowerOn.setEnabled(true);
                    btnPowerOff.setEnabled(false);
                });
            }
            public void onFatalError(String msg) {
                SwingUtilities.invokeLater(() -> {
                    appendConsole("  ⚠ " + msg, C_RED);
                    btnPowerOn.setEnabled(true); btnPowerOff.setEnabled(false);
                });
            }
        });
    }

    private void togglePause() {
        if (interpreter == null) return;
        if (interpreter.isPaused()) {
            interpreter.resume();
            btnSuspend.setText("⏸  Suspend");
            lblPowerDot.setText("⬤ Running"); lblPowerDot.setForeground(C_GREEN);
        } else {
            interpreter.pause();
            btnSuspend.setText("▶  Resume");
            lblPowerDot.setText("⏸ Suspended"); lblPowerDot.setForeground(C_YELLOW);
        }
    }

    private void stepProgram() {
        if (activeEnv != VMEnv.CUSTOM_VM) return;
        if (interpreter == null) {
            try {
                List<String> raw = ProgramLoader.loadFromString(codeEditor.getText());
                currentProgram = new InstructionParser().parse(raw);
            } catch (Exception ex) { appendConsole("⚠ " + ex.getMessage(), C_RED); return; }
            stackViz.update(new double[0]);
            interpreter = new Interpreter();
            interpreter.setProgram(currentProgram);
            interpreter.setListener(this);
            interpreter.setStepDelay(0);
            interpreter.pause();
            execThread = new Thread(interpreter, "VM-Step");
            execThread.setDaemon(true);
            execThread.start();
            setButtons(true);
        }
        interpreter.requestStep();
        lblPowerDot.setText("⬤ Step"); lblPowerDot.setForeground(C_ACCENT);
    }

    private void stopProgram() {
        if (interpreter != null) interpreter.halt();
        if (execThread   != null) execThread.interrupt();
        if (langRunThread != null) langRunThread.interrupt();
        interpreter = null; execThread = null; langRunThread = null;
        setButtons(false);
        lblPowerDot.setText("⬤ Powered Off"); lblPowerDot.setForeground(C_DIM);
        lblStatusMsg.setText("⬤ Stopped");    lblStatusMsg.setForeground(C_DIM);
    }

    private void clearAll() {
        stopProgram();
        codeEditor.setText("");
        try { consoleDoc.remove(0, consoleDoc.getLength()); } catch (Exception ignored) {}
        stackViz.update(new double[0]);
        lblIP.setText("IP: —"); lblDepth.setText("Stack: 0");
    }

    private void loadFile() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<String> lines = new ProgramLoader(fc.getSelectedFile().getAbsolutePath()).load();
                codeEditor.setText(String.join("\n", lines));
                appendConsole("  Loaded: " + fc.getSelectedFile().getName(), C_GREEN);
            } catch (IOException ex) { appendConsole("  Load failed: " + ex.getMessage(), C_RED); }
        }
    }

    private void saveFile() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                pw.print(codeEditor.getText());
                appendConsole("  Saved: " + fc.getSelectedFile().getName(), C_GREEN);
            } catch (IOException ex) { appendConsole("  Save failed: " + ex.getMessage(), C_RED); }
        }
    }

    // ── Interpreter Listener ────────────────────────────────────────────────
    @Override public void onInstructionExecuted(int ip, Instruction instr, double[] stack) {
        SwingUtilities.invokeLater(() -> {
            stackViz.update(stack);
            lblIP.setText("IP: " + ip);
            lblDepth.setText("Stack: " + stack.length);
            appendConsole("  [IP=" + ip + "]  " + instr, C_DIM);
        });
    }
    @Override public void onOutput(String text) {
        SwingUtilities.invokeLater(() -> appendConsole("  >>>  " + text, C_GREEN));
    }
    @Override public void onError(String err) {
        SwingUtilities.invokeLater(() -> {
            appendConsole("  ⚠  ERROR: " + err, C_RED);
            setButtons(false);
            lblPowerDot.setText("⬤ Error"); lblPowerDot.setForeground(C_RED);
        });
    }
    @Override public void onHalt() {
        SwingUtilities.invokeLater(() -> {
            appendConsole("  ━━  VM HALTED  ━━", C_ACCENT);
            setButtons(false);
            lblPowerDot.setText("⬤ Powered Off"); lblPowerDot.setForeground(C_DIM);
            lblStatusMsg.setText("✔ Halted"); lblStatusMsg.setForeground(C_ACCENT);
        });
    }
    @Override public void onReset() {
        SwingUtilities.invokeLater(() -> stackViz.update(new double[0]));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private void appendConsole(String text, Color color) {
        try {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, color);
            StyleConstants.setFontFamily(a, "Consolas");
            StyleConstants.setFontSize(a, 13);
            consoleDoc.insertString(consoleDoc.getLength(), text + "\n", a);
            vmConsole.setCaretPosition(consoleDoc.getLength());
        } catch (Exception ignored) {}
    }

    private void setButtons(boolean running) {
        btnPowerOn .setEnabled(!running);
        btnSuspend .setEnabled(running);
        btnPowerOff.setEnabled(running);
    }

    private void syncLines(JTextArea lnPanel) {
        SwingUtilities.invokeLater(() -> {
            String text = codeEditor.getText();
            int lines = text.split("\n", -1).length;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
            lnPanel.setText(sb.toString());
        });
    }

    private void loadDefaultCode() {
        switch (activeEnv) {
            case CUSTOM_VM:
                codeEditor.setText("# Custom Stack VM Program\nPUSH 5\nPUSH 10\nADD\nPRINT\nPUSH 6\nPUSH 7\nMUL\nPRINT\nHALT");
                break;
            case PYTHON:
                codeEditor.setText(LanguageRunner.starterTemplate(LanguageRunner.Language.PYTHON));
                break;
            case C_LANG:
                codeEditor.setText(LanguageRunner.starterTemplate(LanguageRunner.Language.C));
                break;
            case CPP:
                codeEditor.setText(LanguageRunner.starterTemplate(LanguageRunner.Language.CPP));
                break;
            case JAVA:
                codeEditor.setText(
                    "public class Hello {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello from Java on Linux!\");\n" +
                    "        for (int i = 1; i <= 5; i++)\n" +
                    "            System.out.println(\"  Line \" + i);\n" +
                    "    }\n" +
                    "}");
                break;
            case JAVASCRIPT:
                codeEditor.setText(
                    "// JavaScript — Node.js\n" +
                    "console.log('Hello from JavaScript on Linux!');\n" +
                    "const nums = [1,2,3,4,5];\n" +
                    "const sum = nums.reduce((a,b) => a+b, 0);\n" +
                    "console.log('Sum:', sum);\n" +
                    "console.log('Squares:', nums.map(x => x*x));");
                break;
        }
    }

    private LanguageRunner.Language toRunnerLang(VMEnv env) {
        switch (env) {
            case PYTHON:     return LanguageRunner.Language.PYTHON;
            case C_LANG:     return LanguageRunner.Language.C;
            case CPP:        return LanguageRunner.Language.CPP;
            default:         return null;
        }
    }

    private JButton powerBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(F_BOLD); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private JButton toolBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(F_SMALL); b.setBackground(C_TOOLBAR); b.setForeground(C_TEXT);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(C_HOVER); }
            public void mouseExited (MouseEvent e) { b.setBackground(C_TOOLBAR); }
        });
        return b;
    }

    private JLabel statusLbl(String t) {
        JLabel l = new JLabel(t); l.setFont(F_SMALL); l.setForeground(C_DIM); return l;
    }

    private JSeparator makeSep() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setPreferredSize(new Dimension(1, 18)); s.setForeground(C_BORDER);
        return s;
    }

    // ════════════════════ STACK VISUALIZER ════════════════════════════════
    class StackViz extends JPanel {
        private double[] data = new double[0];
        StackViz() { setBackground(C_HEADER); }
        void update(double[] d) { this.data = d; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight();
            g2.setColor(C_HEADER); g2.fillRect(0, 0, W, H);

            final int EH = 36, EW = W - 24, X = 12, GAP = 3;
            final int BASE = H - 24;
            final int SHOW = Math.min(data.length, Math.max(1, (H - 40) / (EH + GAP)));

            g2.setColor(C_BORDER); g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(X, BASE, X + EW, BASE);
            g2.setFont(F_SMALL); g2.setColor(C_DIM);
            g2.drawString("BASE", X, BASE + 14);

            if (data.length == 0) {
                g2.setFont(F_SMALL); g2.setColor(C_DIM);
                FontMetrics fm = g2.getFontMetrics();
                String msg = "Empty";
                g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                return;
            }

            for (int i = 0; i < SHOW; i++) {
                int y = BASE - (i + 1) * (EH + GAP);
                boolean top = (i == data.length - 1);
                Color bc = top ? C_ACCENT : new Color(45, 55, 70);
                g2.setColor(bc); g2.fillRoundRect(X, y, EW, EH, 8, 8);
                g2.setColor(top ? C_ACCENT.brighter() : C_BORDER);
                g2.drawRoundRect(X, y, EW, EH, 8, 8);

                double val = data[i];
                String txt = (val == Math.floor(val) && !Double.isInfinite(val))
                        ? String.valueOf((long) val) : String.format("%.2f", val);
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(top ? Color.WHITE : C_TEXT);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(txt, X + (EW - fm.stringWidth(txt)) / 2, y + EH / 2 + 5);

                if (top) { g2.setFont(F_SMALL); g2.setColor(C_YELLOW); g2.drawString("▲", X + EW + 2, y + EH / 2 + 4); }
            }
            if (data.length > SHOW) {
                g2.setColor(C_DIM); g2.setFont(F_SMALL);
                g2.drawString("+" + (data.length - SHOW) + " more", X, BASE - SHOW * (EH + GAP) - 4);
            }
        }
    }
}
