package ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;

import logic.*;
import maze.MazeGraph;
import maze.MazePoint;

public class GameWindow extends JPanel implements ActionListener {

    public enum GamePhase {
        GENERATING,
        RESCUE,
        ESCAPE,
        CLEARED,
        DEFEATED
    }

    private static class FloatingText {
        final String text;
        final Color color;
        double x, y;
        long createdAtMs;
        final long durationMs = 1200;

        FloatingText(String text, double x, double y, Color color, long gameTimeMs) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.createdAtMs = gameTimeMs;
        }
    }

    private static class Particle {
        double x, y, vx, vy;
        Color color;
        long createdAtMs;
        long lifeMs;

        Particle(double x, double y, double vx, double vy, Color color, long gameTimeMs, long lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.createdAtMs = gameTimeMs;
            this.lifeMs = lifeMs;
        }
    }

    private static final int SPRITE_WIDTH = 512;
    private static final int SPRITE_HEIGHT = 512;
    private static final int TILE_SIZE = 40;
    private static final int SCREEN_WIDTH = 700;
    private static final int SCREEN_HEIGHT = 520;

    private static final int HITBOX_SIZE = 24;
    private static final int SPRITE_SIZE = 36;
    private static final int COIN_SIZE = 14;
    private static final double FIXED_DT = 1.0 / 60.0;

    private final MazeGraph mazeGraph;
    private final int[][] map;
    private final int[][] terrain;
    private final int ROWS;
    private final int COLS;
    private final long seed;
    private final GameSettings settings;
    private final int currentLevel;
    private final LevelManager.LevelData levelData;
    private final java.util.function.Consumer<Integer> onAdvanceLevel;
    private final Runnable onExitToMenu;

    // Pause-safe Simulation Clock & Fixed-Timestep Accumulator
    private long gameTimeMs = 0;
    private long lastNanoTime = System.nanoTime();
    private double accumulator = 0.0;
    private GamePhase phase = GamePhase.RESCUE;
    private boolean isPaused = false;

    // Animated Maze Generation State
    private List<MazeGraph.GenerationStep> genSteps;
    private int[][] genPreviewGrid;
    private double genStepCursor = 0.0;
    private int genStepsPerSec = 120;

    // Player stats
    private int playerX;
    private int playerY;
    private final int baseSpeed;
    private int currentSpeed;
    private int health = 100;
    private final int maxHealth = 100;
    private int score = 0;
    private boolean tookDamage = false;
    private long invincibleUntil = 0;
    private static final long INVINCIBILITY_MS = 1200;
    private static final int ENEMY_DAMAGE = 35;

    // Stamina & Sprint & Hiding
    private float stamina = 100.0f;
    private boolean isSprinting = false;
    private boolean isHiding = false;
    private long catnipBoostUntil = 0;

    // LIFO Stack for Yarn Recall ([E])
    private final Deque<int[]> yarnStack = new ArrayDeque<>();
    private long lastYarnPushMs = 0;
    private long recallCooldownUntil = 0;
    private static final long RECALL_COOLDOWN_MS = 6000;

    // Collectibles & Distractions
    private int fishTreatCount = 0;
    private boolean[][] coins;
    private boolean[][] fishPickups;
    private boolean[][] catnipPickups;
    private int[][] pathRotations;
    private int[][] wallRotations;
    private final List<DistractionItem> droppedDistractions = new ArrayList<>();

    // Multi-Chaser Swarm
    private final List<ChaserEntity> chasers = new ArrayList<>();
    private final ChaserAI chaserAI;
    private final int chaserSpeed;
    private final int chaserDelay;

    // Bounded Deque Scent Trail & Stray Kittens
    private ScentTrail scentTrail;
    private final List<int[]> strayKittens = new ArrayList<>();
    private int kittensRescued = 0;
    private int totalKittens = 0;
    private final int requiredKittens;

    // Visual Toggles & Juice
    private boolean showMinimap = true;
    private boolean showAdsXRay = false;
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private double shakeIntensity = 0.0;

    // Smooth Camera
    private double camX;
    private double camY;

    // Input state
    private boolean up, down, left, right;
    private String playerDirection = "down";

    // Carryover Campaign State & Star Goals
    private final int initialScore;
    private final int initialHealth;
    private final int initialTreats;
    private int starsEarned = 0;

    // Sprites
    private BufferedImage playerSheet;
    private final BufferedImage[] catWalkDown = new BufferedImage[4];
    private final BufferedImage[] catWalkUp = new BufferedImage[4];
    private final BufferedImage[] catWalkLeft = new BufferedImage[4];
    private final BufferedImage[] catWalkRight = new BufferedImage[4];

    private BufferedImage dogSheet;
    // [variant: 0=SlateBlue(BFS), 1=GoldenAmber(Scent), 2=Crimson(A*)][dir: 0=up,1=down,2=left,3=right][frame]
    private final BufferedImage[][][] dogVariants = new BufferedImage[3][4][4];

    private int catAnimFrame = 0;
    private int catAnimTimer = 0;

    // Loop & Theme
    private Timer gameLoop;
    private ThemeManager.BiomeAssets currentBiome;
    private float portalAngle = 0.0f;

    public GameWindow(MazeGraph mazeGraph, GameSettings settings, int currentLevel,
                      java.util.function.Consumer<Integer> onAdvanceLevel, Runnable onExitToMenu) {
        this(mazeGraph, settings, currentLevel, 0, 100, 0, onAdvanceLevel, onExitToMenu);
    }

    public GameWindow(MazeGraph mazeGraph, GameSettings settings, int currentLevel,
                      int initialScore, int initialHealth, int initialTreats,
                      java.util.function.Consumer<Integer> onAdvanceLevel, Runnable onExitToMenu) {
        this.mazeGraph = mazeGraph;
        this.map = mazeGraph.getMaze();
        this.terrain = mazeGraph.getTerrain();
        this.ROWS = mazeGraph.getRows();
        this.COLS = mazeGraph.getCols();
        this.seed = mazeGraph.getSeed();
        this.settings = settings;
        this.currentLevel = currentLevel;
        this.initialScore = initialScore;
        this.initialHealth = Math.max(35, Math.min(100, initialHealth));
        this.initialTreats = Math.max(0, initialTreats);
        this.levelData = LevelManager.getLevel(currentLevel);
        this.baseSpeed = settings.getEffectivePlayerSpeed();
        this.onAdvanceLevel = onAdvanceLevel;
        this.onExitToMenu = onExitToMenu;

        this.chaserSpeed = settings.getEffectiveEnemySpeed(levelData.chaserSpeed);
        this.chaserDelay = settings.getEffectiveEnemyDelayMs(levelData.chaserDelayMs);
        this.requiredKittens = Math.min(levelData.requiredKittens, mazeGraph.getKittenSpawns().size());
        this.chaserAI = new ChaserAI(levelData.detectionRadius + settings.difficultyMode.detectionBonus);
        this.currentBiome = ThemeManager.getBiome(levelData.theme != null ? levelData.theme : settings.theme);

        this.coins = new boolean[ROWS][COLS];
        this.fishPickups = new boolean[ROWS][COLS];
        this.catnipPickups = new boolean[ROWS][COLS];
        this.genStepsPerSec = settings.generationStepsPerSec;

        resetPositionsAndEntities();

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        setupKeyBindings();
        loadSpriteSheets();

        lastNanoTime = System.nanoTime();
        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    public int getScore() { return score; }
    public int getHealth() { return health; }
    public int getFishTreatCount() { return fishTreatCount; }

    public void stopGame() {
        if (gameLoop != null) gameLoop.stop();
    }

    public void skipGeneration() {
        if (phase == GamePhase.GENERATING) {
            phase = GamePhase.RESCUE;
            addFloatingText("RESCUE " + requiredKittens + " KITTENS!", playerX, playerY - 20, new Color(255, 182, 193));
        }
    }

    private void resetPositionsAndEntities() {
        MazePoint spawn = mazeGraph.getSpawnPoint();
        MazePoint dogSpawn = mazeGraph.getDogSpawnPoint();
        MazePoint dog2Spawn = mazeGraph.getDog2SpawnPoint();
        MazePoint dog3Spawn = mazeGraph.getDog3SpawnPoint();

        playerX = spawn.x * TILE_SIZE + (TILE_SIZE - HITBOX_SIZE) / 2;
        playerY = spawn.y * TILE_SIZE + (TILE_SIZE - HITBOX_SIZE) / 2;

        chasers.clear();
        int detRad = Math.max(4, levelData.detectionRadius + settings.difficultyMode.detectionBonus);
        chasers.add(new ChaserEntity(1, ChaserType.BFS_TRACKER, dogSpawn.x, dogSpawn.y, TILE_SIZE,
                chaserSpeed, chaserDelay, detRad));
        chasers.add(new ChaserEntity(2, ChaserType.SCENT_HOUND, dog2Spawn.x, dog2Spawn.y, TILE_SIZE,
                Math.max(2, chaserSpeed), chaserDelay + 20, detRad + 2));

        if (levelData.hasAStarTactician) {
            chasers.add(new ChaserEntity(3, ChaserType.ASTAR_TACTICIAN, dog3Spawn.x, dog3Spawn.y, TILE_SIZE,
                    chaserSpeed, chaserDelay + 10, detRad + 3));
        }

        scentTrail = new ScentTrail(ROWS, COLS, 48, 12000);
        scentTrail.deposit(spawn.x, spawn.y, 0);

        strayKittens.clear();
        List<MazePoint> kSpawns = mazeGraph.getKittenSpawns();
        for (int i = 0; i < kSpawns.size(); i++) {
            MazePoint kp = kSpawns.get(i);
            strayKittens.add(new int[]{kp.x, kp.y, i % 3});
        }
        totalKittens = strayKittens.size();
        kittensRescued = 0;

        camX = playerX + (HITBOX_SIZE / 2.0) - (SCREEN_WIDTH / 2.0);
        camY = playerY + (HITBOX_SIZE / 2.0) - (SCREEN_HEIGHT / 2.0);

        health = initialHealth;
        score = initialScore;
        fishTreatCount = initialTreats;
        starsEarned = 0;
        tookDamage = false;
        gameTimeMs = 0;
        accumulator = 0.0;
        isPaused = false;
        invincibleUntil = 0;
        stamina = 100.0f;
        catnipBoostUntil = 0;
        recallCooldownUntil = 0;
        lastYarnPushMs = 0;
        yarnStack.clear();
        yarnStack.push(new int[]{playerX, playerY});

        up = down = left = right = isSprinting = isHiding = false;
        playerDirection = "down";
        droppedDistractions.clear();
        floatingTexts.clear();
        particles.clear();
        shakeIntensity = 0.0;

        this.pathRotations = new int[ROWS][COLS];
        this.wallRotations = new int[ROWS][COLS];
        Random rotRand = new Random(seed);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                pathRotations[r][c] = rotRand.nextInt(4);
                wallRotations[r][c] = rotRand.nextInt(4);
            }
        }

        generateCollectibles();

        // Setup Generation Animation Phase if enabled
        genSteps = mazeGraph.getGenerationSteps();
        if (settings.watchGeneration && genStepsPerSec > 0 && !genSteps.isEmpty()) {
            phase = GamePhase.GENERATING;
            genStepCursor = 0.0;
            int bRows = mazeGraph.getBaseRows();
            int bCols = mazeGraph.getBaseCols();
            genPreviewGrid = new int[bRows][bCols];
            for (int r = 0; r < bRows; r++) {
                Arrays.fill(genPreviewGrid[r], MazeGraph.WALL);
            }
            for (int cr = 0; cr < (bRows - 1) / 2; cr++) {
                for (int cc = 0; cc < (bCols - 1) / 2; cc++) {
                    genPreviewGrid[2 * cr + 1][2 * cc + 1] = MazeGraph.PATH;
                }
            }
        } else {
            phase = GamePhase.RESCUE;
            addFloatingText("RESCUE " + requiredKittens + " KITTENS!", playerX, playerY - 20, new Color(255, 182, 193));
        }
    }

    private void generateCollectibles() {
        Random rand = new Random(seed);
        MazePoint spawn = mazeGraph.getSpawnPoint();
        MazePoint goal = mazeGraph.getGoalPoint();
        List<int[]> openTiles = new ArrayList<>();

        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                coins[r][c] = false;
                fishPickups[r][c] = false;
                catnipPickups[r][c] = false;

                if (map[r][c] == 0 && terrain[r][c] != MazeGraph.TERRAIN_CAT_FLAP) {
                    if ((r == spawn.y && c == spawn.x) || (r == goal.y && c == goal.x)) continue;

                    if (rand.nextDouble() < 0.16) {
                        coins[r][c] = true;
                    } else {
                        openTiles.add(new int[]{r, c});
                    }
                }
            }
        }

        // Place sparse, balanced Fish Treats (4-7) and Catnip (2-4) across the maze
        Collections.shuffle(openTiles, rand);
        int targetFish = Math.min(7, Math.max(4, openTiles.size() / 140));
        int targetCatnip = Math.min(4, Math.max(2, openTiles.size() / 240));
        int idx = 0;

        for (int i = 0; i < targetFish && idx < openTiles.size(); i++, idx++) {
            int[] pos = openTiles.get(idx);
            fishPickups[pos[0]][pos[1]] = true;
        }
        for (int i = 0; i < targetCatnip && idx < openTiles.size(); i++, idx++) {
            int[] pos = openTiles.get(idx);
            catnipPickups[pos[0]][pos[1]] = true;
        }
    }

    private void loadSpriteSheets() {
        try {
            File catFile = new File("assets/player_sheet.png");
            if (catFile.exists()) {
                playerSheet = ImageIO.read(catFile);
                for (int i = 0; i < 4; i++) {
                    int sx = i * SPRITE_WIDTH;
                    catWalkUp[i] = playerSheet.getSubimage(sx, 0, SPRITE_WIDTH, SPRITE_HEIGHT);
                    catWalkDown[i] = playerSheet.getSubimage(sx, 1 * SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);
                    catWalkLeft[i] = playerSheet.getSubimage(sx, 2 * SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);
                    catWalkRight[i] = playerSheet.getSubimage(sx, 3 * SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);
                }
            }

            File dogFile = new File("assets/dog_sheet.png");
            if (dogFile.exists()) {
                dogSheet = ImageIO.read(dogFile);
                int dogFrameW = dogSheet.getWidth() / 4;
                int dogFrameH = dogSheet.getHeight() / 4;
                for (int dir = 0; dir < 4; dir++) {
                    for (int i = 0; i < 4; i++) {
                        BufferedImage base = dogSheet.getSubimage(i * dogFrameW, dir * dogFrameH, dogFrameW, dogFrameH);
                        dogVariants[0][dir][i] = base; // Slate-Blue Husky (BFS Tracker)
                        dogVariants[1][dir][i] = recolorDogFrame(base, 36f, 1.65f); // Golden Amber (Scent Hound)
                        dogVariants[2][dir][i] = recolorDogFrame(base, 4f, 1.75f);  // Crimson-Russet (A* Tactician)
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Warning loading character sprites: " + e.getMessage());
        }
    }

    private BufferedImage recolorDogFrame(BufferedImage src, float targetHueDeg, float satMult) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        float targetHue = targetHueDeg / 360.0f;
        float[] hsb = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 25) {
                    out.setRGB(x, y, argb);
                    continue;
                }
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                Color.RGBtoHSB(r, g, b, hsb);
                // Recolor cool fur pixels while preserving black outlines, dark eyes, and white belly
                if (hsb[1] > 0.14f && hsb[2] > 0.22f) {
                    int rgb = Color.HSBtoRGB(targetHue, Math.min(1.0f, hsb[1] * satMult), hsb[2]);
                    out.setRGB(x, y, (a << 24) | (rgb & 0x00FFFFFF));
                } else {
                    out.setRGB(x, y, argb);
                }
            }
        }
        return out;
    }

    private void setupKeyBindings() {
        this.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                // Only handle movement/sprint in KeyListener; action/toggle keys are handled once by ActionMap
                if (isMovementOrShiftKey(code)) {
                    handleRawKey(code, true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (isMovementOrShiftKey(code)) {
                    handleRawKey(code, false);
                }
            }
        });

        this.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                up = down = left = right = isSprinting = false;
            }
        });

        InputMap im = this.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        int[] movementKeys = {
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_SHIFT
        };
        for (int k : movementKeys) {
            bindKey(im, am, k);
        }

        int[] actionModifiers = {0, java.awt.event.InputEvent.SHIFT_DOWN_MASK};
        int[] actionKeys = {
                KeyEvent.VK_SPACE, KeyEvent.VK_P, KeyEvent.VK_ESCAPE, KeyEvent.VK_R,
                KeyEvent.VK_N, KeyEvent.VK_E, KeyEvent.VK_Q, KeyEvent.VK_V, KeyEvent.VK_M,
                KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS, KeyEvent.VK_ADD, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT
        };
        for (int mod : actionModifiers) {
            for (int k : actionKeys) {
                im.put(KeyStroke.getKeyStroke(k, mod, false), "ACT_" + k);
            }
        }

        for (int k : actionKeys) {
            final int key = k;
            am.put("ACT_" + k, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleRawKey(key, true);
                }
            });
        }
    }

    private boolean isMovementOrShiftKey(int code) {
        return code == KeyEvent.VK_W || code == KeyEvent.VK_S || code == KeyEvent.VK_A || code == KeyEvent.VK_D
                || code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT
                || code == KeyEvent.VK_SHIFT;
    }

    private void bindKey(InputMap im, ActionMap am, int keyCode) {
        int[] modifiers = {0, java.awt.event.InputEvent.SHIFT_DOWN_MASK};
        String name = "KEY_" + keyCode;
        for (int mod : modifiers) {
            im.put(KeyStroke.getKeyStroke(keyCode, mod, false), name + "_Pressed");
            im.put(KeyStroke.getKeyStroke(keyCode, mod, true), name + "_Released");
        }

        am.put(name + "_Pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { handleRawKey(keyCode, true); }
        });
        am.put(name + "_Released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { handleRawKey(keyCode, false); }
        });
    }

    private void handleRawKey(int keyCode, boolean pressed) {
        if (keyCode == KeyEvent.VK_SHIFT) {
            isSprinting = pressed;
            return;
        }

        if (phase == GamePhase.GENERATING) {
            if (!pressed) return;
            if (keyCode == KeyEvent.VK_SPACE) {
                skipGeneration();
            } else if (keyCode == KeyEvent.VK_EQUALS || keyCode == KeyEvent.VK_PLUS || keyCode == KeyEvent.VK_ADD) {
                genStepsPerSec = Math.min(1200, genStepsPerSec == 0 ? 120 : genStepsPerSec * 2);
            } else if (keyCode == KeyEvent.VK_MINUS || keyCode == KeyEvent.VK_SUBTRACT) {
                genStepsPerSec = Math.max(10, genStepsPerSec / 2);
            } else if (keyCode == KeyEvent.VK_ESCAPE && onExitToMenu != null) {
                onExitToMenu.run();
            }
            return;
        }

        if (phase == GamePhase.CLEARED || phase == GamePhase.DEFEATED || isPaused) {
            if (pressed) {
                if (keyCode == KeyEvent.VK_R) resetPositionsAndEntities();
                if (keyCode == KeyEvent.VK_ESCAPE && onExitToMenu != null) onExitToMenu.run();
                if (keyCode == KeyEvent.VK_P && phase != GamePhase.CLEARED && phase != GamePhase.DEFEATED) isPaused = false;
                if (keyCode == KeyEvent.VK_N && phase == GamePhase.CLEARED && onAdvanceLevel != null) onAdvanceLevel.accept(currentLevel + 1);
            }
            return;
        }

        switch (keyCode) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                up = pressed;
                if (pressed) playerDirection = "up";
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                down = pressed;
                if (pressed) playerDirection = "down";
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                left = pressed;
                if (pressed) playerDirection = "left";
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                right = pressed;
                if (pressed) playerDirection = "right";
                break;
            case KeyEvent.VK_SPACE:
                if (pressed) dropFishTreat();
                break;
            case KeyEvent.VK_E:
                if (pressed) triggerYarnRecall();
                break;
            case KeyEvent.VK_Q:
                if (pressed) toggleHide();
                break;
            case KeyEvent.VK_V:
                if (pressed) showAdsXRay = !showAdsXRay;
                break;
            case KeyEvent.VK_M:
                if (pressed) showMinimap = !showMinimap;
                break;
            case KeyEvent.VK_P:
            case KeyEvent.VK_ESCAPE:
                if (pressed) isPaused = !isPaused;
                break;
            case KeyEvent.VK_R:
                if (pressed) resetPositionsAndEntities();
                break;
        }
    }

    private void dropFishTreat() {
        if (phase != GamePhase.RESCUE && phase != GamePhase.ESCAPE) return;
        if (fishTreatCount <= 0) return;

        int gridX = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int gridY = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;

        droppedDistractions.add(new DistractionItem(gridX, gridY, 5000, gameTimeMs));
        fishTreatCount--;
        addFloatingText("TREAT DROPPED!", playerX, playerY - 10, new Color(52, 152, 219));
    }

    /**
     * LIFO Stack Ability: Pops recent positions from yarnStack to rewind out of danger!
     */
    private void triggerYarnRecall() {
        if (phase != GamePhase.RESCUE && phase != GamePhase.ESCAPE) return;
        if (gameTimeMs < recallCooldownUntil || yarnStack.size() <= 1) return;

        int[] dest = null;
        for (int i = 0; i < 6 && yarnStack.size() > 1; i++) {
            dest = yarnStack.pop();
        }
        if (dest != null) {
            spawnParticles(playerX + 12, playerY + 12, new Color(155, 89, 182), 10);
            playerX = dest[0];
            playerY = dest[1];
            isHiding = false;
            recallCooldownUntil = gameTimeMs + RECALL_COOLDOWN_MS;
            addFloatingText("YARN RECALL!", playerX, playerY - 14, new Color(187, 143, 206));
            SoundFX.play(settings.soundEffects, "RECALL");
        }
    }

    private boolean isNearHidingPlace() {
        int pCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int pRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = pRow + dr;
                int c = pCol + dc;
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    int t = terrain[r][c];
                    if (t == MazeGraph.TERRAIN_BUSH || t == MazeGraph.TERRAIN_CRACKED_WALL) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private long lastHideToggleNanos = 0;

    private void toggleHide() {
        if (phase != GamePhase.RESCUE && phase != GamePhase.ESCAPE) return;
        long nowNanos = System.nanoTime();
        if (nowNanos - lastHideToggleNanos < 180_000_000L) return; // Prevent OS key-repeat flicker
        lastHideToggleNanos = nowNanos;

        if (isHiding) {
            isHiding = false;
            addFloatingText("UNHIDDEN", playerX, playerY - 12, Color.LIGHT_GRAY);
            return;
        }
        if (isNearHidingPlace()) {
            isHiding = true;
            isSprinting = false;
            int pCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
            int pRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    scentTrail.consume(pCol + dc, pRow + dr);
                }
            }
            addFloatingText("HIDDEN! (INVISIBLE)", playerX - 10, playerY - 14, new Color(88, 214, 141));
        } else {
            addFloatingText("FIND BUSH OR CRACKED WALL!", playerX - 24, playerY - 14, new Color(241, 196, 15));
        }
    }

    private boolean canMove(int nx, int ny) {
        if (nx < 0 || ny < 0 || nx + HITBOX_SIZE > COLS * TILE_SIZE || ny + HITBOX_SIZE > ROWS * TILE_SIZE) {
            return false;
        }

        int leftCol = nx / TILE_SIZE;
        int rightCol = (nx + HITBOX_SIZE - 1) / TILE_SIZE;
        int topRow = ny / TILE_SIZE;
        int bottomRow = (ny + HITBOX_SIZE - 1) / TILE_SIZE;

        return map[topRow][leftCol] == 0
                && map[topRow][rightCol] == 0
                && map[bottomRow][leftCol] == 0
                && map[bottomRow][rightCol] == 0;
    }

    private void movePlayer(int dx, int dy) {
        int speedX = Math.abs(dx);
        int stepX = Integer.signum(dx);
        int speedY = Math.abs(dy);
        int stepY = Integer.signum(dy);

        // Pre-turn corner assist ONLY when not actively pressing a perpendicular key
        if (dy != 0 && dx == 0 && !canMove(playerX, playerY + stepY)) {
            int targetCenterX = ((playerX + HITBOX_SIZE / 2) / TILE_SIZE) * TILE_SIZE + (TILE_SIZE - HITBOX_SIZE) / 2;
            int diffX = targetCenterX - playerX;
            if (Math.abs(diffX) <= 12) {
                int slideX = Integer.signum(diffX);
                if (canMove(playerX + slideX, playerY)) {
                    playerX += slideX * Math.min(Math.abs(diffX), 3);
                }
            }
        }

        if (dx != 0 && dy == 0 && !canMove(playerX + stepX, playerY)) {
            int targetCenterY = ((playerY + HITBOX_SIZE / 2) / TILE_SIZE) * TILE_SIZE + (TILE_SIZE - HITBOX_SIZE) / 2;
            int diffY = targetCenterY - playerY;
            if (Math.abs(diffY) <= 12) {
                int slideY = Integer.signum(diffY);
                if (canMove(playerX, playerY + slideY)) {
                    playerY += slideY * Math.min(Math.abs(diffY), 3);
                }
            }
        }

        for (int i = 0; i < speedX; i++) {
            if (canMove(playerX + stepX, playerY)) {
                playerX += stepX;
            } else {
                if (dy == 0) {
                    int targetCenterY = ((playerY + HITBOX_SIZE / 2) / TILE_SIZE) * TILE_SIZE + (TILE_SIZE - HITBOX_SIZE) / 2;
                    if (playerY < targetCenterY && canMove(playerX, playerY + 1)) {
                        playerY++;
                    } else if (playerY > targetCenterY && canMove(playerX, playerY - 1)) {
                        playerY--;
                    }
                }
                break;
            }
        }

        for (int i = 0; i < speedY; i++) {
            if (canMove(playerX, playerY + stepY)) {
                playerY += stepY;
            } else {
                if (dx == 0) {
                    int targetCenterX = ((playerX + HITBOX_SIZE / 2) / TILE_SIZE) * TILE_SIZE + (TILE_SIZE - HITBOX_SIZE) / 2;
                    if (playerX < targetCenterX && canMove(playerX + 1, playerY)) {
                        playerX++;
                    } else if (playerX > targetCenterX && canMove(playerX - 1, playerY)) {
                        playerX--;
                    }
                }
                break;
            }
        }

        int pCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int pRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;
        if (pRow >= 0 && pRow < ROWS && pCol >= 0 && pCol < COLS && scentTrail != null) {
            scentTrail.deposit(pCol, pRow, gameTimeMs);
        }
    }

    private void checkPickups() {
        int centerCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int centerRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;

        if (centerRow >= 0 && centerRow < ROWS && centerCol >= 0 && centerCol < COLS) {
            if (coins[centerRow][centerCol]) {
                coins[centerRow][centerCol] = false;
                score += 10;
                spawnParticles(centerCol * TILE_SIZE + 20, centerRow * TILE_SIZE + 20, new Color(255, 215, 0), 5);
                SoundFX.play(settings.soundEffects, "COIN");
            }
            if (fishPickups[centerRow][centerCol]) {
                fishPickups[centerRow][centerCol] = false;
                fishTreatCount++;
                score += 25;
                addFloatingText("+1 FISH TREAT", playerX, playerY - 12, new Color(52, 152, 219));
            }
            if (catnipPickups[centerRow][centerCol]) {
                catnipPickups[centerRow][centerCol] = false;
                catnipBoostUntil = gameTimeMs + 4500;
                score += 50;
                addFloatingText("CATNIP ZOOMIES!", playerX, playerY - 12, new Color(46, 204, 113));
            }

            // Stray Kittens Rescue
            Iterator<int[]> it = strayKittens.iterator();
            while (it.hasNext()) {
                int[] k = it.next();
                if (k[0] == centerCol && k[1] == centerRow) {
                    it.remove();
                    kittensRescued++;
                    score += 150;
                    spawnParticles(centerCol * TILE_SIZE + 20, centerRow * TILE_SIZE + 20, new Color(255, 182, 193), 12);
                    addFloatingText("KITTEN RESCUED! +150", playerX, playerY - 16, new Color(255, 182, 193));
                    SoundFX.play(settings.soundEffects, "KITTEN");

                    if (phase == GamePhase.RESCUE && kittensRescued >= requiredKittens) {
                        phase = GamePhase.ESCAPE;
                        addFloatingText("PORTAL UNLOCKED! ESCAPE!", playerX - 30, playerY - 34, new Color(187, 143, 206));
                        SoundFX.play(settings.soundEffects, "PORTAL");
                        shakeIntensity = 4.0;
                    }
                }
            }
        }
    }

    private void checkGoalPortal() {
        if (phase != GamePhase.ESCAPE) return;
        MazePoint goal = mazeGraph.getGoalPoint();
        int centerCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int centerRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;

        if (centerCol == goal.x && centerRow == goal.y) {
            phase = GamePhase.CLEARED;
            starsEarned = 1; // Goal 1: Escaped!
            int allKittensBonus = (kittensRescued >= totalKittens) ? 200 : 0;
            if (kittensRescued >= totalKittens) starsEarned++;
            int flawlessBonus = (!tookDamage && gameTimeMs <= 75000) ? 200 : 0;
            if (!tookDamage && gameTimeMs <= 75000) starsEarned++;

            int timeBonus = Math.max(0, 300 - (int) (gameTimeMs / 1000) * 3);
            score += levelData.targetBonusScore + timeBonus + allKittensBonus + flawlessBonus;
            RunHistory.saveRun(seed, score, "ESCAPED (" + starsEarned + "* LVL " + currentLevel + ")", ROWS, COLS);
            SoundFX.play(settings.soundEffects, "PORTAL");
        }
    }

    private void checkEnemyCollision() {
        if (isHiding || gameTimeMs < invincibleUntil) return;

        Rectangle playerRect = new Rectangle(playerX, playerY, HITBOX_SIZE, HITBOX_SIZE);
        boolean catnipActive = gameTimeMs < catnipBoostUntil;

        for (ChaserEntity chaser : chasers) {
            if (chaser.isStunned(gameTimeMs)) continue;

            Rectangle enemyRect = new Rectangle(chaser.pixelX + 4, chaser.pixelY + 4, 32, 32);
            if (playerRect.intersects(enemyRect)) {
                // Catnip zoomies or stunning contact knocks a carried kitten loose!
                if (catnipActive) {
                    boolean dropped = chaser.stun(2500, gameTimeMs);
                    if (dropped) {
                        strayKittens.add(new int[]{chaser.gridX, chaser.gridY});
                        addFloatingText("KITTEN DROPPED!", chaser.pixelX, chaser.pixelY - 12, Color.ORANGE);
                    } else {
                        addFloatingText("DOG BONKED!", chaser.pixelX, chaser.pixelY - 12, Color.YELLOW);
                    }
                    SoundFX.play(settings.soundEffects, "STUN");
                    continue;
                }

                health = Math.max(0, health - ENEMY_DAMAGE);
                tookDamage = true;
                invincibleUntil = gameTimeMs + INVINCIBILITY_MS;
                shakeIntensity = 6.0;
                SoundFX.play(settings.soundEffects, "HIT");

                if (health <= 0) {
                    phase = GamePhase.DEFEATED;
                    RunHistory.saveRun(seed, score, "DEFEATED (LVL " + currentLevel + ")", ROWS, COLS);
                }
                break;
            }
        }
    }

    private void updateChasers() {
        int playerGridX = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int playerGridY = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;
        int dirX = (right ? 1 : 0) - (left ? 1 : 0);
        int dirY = (down ? 1 : 0) - (up ? 1 : 0);

        chaserAI.updateSwarm(
                map,
                terrain,
                chasers,
                playerGridX,
                playerGridY,
                dirX,
                dirY,
                isHiding,
                scentTrail,
                droppedDistractions,
                strayKittens,
                TILE_SIZE,
                gameTimeMs,
                phase == GamePhase.ESCAPE
        );

        for (ChaserEntity chaser : chasers) {
            chaser.updatePixelStep();
        }
    }

    private void fixedUpdate(double dt) {
        gameTimeMs += (long) (dt * 1000.0);

        if (phase == GamePhase.GENERATING) {
            int prevIdx = (int) genStepCursor;
            genStepCursor += genStepsPerSec * dt;
            int nextIdx = Math.min(genSteps.size(), (int) genStepCursor);
            for (int i = prevIdx; i < nextIdx; i++) {
                MazeGraph.GenerationStep step = genSteps.get(i);
                genPreviewGrid[step.baseRow][step.baseCol] = step.value;
            }
            if (nextIdx >= genSteps.size()) {
                phase = GamePhase.RESCUE;
                addFloatingText("RESCUE " + requiredKittens + " KITTENS!", playerX, playerY - 20, new Color(255, 182, 193));
            }
            return;
        }

        if (phase != GamePhase.RESCUE && phase != GamePhase.ESCAPE) return;

        boolean moving = (up || down || left || right);
        boolean boosted = gameTimeMs < catnipBoostUntil;

        int pCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int pRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;
        boolean inPuddle = (pRow >= 0 && pRow < ROWS && pCol >= 0 && pCol < COLS)
                && terrain[pRow][pCol] == MazeGraph.TERRAIN_PUDDLE;

        if (isSprinting && moving && stamina > 0) {
            currentSpeed = baseSpeed + 3 + (boosted ? 2 : 0);
            stamina = Math.max(0, stamina - 1.0f);
            if (settings.particles && gameTimeMs % 80 < 20) {
                spawnParticles(playerX + 12, playerY + 20, new Color(255, 255, 255, 180), 1);
            }
        } else {
            currentSpeed = baseSpeed + (boosted ? 3 : 0);
            if (!isSprinting || !moving) {
                stamina = Math.min(100.0f, stamina + 0.6f);
            }
        }

        if (inPuddle && !boosted) {
            currentSpeed = Math.max(2, currentSpeed - 2);
        }

        int dx = 0, dy = 0;
        if (up) dy -= currentSpeed;
        if (down) dy += currentSpeed;
        if (left) dx -= currentSpeed;
        if (right) dx += currentSpeed;

        if (moving) {
            isHiding = false;
            movePlayer(dx, dy);
            checkPickups();
            checkGoalPortal();

            if (gameTimeMs - lastYarnPushMs >= 180) {
                yarnStack.push(new int[]{playerX, playerY});
                if (yarnStack.size() > 18) {
                    // Trim oldest from bottom of stack
                    Iterator<int[]> desc = yarnStack.descendingIterator();
                    if (desc.hasNext()) {
                        desc.next();
                        desc.remove();
                    }
                }
                lastYarnPushMs = gameTimeMs;
            }

            catAnimTimer++;
            int animThreshold = (isSprinting && stamina > 0) ? 5 : 8;
            if (catAnimTimer > animThreshold) {
                catAnimFrame = (catAnimFrame + 1) % 4;
                catAnimTimer = 0;
            }
        } else {
            catAnimFrame = 0;
        }

        scentTrail.update(gameTimeMs);
        updateChasers();
        checkEnemyCollision();

        droppedDistractions.removeIf(item -> item.isExpired(gameTimeMs));
        floatingTexts.removeIf(ft -> (gameTimeMs - ft.createdAtMs) > ft.durationMs);
        for (FloatingText ft : floatingTexts) ft.y -= 0.35;

        particles.removeIf(p -> (gameTimeMs - p.createdAtMs) > p.lifeMs);
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
        }

        shakeIntensity = Math.max(0.0, shakeIntensity * 0.85);

        double targetCamX = playerX + (HITBOX_SIZE / 2.0) - (SCREEN_WIDTH / 2.0);
        double targetCamY = playerY + (HITBOX_SIZE / 2.0) - (SCREEN_HEIGHT / 2.0);
        camX += (targetCamX - camX) * 0.15;
        camY += (targetCamY - camY) * 0.15;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long nowNanos = System.nanoTime();
        double dt = (e == null) ? FIXED_DT : Math.min(0.05, (nowNanos - lastNanoTime) / 1_000_000_000.0);
        lastNanoTime = nowNanos;

        if (!isPaused && phase != GamePhase.CLEARED && phase != GamePhase.DEFEATED) {
            accumulator += dt;
            while (accumulator >= FIXED_DT) {
                fixedUpdate(FIXED_DT);
                accumulator -= FIXED_DT;
            }
        }

        portalAngle += (phase == GamePhase.ESCAPE ? 0.11f : 0.04f);
        repaint();
    }

    private void addFloatingText(String text, double x, double y, Color color) {
        floatingTexts.add(new FloatingText(text, x, y, color, gameTimeMs));
    }

    private void spawnParticles(double x, double y, Color color, int count) {
        if (!settings.particles) return;
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double spd = 0.6 + rng.nextDouble() * 1.6;
            particles.add(new Particle(x, y, Math.cos(ang) * spd, Math.sin(ang) * spd, color, gameTimeMs, 450));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (phase == GamePhase.GENERATING) {
            drawGenerationVisualizer(g2);
            g2.dispose();
            return;
        }

        int shakeX = shakeIntensity > 0.2 ? (int) ((Math.random() - 0.5) * shakeIntensity * 2) : 0;
        int shakeY = shakeIntensity > 0.2 ? (int) ((Math.random() - 0.5) * shakeIntensity * 2) : 0;
        int camOffsetX = (int) Math.round(camX) + shakeX;
        int camOffsetY = (int) Math.round(camY) + shakeY;

        g2.translate(-camOffsetX, -camOffsetY);

        int startCol = Math.max(0, camOffsetX / TILE_SIZE);
        int endCol = Math.min(COLS, (camOffsetX + SCREEN_WIDTH) / TILE_SIZE + 2);
        int startRow = Math.max(0, camOffsetY / TILE_SIZE);
        int endRow = Math.min(ROWS, (camOffsetY + SCREEN_HEIGHT) / TILE_SIZE + 2);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                int px = c * TILE_SIZE;
                int py = r * TILE_SIZE;

                int rot = (map[r][c] == 1) ? wallRotations[r][c] : pathRotations[r][c];
                boolean isCrackedWall = (map[r][c] == 1 && terrain[r][c] == MazeGraph.TERRAIN_CRACKED_WALL);
                Image tileImg = (map[r][c] == 1)
                        ? (isCrackedWall ? currentBiome.crackedWall : currentBiome.wall)
                        : currentBiome.path;

                if (rot == 0 || isCrackedWall) {
                    g2.drawImage(tileImg, px, py, TILE_SIZE, TILE_SIZE, null);
                } else {
                    Graphics2D gTile = (Graphics2D) g2.create();
                    gTile.translate(px + TILE_SIZE / 2, py + TILE_SIZE / 2);
                    gTile.rotate(rot * (Math.PI / 2.0));
                    gTile.drawImage(tileImg, -TILE_SIZE / 2, -TILE_SIZE / 2, TILE_SIZE, TILE_SIZE, null);
                    gTile.dispose();
                }

                if (map[r][c] == 1) {
                    g2.setColor(isCrackedWall ? new Color(88, 214, 141, 140) : currentBiome.wallBorderColor);
                    g2.drawRect(px, py, TILE_SIZE, TILE_SIZE);
                } else {
                    // Interactive Terrain Overlays
                    int terr = terrain[r][c];
                    if (terr == MazeGraph.TERRAIN_PUDDLE) {
                        g2.setColor(new Color(41, 128, 185, 130));
                        g2.fillOval(px + 4, py + 6, 32, 26);
                        g2.setColor(new Color(174, 214, 241, 160));
                        g2.drawOval(px + 6, py + 8, 28, 22);
                    } else if (terr == MazeGraph.TERRAIN_BUSH) {
                        Image bushImg = ThemeManager.getBushIcon();
                        if (bushImg != null) {
                            g2.drawImage(bushImg, px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2, null);
                        } else {
                            g2.setColor(new Color(30, 132, 73, 175));
                            g2.fillOval(px + 4, py + 4, 32, 32);
                        }
                    } else if (terr == MazeGraph.TERRAIN_CAT_FLAP) {
                        g2.setColor(new Color(230, 126, 34, 180));
                        g2.fillRoundRect(px + 6, py + 6, 28, 28, 10, 10);
                        g2.setFont(new Font("Arial", Font.BOLD, 10));
                        drawOutlinedString(g2, "CAT", px + 9, py + 24, new Color(255, 220, 120));
                    }

                    if (coins[r][c]) {
                        int bob = (int) (Math.sin(gameTimeMs / 150.0 + c + r) * 3);
                        int cx = px + (TILE_SIZE - COIN_SIZE) / 2;
                        int cy = py + (TILE_SIZE - COIN_SIZE) / 2 + bob;
                        g2.setColor(new Color(255, 215, 0));
                        g2.fillOval(cx, cy, COIN_SIZE, COIN_SIZE);
                        g2.setColor(new Color(212, 160, 23));
                        g2.drawOval(cx, cy, COIN_SIZE, COIN_SIZE);
                    }

                    if (fishPickups[r][c]) {
                        g2.drawImage(ThemeManager.getFishTreatIcon(), px + 6, py + 6, 28, 28, null);
                    }

                    if (catnipPickups[r][c]) {
                        g2.drawImage(ThemeManager.getCatnipIcon(), px + 6, py + 6, 28, 28, null);
                    }
                }
            }
        }

        // Optional Live ADS X-Ray Overlay ([V])
        if (showAdsXRay) {
            drawAdsXRayOverlay(g2);
        }

        // Dropped Distractions
        for (DistractionItem item : droppedDistractions) {
            if (!item.isExpired(gameTimeMs)) {
                int ix = item.gridX * TILE_SIZE + 4;
                int iy = item.gridY * TILE_SIZE + 4;
                g2.drawImage(ThemeManager.getFishTreatIcon(), ix, iy, 32, 32, null);
                g2.setColor(new Color(52, 152, 219, 90));
                int pulse = (int) ((gameTimeMs % 1000) / 25);
                g2.drawOval(ix - pulse / 2, iy - pulse / 2, 32 + pulse, 32 + pulse);
            }
        }

        // Goal Portal (Locked vs Unlocked)
        MazePoint goal = mazeGraph.getGoalPoint();
        int gx = goal.x * TILE_SIZE;
        int gy = goal.y * TILE_SIZE;
        Image portalRune = ThemeManager.getPortalRuneIcon();
        if (portalRune != null) {
            Graphics2D gPortal = (Graphics2D) g2.create();
            if (phase == GamePhase.RESCUE) {
                gPortal.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f));
            }
            gPortal.translate(gx + TILE_SIZE / 2, gy + TILE_SIZE / 2);
            gPortal.rotate(portalAngle);
            gPortal.drawImage(portalRune, -27, -27, 54, 54, null);
            gPortal.dispose();

            if (phase == GamePhase.RESCUE) {
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                drawOutlinedString(g2, "LOCKED", gx + 1, gy - 4, new Color(255, 110, 100));
            }
        }

        // Stray Kittens (Multi-color Cat-style sprites)
        for (int[] k : strayKittens) {
            int variant = k.length > 2 ? k[2] : 0;
            Image kittenImg = ThemeManager.getKittenIcon(variant);
            int kx = k[0] * TILE_SIZE + 5;
            int ky = k[1] * TILE_SIZE + 5 + (int) (Math.sin(gameTimeMs / 180.0 + k[0]) * 3);
            if (kittenImg != null) {
                g2.drawImage(kittenImg, kx, ky, 30, 30, null);
            } else {
                g2.setColor(new Color(255, 210, 175));
                g2.fillOval(kx, ky, 26, 26);
            }
            g2.setColor(new Color(255, 225, 120, 110));
            g2.drawOval(kx - 2, ky - 2, 34, 34);
        }

        // Particles
        for (Particle p : particles) {
            g2.setColor(p.color);
            g2.fillOval((int) p.x, (int) p.y, 5, 5);
        }

        drawDogs(g2);
        drawCat(g2);

        // Floating Text Popups with crisp black border
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        for (FloatingText ft : floatingTexts) {
            drawOutlinedString(g2, ft.text, (int) ft.x, (int) ft.y, ft.color);
        }

        if (settings.dynamicLighting) {
            drawDynamicLighting(g2, camOffsetX, camOffsetY);
        }

        g2.translate(camOffsetX, camOffsetY);

        drawHUD(g2);
        if (showMinimap) {
            drawMinimap(g2);
        }

        if (isPaused) {
            drawPauseOverlay(g2);
        } else if (phase == GamePhase.DEFEATED) {
            drawEndScreen(g2, "GAME OVER", "The Dog Swarm caught you!", new Color(231, 76, 60), false);
        } else if (phase == GamePhase.CLEARED) {
            drawEndScreen(g2, "LEVEL " + currentLevel + " CLEARED!",
                    "Kittens: " + kittensRescued + "/" + totalKittens + " | AI Nodes: " + chaserAI.getTotalNodesExplored(),
                    new Color(46, 204, 113), true);
        }

        g2.dispose();
    }

    private void drawGenerationVisualizer(Graphics2D g2) {
        g2.setColor(new Color(18, 20, 28));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        int bRows = genPreviewGrid.length;
        int bCols = genPreviewGrid[0].length;
        int padTop = 64;
        int availW = SCREEN_WIDTH - 32;
        int availH = SCREEN_HEIGHT - padTop - 24;
        int cellPx = Math.max(4, Math.min(availW / bCols, availH / bRows));
        int offsetX = (SCREEN_WIDTH - bCols * cellPx) / 2;
        int offsetY = padTop + (availH - bRows * cellPx) / 2;

        // Highlight recent 10 steps
        int currentIdx = Math.min(genSteps.size(), (int) genStepCursor);
        Set<String> recent = new HashSet<>();
        String activePhase = mazeGraph.getMazeType().displayName;
        for (int i = Math.max(0, currentIdx - 10); i < currentIdx; i++) {
            MazeGraph.GenerationStep s = genSteps.get(i);
            recent.add(s.baseRow + "," + s.baseCol);
            activePhase = s.phase;
        }

        for (int r = 0; r < bRows; r++) {
            for (int c = 0; c < bCols; c++) {
                int x = offsetX + c * cellPx;
                int y = offsetY + r * cellPx;
                if (recent.contains(r + "," + c)) {
                    g2.setColor(new Color(241, 196, 15));
                } else if (genPreviewGrid[r][c] == MazeGraph.PATH) {
                    g2.setColor(new Color(46, 204, 113, 210));
                } else {
                    g2.setColor(new Color(32, 36, 48));
                }
                g2.fillRect(x, y, cellPx, cellPx);
            }
        }

        // Top Generation HUD
        g2.setColor(new Color(24, 27, 36, 240));
        g2.fillRoundRect(8, 6, SCREEN_WIDTH - 16, 50, 12, 12);
        g2.setColor(new Color(245, 180, 65));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("LIVE MAZE GENERATION: " + mazeGraph.getMazeType().displayName + " [" + activePhase + "]", 20, 26);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Step: " + currentIdx + " / " + genSteps.size()
                + "   |   Speed: " + genStepsPerSec + " steps/s   |   [+] Faster   [-] Slower   [SPACE] Skip", 20, 46);
    }

    private void drawAdsXRayOverlay(Graphics2D g2) {
        // 1. Scent Trail Deque nodes
        for (ScentTrail.ScentNode node : scentTrail.getNodes()) {
            long age = gameTimeMs - node.timestampMs;
            int alpha = Math.max(30, (int) (180 * (1.0 - (double) age / 12000.0)));
            g2.setColor(new Color(241, 196, 15, alpha));
            g2.fillOval(node.x * TILE_SIZE + 14, node.y * TILE_SIZE + 14, 12, 12);
        }

        // 2. Player Yarn Recall LIFO Stack
        g2.setColor(new Color(187, 143, 206, 180));
        for (int[] pos : yarnStack) {
            g2.drawOval(pos[0] + 4, pos[1] + 4, 16, 16);
        }

        // 3. Chasers' BFS / A* Debug Paths
        for (ChaserEntity chaser : chasers) {
            if (chaser.debugPath == null || chaser.debugPath.isEmpty()) continue;
            if (chaser.type == ChaserType.BFS_TRACKER) {
                g2.setColor(new Color(52, 152, 219, 190));
            } else if (chaser.type == ChaserType.ASTAR_TACTICIAN) {
                g2.setColor(new Color(155, 89, 182, 200));
            } else {
                g2.setColor(new Color(241, 196, 15, 190));
            }
            g2.setStroke(new BasicStroke(2.5f));
            int prevX = chaser.gridX * TILE_SIZE + TILE_SIZE / 2;
            int prevY = chaser.gridY * TILE_SIZE + TILE_SIZE / 2;
            for (int[] step : chaser.debugPath) {
                int nx = step[0] * TILE_SIZE + TILE_SIZE / 2;
                int ny = step[1] * TILE_SIZE + TILE_SIZE / 2;
                g2.drawLine(prevX, prevY, nx, ny);
                prevX = nx;
                prevY = ny;
            }
        }

        // 4. Predicted Player Intercept Target
        int pxPred = chaserAI.getPredictedTargetX();
        int pyPred = chaserAI.getPredictedTargetY();
        if (pxPred >= 0 && pyPred >= 0) {
            int cx = pxPred * TILE_SIZE + TILE_SIZE / 2;
            int cy = pyPred * TILE_SIZE + TILE_SIZE / 2;
            g2.setColor(new Color(231, 76, 60, 210));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(cx - 12, cy - 12, 24, 24);
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            drawOutlinedString(g2, "PREDICT", cx - 18, cy - 15, new Color(255, 130, 120));
        }
    }

    private void drawMinimap(Graphics2D g2) {
        int mmW = 148;
        int mmH = 104;
        int mmX = SCREEN_WIDTH - mmW - 12;
        int mmY = SCREEN_HEIGHT - mmH - 12;

        g2.setColor(new Color(18, 20, 28, 210));
        g2.fillRoundRect(mmX - 4, mmY - 4, mmW + 8, mmH + 8, 10, 10);
        g2.setColor(new Color(65, 75, 95));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(mmX - 4, mmY - 4, mmW + 8, mmH + 8, 10, 10);

        double sx = (double) mmW / COLS;
        double sy = (double) mmH / ROWS;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (map[r][c] == 0) {
                    g2.setColor(new Color(70, 80, 100));
                    g2.fillRect(mmX + (int) (c * sx), mmY + (int) (r * sy), Math.max(1, (int) Math.ceil(sx)), Math.max(1, (int) Math.ceil(sy)));
                }
            }
        }

        // Goal Portal
        MazePoint goal = mazeGraph.getGoalPoint();
        g2.setColor(phase == GamePhase.ESCAPE ? new Color(187, 143, 206) : Color.GRAY);
        g2.fillRect(mmX + (int) (goal.x * sx) - 2, mmY + (int) (goal.y * sy) - 2, 6, 6);

        // Stray Kittens
        g2.setColor(new Color(255, 182, 193));
        for (int[] k : strayKittens) {
            g2.fillOval(mmX + (int) (k[0] * sx) - 1, mmY + (int) (k[1] * sy) - 1, 4, 4);
        }

        // Dogs
        g2.setColor(new Color(231, 76, 60));
        for (ChaserEntity chaser : chasers) {
            g2.fillOval(mmX + (int) (chaser.gridX * sx) - 1, mmY + (int) (chaser.gridY * sy) - 1, 5, 5);
        }

        // Player Cat
        int pCol = (playerX + HITBOX_SIZE / 2) / TILE_SIZE;
        int pRow = (playerY + HITBOX_SIZE / 2) / TILE_SIZE;
        g2.setColor(new Color(46, 204, 113));
        g2.fillOval(mmX + (int) (pCol * sx) - 2, mmY + (int) (pRow * sy) - 2, 6, 6);
    }

    private void drawCat(Graphics2D g2) {
        boolean isInvincible = gameTimeMs < invincibleUntil;
        boolean blink = !isInvincible || (gameTimeMs / 120) % 2 == 0;

        if (blink) {
            BufferedImage frame = null;
            switch (playerDirection) {
                case "up": frame = catWalkUp[catAnimFrame]; break;
                case "down": frame = catWalkDown[catAnimFrame]; break;
                case "left": frame = catWalkLeft[catAnimFrame]; break;
                case "right": frame = catWalkRight[catAnimFrame]; break;
            }

            int drawOffset = (SPRITE_SIZE - HITBOX_SIZE) / 2;

            if (isSprinting && (up || down || left || right) && stamina > 0) {
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillOval(playerX - drawOffset + 2, playerY - drawOffset + 2, SPRITE_SIZE - 4, SPRITE_SIZE - 4);
            }

            if (frame != null) {
                if (isHiding) {
                    Graphics2D gInv = (Graphics2D) g2.create();
                    gInv.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
                    gInv.drawImage(frame, playerX - drawOffset, playerY - drawOffset, SPRITE_SIZE, SPRITE_SIZE, null);
                    gInv.dispose();
                } else {
                    g2.drawImage(frame, playerX - drawOffset, playerY - drawOffset, SPRITE_SIZE, SPRITE_SIZE, null);
                }
            } else {
                g2.setColor(new Color(243, 156, 18));
                g2.fillOval(playerX, playerY, HITBOX_SIZE, HITBOX_SIZE);
            }

            if (gameTimeMs < catnipBoostUntil) {
                g2.setColor(new Color(46, 204, 113, 140));
                g2.drawOval(playerX - drawOffset - 2, playerY - drawOffset - 2, SPRITE_SIZE + 4, SPRITE_SIZE + 4);
            }

            if (isHiding) {
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                drawOutlinedString(g2, "INVISIBLE [Q]", playerX - 18, playerY - 10, new Color(88, 214, 141));
            } else if (isNearHidingPlace()) {
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                drawOutlinedString(g2, "[Q] HIDE", playerX - 8, playerY - 10, new Color(241, 196, 15));
            }
        }

        // Objective Compass Arrow pointing to next Goal (nearest Kitten or unlocked Portal)
        drawObjectiveCompass(g2);
    }

    private void drawObjectiveCompass(Graphics2D g2) {
        if (phase != GamePhase.RESCUE && phase != GamePhase.ESCAPE) return;
        double cx = playerX + HITBOX_SIZE / 2.0;
        double cy = playerY + HITBOX_SIZE / 2.0;
        double targetPx = -1, targetPy = -1;
        Color arrowColor = new Color(255, 195, 215);

        if (phase == GamePhase.ESCAPE) {
            MazePoint goal = mazeGraph.getGoalPoint();
            targetPx = goal.x * TILE_SIZE + TILE_SIZE / 2.0;
            targetPy = goal.y * TILE_SIZE + TILE_SIZE / 2.0;
            arrowColor = new Color(205, 150, 245);
        } else if (!strayKittens.isEmpty()) {
            double bestDist = Double.MAX_VALUE;
            for (int[] k : strayKittens) {
                double kx = k[0] * TILE_SIZE + TILE_SIZE / 2.0;
                double ky = k[1] * TILE_SIZE + TILE_SIZE / 2.0;
                double d = Math.hypot(kx - cx, ky - cy);
                if (d < bestDist) {
                    bestDist = d;
                    targetPx = kx;
                    targetPy = ky;
                }
            }
        }

        if (targetPx < 0) return;
        double dist = Math.hypot(targetPx - cx, targetPy - cy);
        if (dist < TILE_SIZE * 2.2) return; // Hide when right next to target

        double angle = Math.atan2(targetPy - cy, targetPx - cx);
        Graphics2D ga = (Graphics2D) g2.create();
        ga.translate(cx + Math.cos(angle) * 28, cy + Math.sin(angle) * 28);
        ga.rotate(angle);
        int[] xs = {8, -5, -5};
        int[] ys = {0, -5, 5};
        ga.setColor(Color.BLACK);
        ga.setStroke(new BasicStroke(2.5f));
        ga.drawPolygon(xs, ys, 3);
        ga.setColor(arrowColor);
        ga.fillPolygon(xs, ys, 3);
        ga.dispose();
    }

    private void drawDogs(Graphics2D g2) {
        Image kittenIcon = ThemeManager.getKittenIcon();

        for (ChaserEntity chaser : chasers) {
            int variant = 0;
            if (chaser.type == ChaserType.SCENT_HOUND) variant = 1;
            else if (chaser.type == ChaserType.ASTAR_TACTICIAN) variant = 2;

            int dirIdx = 1;
            switch (chaser.direction) {
                case "up": dirIdx = 0; break;
                case "down": dirIdx = 1; break;
                case "left": dirIdx = 2; break;
                case "right": dirIdx = 3; break;
            }
            BufferedImage frame = dogVariants[variant][dirIdx][chaser.animFrame];

            int dx = chaser.pixelX - 6;
            int dy = chaser.pixelY - 6;

            if (frame != null) {
                g2.drawImage(frame, dx, dy, 52, 52, null);
            } else {
                g2.setColor(variant == 1 ? new Color(230, 170, 70) : (variant == 2 ? new Color(210, 85, 80) : new Color(110, 140, 180)));
                g2.fillRect(chaser.pixelX + 4, chaser.pixelY + 4, 32, 32);
            }

            if (chaser.isStunned(gameTimeMs)) {
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                long t = gameTimeMs / 70;
                for (int i = 0; i < 3; i++) {
                    double starAng = (t * 0.15) + (i * (2 * Math.PI / 3));
                    int sx = chaser.pixelX + 16 + (int) (Math.cos(starAng) * 14);
                    int sy = chaser.pixelY - 6 + (int) (Math.sin(starAng) * 6);
                    drawOutlinedString(g2, "★", sx, sy, new Color(241, 196, 15));
                }
            }

            if (chaser.carryingKitten) {
                if (kittenIcon != null) {
                    g2.drawImage(kittenIcon, chaser.pixelX + 8, chaser.pixelY - 14, 24, 24, null);
                } else {
                    g2.setColor(new Color(255, 210, 175));
                    g2.fillOval(chaser.pixelX + 10, chaser.pixelY - 10, 20, 20);
                }
            }

            if (chaser.speechBubble != null) {
                drawSpeechBubble(g2, chaser.pixelX + TILE_SIZE / 2, chaser.pixelY - 14, chaser.speechBubble, chaser.speechColor);
            }
        }
    }

    private void drawSpeechBubble(Graphics2D g2, int cx, int cy, String text, Color accent) {
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int bw = Math.max(22, fm.stringWidth(text) + 12);
        int bh = 18;

        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(cx - bw / 2, cy - bh / 2, bw, bh, 6, 6);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cx - bw / 2, cy - bh / 2, bw, bh, 6, 6);
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + 5);
    }

    private void drawDynamicLighting(Graphics2D g2, int camX, int camY) {
        int centerX = playerX + HITBOX_SIZE / 2;
        int centerY = playerY + HITBOX_SIZE / 2;
        float radius = 250.0f;

        Point center = new Point(centerX, centerY);
        float[] dist = {0.0f, 0.65f, 1.0f};
        Color[] colors = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 105), new Color(5, 5, 12, 205)};

        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g2.setPaint(p);
        g2.fillRect(camX, camY, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    private void drawHUD(Graphics2D g2) {
        int hudH = 74;
        g2.setColor(new Color(24, 27, 36, 235));
        g2.fillRoundRect(8, 6, SCREEN_WIDTH - 16, hudH, 14, 14);
        g2.setColor(phase == GamePhase.ESCAPE ? new Color(187, 143, 206) : new Color(45, 52, 68));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(8, 6, SCREEN_WIDTH - 16, hudH, 14, 14);

        // --- ROW 1 ---
        int barX = 18, barY = 12, barW = 90, barH = 16;
        g2.setColor(new Color(36, 40, 52));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        int fillW = (int) ((health / (double) maxHealth) * barW);
        g2.setColor(health > 35 ? new Color(46, 204, 113) : new Color(231, 76, 60));
        g2.fillRoundRect(barX, barY, fillW, barH, 8, 8);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        drawOutlinedString(g2, health + " HP", barX + 24, barY + 12, Color.WHITE);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        String obj = (phase == GamePhase.ESCAPE) ? "ESCAPE PORTAL!" : ("LVL " + currentLevel + " (" + ROWS + "x" + COLS + ")");
        drawOutlinedString(g2, obj, 118, 24, new Color(245, 180, 65));

        g2.setFont(new Font("Arial", Font.BOLD, 11));
        drawOutlinedString(g2, "Treats[SPC]:" + fishTreatCount, 270, 24, new Color(240, 240, 245));

        boolean recallReady = gameTimeMs >= recallCooldownUntil;
        String recallTxt = recallReady ? "Recall[E]:READY" : "Recall[E]:" + ((recallCooldownUntil - gameTimeMs) / 1000 + 1) + "s";
        drawOutlinedString(g2, recallTxt, 368, 24, recallReady ? new Color(187, 143, 206) : Color.GRAY);

        boolean sprintActive = isSprinting && stamina > 0;
        int stamX = 480, stamY = 14;
        g2.setColor(new Color(36, 40, 52));
        g2.fillRoundRect(stamX, stamY, 55, 12, 6, 6);
        g2.setColor(sprintActive ? new Color(241, 196, 15) : new Color(52, 152, 219));
        g2.fillRoundRect(stamX, stamY, (int) (stamina * 0.55), 12, 6, 6);
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        drawOutlinedString(g2, sprintActive ? "SPRINT" : "Sprint", stamX + 11, stamY + 9, Color.WHITE);

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String scoreText = "Score: " + score;
        FontMetrics fm = g2.getFontMetrics();
        drawOutlinedString(g2, scoreText, SCREEN_WIDTH - fm.stringWidth(scoreText) - 18, 24, new Color(245, 180, 65));

        // --- ROW 2 (Swarm FSM) ---
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        drawOutlinedString(g2, "♥ Kittens: " + kittensRescued + "/" + requiredKittens + " req (" + totalKittens + ")",
                18, 46, kittensRescued >= requiredKittens ? new Color(46, 204, 113) : new Color(255, 182, 193));

        String d1 = chasers.size() > 0 ? chasers.get(0).state.name() : "-";
        drawOutlinedString(g2, "[BFS]:" + d1, 200, 46, new Color(93, 173, 226));

        String d2 = chasers.size() > 1 ? chasers.get(1).state.name() : "-";
        drawOutlinedString(g2, "[Scent]:" + d2, 315, 46, new Color(245, 176, 65));

        if (chasers.size() > 2) {
            drawOutlinedString(g2, "[A*]:" + chasers.get(2).state.name(), 440, 46, new Color(210, 120, 120));
        }

        drawOutlinedString(g2, "[V] X-Ray  [M] Map", 565, 46, showAdsXRay ? new Color(46, 204, 113) : new Color(170, 180, 200));

        // --- ROW 3 (Live Star Goals Bar) ---
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        String g1 = (kittensRescued >= requiredKittens ? "★ " : "☆ ") + "Goal1: Unlock Portal (" + kittensRescued + "/" + requiredKittens + ")";
        String g2Txt = (kittensRescued >= totalKittens ? "★ " : "☆ ") + "Goal2: All Kittens (" + kittensRescued + "/" + totalKittens + ")";
        String g3 = (!tookDamage && gameTimeMs <= 75000 ? "★ " : "☆ ") + "Goal3: No-Hit <75s (" + (gameTimeMs / 1000) + "s)";
        drawOutlinedString(g2, g1, 18, 68, kittensRescued >= requiredKittens ? new Color(46, 204, 113) : new Color(220, 225, 235));
        drawOutlinedString(g2, g2Txt, 240, 68, kittensRescued >= totalKittens ? new Color(241, 196, 15) : new Color(220, 225, 235));
        drawOutlinedString(g2, g3, 455, 68, !tookDamage ? new Color(88, 214, 141) : new Color(231, 76, 60));
    }

    private void drawPauseOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2.setFont(new Font("Arial", Font.BOLD, 38));
        drawCenteredOutlinedString(g2, "GAME PAUSED", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 40, Color.WHITE);

        g2.setFont(new Font("Arial", Font.BOLD, 15));
        drawCenteredOutlinedString(g2, "P: Resume  |  R: Restart  |  V: ADS X-Ray  |  M: Minimap  |  ESC: Menu",
                SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 15, Color.LIGHT_GRAY);
    }

    private void drawEndScreen(Graphics2D g2, String title, String subtitle, Color titleColor, boolean win) {
        g2.setColor(new Color(0, 0, 0, 210));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2.setFont(new Font("Arial", Font.BOLD, 38));
        drawCenteredOutlinedString(g2, title, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 80, titleColor);

        if (win) {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 3; i++) stars.append(i < starsEarned ? "★ " : "☆ ");
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            drawCenteredOutlinedString(g2, stars.toString().trim(), SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 40, new Color(241, 196, 15));
        }

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        drawCenteredOutlinedString(g2, subtitle, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 8, Color.WHITE);

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        drawCenteredOutlinedString(g2, "Campaign Score: " + score + "  (Time: " + (gameTimeMs / 1000) + "s)",
                SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 28, new Color(241, 196, 15));

        if (win) {
            LevelManager.LevelData nextLvl = LevelManager.getLevel(currentLevel + 1);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            drawCenteredOutlinedString(g2, "Next: LVL " + nextLvl.levelNumber + " - " + nextLvl.name + " (+1 Treat & +25 HP!)",
                    SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 60, new Color(88, 214, 141));
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            drawCenteredOutlinedString(g2, "Press N for Next Level  |  R to Replay  |  ESC for Menu",
                    SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 95, Color.LIGHT_GRAY);
        } else {
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            drawCenteredOutlinedString(g2, "Press R to Try Again  |  ESC for Menu",
                    SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 75, Color.LIGHT_GRAY);
        }
    }

    private void drawOutlinedString(Graphics2D g2, String text, int x, int y, Color fillColor) {
        g2.setColor(Color.BLACK);
        g2.drawString(text, x - 1, y - 1);
        g2.drawString(text, x + 1, y - 1);
        g2.drawString(text, x - 1, y + 1);
        g2.drawString(text, x + 1, y + 1);
        g2.drawString(text, x, y - 1);
        g2.drawString(text, x, y + 1);
        g2.drawString(text, x - 1, y);
        g2.drawString(text, x + 1, y);
        g2.setColor(fillColor);
        g2.drawString(text, x, y);
    }

    private void drawCenteredOutlinedString(Graphics2D g2, String text, int cx, int cy, Color fillColor) {
        FontMetrics fm = g2.getFontMetrics();
        drawOutlinedString(g2, text, cx - fm.stringWidth(text) / 2, cy, fillColor);
    }
}
