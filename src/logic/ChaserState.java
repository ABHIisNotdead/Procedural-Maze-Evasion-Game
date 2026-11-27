package logic;

/**
 * Finite State Machine (FSM) States for Chasers:
 * - PATROLLING: Roaming the maze corridors when player/scent is out of range.
 * - HUNTING: Actively tracking and pursuing the player (via BFS or Greedy Scent).
 * - RETRIEVING: Carrying a kitten or retrieved item back to the home kennel.
 * - STUNNED: Temporarily incapacitated by distraction treats or catnip.
 */
public enum ChaserState {
    PATROLLING,
    HUNTING,
    RETRIEVING,
    STUNNED
}
