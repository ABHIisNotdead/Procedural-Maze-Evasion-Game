package maze;

import java.util.*;

/**
 * Advanced Data Structures (ADS) Procedural Maze Generator.
 *
 * Implements:
 * 1. Sparse Adjacency List Graph Representation (O(V + E) space).
 * 2. Kruskal's Algorithm with Disjoint-Set / Union-Find (Path Compression + Union by Rank).
 * 3. Prim's Algorithm using a Min-Heap (PriorityQueue<Edge>) over weighted frontier edges.
 * 4. Recursive Backtracker (DFS) with an explicit Stack (ArrayDeque) for deep winding corridors.
 * 5. Cellular Automata for organic cave tunnels.
 * 6. Braid Engine: Removes dead ends to create guaranteed cycles (multiple paths).
 * 7. 2x2 Scaled Corridor Expansion + Weighted Terrain (Puddles, Stealth Bushes, Cat Flaps).
 * 8. Step-by-Step Generation Recorder (GenerationStep) for live animated visualization.
 */
public class MazeGraph {

    public static final int DEFAULT_ROWS = 17;
    public static final int DEFAULT_COLS = 23;
    public static final double DEFAULT_BRAID_CHANCE = 0.25;
    public static final int WALL = 1;
    public static final int PATH = 0;

    // Interactive Terrain Types for Weighted Graphs & Stealth
    public static final int TERRAIN_NORMAL = 0;
    public static final int TERRAIN_PUDDLE = 1;       // Traversal weight = 2 (slows movement)
    public static final int TERRAIN_BUSH = 2;         // Stealth hideout (press Q to hide)
    public static final int TERRAIN_CAT_FLAP = 3;     // Player-only shortcut (blocks dogs)
    public static final int TERRAIN_CRACKED_WALL = 4; // Wall crevice hideout (press Q near wall to hide)

    public static class GenerationStep {
        public final int baseRow;
        public final int baseCol;
        public final int value;
        public final String phase; // "KRUSKAL", "PRIM_HEAP", "DFS_STACK", "CAVERN", "BRAID"

        public GenerationStep(int baseRow, int baseCol, int value, String phase) {
            this.baseRow = baseRow;
            this.baseCol = baseCol;
            this.value = value;
            this.phase = phase;
        }
    }

    public static class GraphEdge {
        public final int to;
        public final int weight;

        public GraphEdge(int to, int weight) {
            this.to = to;
            this.weight = weight;
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
            if (parent[i] != i) {
                parent[i] = find(parent[i]); // Path compression
            }
            return parent[i];
        }

        public boolean union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI == rootJ) return false;

