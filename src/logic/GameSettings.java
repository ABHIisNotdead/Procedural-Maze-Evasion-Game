package logic;

import maze.MazeGraph;

public class GameSettings {
    public int rows = MazeGraph.DEFAULT_ROWS;          // 15 (odd)
    public int cols = MazeGraph.DEFAULT_COLS;          // 21 (odd)
    public double braidChance = MazeGraph.DEFAULT_BRAID_CHANCE; // 0.15
    public int playerSpeed = 6;
}