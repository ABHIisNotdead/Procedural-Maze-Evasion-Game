package logic;

/**
 * Represents a treat (Fish / Bone) dropped by the player cat to distract and stun dog chasers.
 * Uses pause-safe gameTimeMs so treats do not expire while the game is paused.
 */
public class DistractionItem {
    public final int gridX;
    public final int gridY;
    public final long spawnGameTimeMs;
    public final long durationMs;
    private boolean consumed = false;

    public DistractionItem(int gridX, int gridY, long durationMs, long gameTimeMs) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.spawnGameTimeMs = gameTimeMs;
        this.durationMs = durationMs;
    }

    public DistractionItem(int gridX, int gridY, long durationMs) {
        this(gridX, gridY, durationMs, System.currentTimeMillis());
    }

    public boolean isExpired(long gameTimeMs) {
        return consumed || (gameTimeMs - spawnGameTimeMs > durationMs);
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        this.consumed = true;
    }
}
