package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import javax.swing.JPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import logic.ChaserAI;

public class GameWindow extends JPanel implements ActionListener {

    private static final int SPRITE_WIDTH = 512;
    private static final int SPRITE_HEIGHT = 512;

    private static final int TILE_SIZE = 40;
    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 480;

    // CHANGED: size now comes from the generated maze (was fixed 20 x 20)
    private final int COLS;
    private final int ROWS;

    // CHANGED: was 42. Maze corridors are exactly one tile (40px) wide,
    // so the player must be smaller than a tile or it can never move.
    private static final int PLAYER_SIZE = 30;

    // CHANGED: map is now the maze from MazeGraph (1 = wall, 0 = path)
    private final int[][] map;
    private final long seed;

    // players co-odinat (Grid based)
    private int playerX = TILE_SIZE + 5;   // CHANGED: centered in tile (1,1) for the 30px player
    private int playerY = TILE_SIZE + 5;
    private int speed = 6;

    private boolean up, down, left, right;
    private String direction = "down";

    //animation variables
    private int spriteCounter = 0;
    private int spriteNum = 0;
    private BufferedImage playerSheet;
    private BufferedImage[] walkDown = new BufferedImage[4];
    private BufferedImage[] walkUp = new BufferedImage[4];
    private BufferedImage[] walkRight = new BufferedImage[4];
    private BufferedImage[] walkLeft = new BufferedImage[4];

    //wall , path, player images
    private Image wallSprite;
    private Image pathSprite;
    //private Image playerSprite;

    // the Game timer runs every this
    private Timer gameLoop;

    private int enemyX = TILE_SIZE * 8;
    private int enemyY = TILE_SIZE * 8;
    // ------------code for chaserAI -------------------------
    private int targetX = enemyX;
    private int targetY = enemyY;

    private final int chaserSpeed = 3;
    private boolean chaserMoving = false;

    private ChaserAI chaserAI = new ChaserAI(5);
    private long lastChaserMove = 0;
    private final int chaserDelay = 300;
    // -------------- end of code for chaserAI --------------------

    // CHANGED: constructor receives the maze and its seed
    public GameWindow(int[][] maze, long seed) {
        this.map = maze;
        this.ROWS = maze.length;
        this.COLS = maze[0].length;
        this.seed = seed;

        // CHANGED: the old start tile (8,8) can be a wall in a generated maze.
        // Open cells always sit on odd rows/columns, so (odd, odd) is always open.
        enemyX = ((COLS / 2) | 1) * TILE_SIZE;
        enemyY = ((ROWS / 2) | 1) * TILE_SIZE;
        targetX = enemyX;
        targetY = enemyY;

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.setFocusable(true); // capture keyboard inputs

        // generateDummyMatrix() removed - the maze replaces it
        setupKeyBindings();

        loadImages();

        //starting game loop the timer around 60 FPS (16ms)
        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    // ADDED: for pause / Continue / multiplayer
    public long getSeed() {
        return seed;
    }

    private void loadImages(){
        try{
            wallSprite = ImageIO.read(new File("assets/wall.png"));
            pathSprite = ImageIO.read(new File("assets/path.png"));
            playerSheet = ImageIO.read(new File("assets/player_sheet.png"));

            //
            for (int i = 0; i < 4; i++){
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

    private void setupKeyBindings(){
        InputMap im = this.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,0, false),"W_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S,0, false),"S_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A,0, false),"A_Pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D,0, false),"D_Pressed");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,0, true),"W_Released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S,0, true),"S_Released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A,0, true),"A_Released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D,0, true),"D_Released");

        am.put("W_Pressed", new KeyAction("W",true));
        am.put("S_Pressed", new KeyAction("S",true));
        am.put("A_Pressed", new KeyAction("A",true));
        am.put("D_Pressed", new KeyAction("D",true));

