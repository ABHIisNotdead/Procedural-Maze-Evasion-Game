package maze;

/**
 * Enumeration of Procedural Maze Generation Algorithms.
 * Fulfills Advanced Data Structures (ADS) syllabus modules:
 * - KRUSKAL_MST: Minimum Spanning Tree via Disjoint-Set (Union-Find with Rank & Path Compression).
 * - PRIMS_MST: Minimum Spanning Tree via Frontier Set / Priority Queue.
 * - DFS_BACKTRACKER: Depth-First Search Maze Carving using an explicit Stack.
 * - CAVERN_CELLULAR: Procedural Cellular Automata for organic cave topology.
 */
public enum MazeType {
    KRUSKAL_MST("Kruskal's MST (Disjoint-Set)", "Module 6: Minimum Spanning Tree via Kruskal & Union-Find"),
    PRIMS_MST("Prim's MST (Frontier Queue)", "Module 6: Minimum Spanning Tree via Prim's Algorithm"),
    DFS_BACKTRACKER("Recursive Backtracker (DFS Stack)", "Module 6 / Practical 10: Graph Traversal via Explicit Stack"),
    CAVERN_CELLULAR("Cellular Automata (Cavern)", "Advanced Procedural Simulation: Cave Network Topology");

    public final String displayName;
    public final String adsDescription;

    MazeType(String displayName, String adsDescription) {
        this.displayName = displayName;
        this.adsDescription = adsDescription;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
