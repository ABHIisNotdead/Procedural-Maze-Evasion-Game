package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import logic.ChaserAI;
import logic.RunHistory;

public class GameWindow extends JPanel implements ActionListener {

    private static final int SPRITE_WIDTH = 512;
    private static final int SPRITE_HEIGHT = 512;

    private static final int TILE_SIZE = 40;
    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 480;

    private final int COLS;
    private final int ROWS;

    private static final int PLAYER_SIZE = 30;
    private static final int COIN_SIZE = 14;

    private final int[][] map;
    private final long seed;
    private final Runnable onExitToMenu;

    // Player coordinates
    private int playerX = TILE_SIZE + 5;
    private int playerY = TILE_SIZE + 5;
    private int speed = 6;

    // Health, Score, and Game State
    private final int maxHealth = 100;
    private int health = maxHealth;
    private int score = 0;
    private boolean gameOver = false;
    private boolean gameWon = false;
    private long invincibleUntil = 0;
    private static final int INVINCIBILITY_MS = 1500;
    private static final int ENEMY_DAMAGE = 25;

    // Coins grid (true = coin present at [row][col])
    private boolean[][] coins;

    // Smooth camera coordinates
    private double camX;
    private double camY;

    private boolean up, down, left, right;
    private String direction = "down";

    // Animation variables
    private int spriteCounter = 0;
    private int spriteNum = 0;
    private BufferedImage playerSheet;
    private BufferedImage[] walkDown = new BufferedImage[4];
    private BufferedImage[] walkUp = new BufferedImage[4];
    private BufferedImage[] walkRight = new BufferedImage[4];
    private BufferedImage[] walkLeft = new BufferedImage[4];

    // Wall and path images
    private Image wallSprite;
    private Image pathSprite;

    // Game timer (~60 FPS)
    private Timer gameLoop;

    // ------------ code for ChaserAI -------------------------
    private int enemyX;
    private int enemyY;
    private int targetX;
    private int targetY;

    private final int chaserSpeed = 3;
    private boolean chaserMoving = false;

    private ChaserAI chaserAI = new ChaserAI(5);
    private long lastChaserMove = 0;
    private final int chaserDelay = 300;
    // -------------- end of code for ChaserAI ----------------

    // Keeps compatibility if anything instantiates GameWindow directly
    public GameWindow(int[][] maze, long seed) {
        this(maze, seed, 6, null);
    }

