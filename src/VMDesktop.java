import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * VMDesktop.java — Simulated Linux Desktop Environment
 *
 * Mimics the guest-OS screen you see in VMware when a Linux VM is running.
 * Rendered inside a JDesktopPane so every app opens as a draggable,
 * resizable, minimise-able internal window — exactly like a real desktop.
 *
 * Apps available on the desktop:
 *   🖥  Terminal    — functional terminal (cmd / bash via ProcessBuilder)
 *   📁  Files       — file-system browser using JTree
 *   📝  Text Editor — basic notepad
 *   ⚡  VM Runner   — embedded code editor + stack VM executor
 *   ℹ   System Info — virtual-hardware & OS details
 *   ⚙   Settings    — appearance settings
 */
public class VMDesktop extends JPanel {

    // ── Desktop colour scheme (Ubuntu Yaru Dark) ────────────────────────────
    static final Color D_BG1      = new Color(48,  18,  82);   // wallpaper top
    static final Color D_BG2      = new Color(16,  16,  48);   // wallpaper bottom
    static final Color D_TASKBAR  = new Color(18,  18,  18);   // top bar + dock
    static final Color D_DOCK     = new Color(26,  26,  26);
    static final Color D_TEXT     = new Color(255, 255, 255);
    static final Color D_DIM      = new Color(180, 180, 180);
    static final Color D_ACCENT   = new Color(233,  84,  32);   // Ubuntu orange
    static final Color D_BLUE     = new Color( 52, 152, 219);
    static final Color D_GREEN    = new Color( 39, 174,  96);
    static final Color D_RED      = new Color(231,  76,  60);
    static final Color D_CONSOLE  = new Color( 13,  17,  23);
    static final Color D_TERM_FG  = new Color( 78, 201, 176);
    static final Color D_BORDER   = new Color( 60,  60,  60);

    static final Font F_MONO  = new Font("Consolas",  Font.PLAIN,  13);
    static final Font F_UI    = new Font("Segoe UI",  Font.PLAIN,  12);
    static final Font F_BOLD  = new Font("Segoe UI",  Font.BOLD,   12);
    static final Font F_SMALL = new Font("Segoe UI",  Font.PLAIN,  11);
    static final Font F_ICON  = new Font("Segoe UI Emoji", Font.PLAIN, 28);

    private JDesktopPane desktop;
    private JPanel       topBar;
    private JPanel       dock;
    private JLabel       clockLbl;
    private JLabel       dateLbl;

    // Icon definitions: emoji, label, x, y
    private static final Object[][] ICONS = {
        {"🖥",  "Terminal",    40,  30},
        {"📁",  "Files",       40, 130},
        {"📝",  "Text Editor", 40, 230},
        {"⚡",  "VM Runner",   40, 330},
        {"ℹ",   "System Info", 40, 430},
        {"⚙",   "Settings",   40, 530},
    };

    // Reference to parent GUI for VM runner integration
    private GUI parentGUI;

    // ════════════════════ CONSTRUCTOR ══════════════════════════════════════
    public VMDesktop(GUI parent) {
        this.parentGUI = parent;
        setLayout(new BorderLayout(0, 0));
        setBackground(D_BG2);

        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildDesktopPane(), BorderLayout.CENTER);
        add(buildDock(),    BorderLayout.SOUTH);

