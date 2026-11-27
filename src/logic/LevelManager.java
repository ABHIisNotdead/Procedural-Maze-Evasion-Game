package logic;

import maze.MazeType;

/**
 * Manages game campaign progression, multi-level parameters, progressive big maps,
 * kitten rescue quotas, and chaser swarm escalation.
 */
public class LevelManager {

    public static class LevelData {
        public final int levelNumber;
        public final String name;
        public final String subtitle;
        public final String theme;
        public final MazeType mazeType;
        public final int rows;
        public final int cols;
        public final int chaserSpeed;
        public final int chaserDelayMs;
        public final int detectionRadius;
        public final int targetBonusScore;
        public final int totalKittens;
        public final int requiredKittens;
        public final boolean hasAStarTactician;

        public LevelData(int levelNumber, String name, String subtitle, String theme, MazeType mazeType,
                         int rows, int cols, int chaserSpeed, int chaserDelayMs, int detectionRadius,
                         int targetBonusScore, int totalKittens, int requiredKittens, boolean hasAStarTactician) {
            this.levelNumber = levelNumber;
            this.name = name;
            this.subtitle = subtitle;
            this.theme = theme;
            this.mazeType = mazeType;
            this.rows = rows;
            this.cols = cols;
            this.chaserSpeed = chaserSpeed;
            this.chaserDelayMs = chaserDelayMs;
            this.detectionRadius = detectionRadius;
            this.targetBonusScore = targetBonusScore;
            this.totalKittens = totalKittens;
            this.requiredKittens = requiredKittens;
            this.hasAStarTactician = hasAStarTactician;
        }
    }

    private static final LevelData[] CAMPAIGN_LEVELS = {
            new LevelData(1, "Sunny Meadow", "Green Garden • Kruskal's MST (Scaled 2x2)", "GARDEN", MazeType.KRUSKAL_MST, 17, 23, 4, 130, 9, 250, 4, 2, false),
            new LevelData(2, "Stone Crypt", "Ancient Dungeon • DFS Backtracker (Scaled 2x2)", "DUNGEON", MazeType.DFS_BACKTRACKER, 21, 29, 4, 110, 10, 450, 4, 2, false),
            new LevelData(3, "Sunken Ruins", "Golden Temple • Prim's Min-Heap MST (Scaled 2x2)", "TEMPLE", MazeType.PRIMS_MST, 25, 35, 4, 95, 11, 650, 5, 3, true),
            new LevelData(4, "Deep Caverns", "Crystal Caves • Cellular Automata (Scaled 2x2)", "DUNGEON", MazeType.CAVERN_CELLULAR, 29, 41, 5, 80, 12, 900, 5, 3, true),
            new LevelData(5, "Cyber Matrix", "Neon Grid • High Loop Gauntlet (Scaled 2x2)", "CYBER", MazeType.KRUSKAL_MST, 33, 47, 5, 65, 13, 1300, 6, 4, true)
    };

    public static LevelData getLevel(int levelNumber) {
        if (levelNumber <= 0) levelNumber = 1;
        if (levelNumber <= CAMPAIGN_LEVELS.length) {
            return CAMPAIGN_LEVELS[levelNumber - 1];
        }

        // Procedural endless levels beyond level 5 with progressive big maps
        int endlessLevel = levelNumber;
        String[] themes = {"GARDEN", "DUNGEON", "TEMPLE", "CYBER"};
        MazeType[] types = MazeType.values();
        String theme = themes[(endlessLevel - 1) % themes.length];
        MazeType type = types[(endlessLevel - 1) % types.length];
        int r = Math.min(39, 33 + (endlessLevel - 5) * 2);
        int c = Math.min(55, 47 + (endlessLevel - 5) * 2);
        int delay = Math.max(140, 185 - (endlessLevel - 5) * 8);

        return new LevelData(
                endlessLevel,
                "Master Level " + endlessLevel,
                "Endless Challenge • " + type.displayName + " (Scaled 2x2)",
                theme,
                type,
                r,
                c,
                5,
                delay,
                13,
                1300 + endlessLevel * 300,
                6,
                4,
                true
        );
    }

    /**
     * Resolves actual base maze dimensions honoring the user's selected MapSizePreset
     * while scaling up progressively across levels.
     */
    public static int[] getDimensionsForLevel(int levelNumber, GameSettings settings) {
        LevelData lvl = getLevel(levelNumber);
        if (settings == null || settings.mapSizePreset == GameSettings.MapSizePreset.PROGRESSIVE) {
            return new int[]{lvl.rows, lvl.cols};
        }

        int growth = Math.min(8, Math.max(0, levelNumber - 1) * 2);
        int r = settings.mapSizePreset.baseRows + growth;
        int c = settings.mapSizePreset.baseCols + growth;
        return new int[]{r, c};
    }

    public static int getMaxCampaignLevels() {
        return CAMPAIGN_LEVELS.length;
    }
}