    public GameWindow(int[][] maze, long seed, int playerSpeed, Runnable onExitToMenu) {
        this.map = maze;
        this.ROWS = maze.length;
        this.COLS = maze[0].length;
        this.seed = seed;
        this.speed = playerSpeed;
        this.onExitToMenu = onExitToMenu;
        this.coins = new boolean[ROWS][COLS];

        resetPositionsAndState();

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        setupKeyBindings();
        loadImages();

        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    public void stopGame() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    private void resetPositionsAndState() {
        playerX = TILE_SIZE + 5;
        playerY = TILE_SIZE + 5;

        // Open cells always sit on odd rows/columns
        enemyX = ((COLS / 2) | 1) * TILE_SIZE;
        enemyY = ((ROWS / 2) | 1) * TILE_SIZE;
        targetX = enemyX;
        targetY = enemyY;
        chaserMoving = false;

        camX = playerX + (PLAYER_SIZE / 2.0) - (SCREEN_WIDTH / 2.0);
        camY = playerY + (PLAYER_SIZE / 2.0) - (SCREEN_HEIGHT / 2.0);

        health = maxHealth;
        score = 0;
        gameOver = false;
        gameWon = false;
        invincibleUntil = 0;
        up = down = left = right = false;
        direction = "down";

        generateCoins();
    }

    private void generateCoins() {
        Random rand = new Random(seed);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                // Place coins on ~20% of open path tiles, skipping spawn (1,1) and exit column
                if (map[r][c] == 0 && !(r == 1 && c == 1) && c < COLS - 1 && rand.nextDouble() < 0.20) {
                    coins[r][c] = true;
                } else {
                    coins[r][c] = false;
                }
            }
        }
    }

    public long getSeed() {
        return seed;
    }

    private void loadImages() {
        try {
            wallSprite = ImageIO.read(new File("assets/wall.png"));
            pathSprite = ImageIO.read(new File("assets/path.png"));
            playerSheet = ImageIO.read(new File("assets/player_sheet.png"));

            for (int i = 0; i < 4; i++) {
                int startX = i * SPRITE_WIDTH;
                walkUp[i] = playerSheet.getSubimage(startX, 0, SPRITE_WIDTH, SPRITE_HEIGHT);
                walkDown[i] = playerSheet.getSubimage(startX, 1 * SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);
                walkLeft[i] = playerSheet.getSubimage(startX, 2 * SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);
                walkRight[i] = playerSheet.getSubimage(startX, 3 * SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT);
            }
        } catch (IOException e) {
            System.out.println("ERROR loading images");
            e.printStackTrace();
        }
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "W_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "S_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "A_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "D_Pressed");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "W_Released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "S_Released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "A_Released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "D_Released");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "R_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "ESC_Pressed");

        am.put("W_Pressed", new KeyAction("W", true));
        am.put("S_Pressed", new KeyAction("S", true));
        am.put("A_Pressed", new KeyAction("A", true));
        am.put("D_Pressed", new KeyAction("D", true));

        am.put("W_Released", new KeyAction("W", false));
        am.put("S_Released", new KeyAction("S", false));
        am.put("A_Released", new KeyAction("A", false));
        am.put("D_Released", new KeyAction("D", false));

        am.put("R_Pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver || gameWon) {
                    resetPositionsAndState();
                }
            }
        });

        am.put("ESC_Pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onExitToMenu != null) {
                    onExitToMenu.run();
                }
            }
        });
    }

    private class KeyAction extends AbstractAction {
        String key;
        boolean state;

        KeyAction(String key, boolean state) {
            this.key = key;
            this.state = state;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver || gameWon) return;

            if (key.equals("W")) {
                up = state;
                if (state) direction = "up";
            }
            if (key.equals("S")) {
                down = state;
                if (state) direction = "down";
            }
            if (key.equals("A")) {
                left = state;
                if (state) direction = "left";
            }
            if (key.equals("D")) {
                right = state;
                if (state) direction = "right";
            }
        }
    }

    private boolean canMove(int nextX, int nextY) {
        if (nextX < 0 || nextY < 0
                || nextX + PLAYER_SIZE > COLS * TILE_SIZE
                || nextY + PLAYER_SIZE > ROWS * TILE_SIZE) {
            return false;
        }

        int leftCol = nextX / TILE_SIZE;
        int rightCol = (nextX + PLAYER_SIZE - 1) / TILE_SIZE;
        int topRow = nextY / TILE_SIZE;
        int bottomRow = (nextY + PLAYER_SIZE - 1) / TILE_SIZE;

        if (map[topRow][leftCol] == 1) return false;
        if (map[topRow][rightCol] == 1) return false;
        if (map[bottomRow][leftCol] == 1) return false;
        if (map[bottomRow][rightCol] == 1) return false;

        return true;
    }

    private boolean isWall(int px, int py) {
        if (px < 0 || py < 0 || px >= COLS * TILE_SIZE || py >= ROWS * TILE_SIZE) {
            return true;
        }
        return map[py / TILE_SIZE][px / TILE_SIZE] == 1;
    }

    private void movePlayer(int dx, int dy) {
        int stepX = Integer.signum(dx);
        for (int i = 0; i < Math.abs(dx); i++) {
            if (canMove(playerX + stepX, playerY)) {
                playerX += stepX;
            } else if (dy == 0) {
                int checkX = (stepX > 0) ? (playerX + PLAYER_SIZE) : (playerX - 1);
                boolean topBlocked = isWall(checkX, playerY);
                boolean bottomBlocked = isWall(checkX, playerY + PLAYER_SIZE - 1);

                if (topBlocked && !bottomBlocked && canMove(playerX, playerY + 1)) {
                    playerY++;
                } else if (!topBlocked && bottomBlocked && canMove(playerX, playerY - 1)) {
                    playerY--;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        int stepY = Integer.signum(dy);
        for (int i = 0; i < Math.abs(dy); i++) {
            if (canMove(playerX, playerY + stepY)) {
                playerY += stepY;
            } else if (dx == 0) {
                int checkY = (stepY > 0) ? (playerY + PLAYER_SIZE) : (playerY - 1);
                boolean leftBlocked = isWall(playerX, checkY);
                boolean rightBlocked = isWall(playerX + PLAYER_SIZE - 1, checkY);

                if (leftBlocked && !rightBlocked && canMove(playerX + 1, playerY)) {
                    playerX++;
                } else if (!leftBlocked && rightBlocked && canMove(playerX - 1, playerY)) {
                    playerX--;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private void checkCoinCollection() {
        int centerCol = (playerX + PLAYER_SIZE / 2) / TILE_SIZE;
        int centerRow = (playerY + PLAYER_SIZE / 2) / TILE_SIZE;

        if (centerRow >= 0 && centerRow < ROWS && centerCol >= 0 && centerCol < COLS) {
            if (coins[centerRow][centerCol]) {
                coins[centerRow][centerCol] = false;
                score += 10;
            }
        }
    }

    private void checkWinCondition() {
        int centerCol = (playerX + PLAYER_SIZE / 2) / TILE_SIZE;
        if (centerCol == COLS - 1) {
            gameWon = true;
            RunHistory.saveRun(seed, score, "ESCAPED", ROWS, COLS);
        }
    }

    private void checkEnemyCollision() {
        long now = System.currentTimeMillis();
        if (now < invincibleUntil) {
            return;
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
        Rectangle enemyRect = new Rectangle(enemyX, enemyY, PLAYER_SIZE, PLAYER_SIZE);

        if (playerRect.intersects(enemyRect)) {
            health = Math.max(0, health - ENEMY_DAMAGE);
            invincibleUntil = now + INVINCIBILITY_MS;

            if (health == 0) {
                gameOver = true;
                RunHistory.saveRun(seed, score, "GAME OVER", ROWS, COLS);
            }
        }
    }

    private void updateChaser() {
        if (chaserMoving) {
            if (enemyX < targetX) {
                enemyX = Math.min(enemyX + chaserSpeed, targetX);
            } else if (enemyX > targetX) {
                enemyX = Math.max(enemyX - chaserSpeed, targetX);
            }

            if (enemyY < targetY) {
                enemyY = Math.min(enemyY + chaserSpeed, targetY);
            } else if (enemyY > targetY) {
                enemyY = Math.max(enemyY - chaserSpeed, targetY);
            }

            if (enemyX == targetX && enemyY == targetY) {
                chaserMoving = false;
            }

            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastChaserMove < chaserDelay) {
            return;
        }

        int chaserGridX = enemyX / TILE_SIZE;
        int chaserGridY = enemyY / TILE_SIZE;
        int playerGridX = (playerX + PLAYER_SIZE / 2) / TILE_SIZE;
        int playerGridY = (playerY + PLAYER_SIZE / 2) / TILE_SIZE;

        int[] next = chaserAI.nextMove(
                map,
                chaserGridX,
                chaserGridY,
                playerGridX,
                playerGridY
        );

        targetX = next[0] * TILE_SIZE;
        targetY = next[1] * TILE_SIZE;

        chaserMoving = (targetX != enemyX || targetY != enemyY);
        lastChaserMove = now;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !gameWon) {
            int dx = 0, dy = 0;
            boolean moving = false;

            if (up) { dy -= speed; moving = true; }
            if (down) { dy += speed; moving = true; }
            if (left) { dx -= speed; moving = true; }
            if (right) { dx += speed; moving = true; }

            movePlayer(dx, dy);
            checkCoinCollection();
            checkWinCondition();

            if (moving) {
                spriteCounter++;
                if (spriteCounter > 10) {
                    spriteNum++;
                    if (spriteNum >= 4) spriteNum = 0;
                    spriteCounter = 0;
                }
            } else {
                spriteNum = 0;
            }

            if (!gameWon) {
                updateChaser();
                checkEnemyCollision();
            }

            // Smooth camera follow
            double targetCamX = playerX + (PLAYER_SIZE / 2.0) - (SCREEN_WIDTH / 2.0);
            double targetCamY = playerY + (PLAYER_SIZE / 2.0) - (SCREEN_HEIGHT / 2.0);
            camX += (targetCamX - camX) * 0.15;
            camY += (targetCamY - camY) * 0.15;
        }

        repaint();
    }

    private void drawHUD(Graphics2D g2) {
        // Top HUD bar background
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, SCREEN_WIDTH, 38);

        // Health bar
        int barX = 15;
        int barY = 10;
        int barW = 140;
        int barH = 18;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(barX, barY, barW, barH);

        int fillW = (int) ((health / (double) maxHealth) * barW);
        g2.setColor(health > 30 ? new Color(46, 204, 113) : new Color(231, 76, 60));
        g2.fillRect(barX, barY, fillW, barH);

        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barW, barH);

        // HP text
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("HP: " + health + "/" + maxHealth, barX + barW + 10, 24);

        // Score text (right-aligned)
        String scoreText = "Score: " + score;
        FontMetrics fm = g2.getFontMetrics();
        int scoreX = SCREEN_WIDTH - fm.stringWidth(scoreText) - 18;
        g2.setColor(new Color(241, 196, 15));
        g2.drawString(scoreText, scoreX, 24);
    }

    private void drawEndScreen(Graphics2D g2, String title, Color titleColor) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Title
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(titleColor);
        FontMetrics fmTitle = g2.getFontMetrics();
        g2.drawString(title, (SCREEN_WIDTH - fmTitle.stringWidth(title)) / 2, SCREEN_HEIGHT / 2 - 20);

        // Final score
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        String scoreMsg = "Final Score: " + score;
        FontMetrics fmScore = g2.getFontMetrics();
        g2.drawString(scoreMsg, (SCREEN_WIDTH - fmScore.stringWidth(scoreMsg)) / 2, SCREEN_HEIGHT / 2 + 25);

        // Restart & Menu prompt
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);
        String restartMsg = "Press R to Play Again  |  ESC for Menu";
        FontMetrics fmRestart = g2.getFontMetrics();
        g2.drawString(restartMsg, (SCREEN_WIDTH - fmRestart.stringWidth(restartMsg)) / 2, SCREEN_HEIGHT / 2 + 65);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int camOffsetX = (int) Math.round(camX);
        int camOffsetY = (int) Math.round(camY);

        // --- World Space ---
        g2.translate(-camOffsetX, -camOffsetY);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (map[r][c] == 1) {
                    g2.drawImage(wallSprite, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                } else {
                    g2.drawImage(pathSprite, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);

                    // Highlight the maze exit tile on the right edge
                    if (c == COLS - 1) {
                        g2.setColor(new Color(46, 204, 113, 140));
                        g2.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }

                    if (coins[r][c]) {
                        int coinX = c * TILE_SIZE + (TILE_SIZE - COIN_SIZE) / 2;
                        int coinY = r * TILE_SIZE + (TILE_SIZE - COIN_SIZE) / 2;
                        g2.setColor(new Color(255, 215, 0));
                        g2.fillOval(coinX, coinY, COIN_SIZE, COIN_SIZE);
                        g2.setColor(new Color(184, 134, 11));
                        g2.drawOval(coinX, coinY, COIN_SIZE, COIN_SIZE);
                    }
                }
            }
        }

        // Draw Enemy (Placeholder)
        g2.setColor(Color.RED);
        g2.fillRect(enemyX, enemyY, PLAYER_SIZE, PLAYER_SIZE);

        // Draw Player (blinks while invincible after taking damage)
        boolean isInvincible = System.currentTimeMillis() < invincibleUntil;
        boolean showPlayer = !isInvincible || (System.currentTimeMillis() / 100) % 2 == 0;

        if (showPlayer) {
            BufferedImage currentFrame = null;
            switch (direction) {
                case "up": currentFrame = walkUp[spriteNum]; break;
                case "down": currentFrame = walkDown[spriteNum]; break;
                case "left": currentFrame = walkLeft[spriteNum]; break;
                case "right": currentFrame = walkRight[spriteNum]; break;
            }

            if (currentFrame != null) {
                g2.drawImage(currentFrame, playerX, playerY, PLAYER_SIZE, PLAYER_SIZE, null);
            }
        }

        // --- Screen Space (HUD & Overlays) ---
        g2.translate(camOffsetX, camOffsetY);

        drawHUD(g2);

        if (gameOver) {
            drawEndScreen(g2, "GAME OVER", new Color(231, 76, 60));
        } else if (gameWon) {
            drawEndScreen(g2, "YOU ESCAPED!", new Color(46, 204, 113));
        }

        g2.dispose();
    }
}