        startClock();
    }

    // ── Top Bar (Ubuntu-style) ───────────────────────────────────────────────
    private JPanel buildTopBar() {
        topBar = new JPanel(new BorderLayout(0, 0));
        topBar.setBackground(D_TASKBAR);
        topBar.setPreferredSize(new Dimension(0, 32));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, D_BORDER));

        // Left: Activities button
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        left.setBackground(D_TASKBAR);
        JLabel activities = new JLabel("  Activities");
        activities.setFont(F_BOLD); activities.setForeground(D_TEXT);
        left.add(activities);

        // Center: App name + menu stub
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        center.setBackground(D_TASKBAR);
        for (String m : new String[]{"Files", "Edit", "View", "Help"}) {
            JLabel ml = new JLabel(m);
            ml.setFont(F_UI); ml.setForeground(D_DIM);
            ml.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            center.add(ml);
        }

        // Right: System tray (clock, icons)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setBackground(D_TASKBAR);

        JLabel wifiLbl  = new JLabel("📶"); wifiLbl.setFont(F_UI);
        JLabel soundLbl = new JLabel("🔊"); soundLbl.setFont(F_UI);
        JLabel battLbl  = new JLabel("🔋"); battLbl.setFont(F_UI);

        clockLbl = new JLabel("00:00");
        clockLbl.setFont(F_BOLD); clockLbl.setForeground(D_TEXT);

        dateLbl = new JLabel("Fri 20 Mar");
        dateLbl.setFont(F_SMALL); dateLbl.setForeground(D_DIM);

        right.add(wifiLbl); right.add(soundLbl); right.add(battLbl);
        right.add(dateLbl);
        right.add(clockLbl);

        JLabel powerIcon = new JLabel("⏻ ");
        powerIcon.setFont(F_UI); powerIcon.setForeground(D_DIM);
        powerIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        right.add(powerIcon);

        topBar.add(left,   BorderLayout.WEST);
        topBar.add(center, BorderLayout.CENTER);
        topBar.add(right,  BorderLayout.EAST);
        return topBar;
    }

    // ── JDesktopPane (wallpaper + icons) ────────────────────────────────────
    private JDesktopPane buildDesktopPane() {
        desktop = new JDesktopPane() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Wallpaper gradient
                GradientPaint gp = new GradientPaint(0, 0, D_BG1, getWidth(), getHeight(), D_BG2);
                g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight());
                // Subtle dot grid
                g2.setColor(new Color(255, 255, 255, 12));
                for (int x = 0; x < getWidth();  x += 40) g2.fillOval(x-1, 20, 2, 2);
                for (int y = 0; y < getHeight(); y += 40) g2.fillOval(20, y-1, 2, 2);
                // Subtle orange glow at top-right
                RadialGradientPaint rg = new RadialGradientPaint(
                    new Point(getWidth() - 80, 60), 160,
                    new float[]{0f, 1f},
                    new Color[]{new Color(233, 84, 32, 55), new Color(0,0,0,0)});
                g2.setPaint(rg); g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        desktop.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
        desktop.setBackground(D_BG2);

        // Place desktop icons
        for (Object[] ic : ICONS) {
            String emoji = (String) ic[0];
            String label = (String) ic[1];
            int x = (int) ic[2];
            int y = (int) ic[3];
            desktop.add(makeDesktopIcon(emoji, label, x, y));
        }
        return desktop;
    }

    // ── Desktop Icon ────────────────────────────────────────────────────────
    private JPanel makeDesktopIcon(String emoji, String label, int x, int y) {
        JPanel icon = new JPanel(new BorderLayout(0, 2)) {
            boolean hovered = false;
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                if (hovered) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(new Color(255,255,255,30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(new Color(255,255,255,60));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                }
            }
        };

        JLabel emojiLbl = new JLabel(emoji, SwingConstants.CENTER);
        emojiLbl.setFont(F_ICON); emojiLbl.setForeground(D_TEXT);

        JLabel nameLbl = new JLabel(label, SwingConstants.CENTER);
        nameLbl.setFont(F_SMALL); nameLbl.setForeground(D_TEXT);

        icon.add(emojiLbl, BorderLayout.CENTER);
        icon.add(nameLbl,  BorderLayout.SOUTH);
        icon.setBounds(x, y, 80, 72);
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        icon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) launchApp(label);
            }
            public void mouseEntered(MouseEvent e) {
                try { icon.getClass().getDeclaredField("hovered").set(icon, true); } catch(Exception ignored){}
                icon.repaint();
            }
            public void mouseExited(MouseEvent e) {
                try { icon.getClass().getDeclaredField("hovered").set(icon, false); } catch(Exception ignored){}
                icon.repaint();
            }
        });
        return icon;
    }

    // ── Dock at Bottom ──────────────────────────────────────────────────────
    private JPanel buildDock() {
        JPanel dockWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        dockWrap.setBackground(D_TASKBAR);
        dockWrap.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, D_BORDER));

        JPanel dockInner = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        dockInner.setBackground(new Color(30, 30, 30, 200));
        dockInner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80,80,80,120), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        Object[][] dockIcons = {
            {"🖥",  "Terminal"},
            {"📁",  "Files"},
            {"📝",  "Text Editor"},
            {"⚡",  "VM Runner"},
            {"🌐",  "Browser"},
            {"⚙",   "Settings"},
            {"ℹ",   "System Info"},
        };
        for (Object[] d : dockIcons) {
            dockInner.add(makeDockBtn((String)d[0], (String)d[1]));
        }
        dockWrap.add(dockInner);
        return dockWrap;
    }

    private JButton makeDockBtn(String emoji, String appName) {
        JButton b = new JButton(emoji) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255,255,255,40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        b.setForeground(D_TEXT);
        b.setBackground(new Color(0,0,0,0));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(46, 46));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(appName);
        b.addActionListener(e -> launchApp(appName));
        return b;
    }

    // ── App Launcher ────────────────────────────────────────────────────────
    private void launchApp(String name) {
        switch (name) {
            case "Terminal":    openTerminal();   break;
            case "Files":       openFileManager();break;
            case "Text Editor": openTextEditor(); break;
            case "VM Runner":   openVMRunner();   break;
            case "System Info": openSystemInfo(); break;
            case "Settings":    openSettings();   break;
            case "Browser":     openBrowser();    break;
            default: break;
        }
    }

    // ════════════════════ APPS ═════════════════════════════════════════════

    // ── Terminal App ────────────────────────────────────────────────────────
    private void openTerminal() {
        JInternalFrame frame = makeFrame("🖥  Terminal — bash", 520, 360);
        frame.setLocation(120, 60);

        JTextArea output = new JTextArea();
        output.setFont(F_MONO);
        output.setBackground(D_CONSOLE);
        output.setForeground(D_TERM_FG);
        output.setCaretColor(D_TERM_FG);
        output.setEditable(false);
        output.setMargin(new Insets(8, 10, 8, 10));

        JTextField input = new JTextField();
        input.setFont(F_MONO);
        input.setBackground(new Color(20, 24, 30));
        input.setForeground(D_TERM_FG);
        input.setCaretColor(D_TERM_FG);
        input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, D_BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JLabel prompt = new JLabel("user@hypervisor-vm:~$ ");
        prompt.setFont(F_MONO); prompt.setForeground(D_ACCENT);

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setBackground(new Color(20, 24, 30));
        inputRow.add(prompt, BorderLayout.WEST);
        inputRow.add(input,  BorderLayout.CENTER);

        JScrollPane sp = new JScrollPane(output);
        sp.setBorder(null); sp.getViewport().setBackground(D_CONSOLE);

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(D_CONSOLE);
        content.add(sp,       BorderLayout.CENTER);
        content.add(inputRow, BorderLayout.SOUTH);
        frame.setContentPane(content);

        // Terminal executor
        output.append("HypervisorVM Terminal  [Ubuntu 22.04 LTS]\n");
        output.append("Type commands and press Enter. Type 'exit' to close.\n\n");

        input.addActionListener(e -> {
            String cmd = input.getText().trim();
            if (cmd.isEmpty()) return;
            output.append("user@hypervisor-vm:~$ " + cmd + "\n");
            input.setText("");
            if (cmd.equals("exit") || cmd.equals("quit")) { frame.dispose(); return; }
            runTerminalCmd(cmd, output);
            output.setCaretPosition(output.getDocument().getLength());
        });

        showFrame(frame);
        SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
    }

    private void runTerminalCmd(String cmd, JTextArea out) {
        new Thread(() -> {
            try {
                String[] shell = System.getProperty("os.name").toLowerCase().contains("win")
                        ? new String[]{"cmd.exe", "/c", cmd}
                        : new String[]{"bash",    "-c", cmd};
                ProcessBuilder pb = new ProcessBuilder(shell);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String l = line;
                        SwingUtilities.invokeLater(() -> {
                            out.append("  " + l + "\n");
                            out.setCaretPosition(out.getDocument().getLength());
                        });
                    }
                }
                p.waitFor();
                SwingUtilities.invokeLater(() -> out.append("\n"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> out.append("  Error: " + ex.getMessage() + "\n"));
            }
        }, "term-exec").start();
    }

    // ── File Manager App ────────────────────────────────────────────────────
    private void openFileManager() {
        JInternalFrame frame = makeFrame("📁  Files — Home Folder", 540, 420);
        frame.setLocation(180, 80);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(32, 32, 38));
        sidebar.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        sidebar.setPreferredSize(new Dimension(150, 0));

        String[][] places = {
            {"🏠", "Home"},
            {"📥", "Downloads"},
            {"📄", "Documents"},
            {"🖼", "Pictures"},
            {"🎵", "Music"},
            {"🗑", "Trash"},
            {"⚡", "VM Project"},
        };
        for (String[] p : places) {
            JLabel l = new JLabel("  " + p[0] + "  " + p[1]);
            l.setFont(F_UI); l.setForeground(new Color(210, 210, 210));
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            l.setOpaque(true); l.setBackground(new Color(32, 32, 38));
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            l.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { l.setBackground(new Color(50,50,60)); }
                public void mouseExited (MouseEvent e) { l.setBackground(new Color(32,32,38)); }
            });
            sidebar.add(l);
        }

        // File tree (actual filesystem)
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("VM Project");
        File projectDir = new File(".");
        buildTree(root, projectDir, 2);
        JTree tree = new JTree(root);
        tree.setFont(F_UI);
        tree.setBackground(new Color(24, 24, 30));
        tree.setForeground(D_TEXT);
        tree.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            { setBackgroundNonSelectionColor(new Color(24,24,30)); setTextNonSelectionColor(D_TEXT); setFont(F_UI); }
        });

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(null);
        treeScroll.getViewport().setBackground(new Color(24, 24, 30));

        // Path bar at top
        JPanel pathBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        pathBar.setBackground(new Color(28, 28, 34));
        pathBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, D_BORDER));
        JLabel pathLbl = new JLabel("📍 /home/user/vm-project");
        pathLbl.setFont(F_SMALL); pathLbl.setForeground(D_DIM);
        pathBar.add(pathLbl);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, treeScroll);
        split.setDividerLocation(150);
        split.setBackground(new Color(24, 24, 30));
        split.setDividerSize(3);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(24, 24, 30));
        content.add(pathBar, BorderLayout.NORTH);
        content.add(split,   BorderLayout.CENTER);
        frame.setContentPane(content);
        showFrame(frame);
    }

    private void buildTree(DefaultMutableTreeNode node, File dir, int depth) {
        if (depth == 0 || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> Boolean.compare(!a.isDirectory(), !b.isDirectory()));
        for (File f : files) {
            if (f.getName().startsWith(".")) continue;
            String label = (f.isDirectory() ? "📁 " : "📄 ") + f.getName();
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(label);
            node.add(child);
            if (f.isDirectory()) buildTree(child, f, depth - 1);
        }
    }

    // ── Text Editor App ─────────────────────────────────────────────────────
    private void openTextEditor() {
        JInternalFrame frame = makeFrame("📝  Text Editor — Untitled", 500, 380);
        frame.setLocation(200, 100);

        JTextArea editor = new JTextArea("# Welcome to HypervisorVM Text Editor\n\nStart typing here...\n");
        editor.setFont(F_MONO);
        editor.setBackground(new Color(20, 20, 28));
        editor.setForeground(D_TEXT);
        editor.setCaretColor(D_BLUE);
        editor.setMargin(new Insets(10, 14, 10, 10));
        editor.setTabSize(4);

        JScrollPane sp = new JScrollPane(editor);
        sp.setBorder(null); sp.getViewport().setBackground(new Color(20, 20, 28));

        // Toolbar
        JPanel tbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        tbar.setBackground(new Color(28, 28, 35));
        tbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, D_BORDER));
        for (String lbl : new String[]{"📂 Open", "💾 Save", "✂ Cut", "📋 Paste"}) {
            JButton b = appBtn(lbl);
            tbar.add(b);
        }
        JButton saveBtn = appBtn("💾 Save");
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                    pw.print(editor.getText());
                } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Save failed: " + ex.getMessage()); }
            }
        });

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(20, 20, 28));
        content.add(tbar, BorderLayout.NORTH);
        content.add(sp,   BorderLayout.CENTER);
        frame.setContentPane(content);
        showFrame(frame);
    }

    // ── VM Runner App ───────────────────────────────────────────────────────
    private void openVMRunner() {
        JInternalFrame frame = makeFrame("⚡  VM Runner — Stack Machine", 660, 460);
        frame.setLocation(150, 70);

        JTextArea codeArea = new JTextArea(
            "# VM Runner — Write your program below\n" +
            "PUSH 10\nPUSH 20\nADD\nPRINT\n" +
            "PUSH 6\nPUSH 7\nMUL\nPRINT\nHALT");
        codeArea.setFont(F_MONO);
        codeArea.setBackground(new Color(20, 20, 30));
        codeArea.setForeground(D_TEXT);
        codeArea.setCaretColor(D_BLUE);
        codeArea.setMargin(new Insets(8, 12, 8, 8));

        JTextArea output = new JTextArea();
        output.setFont(F_MONO);
        output.setBackground(D_CONSOLE);
        output.setForeground(D_TERM_FG);
        output.setEditable(false);
        output.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane codeSP = new JScrollPane(codeArea);
        codeSP.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(D_BORDER), "Code", 0, 0, F_SMALL, D_DIM));
        codeSP.getViewport().setBackground(new Color(20,20,30));

        JScrollPane outSP = new JScrollPane(output);
        outSP.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(D_BORDER), "Output", 0, 0, F_SMALL, D_DIM));
        outSP.getViewport().setBackground(D_CONSOLE);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codeSP, outSP);
        split.setDividerLocation(330);
        split.setBackground(new Color(22,22,30));

        JPanel tbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        tbar.setBackground(new Color(24, 24, 32));
        tbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, D_BORDER));

        JButton runBtn = appBtn("▶  Run");
        runBtn.setBackground(new Color(34, 140, 80));
        runBtn.setForeground(Color.WHITE);
        JButton clrBtn = appBtn("🗑 Clear");

        runBtn.addActionListener(e -> {
            output.setText("");
            String code = codeArea.getText();
            new Thread(() -> {
                try {
                    java.util.List<String> raw = ProgramLoader.loadFromString(code);
                    java.util.List<Instruction> prog = new InstructionParser().parse(raw);
                    Interpreter interp = new Interpreter();
                    interp.setProgram(prog);
                    interp.setStepDelay(0);
                    interp.setListener(new Interpreter.InterpreterListener() {
                        public void onInstructionExecuted(int ip, Instruction i, double[] s) {}
                        public void onOutput(String text) {
                            SwingUtilities.invokeLater(() -> output.append("  >>> " + text + "\n")); }
                        public void onError(String err) {
                            SwingUtilities.invokeLater(() -> output.append("  ERROR: " + err + "\n")); }
                        public void onHalt() {
                            SwingUtilities.invokeLater(() -> output.append("  [HALT]\n")); }
                        public void onReset() {}
                    });
                    interp.run();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> output.append("  Error: " + ex.getMessage() + "\n"));
                }
            }, "vmrunner").start();
        });
        clrBtn.addActionListener(e -> { codeArea.setText(""); output.setText(""); });

        tbar.add(runBtn); tbar.add(clrBtn);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(22,22,30));
        content.add(tbar,  BorderLayout.NORTH);
        content.add(split, BorderLayout.CENTER);
        frame.setContentPane(content);
        showFrame(frame);
    }

    // ── System Info App ─────────────────────────────────────────────────────
    private void openSystemInfo() {
        JInternalFrame frame = makeFrame("ℹ   System Information", 420, 380);
        frame.setLocation(260, 120);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(22, 22, 28));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // OS banner
        JLabel banner = new JLabel("Ubuntu 22.04.3 LTS", SwingConstants.CENTER);
        banner.setFont(new Font("Segoe UI", Font.BOLD, 18));
        banner.setForeground(D_ACCENT);
        banner.setAlignmentX(CENTER_ALIGNMENT);
        content.add(banner);
        content.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("HypervisorVM Guest OS — Docker Container", SwingConstants.CENTER);
        sub.setFont(F_SMALL); sub.setForeground(D_DIM); sub.setAlignmentX(CENTER_ALIGNMENT);
        content.add(sub);
        content.add(Box.createVerticalStrut(16));

        String[][] info = {
            {"Hostname",    "hypervisor-vm"},
            {"Kernel",      "Linux 5.15 (Docker)"},
            {"Hypervisor",  "Hyper-V / WSL2"},
            {"Java VM",     "Eclipse Temurin 17"},
            {"CPU",         Runtime.getRuntime().availableProcessors() + " virtual cores"},
            {"RAM",         (Runtime.getRuntime().maxMemory()/1024/1024) + " MB (Java heap)"},
            {"OS (host)",   System.getProperty("os.name")},
            {"Arch",        System.getProperty("os.arch")},
            {"Python",      "3.11.x"},
            {"GCC",         "11.4.0"},
            {"Node.js",     "18.x LTS"},
            {"Docker img",  "eclipse-temurin:17-jre-jammy"},
        };
        for (String[] row : info) {
            JPanel rowP = new JPanel(new BorderLayout(16, 0));
            rowP.setBackground(new Color(28, 28, 35));
            rowP.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            rowP.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel key = new JLabel(row[0]); key.setFont(F_BOLD); key.setForeground(D_ACCENT);
            JLabel val = new JLabel(row[1]); val.setFont(F_UI);   val.setForeground(D_TEXT);
            rowP.add(key, BorderLayout.WEST);
            rowP.add(val, BorderLayout.EAST);
            content.add(rowP);
            content.add(Box.createVerticalStrut(2));
        }

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null); sp.getViewport().setBackground(new Color(22,22,28));
        frame.setContentPane(sp);
        showFrame(frame);
    }

    // ── Settings App ────────────────────────────────────────────────────────
    private void openSettings() {
        JInternalFrame frame = makeFrame("⚙   Settings", 380, 320);
        frame.setLocation(300, 150);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(22, 22, 28));
        content.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        String[] sections = {"🌐 Network", "🖥 Display", "🔊 Sound", "🖱 Mouse & Keyboard", "🔒 Privacy", "🧩 About"};
        for (String s : sections) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(new Color(32,32,40));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,D_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            JLabel lbl = new JLabel(s); lbl.setFont(F_UI); lbl.setForeground(D_TEXT);
            JLabel arr = new JLabel("›"); arr.setFont(F_BOLD); arr.setForeground(D_DIM);
            row.add(lbl, BorderLayout.WEST); row.add(arr, BorderLayout.EAST);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { row.setBackground(new Color(45,45,55)); }
                public void mouseExited (MouseEvent e) { row.setBackground(new Color(32,32,40)); }
            });
            content.add(row);
            content.add(Box.createVerticalStrut(2));
        }
        frame.setContentPane(content);
        showFrame(frame);
    }

    // ── Browser App ─────────────────────────────────────────────────────────
    private void openBrowser() {
        JInternalFrame frame = makeFrame("🌐  Browser — HypervisorVM", 640, 440);
        frame.setLocation(100, 50);

        JPanel urlBar = new JPanel(new BorderLayout(6, 0));
        urlBar.setBackground(new Color(24, 24, 32));
        urlBar.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        JButton back = appBtn("←"); JButton fwd = appBtn("→"); JButton reload = appBtn("⟳");
        JTextField urlField = new JTextField("https://github.com/SAGAR95131/Virtual-Machine");
        urlField.setFont(F_MONO); urlField.setBackground(new Color(36,36,46)); urlField.setForeground(D_TEXT);
        urlField.setCaretColor(D_BLUE);
        urlBar.add(back, BorderLayout.WEST);
        urlBar.add(urlField, BorderLayout.CENTER);
        urlBar.add(reload, BorderLayout.EAST);

        JTextArea pageArea = new JTextArea();
        pageArea.setFont(F_MONO); pageArea.setBackground(D_CONSOLE); pageArea.setForeground(D_TEXT);
        pageArea.setEditable(false); pageArea.setMargin(new Insets(10,14,10,10));
        pageArea.setText(
            "  ╔══════════════════════════════════════════════════╗\n" +
            "  ║  🌐  HypervisorVM Browser                       ║\n" +
            "  ╠══════════════════════════════════════════════════╣\n" +
            "  ║                                                  ║\n" +
            "  ║  SAGAR95131 / Virtual-Machine                    ║\n" +
            "  ║  ─────────────────────────────────────────────   ║\n" +
            "  ║  Stack-Based Language VM IDE                     ║\n" +
            "  ║  Running on Linux Docker Hypervisor              ║\n" +
            "  ║                                                  ║\n" +
            "  ║  Files:                                          ║\n" +
            "  ║    📁 src/          Java source files            ║\n" +
            "  ║    📁 examples/     Multi-language examples      ║\n" +
            "  ║    📄 Dockerfile    Linux container definition   ║\n" +
            "  ║    📄 program.vm    Default VM program           ║\n" +
            "  ║    📄 README.md     Documentation               ║\n" +
            "  ║                                                  ║\n" +
            "  ╚══════════════════════════════════════════════════╝\n");

        JScrollPane sp = new JScrollPane(pageArea);
        sp.setBorder(null); sp.getViewport().setBackground(D_CONSOLE);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(D_CONSOLE);
        content.add(urlBar,  BorderLayout.NORTH);
        content.add(sp,      BorderLayout.CENTER);
        frame.setContentPane(content);
        showFrame(frame);
    }

    // ── Frame factory ────────────────────────────────────────────────────────
    private JInternalFrame makeFrame(String title, int w, int h) {
        JInternalFrame f = new JInternalFrame(title, true, true, true, true);
        f.setSize(w, h);
        f.setFrameIcon(null);
        // Dark title bar via UI
        f.setBackground(new Color(28, 28, 35));
        f.getContentPane().setBackground(new Color(28, 28, 35));
        return f;
    }

    private void showFrame(JInternalFrame f) {
        desktop.add(f);
        f.setVisible(true);
        try { f.setSelected(true); } catch (Exception ignored) {}
    }

    private JButton appBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(F_SMALL); b.setBackground(new Color(42,42,52)); b.setForeground(D_TEXT);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(60,60,75)); }
            public void mouseExited (MouseEvent e) { b.setBackground(new Color(42,42,52)); }
        });
        return b;
    }

    // ── Live clock ───────────────────────────────────────────────────────────
    private void startClock() {
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> {
            LocalTime now = LocalTime.now();
            clockLbl.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
        });
        t.setInitialDelay(0);
        t.start();
    }
}
