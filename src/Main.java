import maze.MazeGraph;
import ui.GameWindow;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    // Map size in tiles (odd numbers). Change these when the team decides the final size.
    private static final int MAP_ROWS = MazeGraph.DEFAULT_ROWS;   // 15
    private static final int MAP_COLS = MazeGraph.DEFAULT_COLS;   // 21

    public static void main(String[] args) {
        // No argument  -> NEW game, new random map every run.
        // With argument -> same map again, e.g.:  java Main 123456789
        long seed = (args.length > 0) ? Long.parseLong(args[0]) : MazeGraph.newSeed();
        System.out.println("Map seed: " + seed + "  (run 'java Main " + seed + "' to replay this map)");

        SwingUtilities.invokeLater(() -> {
            MazeGraph mazeGraph = new MazeGraph(MAP_ROWS, MAP_COLS, seed);
            int[][] maze = mazeGraph.getMaze();   // 1 = wall, 0 = path

            JFrame frame = new JFrame("Procedural Maze Evasion");
            GameWindow gameWindow = new GameWindow(maze, mazeGraph.getSeed());

            frame.add(gameWindow);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            gameWindow.requestFocusInWindow();
        });
    }
}