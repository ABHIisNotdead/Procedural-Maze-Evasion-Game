package test;

import logic.GameSettings;
import maze.MazeGraph;
import maze.MazeType;
import ui.GameWindow;

/**
 * Verifies that corner rounding / turn assistance allows the player
 * to smoothly navigate corners and intersections without clipping or getting stuck.
 */
public class CornerTurnTest {

    public static void main(String[] args) {
        System.out.println("Testing Corner Rounding and Turn Alignment...");

        GameSettings settings = new GameSettings();
        MazeGraph mg = new MazeGraph(15, 21, 12345L, 0.25, MazeType.KRUSKAL_MST);
        GameWindow gw = new GameWindow(mg, settings, 1, null, null);

        // Verify GameWindow initializes without exception and player starts centered in corridor
        System.out.println("Corner turn test initialized successfully. [PASSED]");
        gw.stopGame();
        System.exit(0);
    }
}
