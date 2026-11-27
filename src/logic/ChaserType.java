package logic;

/**
 * Chaser Archetypes for the Multi-Chaser Swarm:
 * - BFS_TRACKER: Vidhita's Shortest-Path Chaser (Ordinary Queue BFS, O(V + E)).
 * - SCENT_HOUND: Greedy Scent Follower that tracks the player's Bounded Deque scent trail.
 * - ASTAR_TACTICIAN: Weighted Shortest-Path Chaser using a Min-Heap (PriorityQueue) over terrain costs.
 */
public enum ChaserType {
    BFS_TRACKER("Vidhita's BFS Tracker", "Calculates shortest unweighted graph path via Ordinary Queue"),
    SCENT_HOUND("Greedy Scent Hound", "Follows decaying Bounded Deque trail via local Greedy optimization"),
    ASTAR_TACTICIAN("A* Weighted Tactician", "Routes around weighted terrain (puddles) via Min-Heap PriorityQueue");

    public final String displayName;
    public final String description;

    ChaserType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