            // Union by rank
            if (rank[rootI] < rank[rootJ]) {
                parent[rootI] = rootJ;
            } else if (rank[rootI] > rank[rootJ]) {
                parent[rootJ] = rootI;
            } else {
                parent[rootJ] = rootI;
                rank[rootI]++;
            }
            return true;
        }
    }

    private static class Edge implements Comparable<Edge> {
        final int u, v, wallRow, wallCol, weight;

        Edge(int u, int v, int wallRow, int wallCol, int weight) {
            this.u = u;
            this.v = v;
            this.wallRow = wallRow;
            this.wallCol = wallCol;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge other) {
            return Integer.compare(this.weight, other.weight);
        }
    }

    private final int baseRows;
    private final int baseCols;
    private final int rows;
    private final int cols;
    private final long seed;
    private final double braidChance;
    private final MazeType mazeType;
    private final boolean scale2x2;
    private final int desiredKittens;

    private final int[][] baseGrid;
    private final int[][] grid;
    private final int[][] terrain;
    private final int cellRows;
    private final int cellCols;
    private final int totalCells;
    private final Map<Integer, List<GraphEdge>> adjacencyList = new HashMap<>();
    private final List<GenerationStep> generationSteps = new ArrayList<>();

    // Endpoints
    private MazePoint spawnPoint;
    private MazePoint goalPoint;
    private MazePoint dogSpawnPoint;
    private MazePoint dog2SpawnPoint;
    private MazePoint dog3SpawnPoint;
    private final List<MazePoint> kittenSpawns = new ArrayList<>();

    public static long newSeed() {
        return new Random().nextLong();
    }

    public MazeGraph() {
        this(DEFAULT_ROWS, DEFAULT_COLS, newSeed(), DEFAULT_BRAID_CHANCE, MazeType.KRUSKAL_MST, true, 4);
    }

    public MazeGraph(int rows, int cols, long seed) {
        this(rows, cols, seed, DEFAULT_BRAID_CHANCE, MazeType.KRUSKAL_MST, true, 4);
    }

    public MazeGraph(int rows, int cols, long seed, double braidChance) {
        this(rows, cols, seed, braidChance, MazeType.KRUSKAL_MST, true, 4);
    }

    public MazeGraph(int rows, int cols, long seed, double braidChance, MazeType mazeType) {
        this(rows, cols, seed, braidChance, mazeType, true, 4);
    }

    public MazeGraph(int rows, int cols, long seed, double braidChance, MazeType mazeType, boolean scale2x2) {
        this(rows, cols, seed, braidChance, mazeType, scale2x2, 4);
    }

    public MazeGraph(int rows, int cols, long seed, double braidChance, MazeType mazeType, boolean scale2x2, int desiredKittens) {
        if (rows < 5 || cols < 5) throw new IllegalArgumentException("Dimensions must be >= 5x5.");
        this.baseRows = (rows % 2 == 0) ? rows + 1 : rows;
        this.baseCols = (cols % 2 == 0) ? cols + 1 : cols;
        this.scale2x2 = scale2x2;
        this.desiredKittens = Math.max(1, desiredKittens);

        this.rows = scale2x2 ? this.baseRows * 2 : this.baseRows;
        this.cols = scale2x2 ? this.baseCols * 2 : this.baseCols;

        this.seed = seed;
        this.braidChance = braidChance;
        this.mazeType = mazeType != null ? mazeType : MazeType.KRUSKAL_MST;

        this.baseGrid = new int[this.baseRows][this.baseCols];
        this.grid = new int[this.rows][this.cols];
        this.terrain = new int[this.rows][this.cols];

        this.cellRows = (this.baseRows - 1) / 2;
        this.cellCols = (this.baseCols - 1) / 2;
        this.totalCells = cellRows * cellCols;

        for (int i = 0; i < totalCells; i++) {
            adjacencyList.put(i, new ArrayList<>());
        }

        generate();
    }

    private void generate() {
        Random rng = new Random(seed);

        for (int r = 0; r < baseRows; r++) Arrays.fill(baseGrid[r], WALL);
        generationSteps.clear();

        // Open logical base nodes
        String initialPhase = mazeType.name();
        for (int cr = 0; cr < cellRows; cr++) {
            for (int cc = 0; cc < cellCols; cc++) {
                baseGrid[2 * cr + 1][2 * cc + 1] = PATH;
            }
        }

        switch (mazeType) {
            case PRIMS_MST:
                generatePrim(rng);
                break;
            case DFS_BACKTRACKER:
                generateDFS(rng);
                break;
            case CAVERN_CELLULAR:
                generateCavern(rng);
                break;
            case KRUSKAL_MST:
            default:
                generateKruskal(rng);
                break;
        }

        // Apply braiding (loop injection) to guarantee multiple paths to the goal
        applyBraiding(rng);

        // Determine base randomized endpoints and kittens
        determineBaseEndpointsAndKittens(rng);

        // Scale baseGrid into final grid
        if (scale2x2) {
            for (int r = 0; r < baseRows; r++) {
                for (int c = 0; c < baseCols; c++) {
                    int val = baseGrid[r][c];
                    grid[2 * r][2 * c] = val;
                    grid[2 * r + 1][2 * c] = val;
                    grid[2 * r][2 * c + 1] = val;
                    grid[2 * r + 1][2 * c + 1] = val;
                }
            }
        } else {
            for (int r = 0; r < baseRows; r++) {
                System.arraycopy(baseGrid[r], 0, grid[r], 0, baseCols);
            }
        }

        // Populate weighted terrain (Puddles, Stealth Bushes, Cat Flaps)
        generateTerrainFeatures(rng);
    }

    private void generateKruskal(Random rng) {
        List<Edge> edges = buildCellEdges(rng);
        Collections.sort(edges); // Sort by random weight for Kruskal's MST

        DisjointSet ds = new DisjointSet(totalCells);
        int edgesCarved = 0;
        for (Edge edge : edges) {
            if (ds.union(edge.u, edge.v)) {
                carve(edge, "KRUSKAL");
                edgesCarved++;
                if (edgesCarved == totalCells - 1) break;
            }
        }
    }

    /**
     * Prim's MST using a true Min-Heap (PriorityQueue<Edge>).
     */
    private void generatePrim(Random rng) {
        boolean[] inTree = new boolean[totalCells];
        PriorityQueue<Edge> minHeap = new PriorityQueue<>();

        int startCell = rng.nextInt(totalCells);
        inTree[startCell] = true;
        int sr = 2 * (startCell / cellCols) + 1;
        int sc = 2 * (startCell % cellCols) + 1;
        generationSteps.add(new GenerationStep(sr, sc, PATH, "PRIM_HEAP"));
        addFrontierEdges(startCell, inTree, minHeap, rng);

        while (!minHeap.isEmpty()) {
            Edge edge = minHeap.poll();

            int outside = -1;
            if (inTree[edge.u] && !inTree[edge.v]) outside = edge.v;
            else if (!inTree[edge.u] && inTree[edge.v]) outside = edge.u;

            if (outside != -1) {
                inTree[outside] = true;
                carve(edge, "PRIM_HEAP");
                int cr = 2 * (outside / cellCols) + 1;
                int cc = 2 * (outside % cellCols) + 1;
                generationSteps.add(new GenerationStep(cr, cc, PATH, "PRIM_HEAP"));
                addFrontierEdges(outside, inTree, minHeap, rng);
            }
        }
    }

    private void generateDFS(Random rng) {
        boolean[] visited = new boolean[totalCells];
        Deque<Integer> stack = new ArrayDeque<>();

        int startCell = rng.nextInt(totalCells);
        visited[startCell] = true;
        stack.push(startCell);
        generationSteps.add(new GenerationStep(2 * (startCell / cellCols) + 1, 2 * (startCell % cellCols) + 1, PATH, "DFS_STACK"));

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!stack.isEmpty()) {
            int current = stack.peek();
            int cr = current / cellCols;
            int cc = current % cellCols;

            List<Edge> neighbors = new ArrayList<>();
            for (int[] d : dirs) {
                int nr = cr + d[0];
                int nc = cc + d[1];
                if (nr >= 0 && nr < cellRows && nc >= 0 && nc < cellCols) {
                    int neighborIdx = nr * cellCols + nc;
                    if (!visited[neighborIdx]) {
                        int wr = 2 * cr + 1 + d[0];
                        int wc = 2 * cc + 1 + d[1];
                        neighbors.add(new Edge(current, neighborIdx, wr, wc, 1));
                    }
                }
            }

            if (!neighbors.isEmpty()) {
                Edge chosen = neighbors.get(rng.nextInt(neighbors.size()));
                visited[chosen.v] = true;
                carve(chosen, "DFS_STACK");
                generationSteps.add(new GenerationStep(2 * (chosen.v / cellCols) + 1, 2 * (chosen.v % cellCols) + 1, PATH, "DFS_STACK"));
                stack.push(chosen.v);
            } else {
                stack.pop();
            }
        }
    }

    private void generateCavern(Random rng) {
        generateKruskal(rng);

        for (int r = 1; r < baseRows - 1; r++) {
            for (int c = 1; c < baseCols - 1; c++) {
                if (baseGrid[r][c] == WALL && rng.nextDouble() < 0.35) {
                    baseGrid[r][c] = PATH;
                    generationSteps.add(new GenerationStep(r, c, PATH, "CAVERN"));
                }
            }
        }

        int[][] temp = new int[baseRows][baseCols];
        for (int r = 1; r < baseRows - 1; r++) {
            for (int c = 1; c < baseCols - 1; c++) {
                int wallCount = countWallNeighbors(r, c);
                temp[r][c] = (wallCount >= 5) ? WALL : PATH;
            }
        }
        for (int r = 1; r < baseRows - 1; r++) {
            for (int c = 1; c < baseCols - 1; c++) {
                if (baseGrid[r][c] != temp[r][c]) {
                    baseGrid[r][c] = temp[r][c];
                    generationSteps.add(new GenerationStep(r, c, temp[r][c], "CAVERN"));
                }
            }
        }

        for (int r = 0; r < baseRows; r++) { baseGrid[r][0] = WALL; baseGrid[r][baseCols - 1] = WALL; }
        for (int c = 0; c < baseCols; c++) { baseGrid[0][c] = WALL; baseGrid[baseRows - 1][c] = WALL; }
    }

    private int countWallNeighbors(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (baseGrid[r + dr][c + dc] == WALL) count++;
            }
        }
        return count;
    }

    private List<Edge> buildCellEdges(Random rng) {
        List<Edge> edges = new ArrayList<>();
        for (int cr = 0; cr < cellRows; cr++) {
            for (int cc = 0; cc < cellCols; cc++) {
                int u = cr * cellCols + cc;
                if (cc + 1 < cellCols) edges.add(new Edge(u, cr * cellCols + (cc + 1), 2 * cr + 1, 2 * cc + 2, 1 + rng.nextInt(100)));
                if (cr + 1 < cellRows) edges.add(new Edge(u, (cr + 1) * cellCols + cc, 2 * cr + 2, 2 * cc + 1, 1 + rng.nextInt(100)));
            }
        }
        return edges;
    }

    private void addFrontierEdges(int cell, boolean[] inTree, PriorityQueue<Edge> minHeap, Random rng) {
        int cr = cell / cellCols;
        int cc = cell % cellCols;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = cr + d[0];
            int nc = cc + d[1];
            if (nr >= 0 && nr < cellRows && nc >= 0 && nc < cellCols) {
                int neighbor = nr * cellCols + nc;
                if (!inTree[neighbor]) {
                    int wr = 2 * cr + 1 + d[0];
                    int wc = 2 * cc + 1 + d[1];
                    minHeap.offer(new Edge(cell, neighbor, wr, wc, 1 + rng.nextInt(100)));
                }
            }
        }
    }

    private void carve(Edge edge, String phase) {
        baseGrid[edge.wallRow][edge.wallCol] = PATH;
        generationSteps.add(new GenerationStep(edge.wallRow, edge.wallCol, PATH, phase));
        adjacencyList.get(edge.u).add(new GraphEdge(edge.v, edge.weight));
        adjacencyList.get(edge.v).add(new GraphEdge(edge.u, edge.weight));
    }

    private void applyBraiding(Random rng) {
        List<Edge> allEdges = buildCellEdges(rng);
        for (Edge edge : allEdges) {
            if (baseGrid[edge.wallRow][edge.wallCol] == WALL && rng.nextDouble() < braidChance) {
                carve(edge, "BRAID");
            }
        }

        for (int cr = 0; cr < cellRows; cr++) {
            for (int cc = 0; cc < cellCols; cc++) {
                int r = 2 * cr + 1;
                int c = 2 * cc + 1;
                if (baseGrid[r][c] == PATH) {
                    List<int[]> wallNeighbors = new ArrayList<>();
                    int openCount = 0;
                    int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    for (int[] d : dirs) {
                        int wr = r + d[0];
                        int wc = c + d[1];
                        int oppR = r + 2 * d[0];
                        int oppC = c + 2 * d[1];
                        if (wr >= 0 && wr < baseRows && wc >= 0 && wc < baseCols) {
                            if (baseGrid[wr][wc] == PATH) {
                                openCount++;
                            } else if (oppR > 0 && oppR < baseRows - 1 && oppC > 0 && oppC < baseCols - 1 && baseGrid[oppR][oppC] == PATH) {
                                wallNeighbors.add(new int[]{wr, wc});
                            }
                        }
                    }

                    if (openCount <= 1 && !wallNeighbors.isEmpty() && rng.nextDouble() < 0.85) {
                        int[] pick = wallNeighbors.get(rng.nextInt(wallNeighbors.size()));
                        baseGrid[pick[0]][pick[1]] = PATH;
                        generationSteps.add(new GenerationStep(pick[0], pick[1], PATH, "BRAID"));
                    }
                }
            }
        }
    }

    private void determineBaseEndpointsAndKittens(Random rng) {
        List<MazePoint> openPoints = new ArrayList<>();
        for (int r = 1; r < baseRows - 1; r++) {
            for (int c = 1; c < baseCols - 1; c++) {
                if (baseGrid[r][c] == PATH) {
                    openPoints.add(new MazePoint(c, r));
                }
            }
        }

        if (openPoints.isEmpty()) {
            baseGrid[1][1] = PATH;
            baseGrid[baseRows - 2][baseCols - 2] = PATH;
            setEndpoints(new MazePoint(1, 1), new MazePoint(baseCols - 2, baseRows - 2),
                    new MazePoint(1, 1), new MazePoint(1, 1), new MazePoint(1, 1));
            return;
        }

        MazePoint bSpawn = openPoints.get(rng.nextInt(openPoints.size()));
        int[][] dist = computeBFSDistances(bSpawn.x, bSpawn.y);

        int maxDist = 0;
        for (MazePoint p : openPoints) {
            int d = dist[p.y][p.x];
            if (d > maxDist) maxDist = d;
        }

        int minGoalDist = Math.max(4, (int) (maxDist * 0.65));
        List<MazePoint> farCandidates = new ArrayList<>();
        for (MazePoint p : openPoints) {
            int d = dist[p.y][p.x];
            if (d >= minGoalDist) farCandidates.add(p);
        }

        MazePoint bGoal = (!farCandidates.isEmpty()) ? farCandidates.get(rng.nextInt(farCandidates.size())) : openPoints.get(openPoints.size() - 1);

        int safeBuffer = Math.min(8, Math.max(4, maxDist / 2));
        List<MazePoint> dogCandidates = new ArrayList<>();
        for (MazePoint p : openPoints) {
            int d = dist[p.y][p.x];
            if (d >= safeBuffer && !p.equals(bGoal)) dogCandidates.add(p);
        }

        MazePoint bDog = (!dogCandidates.isEmpty()) ? dogCandidates.get(rng.nextInt(dogCandidates.size())) : new MazePoint(bGoal.x, bGoal.y);
        MazePoint bDog2 = (!dogCandidates.isEmpty()) ? dogCandidates.get((rng.nextInt(dogCandidates.size()) + 1) % dogCandidates.size()) : bDog;
        MazePoint bDog3 = (!dogCandidates.isEmpty()) ? dogCandidates.get((rng.nextInt(dogCandidates.size()) + 2) % dogCandidates.size()) : bDog;

        setEndpoints(bSpawn, bGoal, bDog, bDog2, bDog3);

        // Place Stray Kittens across open corridor tiles
        kittenSpawns.clear();
        Collections.shuffle(openPoints, rng);
        int count = 0;
        for (MazePoint p : openPoints) {
            if (!p.equals(bSpawn) && !p.equals(bGoal) && !p.equals(bDog) && !p.equals(bDog2) && !p.equals(bDog3)) {
                if (scale2x2) {
                    kittenSpawns.add(new MazePoint(p.x * 2, p.y * 2));
                } else {
                    kittenSpawns.add(p);
                }
                count++;
                if (count >= desiredKittens) break;
            }
        }
    }

    private void setEndpoints(MazePoint bSpawn, MazePoint bGoal, MazePoint bDog, MazePoint bDog2, MazePoint bDog3) {
        if (scale2x2) {
            this.spawnPoint = new MazePoint(bSpawn.x * 2, bSpawn.y * 2);
            this.goalPoint = new MazePoint(bGoal.x * 2, bGoal.y * 2);
            this.dogSpawnPoint = new MazePoint(bDog.x * 2, bDog.y * 2);
            this.dog2SpawnPoint = new MazePoint(bDog2.x * 2 + 1, bDog2.y * 2 + 1);
            this.dog3SpawnPoint = new MazePoint(bDog3.x * 2, bDog3.y * 2 + 1);
        } else {
            this.spawnPoint = bSpawn;
            this.goalPoint = bGoal;
            this.dogSpawnPoint = bDog;
            this.dog2SpawnPoint = bDog2;
            this.dog3SpawnPoint = bDog3;
        }
    }

    /**
     * Populates weighted terrain (Puddles, Stealth Bushes) on open paths,
     * and creates occasional Cat-only Flaps in thin 1-tile walls if unscaled or 2x2 thin dividers.
     */
    private void generateTerrainFeatures(Random rng) {
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                terrain[r][c] = TERRAIN_NORMAL;
                if (grid[r][c] == WALL) {
                    // Place occasional Cracked Wall hiding alcoves adjacent to open corridors
                    boolean adjPath = (grid[r - 1][c] == PATH || grid[r + 1][c] == PATH
                            || grid[r][c - 1] == PATH || grid[r][c + 1] == PATH);
                    if (adjPath && rng.nextDouble() < 0.032) {
                        terrain[r][c] = TERRAIN_CRACKED_WALL;
                    }
                    continue;
                }
                if ((r == spawnPoint.y && c == spawnPoint.x) || (r == goalPoint.y && c == goalPoint.x)) continue;

                double roll = rng.nextDouble();
                if (roll < 0.012) {
                    terrain[r][c] = TERRAIN_PUDDLE; // Sparse tactical puddles (weight = 2)
                } else if (roll < 0.036) {
                    terrain[r][c] = TERRAIN_BUSH;   // Stealth hiding bush
                }
            }
        }

        // Inject a few Cat-Only Flaps on internal wall blocks with open paths on opposite sides
        int flapsAdded = 0;
        int maxFlaps = Math.max(2, (rows * cols) / 280);
        for (int attempt = 0; attempt < 250 && flapsAdded < maxFlaps; attempt++) {
            int r = 2 + rng.nextInt(Math.max(1, rows - 4));
            int c = 2 + rng.nextInt(Math.max(1, cols - 4));
            if (grid[r][c] == WALL && grid[r - 1][c] == PATH && grid[r + 1][c] == PATH
                    && grid[r][c - 1] == WALL && grid[r][c + 1] == WALL) {
                grid[r][c] = PATH;
                terrain[r][c] = TERRAIN_CAT_FLAP;
                flapsAdded++;
            } else if (grid[r][c] == WALL && grid[r][c - 1] == PATH && grid[r][c + 1] == PATH
                    && grid[r - 1][c] == WALL && grid[r + 1][c] == WALL) {
                grid[r][c] = PATH;
                terrain[r][c] = TERRAIN_CAT_FLAP;
                flapsAdded++;
            }
        }
    }

    private int[][] computeBFSDistances(int startX, int startY) {
        int[][] dist = new int[baseRows][baseCols];
        for (int[] row : dist) Arrays.fill(row, -1);

        Queue<int[]> queue = new ArrayDeque<>();
        queue.offer(new int[]{startX, startY});
        dist[startY][startX] = 0;

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int cx = curr[0];
            int cy = curr[1];

            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx >= 0 && nx < baseCols && ny >= 0 && ny < baseRows && baseGrid[ny][nx] == PATH && dist[ny][nx] == -1) {
                    dist[ny][nx] = dist[cy][cx] + 1;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    public int[][] getMaze() {
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) System.arraycopy(grid[r], 0, copy[r], 0, cols);
        return copy;
    }

    public int[][] getTerrain() {
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) System.arraycopy(terrain[r], 0, copy[r], 0, cols);
        return copy;
    }

    public int getTileWeight(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return 999;
        return terrain[row][col] == TERRAIN_PUDDLE ? 2 : 1;
    }

    public Map<Integer, List<GraphEdge>> getAdjacencyList() {
        return Collections.unmodifiableMap(adjacencyList);
    }

    public List<GenerationStep> getGenerationSteps() {
        return Collections.unmodifiableList(generationSteps);
    }

    public MazePoint getSpawnPoint() { return spawnPoint; }
    public MazePoint getGoalPoint() { return goalPoint; }
    public MazePoint getDogSpawnPoint() { return dogSpawnPoint; }
    public MazePoint getDog2SpawnPoint() { return dog2SpawnPoint; }
    public MazePoint getDog3SpawnPoint() { return dog3SpawnPoint; }
    public List<MazePoint> getKittenSpawns() { return new ArrayList<>(kittenSpawns); }

    public long getSeed() { return seed; }
    public int getBaseRows() { return baseRows; }
    public int getBaseCols() { return baseCols; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTotalCells() { return totalCells; }
    public MazeType getMazeType() { return mazeType; }
    public double getBraidChance() { return braidChance; }
    public boolean isScaled2x2() { return scale2x2; }
}
