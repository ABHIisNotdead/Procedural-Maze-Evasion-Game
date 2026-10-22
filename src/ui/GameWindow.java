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

public class GameWindow extends JPanel implements ActionListener {

    private final int TILE_SIZE = 32;
    private final int COLS = 20;
    private final int ROWS = 15;

    private static final int PLAYER_SIZE = 24;

    //here 1 = wall , 0 = path (this is my Dummy matrix untill member 1 delivers the map generation script)
    private int[][] map = new int[ROWS][COLS];

    // players co-odinat (Grid based)
    private int playerX = TILE_SIZE + 4;
    private int playerY = TILE_SIZE + 4;
    private int speed = 6;

    private boolean up, down, left, right;

    //wall , path, player images
    private Image wallSprite;
    private Image pathSprite;
    private Image playerSprite;

    // the Game timer runs every this
    private Timer gameLoop;

    public GameWindow() {
        this.setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.setFocusable(true); // capture keyboard inputs

        generateDummyMatrix();
        setupKeyBindings();

        loadImages();

        //starting game loop the timer around 60 FPS (16ms)
        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    private void loadImages(){
        try{
            wallSprite = ImageIO.read(new File("assets/wall.png"));
            pathSprite = ImageIO.read(new File("assets/path.png"));
            playerSprite = ImageIO.read(new File("assets/player.png"));
        } catch (IOException e) {
            System.out.println("ERROR loading images");
            e.printStackTrace();
        }
    }

    private void generateDummyMatrix(){
        for(int r = 0; r < ROWS; r++){
            for(int c = 0; c < COLS; c++){
                if (r == 0 || r == ROWS -1 || c == 0 || c == COLS -1) {
                    map[r][c] = 1;
                }else{
                    map[r][c] = 0;
                }
            }
        }
        map[5][5] = 1;
        map[5][6] = 1;
        map[6][5] = 1;
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
            if (key.equals("W")) up = state;
            if (key.equals("S")) down = state;
            if (key.equals("A")) left = state;
            if (key.equals("D")) right = state;
        }
    }

    private boolean canMove(int nextX, int nextY){
        int leftCol = nextX / TILE_SIZE;
        int rightCol = (nextX + PLAYER_SIZE - 1 ) / TILE_SIZE;
        int topRow = nextY / TILE_SIZE;
        int bottomRow = (nextY + PLAYER_SIZE - 1)/ TILE_SIZE;

        if(map[topRow][leftCol] == 1) return false;
        if(map[topRow][rightCol] == 1) return false;
        if(map[bottomRow][leftCol] == 1) return false;
        if(map[bottomRow][rightCol] == 1) return false;

        return true;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int dx = 0, dy = 0;

        if (up) dy -= speed;
        if (down) dy += speed;
        if (left) dx -= speed;
        if (right) dx += speed;

        //check x and y axis independently to allow sliding on walls
        if(canMove(playerX + dx, playerY)) {
            playerX += dx;
        }
        if (canMove(playerX, playerY + dy)) {
            playerY += dy;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics g2 = (Graphics2D) g;
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

        g2.drawImage(playerSprite,playerX, playerY, PLAYER_SIZE,PLAYER_SIZE, null);







        g2.dispose();
    }
}
