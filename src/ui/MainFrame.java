package ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import logic.GameSettings;
import logic.LevelManager;
import logic.RunHistory;
import maze.MazeGraph;
import maze.MazeType;

/**
 * Main application window featuring modern arcade navigation,
 * Campaign Level Progression, Custom Seed setup, ADS Architecture overview,
 * and comprehensive Settings.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);
    private final GameSettings settings = new GameSettings();

    private GameWindow activeGame;
    private DefaultTableModel historyTableModel;
    private JTable historyTable;
    private JTextField seedField;

    public MainFrame() {
        setTitle("Maze Evasion: Cat & Dog (ADS Project)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        rootPanel.add(buildMenuPanel(), "MENU");
        rootPanel.add(buildSettingsPanel(), "SETTINGS");
        rootPanel.add(buildHistoryPanel(), "HISTORY");
        rootPanel.add(buildLevelSelectPanel(), "LEVEL_SELECT");

        add(rootPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 27, 36));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Storybook decorative hand-inked inner border
                g2.setColor(new Color(42, 48, 64));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(14, 14, getWidth() - 28, getHeight() - 28, 20, 20);
                g2.dispose();
            }
        };
        panel.setPreferredSize(new Dimension(700, 520));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title Header with illustrated styling
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("~ MAZE EVASION ~", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 38));
        title.setForeground(new Color(245, 180, 65));

        JLabel subtitle = new JLabel("Cat vs Dog • Advanced Data Structures Mini-Project", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.BOLD, 13));
        subtitle.setForeground(new Color(185, 195, 215));

        titlePanel.add(title);
        titlePanel.add(subtitle);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 18, 0);
        panel.add(titlePanel, gbc);

        // Seed Quick Bar
        JPanel seedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        seedPanel.setOpaque(false);
        JLabel seedLabel = new JLabel("Seed:");
        seedLabel.setFont(new Font("Arial", Font.BOLD, 14));
        seedLabel.setForeground(new Color(230, 230, 240));

        seedField = new JTextField(12);
        seedField.setBackground(new Color(34, 38, 50));
        seedField.setForeground(Color.WHITE);
        seedField.setCaretColor(Color.WHITE);
        seedField.setFont(new Font("Arial", Font.BOLD, 13));
        seedField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 85), 2),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        seedField.setToolTipText("Leave blank for random seed");

        JButton randomBtn = createActionButton("Random", e -> seedField.setText(String.valueOf(MazeGraph.newSeed())));
        seedPanel.add(seedLabel);
        seedPanel.add(seedField);
        seedPanel.add(randomBtn);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(seedPanel, gbc);

        // Illustrated Menu Buttons
        gbc.insets = new Insets(5, 110, 5, 110);

        JButton campaignBtn = createStyledButton("Play Campaign (Level 1)", new Color(39, 174, 96), e -> startCampaign(1));
        gbc.gridy = 2;
        panel.add(campaignBtn, gbc);

        JButton levelSelectBtn = createStyledButton("Level Select / Custom", new Color(41, 128, 185), e -> cardLayout.show(rootPanel, "LEVEL_SELECT"));
        gbc.gridy = 3;
        panel.add(levelSelectBtn, gbc);

        JButton adsInfoBtn = createStyledButton("How to Play & ADS Info", new Color(142, 68, 173), e -> showADSInfoDialog());
        gbc.gridy = 4;
        panel.add(adsInfoBtn, gbc);

        JButton settingsBtn = createStyledButton("Settings", new Color(211, 84, 0), e -> cardLayout.show(rootPanel, "SETTINGS"));
        gbc.gridy = 5;
        panel.add(settingsBtn, gbc);

        JButton historyBtn = createStyledButton("Run History", new Color(52, 73, 94), e -> {
            refreshHistoryTable();
            cardLayout.show(rootPanel, "HISTORY");
        });
        gbc.gridy = 6;
        panel.add(historyBtn, gbc);

        JButton exitBtn = createStyledButton("Exit Game", new Color(192, 57, 43), e -> System.exit(0));
        gbc.gridy = 7;
        panel.add(exitBtn, gbc);

        return panel;
    }

    private JPanel buildLevelSelectPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setPreferredSize(new Dimension(700, 520));
        panel.setBackground(new Color(18, 20, 26));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel header = new JLabel("CAMPAIGN LEVEL SELECT", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 26));
        header.setForeground(new Color(241, 196, 15));
        panel.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new GridLayout(5, 1, 8, 8));
        listPanel.setOpaque(false);

        for (int i = 1; i <= 5; i++) {
            LevelManager.LevelData lvl = LevelManager.getLevel(i);
            JButton lvlBtn = new JButton("Level " + i + ": " + lvl.name + " (" + lvl.subtitle + ")");
            lvlBtn.setFont(new Font("Arial", Font.BOLD, 14));
            lvlBtn.setBackground(new Color(30, 36, 48));
            lvlBtn.setForeground(Color.WHITE);
            lvlBtn.setFocusPainted(false);
            lvlBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 75, 105), 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));

            final int chosenLvl = i;
            lvlBtn.addActionListener(e -> startCampaign(chosenLvl));
            listPanel.add(lvlBtn);
        }

        panel.add(listPanel, BorderLayout.CENTER);

        JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botPanel.setOpaque(false);
        JButton backBtn = createActionButton("Back to Menu", e -> cardLayout.show(rootPanel, "MENU"));
        botPanel.add(backBtn);
        panel.add(botPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(700, 520));
        panel.setBackground(new Color(18, 20, 26));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 14, 8, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("GAME SETTINGS", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 26));
        header.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(header, gbc);

        gbc.gridwidth = 1;

        // Maze Algorithm
        JLabel typeLabel = styledLabel("Maze Generation (ADS):");
        JComboBox<MazeType> typeBox = new JComboBox<>(MazeType.values());
        typeBox.setSelectedItem(settings.mazeType);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(typeLabel, gbc);
        gbc.gridx = 1; panel.add(typeBox, gbc);

        // Difficulty Mode (Easy, Medium, Hard, Custom)
        JLabel diffLabel = styledLabel("Difficulty Mode:");
        JComboBox<GameSettings.DifficultyMode> diffBox = new JComboBox<>(GameSettings.DifficultyMode.values());
        diffBox.setSelectedItem(settings.difficultyMode);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(diffLabel, gbc);
        gbc.gridx = 1; panel.add(diffBox, gbc);

        // Biome Theme & Map Size Preset
        JLabel themeLabel = styledLabel("Biome & Map Size:");
        JPanel bioSizePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        bioSizePanel.setOpaque(false);
        String[] themes = {"GARDEN", "DUNGEON", "TEMPLE", "CYBER"};
        JComboBox<String> themeBox = new JComboBox<>(themes);
        themeBox.setSelectedItem(settings.theme);
        JComboBox<GameSettings.MapSizePreset> sizeBox = new JComboBox<>(GameSettings.MapSizePreset.values());
        sizeBox.setSelectedItem(settings.mapSizePreset);
        bioSizePanel.add(themeBox);
        bioSizePanel.add(sizeBox);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(themeLabel, gbc);
        gbc.gridx = 1; panel.add(bioSizePanel, gbc);

        // 2x2 Scaled Corridors & Watch Generation
        JLabel scaleLabel = styledLabel("2x2 Corridors & FX:");
        JPanel fxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fxPanel.setOpaque(false);
        JCheckBox scaleBox = new JCheckBox("2x2 Wide", settings.scale2x2);
        scaleBox.setOpaque(false);
        scaleBox.setForeground(Color.WHITE);
        JCheckBox watchGenBox = new JCheckBox("Watch Gen", settings.watchGeneration);
        watchGenBox.setOpaque(false);
        watchGenBox.setForeground(Color.WHITE);
        JCheckBox lightBox = new JCheckBox("Torch FX", settings.dynamicLighting);
        lightBox.setOpaque(false);
        lightBox.setForeground(Color.WHITE);
        JCheckBox sfxBox = new JCheckBox("SFX", settings.soundEffects);
        sfxBox.setOpaque(false);
        sfxBox.setForeground(Color.WHITE);
        fxPanel.add(scaleBox);
        fxPanel.add(watchGenBox);
        fxPanel.add(lightBox);
        fxPanel.add(sfxBox);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(scaleLabel, gbc);
        gbc.gridx = 1; panel.add(fxPanel, gbc);

        // Map Generation Speed
        JLabel genSpeedLabel = styledLabel("Gen Speed (steps/s):");
        JSlider genSpeedSlider = new JSlider(10, 400, Math.max(10, Math.min(400, settings.generationStepsPerSec)));
        genSpeedSlider.setOpaque(false);
        genSpeedSlider.setForeground(Color.WHITE);
        genSpeedSlider.setMajorTickSpacing(100);
        genSpeedSlider.setPaintTicks(true);
        genSpeedSlider.setPaintLabels(true);
        gbc.gridx = 0; gbc.gridy = 5; panel.add(genSpeedLabel, gbc);
        gbc.gridx = 1; panel.add(genSpeedSlider, gbc);

        // Loops (Braid Chance)
        JLabel braidLabel = styledLabel("Multi-Path Loops (%):");
        JSlider braidSlider = new JSlider(10, 60, (int) (settings.braidChance * 100));
        braidSlider.setOpaque(false);
        braidSlider.setForeground(Color.WHITE);
        braidSlider.setMajorTickSpacing(10);
        braidSlider.setPaintTicks(true);
        braidSlider.setPaintLabels(true);
        gbc.gridx = 0; gbc.gridy = 6; panel.add(braidLabel, gbc);
        gbc.gridx = 1; panel.add(braidSlider, gbc);

        // Custom Speed Sliders (Player & Enemy)
        JLabel speedLabel = styledLabel("Custom Speed (Cat / Dog):");
        JPanel speedPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        speedPanel.setOpaque(false);
        JSlider speedSlider = new JSlider(2, 8, Math.max(2, Math.min(8, settings.playerSpeed)));
        speedSlider.setOpaque(false);
        speedSlider.setForeground(Color.WHITE);
        speedSlider.setMajorTickSpacing(2);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        JSlider enemySpeedSlider = new JSlider(2, 8, Math.max(2, Math.min(8, settings.enemySpeed)));
        enemySpeedSlider.setOpaque(false);
        enemySpeedSlider.setForeground(new Color(245, 180, 65));
        enemySpeedSlider.setMajorTickSpacing(2);
        enemySpeedSlider.setPaintTicks(true);
        enemySpeedSlider.setPaintLabels(true);
        speedPanel.add(speedSlider);
        speedPanel.add(enemySpeedSlider);
        gbc.gridx = 0; gbc.gridy = 7; panel.add(speedLabel, gbc);
        gbc.gridx = 1; panel.add(speedPanel, gbc);

        // Sync preset difficulty -> sliders
        final boolean[] updatingPreset = {false};
        diffBox.addActionListener(e -> {
            GameSettings.DifficultyMode mode = (GameSettings.DifficultyMode) diffBox.getSelectedItem();
            if (mode != null && mode != GameSettings.DifficultyMode.CUSTOM) {
                updatingPreset[0] = true;
                speedSlider.setValue(mode.playerSpeed);
                enemySpeedSlider.setValue(mode.enemySpeed);
                updatingPreset[0] = false;
            }
        });
        speedSlider.addChangeListener(e -> {
            if (!updatingPreset[0] && speedSlider.getValueIsAdjusting()) {
                diffBox.setSelectedItem(GameSettings.DifficultyMode.CUSTOM);
            }
        });
        enemySpeedSlider.addChangeListener(e -> {
            if (!updatingPreset[0] && enemySpeedSlider.getValueIsAdjusting()) {
                diffBox.setSelectedItem(GameSettings.DifficultyMode.CUSTOM);
            }
        });

        // Save & Back
        JButton saveBtn = createStyledButton("Save & Back to Menu", new Color(46, 204, 113), e -> {
            settings.mazeType = (MazeType) typeBox.getSelectedItem();
            settings.difficultyMode = (GameSettings.DifficultyMode) diffBox.getSelectedItem();
            settings.theme = (String) themeBox.getSelectedItem();
            settings.mapSizePreset = (GameSettings.MapSizePreset) sizeBox.getSelectedItem();
            settings.rows = settings.mapSizePreset.baseRows;
            settings.cols = settings.mapSizePreset.baseCols;
            settings.scale2x2 = scaleBox.isSelected();
            settings.watchGeneration = watchGenBox.isSelected();
            settings.generationStepsPerSec = genSpeedSlider.getValue();
            settings.braidChance = braidSlider.getValue() / 100.0;
            settings.playerSpeed = speedSlider.getValue();
            settings.enemySpeed = enemySpeedSlider.getValue();
            settings.dynamicLighting = lightBox.isSelected();
            settings.soundEffects = sfxBox.isSelected();
            cardLayout.show(rootPanel, "MENU");
        });

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 80, 10, 80);
        panel.add(saveBtn, gbc);

        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setPreferredSize(new Dimension(700, 520));
        panel.setBackground(new Color(18, 20, 26));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("RUN HISTORY & LEADERBOARD", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(new Color(241, 196, 15));
        panel.add(header, BorderLayout.NORTH);

        String[] cols = {"Date", "Seed", "Score", "Result", "Dimensions"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        historyTable = new JTable(historyTableModel);
        historyTable.setRowHeight(26);
        historyTable.setBackground(new Color(26, 30, 40));
        historyTable.setForeground(Color.WHITE);
        historyTable.getTableHeader().setBackground(new Color(36, 42, 56));
        historyTable.getTableHeader().setForeground(Color.YELLOW);
        historyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnPanel.setOpaque(false);

        JButton replayBtn = createActionButton("Replay Seed", e -> {
            int row = historyTable.getSelectedRow();
            if (row >= 0) {
                long seed = Long.parseLong(historyTableModel.getValueAt(row, 1).toString());
                seedField.setText(String.valueOf(seed));
                startGame(1, seed);
            }
        });

        JButton clearBtn = createActionButton("Clear History", e -> {
            RunHistory.clearHistory();
            refreshHistoryTable();
        });

        JButton backBtn = createActionButton("Back", e -> cardLayout.show(rootPanel, "MENU"));

        btnPanel.add(replayBtn);
        btnPanel.add(clearBtn);
        btnPanel.add(backBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void showADSInfoDialog() {
        String infoText = "<html><body style='width: 520px; font-family: Arial; padding: 10px;'>"
                + "<h2 style='color: #F1C40F;'>Controls & Game Rules</h2>"
                + "<ul>"
                + "<li><b>Movement:</b> Use <b>WASD</b> or <b>Arrow Keys</b> to navigate the Cat.</li>"
                + "<li><b>Distraction Treats [SPACE]:</b> Drop a Fish Treat to distract and stun hungry dogs!</li>"
                + "<li><b>Sprint [SHIFT]:</b> Speed boost consuming stamina.</li>"
                + "<li><b>Stray Kittens [♥]:</b> Rescue stray kittens for +150 bonus score before dogs retrieve them!</li>"
                + "<li><b>Objective:</b> Collect coins, rescue kittens, evade the dog swarm, and reach the glowing Exit Portal.</li>"
                + "</ul>"
                + "<h2 style='color: #3498DB;'>Advanced Data Structures (ADS) Architecture</h2>"
                + "<ul>"
                + "<li><b>2x2 Scaled Procedural Corridors:</b> Expands corridors to 2x2 wide blocks, giving room for tactical flanking.</li>"
                + "<li><b>Multi-Chaser Swarm (Non-Stacking):</b> Uses a spatial reservation table preventing chasers from occupying the same tile.</li>"
                + "<li><b>Chaser 1: Vidhita's BFS Tracker:</b> Evaluates the mathematically shortest graph path using an <b>Ordinary Queue (FIFO)</b> in O(V + E) (Module 6 & Module 3).</li>"
                + "<li><b>Chaser 2: Greedy Scent Hound:</b> Tracks player's decaying integer scent trail via local <b>Greedy optimization</b>: max(scentTrail) (Module 5).</li>"
                + "<li><b>Finite State Machine (FSM):</b> Chasers dynamically switch between <b>PATROLLING</b>, <b>HUNTING</b>, <b>RETRIEVING</b> (returning kittens), and <b>STUNNED</b>.</li>"
                + "<li><b>Kruskal's & Prim's MST:</b> Custom Disjoint-Set / Union-Find with Path Compression and Union by Rank (Module 6).</li>"
                + "<li><b>Cycle / Braid Injection:</b> Removes dead ends to guarantee multiple alternate topological paths.</li>"
                + "</ul>"
                + "</body></html>";

        JOptionPane.showMessageDialog(this, infoText, "ADS Project Documentation & Controls", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startCampaign(int level) {
        long seed = parseSeedInput();
        startGame(level, seed);
    }

    private void startGame(int level, long seed) {
        startGame(level, seed, 0, 100, 0);
    }

    private void startGame(int level, long seed, int carryScore, int carryHealth, int carryTreats) {
        if (activeGame != null) {
            activeGame.stopGame();
            rootPanel.remove(activeGame);
        }

        LevelManager.LevelData lvl = LevelManager.getLevel(level);
        int[] dims = LevelManager.getDimensionsForLevel(level, settings);
        MazeGraph mazeGraph = new MazeGraph(
                dims[0],
                dims[1],
                seed,
                settings.braidChance,
                settings.mazeType != null ? settings.mazeType : lvl.mazeType,
                settings.scale2x2,
                lvl.totalKittens
        );

        activeGame = new GameWindow(
                mazeGraph,
                settings,
                level,
                carryScore,
                carryHealth,
                carryTreats,
                nextLvl -> startGame(
                        nextLvl,
                        MazeGraph.newSeed(),
                        activeGame.getScore(),
                        Math.min(100, activeGame.getHealth() + 25),
                        activeGame.getFishTreatCount()
                ),
                this::returnToMenu
        );

        rootPanel.add(activeGame, "GAME");
        cardLayout.show(rootPanel, "GAME");
        activeGame.requestFocusInWindow();
    }

    private void returnToMenu() {
        if (activeGame != null) {
            activeGame.stopGame();
        }
        cardLayout.show(rootPanel, "MENU");
    }

    private void refreshHistoryTable() {
        historyTableModel.setRowCount(0);
        List<RunHistory.Record> runs = RunHistory.loadRuns();
        for (RunHistory.Record r : runs) {
            historyTableModel.addRow(new Object[]{r.timestamp, r.seed, r.score, r.result, r.mazeSize});
        }
    }

    private long parseSeedInput() {
        String text = seedField.getText().trim();
        if (text.isEmpty()) {
            long s = MazeGraph.newSeed();
            seedField.setText(String.valueOf(s));
            return s;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text.hashCode();
        }
    }

    private JButton createStyledButton(String text, Color baseColor, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Drop shadow
                g2.setColor(new Color(14, 16, 22, 180));
                g2.fillRoundRect(2, 4, w - 4, h - 5, 14, 14);

                // Button body
                Color fill = getModel().isRollover() ? baseColor.brighter() : baseColor;
                g2.setColor(fill);
                g2.fillRoundRect(2, 2, w - 4, h - 6, 14, 14);

                // Hand-inked border matching textures
                g2.setColor(new Color(24, 28, 36));
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawRoundRect(2, 2, w - 5, h - 7, 14, 14);

                // Text
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - 6 + fm.getAscent() - fm.getDescent()) / 2;

                // Drop shadow on text
                g2.setColor(new Color(0, 0, 0, 150));
                g2.drawString(getText(), tx + 1, ty + 1);

                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(260, 42));
        btn.addActionListener(action);
        return btn;
    }

    private JButton createActionButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Color fill = getModel().isRollover() ? new Color(55, 65, 85) : new Color(38, 44, 58);
                g2.setColor(fill);
                g2.fillRoundRect(1, 1, w - 2, h - 3, 10, 10);
                g2.setColor(new Color(24, 28, 36));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, w - 3, h - 4, 10, 10);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - 3 + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        return btn;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 14));
        l.setForeground(Color.WHITE);
        return l;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
