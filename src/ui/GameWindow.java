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

public class GameWindow extends JPanel implements ActionListener {

    private final int TILE_SIZE = 32;
    private final int COLS = 20;
    private final int ROWS = 15;

    //here 1 = wall , 0 = path (this is my Dummy matrix untill member 1 delivers the map generation script)
    private int[][] map = new int[ROWS][COLS];

    // players co-odinat (Grid based)
    private int playerCol = 1;
    private int playerRow = 1;

    // the Game timer runs every this
    private Timer gameLoop;

    public GameWindow() {
        this.setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        this.setFocusable(true); // capture keyboard inputs

        generateDummyMatrix();
        setupKeyBindings();

        //starting game loop the timer around 60 FPS (16ms)
        gameLoop = new Timer(16, this);
        gameLoop.start();
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

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,0),"moveUP");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S,0),"moveDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A,0),"moveLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D,0),"moveRight");

        am.put("moveUP", new MoveAction(0,-1));
        am.put("moveDown", new MoveAction(0,1));
        am.put("moveLeft", new MoveAction(-1,0));
        am.put("moveRight", new MoveAction(1,0));

    }

    private class MoveAction extends AbstractAction {
        int dCol, dRow;
        MoveAction(int dCol, int dRow){
            this.dCol = dCol;
            this.dRow = dRow;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int nextCol = playerCol + dCol;
            int nextRow = playerRow + dRow;

            if (map[nextRow][nextCol] == 0){
                playerCol = nextCol;
                playerRow = nextRow;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
                if (map[r][c] == 1){
                    g2.setColor(Color.DARK_GRAY);
                    g2.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                }
            }
        }
        g2.setColor(Color.GREEN);
        g2.fillOval(playerCol * TILE_SIZE, playerRow * TILE_SIZE, TILE_SIZE, TILE_SIZE);







        g2.dispose();
    }
}
