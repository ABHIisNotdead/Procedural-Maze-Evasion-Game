package logic;

import java.util.*;

public class ChaserAI {

    private final int detectionRadius;
    private final Random random = new Random();
    private int previousX = -1;
    private int previousY = -1;

    private static final int[][] DIRECTIONS = {
            {0, -1},   // Up
            {0, 1},    // Down
            {-1, 0},   // Left
            {1, 0}     // Right
    };

    public ChaserAI(int detectionRadius) {
        this.detectionRadius = detectionRadius;
    }

    public int[] nextMove(
            int[][] maze,
            int chaserX,
            int chaserY,
            int playerX,
            int playerY) {

        int distance = Math.abs(chaserX - playerX)
                + Math.abs(chaserY - playerY);

        // Chase the player using BFS when detected.
        if (distance <= detectionRadius) {
            int[] next = findNextMove(
                    maze, chaserX, chaserY, playerX, playerY
            );

            if (next != null) {
                return next;
            }
        }

        List<int[]> moves = new ArrayList<>();

        for (int[] d : DIRECTIONS) {
            int nx = chaserX + d[0];
            int ny = chaserY + d[1];

            if (isOpen(maze, nx, ny)) {
                moves.add(new int[]{nx, ny});
            }
        }

        if (moves.size() > 1) {
            moves.removeIf(move ->
                    move[0] == previousX && move[1] == previousY
            );
        }

        if (moves.isEmpty()) {
            return new int[]{chaserX, chaserY};
        }

        int[] next = moves.get(random.nextInt(moves.size()));

        previousX = chaserX;
        previousY = chaserY;

        return next;
    }

    private int[] findNextMove(
            int[][] maze,
            int startX,
            int startY,
            int targetX,
            int targetY) {

        int rows = maze.length;
        int cols = maze[0].length;

        boolean[][] visited = new boolean[rows][cols];
        int[][] parentX = new int[rows][cols];
        int[][] parentY = new int[rows][cols];

        for (int y = 0; y < rows; y++) {
            Arrays.fill(parentX[y], -1);
            Arrays.fill(parentY[y], -1);
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{startX, startY});
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();

            int x = current[0];
            int y = current[1];

            if (x == targetX && y == targetY) {
                break;
            }

            for (int[] direction : DIRECTIONS) {
                int nx = x + direction[0];
                int ny = y + direction[1];

                if (isOpen(maze, nx, ny)
                        && !visited[ny][nx]) {

                    visited[ny][nx] = true;
                    parentX[ny][nx] = x;
                    parentY[ny][nx] = y;

                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        // No route to the player.
        if (!visited[targetY][targetX]) {
            return null;
        }

        // Trace backwards to find the first step.
        int x = targetX;
        int y = targetY;

        while (parentX[y][x] != startX
                || parentY[y][x] != startY) {

            int px = parentX[y][x];
            int py = parentY[y][x];

            if (px == -1 || py == -1) {
                return null;
            }

            x = px;
            y = py;
        }

        return new int[]{x, y};
    }

    private boolean isOpen(int[][] maze, int x, int y) {
        return y >= 0
                && y < maze.length
                && x >= 0
                && x < maze[0].length
                && maze[y][x] == 0;
    }
}
