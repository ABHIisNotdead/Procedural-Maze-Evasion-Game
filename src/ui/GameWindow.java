package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class GameWindow extends JPanel {

    private final int TILE_SIZE = 32;
    private final int COLS = 20;
    private final int ROWS = 15;

    public GameWindow() {
        this.setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics g2 = (Graphics2D) g;
        //Rendering lofic goes here later if someone make one
        g2.dispose();
    }
}
