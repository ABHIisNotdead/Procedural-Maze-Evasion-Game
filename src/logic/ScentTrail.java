package logic;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded Deque / Circular Queue Scent Trail Engine.
 *
 * Prevents 2-3 tile oscillation by tracking monotonic footprint timestamps
 * and allowing the Scent Hound to consume/clear sniffed tiles as it advances.
 */
public class ScentTrail {

    public static class ScentNode {
        public final int x;
        public final int y;
        public final long timestampMs;

        public ScentNode(int x, int y, long timestampMs) {
            this.x = x;
            this.y = y;
            this.timestampMs = timestampMs;
        }
    }

    private final int rows;
    private final int cols;
    private final int maxCapacity;
    private final long maxAgeMs;
    private final Deque<ScentNode> deque = new ArrayDeque<>();
    private final int[][] grid;
    private final long[][] timestampGrid;

    public ScentTrail(int rows, int cols, int maxCapacity, long maxAgeMs) {
        this.rows = rows;
        this.cols = cols;
        this.maxCapacity = maxCapacity;
        this.maxAgeMs = maxAgeMs;
        this.grid = new int[rows][cols];
        this.timestampGrid = new long[rows][cols];
    }

    public void deposit(int px, int py, long gameTimeMs) {
        if (px < 0 || px >= cols || py < 0 || py >= rows) return;

        long stamp = gameTimeMs + 1; // Ensure > 0 even at t=0
        ScentNode last = deque.peekLast();
        if (last != null && last.x == px && last.y == py && (gameTimeMs - last.timestampMs) < 120) {
            timestampGrid[py][px] = stamp;
            grid[py][px] = 120;
            return;
        }

        deque.offerLast(new ScentNode(px, py, gameTimeMs));
        grid[py][px] = 120;
        timestampGrid[py][px] = stamp;

        // Subtle 1-tile scent aura so adjacent hound can pick up the trail
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int nx = px + d[0];
            int ny = py + d[1];
            if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) {
                if (grid[ny][nx] < 55) grid[ny][nx] = 55;
                if (timestampGrid[ny][nx] == 0) timestampGrid[ny][nx] = Math.max(1L, stamp - 500);
            }
        }

        while (deque.size() > maxCapacity) {
            ScentNode oldest = deque.pollFirst();
            if (oldest != null) {
                grid[oldest.y][oldest.x] = 0;
                timestampGrid[oldest.y][oldest.x] = 0;
            }
        }
    }

    /**
     * Marks a tile as sniffed/traversed by the Scent Hound so it never oscillates back onto it.
     */
    public void consume(int x, int y) {
        if (x >= 0 && x < cols && y >= 0 && y < rows) {
            grid[y][x] = 0;
            timestampGrid[y][x] = 0;
        }
    }

    public void update(long gameTimeMs) {
        while (!deque.isEmpty() && (gameTimeMs - deque.peekFirst().timestampMs) > maxAgeMs) {
            ScentNode expired = deque.pollFirst();
            grid[expired.y][expired.x] = 0;
            timestampGrid[expired.y][expired.x] = 0;
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] d : dirs) {
                int nx = expired.x + d[0];
                int ny = expired.y + d[1];
                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows && grid[ny][nx] <= 55) {
                    grid[ny][nx] = 0;
                    timestampGrid[ny][nx] = 0;
                }
            }
        }

        for (ScentNode node : deque) {
            if (timestampGrid[node.y][node.x] == 0) continue; // Already consumed by hound
            long age = gameTimeMs - node.timestampMs;
            int strength = Math.max(10, (int) (120 * (1.0 - (double) age / maxAgeMs)));
            grid[node.y][node.x] = Math.max(grid[node.y][node.x], strength);
        }
    }

    public void clear() {
        deque.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = 0;
                timestampGrid[r][c] = 0;
            }
        }
    }

    public int[][] getGrid() {
        return grid;
    }

    public long[][] getTimestampGrid() {
        return timestampGrid;
    }

    public Deque<ScentNode> getNodes() {
        return deque;
    }
}
