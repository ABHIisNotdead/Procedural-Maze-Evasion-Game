package logic;

import maze.MazeGraph;
import maze.MazeType;

/**
 * Game configuration settings for maps, algorithms, difficulty modes, speeds, and playback.
 */
public class GameSettings {

    public enum DifficultyMode {
        EASY("Easy", 4, 3, 180, -2),
        MEDIUM("Medium", 4, 4, 110, 0),
        HARD("Hard", 4, 5, 55, 3),
        CUSTOM("Custom", 4, 4, 110, 0);

        public final String label;
        public final int playerSpeed;
        public final int enemySpeed;
        public final int enemyDelayMs;
        public final int detectionBonus;

        DifficultyMode(String label, int playerSpeed, int enemySpeed, int enemyDelayMs, int detectionBonus) {
            this.label = label;
            this.playerSpeed = playerSpeed;
            this.enemySpeed = enemySpeed;
            this.enemyDelayMs = enemyDelayMs;
            this.detectionBonus = detectionBonus;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum MapSizePreset {
        PROGRESSIVE("Progressive (Auto-Grow)", 17, 23),
        COMPACT("Compact (26x38)", 13, 19),
        STANDARD("Standard (34x46)", 17, 23),
        BIG("Big (46x62)", 23, 31),
        MASSIVE("Massive (58x82)", 29, 41);

        public final String label;
        public final int baseRows;
        public final int baseCols;

        MapSizePreset(String label, int baseRows, int baseCols) {
            this.label = label;
            this.baseRows = baseRows;
            this.baseCols = baseCols;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public DifficultyMode difficultyMode = DifficultyMode.MEDIUM;
    public MapSizePreset mapSizePreset = MapSizePreset.PROGRESSIVE;
    public int rows = MazeGraph.DEFAULT_ROWS;          // 17
    public int cols = MazeGraph.DEFAULT_COLS;          // 23
    public double braidChance = MazeGraph.DEFAULT_BRAID_CHANCE; // 0.25
    public int playerSpeed = 4;                        // Normal balanced player speed
    public int enemySpeed = 4;                         // Balanced chaser speed
    public boolean scale2x2 = true;                    // 2x2 Scaled wide corridors

    // Live Maze Generation Visualization
    public boolean watchGeneration = true;
    public int generationStepsPerSec = 120;

    // Advanced settings
    public MazeType mazeType = MazeType.KRUSKAL_MST;
    public String theme = "GARDEN";                    // GARDEN, DUNGEON, TEMPLE, CYBER
    public String difficulty = "MEDIUM";
    public boolean soundEffects = true;
    public boolean dynamicLighting = true;
    public boolean particles = true;

    public int getEffectivePlayerSpeed() {
        if (difficultyMode == DifficultyMode.CUSTOM) return playerSpeed;
        return difficultyMode.playerSpeed;
    }

    public int getEffectiveEnemySpeed(int levelBaseSpeed) {
        if (difficultyMode == DifficultyMode.CUSTOM) return enemySpeed;
        return Math.max(difficultyMode.enemySpeed, levelBaseSpeed);
    }

    public int getEffectiveEnemyDelayMs(int levelDelayMs) {
        if (difficultyMode == DifficultyMode.CUSTOM) {
            // Map custom enemySpeed (2..8) to smooth step delay (200ms..35ms)
            return Math.max(30, 240 - enemySpeed * 26);
        }
        return Math.max(35, Math.min(difficultyMode.enemyDelayMs, levelDelayMs));
    }
}
