package test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import logic.*;
import maze.MazeGraph;
import maze.MazeType;
import ui.GameWindow;
import ui.MainFrame;

/**
 * Generates clean documentation screenshots in docs/screenshots/ for README.md.
 */
public class RenderDocsScreenshots {

    public static void main(String[] args) throws Exception {
        File dir = new File("docs/screenshots");
        if (!dir.exists()) dir.mkdirs();

        // 1. Main Menu
        MainFrame mf = new MainFrame();
        mf.setSize(700, 520);
        mf.doLayout();
        saveComponent(mf, "docs/screenshots/menu.png");

        // 2. Gameplay (Multi-color dogs, cat-style kittens, bushes, cracked wall, HUD, minimap)
        GameSettings settings = new GameSettings();
        settings.theme = "GARDEN";
        settings.scale2x2 = true;
        settings.watchGeneration = false;

        MazeGraph mg = new MazeGraph(17, 23, 42L, 0.25, MazeType.KRUSKAL_MST, true, 4);
        GameWindow gw = new GameWindow(mg, settings, 1, null, null);
        gw.stopGame();
        gw.setSize(700, 520);
        gw.doLayout();

        positionSwarmForShowcase(gw);
        saveComponent(gw, "docs/screenshots/gameplay.png");

        // 3. ADS X-Ray Overlay ([V])
        java.lang.reflect.Field xrayField = GameWindow.class.getDeclaredField("showAdsXRay");
        xrayField.setAccessible(true);
        xrayField.set(gw, true);

        java.lang.reflect.Field aiField = GameWindow.class.getDeclaredField("chaserAI");
        aiField.setAccessible(true);
        ChaserAI ai = (ChaserAI) aiField.get(gw);

        java.lang.reflect.Field chasersField = GameWindow.class.getDeclaredField("chasers");
        chasersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ChaserEntity> chasers = (List<ChaserEntity>) chasersField.get(gw);

        java.lang.reflect.Field pXField = GameWindow.class.getDeclaredField("playerX");
        java.lang.reflect.Field pYField = GameWindow.class.getDeclaredField("playerY");
        pXField.setAccessible(true);
        pYField.setAccessible(true);
        int pgx = ((int) pXField.get(gw) + 12) / 40;
        int pgy = ((int) pYField.get(gw) + 12) / 40;

        int[][] grid = mg.getMaze();
        int[][] terrain = mg.getTerrain();
        ai.predictPlayerLocation(grid, pgx, pgy, 1, 0);
        if (!chasers.isEmpty()) {
            ChaserEntity d1 = chasers.get(0);
            d1.gridX = d1.pixelX / 40;
            d1.gridY = d1.pixelY / 40;
            ai.findPathBFS(grid, terrain, d1.gridX, d1.gridY, pgx, pgy, null, d1);
        }
        saveComponent(gw, "docs/screenshots/ads_xray.png");

        // 4. Live Maze Generation Visualizer
        GameSettings genSettings = new GameSettings();
        genSettings.watchGeneration = true;
        genSettings.generationStepsPerSec = 120;
        MazeGraph mgGen = new MazeGraph(17, 23, 77L, 0.25, MazeType.DFS_BACKTRACKER, true, 4);
        GameWindow gwGen = new GameWindow(mgGen, genSettings, 2, null, null);
        gwGen.stopGame();
        gwGen.setSize(700, 520);
        gwGen.doLayout();
        for (int i = 0; i < 65; i++) {
            gwGen.actionPerformed(null);
        }
        saveComponent(gwGen, "docs/screenshots/maze_gen.png");

        System.out.println("All 4 documentation screenshots saved to docs/screenshots/");
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    private static void positionSwarmForShowcase(GameWindow gw) throws Exception {
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
    }

    private static void saveComponent(Component comp, String path) throws Exception {
        BufferedImage img = new BufferedImage(700, 520, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        comp.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }
}
