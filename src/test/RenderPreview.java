package test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import logic.GameSettings;
import maze.MazeGraph;
import maze.MazeType;
import ui.GameWindow;

public class RenderPreview {
    public static void main(String[] args) throws Exception {
        GameSettings settings = new GameSettings();
        settings.theme = "GARDEN";
        settings.scale2x2 = true;
        settings.watchGeneration = false;

        MazeGraph mg = new MazeGraph(17, 23, 42L, 0.25, MazeType.KRUSKAL_MST, true);
        GameWindow gw = new GameWindow(mg, settings, 1, null, null);
        gw.setSize(700, 520);
        gw.doLayout();

        // Simulate a few ticks
        for (int i = 0; i < 5; i++) {
            gw.actionPerformed(null);
        }

        BufferedImage img = new BufferedImage(700, 520, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        gw.paint(g);
        g.dispose();

        File out = new File("C:/Users/Admin/.gemini/antigravity/brain/9235a269-dbab-44b8-a2f5-1b4679b775e8/gameplay_preview.png");
        ImageIO.write(img, "png", out);
        System.out.println("Preview saved to: " + out.getAbsolutePath());
        System.exit(0);
    }
}
