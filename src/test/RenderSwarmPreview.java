package test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import logic.ChaserEntity;
import logic.ChaserState;
import logic.ChaserType;
import logic.GameSettings;
import maze.MazeGraph;
import maze.MazeType;
import ui.GameWindow;

public class RenderSwarmPreview {
    public static void main(String[] args) throws Exception {
        GameSettings settings = new GameSettings();
        settings.theme = "GARDEN";
        settings.scale2x2 = true;
        settings.watchGeneration = false;

        MazeGraph mg = new MazeGraph(17, 23, 42L, 0.25, MazeType.KRUSKAL_MST, true);
        GameWindow gw = new GameWindow(mg, settings, 1, null, null);
        gw.stopGame(); // Stop timer so it doesn't overwrite coordinates during snapshot
        gw.setSize(700, 520);
        gw.doLayout();

        // Access entities to position them in view for the preview snapshot
        java.lang.reflect.Field chasersField = GameWindow.class.getDeclaredField("chasers");
        chasersField.setAccessible(true);
        List<ChaserEntity> chasers = (List<ChaserEntity>) chasersField.get(gw);

        java.lang.reflect.Field pX = GameWindow.class.getDeclaredField("playerX");
        java.lang.reflect.Field pY = GameWindow.class.getDeclaredField("playerY");
        pX.setAccessible(true);
        pY.setAccessible(true);
        int px = (int) pX.get(gw);
        int py = (int) pY.get(gw);

        if (chasers.size() >= 2) {
            ChaserEntity d1 = chasers.get(0);
            d1.pixelX = px + 80;
            d1.pixelY = py;
            d1.state = ChaserState.HUNTING;
            d1.speechBubble = "!";
            d1.speechColor = Color.RED;

            ChaserEntity d2 = chasers.get(1);
            d2.pixelX = px - 80;
            d2.pixelY = py;
            d2.state = ChaserState.RETRIEVING;
            d2.carryingKitten = true;
            d2.speechBubble = "KITTEN!";
            d2.speechColor = new Color(230, 126, 34);
        }

        BufferedImage img = new BufferedImage(700, 520, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        gw.paint(g);
        g.dispose();

        File out = new File("C:/Users/Admin/.gemini/antigravity/brain/9235a269-dbab-44b8-a2f5-1b4679b775e8/swarm_preview.png");
        ImageIO.write(img, "png", out);
        System.out.println("Swarm preview saved to: " + out.getAbsolutePath());
        System.exit(0);
    }
}