        am.put("W_Released", new KeyAction("W",false));
        am.put("S_Released", new KeyAction("S",false));
        am.put("A_Released", new KeyAction("A",false));
        am.put("D_Released", new KeyAction("D",false));


    }

    private class KeyAction extends AbstractAction{
        String key;
        boolean state;

        KeyAction(String key, boolean state){
            this.key = key;
            this.state = state;
        }
        @Override
        public void actionPerformed(ActionEvent e){
            if (key.equals("W"))
            {
                up = state; if(state) direction = "up";
            }
            if (key.equals("S")){
                down = state;if(state) direction = "down";
            }
            if (key.equals("A")){
                left = state;if(state) direction = "left";
            }
            if (key.equals("D")){
                right = state;if(state) direction = "right";
            }
        }
    }

    private boolean canMove(int nextX, int nextY){
        // ADDED: the maze has openings on the left and right edges (entrance / exit),
        // so keep the player inside the grid to avoid ArrayIndexOutOfBoundsException.
        if (nextX < 0 || nextY < 0
                || nextX + PLAYER_SIZE > COLS * TILE_SIZE
                || nextY + PLAYER_SIZE > ROWS * TILE_SIZE) {
            return false;
        }

        int leftCol = nextX / TILE_SIZE;
        int rightCol = (nextX + PLAYER_SIZE - 1) / TILE_SIZE;
        int topRow = nextY / TILE_SIZE;
        int bottomRow = (nextY + PLAYER_SIZE - 1) / TILE_SIZE;

        if(map[topRow][leftCol] == 1) return false;
        if(map[topRow][rightCol] == 1) return false;
        if(map[bottomRow][leftCol] == 1) return false;
        if(map[bottomRow][rightCol] == 1) return false;

        return true;

    }
    private void updateChaser() {

        // Move smoothly toward the current target tile
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

            // Stop when the target tile is reached
            if (enemyX == targetX && enemyY == targetY) {
                chaserMoving = false;
            }

            return;
        }

        // Choose the next tile only when not moving
        long now = System.currentTimeMillis();

        if (now - lastChaserMove < chaserDelay) {
            return;
        }

        int chaserGridX = enemyX / TILE_SIZE;
        int chaserGridY = enemyY / TILE_SIZE;

        int playerGridX =
                (playerX + PLAYER_SIZE / 2) / TILE_SIZE;

        int playerGridY =
                (playerY + PLAYER_SIZE / 2) / TILE_SIZE;

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
        int dx = 0, dy = 0;
        boolean moving = false;

        if (up) { dy -= speed;moving = true; }
        if (down) { dy += speed;moving = true; }
        if (left) { dx -= speed;moving = true; }
        if (right) { dx += speed; moving = true; }

        //check x and y axis independently to allow sliding on walls
        if(canMove(playerX + dx, playerY)) {
            playerX += dx;
        }
        if (canMove(playerX, playerY + dy)) {
            playerY += dy;
        }

        // animation tick math
        if (moving) {
            spriteCounter++;
            if (spriteCounter > 10) {
                spriteNum++;
                if (spriteNum >= 4) spriteNum = 0;
                spriteCounter = 0;
            }
        }else {
            spriteNum = 0; // restes to standing still if no keys are pressed
        }

        updateChaser();

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int camX = playerX + (PLAYER_SIZE / 2)-(SCREEN_WIDTH / 2);
        int camY = playerY + (PLAYER_SIZE / 2)-(SCREEN_HEIGHT / 2);
        g2.translate(-camX, -camY);

        //Rendering logic goes here later if someone make one

        //dummy code
        for(int r = 0; r < ROWS; r++){
            for (int c = 0; c < COLS; c++){
                if (map[r][c] == 1) {
                    g2.drawImage(wallSprite, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                }else{
                    g2.drawImage(pathSprite,c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);

                }
            }
        }
        // Draw Enemy (Placeholder)
        g2.setColor(Color.RED);
        g2.fillRect(enemyX, enemyY, PLAYER_SIZE, PLAYER_SIZE);
        // Determine which sprite frame to draw
        BufferedImage currentFrame = null;
        switch(direction) {
            case "up": currentFrame = walkUp[spriteNum]; break;
            case "down": currentFrame = walkDown[spriteNum]; break;
            case "left": currentFrame = walkLeft[spriteNum]; break;
            case "right": currentFrame = walkRight[spriteNum]; break;
        }

        if (currentFrame != null) {
            // Draws the sliced cat slightly offset so it centers nicely on the hitbox
            g2.drawImage(currentFrame, playerX, playerY, PLAYER_SIZE, PLAYER_SIZE, null);

        }
        g2.dispose();
    }
}