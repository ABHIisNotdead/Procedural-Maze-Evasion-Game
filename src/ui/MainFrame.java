package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import logic.GameSettings;
import logic.RunHistory;
import maze.MazeGraph;

public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);
    private final GameSettings settings = new GameSettings();

    private GameWindow activeGame;
    private DefaultTableModel historyTableModel;
    private JTable historyTable;
    private JTextField seedField;

    public MainFrame() {
        setTitle("Maze Evasion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        rootPanel.add(buildMenuPanel(), "MENU");
        rootPanel.add(buildSettingsPanel(), "SETTINGS");
        rootPanel.add(buildHistoryPanel(), "HISTORY");

        add(rootPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setBackground(new Color(24, 24, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("MAZE EVASION", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(new Color(241, 196, 15));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 24, 0);
        panel.add(title, gbc);

        // Seed input row
        JPanel seedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        seedPanel.setOpaque(false);
        JLabel seedLabel = new JLabel("Seed:");
        seedLabel.setForeground(Color.LIGHT_GRAY);
        seedField = new JTextField(14);
        seedField.setToolTipText("Leave blank for random seed");
        JButton randomBtn = new JButton("Random");
        randomBtn.addActionListener(e -> seedField.setText(String.valueOf(MazeGraph.newSeed())));
        seedPanel.add(seedLabel);
        seedPanel.add(seedField);
        seedPanel.add(randomBtn);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(seedPanel, gbc);

        gbc.insets = new Insets(6, 60, 6, 60);

        JButton playBtn = createMenuButton("Play Game");
        playBtn.addActionListener(e -> startGame(parseSeedInput()));
        gbc.gridy = 2;
        panel.add(playBtn, gbc);

        JButton settingsBtn = createMenuButton("Settings");
        settingsBtn.addActionListener(e -> cardLayout.show(rootPanel, "SETTINGS"));
        gbc.gridy = 3;
        panel.add(settingsBtn, gbc);

        JButton historyBtn = createMenuButton("Run History");
        historyBtn.addActionListener(e -> {
            refreshHistoryTable();
            cardLayout.show(rootPanel, "HISTORY");
        });
        gbc.gridy = 4;
        panel.add(historyBtn, gbc);

        JButton exitBtn = createMenuButton("Exit");
        exitBtn.addActionListener(e -> System.exit(0));
        gbc.gridy = 5;
        panel.add(exitBtn, gbc);

        return panel;
    }

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setBackground(new Color(24, 24, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("SETTINGS", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 28));
        header.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(header, gbc);

        gbc.gridwidth = 1;

        // Maze Size
        JLabel sizeLabel = styledLabel("Maze Size:");
        String[] sizes = {"Small (11x15)", "Medium (15x21)", "Large (21x29)"};
        JComboBox<String> sizeBox = new JComboBox<>(sizes);
        sizeBox.setSelectedIndex(1);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(sizeLabel, gbc);
        gbc.gridx = 1; panel.add(sizeBox, gbc);

        // Braid Chance (Loop Density)
        JLabel braidLabel = styledLabel("Maze Loops (Braid %):");
        JSlider braidSlider = new JSlider(0, 50, (int) (settings.braidChance * 100));
        braidSlider.setOpaque(false);
        braidSlider.setForeground(Color.WHITE);
        braidSlider.setMajorTickSpacing(10);
        braidSlider.setPaintTicks(true);
        braidSlider.setPaintLabels(true);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(braidLabel, gbc);
        gbc.gridx = 1; panel.add(braidSlider, gbc);

        // Player Speed
        JLabel speedLabel = styledLabel("Player Speed:");
        JSlider speedSlider = new JSlider(4, 9, settings.playerSpeed);
        speedSlider.setOpaque(false);
        speedSlider.setForeground(Color.WHITE);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(speedLabel, gbc);
        gbc.gridx = 1; panel.add(speedSlider, gbc);

        // Save & Back
        JButton backBtn = createMenuButton("Save & Back");
        backBtn.addActionListener(e -> {
            switch (sizeBox.getSelectedIndex()) {
                case 0: settings.rows = 11; settings.cols = 15; break;
                case 1: settings.rows = 15; settings.cols = 21; break;
                case 2: settings.rows = 21; settings.cols = 29; break;
            }
            settings.braidChance = braidSlider.getValue() / 100.0;
            settings.playerSpeed = speedSlider.getValue();
            cardLayout.show(rootPanel, "MENU");
        });
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(24, 80, 10, 80);
        panel.add(backBtn, gbc);

        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(640, 480));
        panel.setBackground(new Color(24, 24, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("RUN HISTORY", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(Color.WHITE);
        panel.add(header, BorderLayout.NORTH);

        String[] cols = {"Date", "Seed", "Score", "Result", "Size"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        historyTable.setRowHeight(24);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        JButton replayBtn = new JButton("Replay Selected Seed");
        replayBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row >= 0) {
                long seed = Long.parseLong(historyTableModel.getValueAt(row, 1).toString());
                seedField.setText(String.valueOf(seed));
                startGame(seed);
            }
        });

        JButton clearBtn = new JButton("Clear History");
        clearBtn.addActionListener(e -> {
            RunHistory.clearHistory();
            refreshHistoryTable();
        });

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "MENU"));

        btnPanel.add(replayBtn);
        btnPanel.add(clearBtn);
        btnPanel.add(backBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
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
            // Allow text seeds like "abhishek" by hashing them
            return text.hashCode();
        }
    }

    private void startGame(long seed) {
        if (activeGame != null) {
            activeGame.stopGame();
            rootPanel.remove(activeGame);
        }

        MazeGraph mazeGraph = new MazeGraph(settings.rows, settings.cols, seed, settings.braidChance);
        activeGame = new GameWindow(
                mazeGraph.getMaze(),
                mazeGraph.getSeed(),
                settings.playerSpeed,
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

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(200, 38));
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