package maze;
import java.util.*;

/**
 * Seeded maze generator.
 *
 * Same seed + same rows/cols (+ same braidChance) always produces the same maze,
 * which is what Continue / Play Again and future multiplayer rely on.
 *
 * Step 1: Kruskal's algorithm + Union-Find builds a perfect maze (one route between any two cells).
 * Step 2: "Braiding" removes some extra walls so the maze has loops (multiple paths),
 *         which is what a chase game needs.
 */
public class MazeGraph {

    // Odd sizes fit the cell/wall layout exactly. 21x15 tiles at 30px = 630x450.
    public static final int DEFAULT_ROWS = 15;
    public static final int DEFAULT_COLS = 21;
    public static final double DEFAULT_BRAID_CHANCE = 0.15;
    public static final int WALL = 1;
    public static final int PATH = 0;

    private final int rows;
    private final int cols;
    private final long seed;
    private final double braidChance;
    private final int[][] grid;

    private final int cellRows;
    private final int cellCols;
    private final int totalCells;
    private final int[][] adjacencyMatrix;

    private static class Edge {
        int u, v, wallRow, wallCol;
        Edge(int u, int v, int wallRow, int wallCol) {
            this.u = u;
            this.v = v;
            this.wallRow = wallRow;
            this.wallCol = wallCol;
        }
    }

    public static class DisjointSet {
        private final int[] parent;
        private final int[] rank;

        public DisjointSet(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }

        public int find(int i) {
            if (parent[i] != i) parent[i] = find(parent[i]);
            return parent[i];
        }

        public boolean union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI == rootJ) return false;

            if (rank[rootI] < rank[rootJ]) parent[rootI] = rootJ;
            else if (rank[rootI] > rank[rootJ]) parent[rootJ] = rootI;
            else {
                parent[rootJ] = rootI;
                rank[rootI]++;
            }
            return true;
        }
    }

    /** Generates a fresh random seed. Call this for a NEW game and save the result. */
    public static long newSeed() {
        return new Random().nextLong();
    }

    /** New maze with a random seed and default size. */
    public MazeGraph() {
        this(DEFAULT_ROWS, DEFAULT_COLS, newSeed(), DEFAULT_BRAID_CHANCE);
    }

    /** Maze for a given seed (Continue / Play Again / multiplayer). */
    public MazeGraph(int rows, int cols, long seed) {
        this(rows, cols, seed, DEFAULT_BRAID_CHANCE);
    }

    public MazeGraph(int rows, int cols, long seed, double braidChance) {
        if (rows < 3 || cols < 3) throw new IllegalArgumentException("Dimensions must be >= 3x3.");
        this.rows = rows;
        this.cols = cols;
        this.seed = seed;
        this.braidChance = braidChance;
        this.grid = new int[rows][cols];

        this.cellRows = (rows - 1) / 2;
        this.cellCols = (cols - 1) / 2;
        this.totalCells = cellRows * cellCols;
        this.adjacencyMatrix = new int[totalCells][totalCells];

        generate();
    }

    private void generate() {
        Random rng = new Random(seed);   // ALL randomness comes from this one seeded source

        for (int r = 0; r < rows; r++) Arrays.fill(grid[r], WALL);
        for (int i = 0; i < totalCells; i++) Arrays.fill(adjacencyMatrix[i], 0);

        for (int cr = 0; cr < cellRows; cr++) {
            for (int cc = 0; cc < cellCols; cc++) {
                grid[2 * cr + 1][2 * cc + 1] = PATH;
            }
        }

        List<Edge> edges = new ArrayList<>();
        for (int cr = 0; cr < cellRows; cr++) {
            for (int cc = 0; cc < cellCols; cc++) {
                int u = cr * cellCols + cc;
                if (cc + 1 < cellCols) edges.add(new Edge(u, cr * cellCols + (cc + 1), 2 * cr + 1, 2 * cc + 2));
                if (cr + 1 < cellRows) edges.add(new Edge(u, (cr + 1) * cellCols + cc, 2 * cr + 2, 2 * cc + 1));
            }
        }

        // Own Fisher-Yates shuffle: identical result on every machine and Java version
        for (int i = edges.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Edge tmp = edges.get(i);
            edges.set(i, edges.get(j));
            edges.set(j, tmp);
        }

        // Step 1: Kruskal (perfect maze)
        DisjointSet ds = new DisjointSet(totalCells);
        int edgesCarved = 0;
        for (Edge edge : edges) {
            if (ds.union(edge.u, edge.v)) {
                carve(edge);
                edgesCarved++;
                if (edgesCarved == totalCells - 1) break;
            }
        }

        // Step 2: braiding - open some of the remaining walls to create loops
        for (Edge edge : edges) {
            if (grid[edge.wallRow][edge.wallCol] == WALL && rng.nextDouble() < braidChance) {
                carve(edge);
            }
        }

        // Entrance on the left edge, exit on the right edge (bottom-right cell's row)
        grid[1][0] = PATH;
        int lastRow = 2 * (cellRows - 1) + 1;
        int lastCol = 2 * (cellCols - 1) + 1;
        for (int c = lastCol + 1; c < cols; c++) grid[lastRow][c] = PATH;
    }

    private void carve(Edge edge) {
        grid[edge.wallRow][edge.wallCol] = PATH;
        adjacencyMatrix[edge.u][edge.v] = 1;
        adjacencyMatrix[edge.v][edge.u] = 1;
    }

    /** Returns a copy of the maze (1 = wall, 0 = path). */
    public int[][] getMaze() {
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) System.arraycopy(grid[r], 0, copy[r], 0, cols);
        return copy;
    }

    public int[][] getAdjacencyMatrix() {
        int[][] copy = new int[totalCells][totalCells];
        for (int i = 0; i < totalCells; i++) System.arraycopy(adjacencyMatrix[i], 0, copy[i], 0, totalCells);
        return copy;
    }

    public long getSeed() { return seed; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTotalCells() { return totalCells; }

    public void printMaze() {
        System.out.println("Maze (" + rows + "x" + cols + "), seed " + seed + ":");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                System.out.print(grid[r][c] == WALL ? "##" : "  ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        MazeGraph mazeGraph = new MazeGraph();
        mazeGraph.printMaze();
    }
}