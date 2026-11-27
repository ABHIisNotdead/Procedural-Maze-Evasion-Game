package logic;

import java.awt.Color;
import java.util.*;
import maze.MazeGraph;

/**
 * Advanced Data Structures (ADS) Dog AI Pathfinding, Prediction & Swarm Communication Engine.
 *
 * Implements:
 * 1. Swarm Blackboard Communication: When any chaser detects the player, it broadcasts the alert
 *    and predicted intercept coordinates to the entire swarm.
 * 2. Player Next-Location Prediction: Projects up to 4 open tiles ahead along the player's
 *    movement vector so interceptors cut off escape routes instead of blindly trailing behind.
 * 3. Vidhita's BFS Tracker: Ordinary Queue (FIFO) shortest path in O(V + E).
 * 4. Smart Greedy Scent Hound: Follows monotonically fresher timestamps in the Bounded Deque,
 *    consumes visited scent tiles to eliminate 2-3 tile oscillation, and responds to Swarm Comms.
 * 5. A* Weighted Tactician: Min-Heap (PriorityQueue) over terrain weights aiming at predicted target.
 */
public class ChaserAI {

    private final Random random = new Random();
    private long totalNodesExplored = 0;

    // Shared Swarm Blackboard (Inter-Chaser Communication)
    private int sharedAlertX = -1;
    private int sharedAlertY = -1;
    private long sharedAlertUntilMs = 0;
    private int predictedTargetX = -1;
    private int predictedTargetY = -1;

    private static final int[][] DIRECTIONS = {
            {0, -1},   // Up
            {0, 1},    // Down
            {-1, 0},   // Left
            {1, 0}     // Right
    };

    private static class AStarNode implements Comparable<AStarNode> {
        final int x, y;
        final int gCost;
        final int fCost;

        AStarNode(int x, int y, int gCost, int fCost) {
            this.x = x;
            this.y = y;
            this.gCost = gCost;
            this.fCost = fCost;
        }

        @Override
        public int compareTo(AStarNode other) {
            return Integer.compare(this.fCost, other.fCost);
        }
    }

    public ChaserAI(int detectionRadius) {
    }

    public long getTotalNodesExplored() {
        return totalNodesExplored;
    }

    public int getPredictedTargetX() {
        return predictedTargetX;
    }

    public int getPredictedTargetY() {
        return predictedTargetY;
    }

    public boolean isSwarmAlertActive(long gameTimeMs) {
        return gameTimeMs < sharedAlertUntilMs && sharedAlertX >= 0;
    }

    /**
     * Predicts the player's next location up to 4 open tiles ahead along their movement vector.
     */
    public int[] predictPlayerLocation(int[][] maze, int px, int py, int dirX, int dirY) {
        int tx = px;
        int ty = py;
        if (dirX != 0 || dirY != 0) {
            for (int step = 0; step < 4; step++) {
                int nx = tx + dirX;
                int ny = ty + dirY;
                if (isOpen(maze, nx, ny)) {
                    tx = nx;
                    ty = ny;
                } else {
                    break;
                }
            }
        }
        predictedTargetX = tx;
        predictedTargetY = ty;
        return new int[]{tx, ty};
    }

    public void updateSwarm(
            int[][] maze,
            int[][] terrain,
            List<ChaserEntity> chasers,
            int playerGridX,
            int playerGridY,
            int playerDirX,
            int playerDirY,
            boolean playerInStealthBush,
            ScentTrail scentTrailObj,
            List<DistractionItem> distractions,
            List<int[]> strayKittens,
            int tileSize,
            long gameTimeMs,
            boolean frenzyActive) {

        if (chasers == null || chasers.isEmpty()) return;

        int[] predicted = predictPlayerLocation(maze, playerGridX, playerGridY, playerDirX, playerDirY);
        int predX = predicted[0];
        int predY = predicted[1];

        Set<String> reservedTiles = new HashSet<>();
        for (ChaserEntity c : chasers) {
            reservedTiles.add(c.gridX + "," + c.gridY);
            if (c.moving) {
                reservedTiles.add(c.targetGridX + "," + c.targetGridY);
            }
        }

        // Phase A: Perception & Swarm Communication Broadcast
        if (playerInStealthBush) {
            // Player is actively invisible inside a Bush or Cracked Wall: break swarm lock!
            sharedAlertUntilMs = 0;
        } else {
            for (ChaserEntity chaser : chasers) {
                if (chaser.isStunned(gameTimeMs)) continue;
                int playerDist = Math.abs(chaser.gridX - playerGridX) + Math.abs(chaser.gridY - playerGridY);
                int effectiveRadius = frenzyActive ? chaser.detectionRadius + 6 : chaser.detectionRadius;

                if (playerDist <= effectiveRadius) {
                    sharedAlertX = playerGridX;
                    sharedAlertY = playerGridY;
                    sharedAlertUntilMs = gameTimeMs + 4500;
                    break;
                }
            }
        }

        boolean swarmAlert = isSwarmAlertActive(gameTimeMs);

        // Phase B: Coordinated Movement
        for (ChaserEntity chaser : chasers) {
            if (chaser.moving) continue;

            reservedTiles.remove(chaser.gridX + "," + chaser.gridY);

            // 1. Handle STUNNED state
            if (chaser.isStunned(gameTimeMs)) {
                chaser.state = ChaserState.STUNNED;
                chaser.speechBubble = "*STUN*";
                chaser.speechColor = new Color(241, 196, 15);
                reservedTiles.add(chaser.gridX + "," + chaser.gridY);
                continue;
            } else if (chaser.state == ChaserState.STUNNED) {
                chaser.state = ChaserState.PATROLLING;
                chaser.speechBubble = null;
            }

            int effectiveDelay = frenzyActive ? Math.max(35, (int) (chaser.delayMs * 0.68)) : chaser.delayMs;
            if (gameTimeMs - chaser.lastMoveTime < effectiveDelay) {
                reservedTiles.add(chaser.gridX + "," + chaser.gridY);
                continue;
            }

            // 2. Handle RETRIEVING state
            if (chaser.carryingKitten) {
                chaser.state = ChaserState.RETRIEVING;
                chaser.speechBubble = "KITTEN!";
                chaser.speechColor = new Color(230, 126, 34);

                if (chaser.gridX == chaser.homeGridX && chaser.gridY == chaser.homeGridY) {
                    chaser.carryingKitten = false;
                    chaser.state = ChaserState.PATROLLING;
                    chaser.speechBubble = "^_^";
                    chaser.speechColor = new Color(46, 204, 113);
                    chaser.debugPath.clear();
                    // Respawn kitten at a random open tile so the level can never soft-lock!
                    if (strayKittens != null) {
                        int[] respawnPos = pickRandomOpenTile(maze, chaser.homeGridX, chaser.homeGridY);
                        strayKittens.add(new int[]{respawnPos[0], respawnPos[1], chaser.id % 3});
                    }
                } else {
                    List<int[]> path = findPathBFS(maze, terrain, chaser.gridX, chaser.gridY, chaser.homeGridX, chaser.homeGridY, reservedTiles, chaser);
                    if (path != null && !path.isEmpty()) {
                        applyStep(chaser, path.get(0)[0], path.get(0)[1], tileSize, gameTimeMs, reservedTiles);
                        continue;
                    }
                }
            }

            // 3. Check for nearby Stray Kittens
            if (!chaser.carryingKitten && strayKittens != null && !strayKittens.isEmpty()) {
                int[] targetKitten = null;
                int minKittenDist = Integer.MAX_VALUE;

                for (int[] k : strayKittens) {
                    int d = Math.abs(chaser.gridX - k[0]) + Math.abs(chaser.gridY - k[1]);
                    if (d == 0) {
                        chaser.carryingKitten = true;
                        chaser.state = ChaserState.RETRIEVING;
                        chaser.speechBubble = "GOT IT!";
                        chaser.speechColor = new Color(230, 126, 34);
                        strayKittens.remove(k);
                        break;
                    } else if (d <= 6 && d < minKittenDist) {
                        minKittenDist = d;
                        targetKitten = k;
                    }
                }

                if (chaser.carryingKitten) {
                    reservedTiles.add(chaser.gridX + "," + chaser.gridY);
                    continue;
                }

                if (targetKitten != null) {
                    List<int[]> path = findPathBFS(maze, terrain, chaser.gridX, chaser.gridY, targetKitten[0], targetKitten[1], reservedTiles, chaser);
                    if (path != null && !path.isEmpty()) {
                        chaser.speechBubble = "KITTEN?";
                        chaser.speechColor = new Color(243, 156, 18);
                        applyStep(chaser, path.get(0)[0], path.get(0)[1], tileSize, gameTimeMs, reservedTiles);
                        continue;
                    }
                }
            }

            // 4. Check for active Distraction Treats
            DistractionItem closestTreat = null;
            int minTreatDist = Integer.MAX_VALUE;
            if (distractions != null) {
                for (DistractionItem item : distractions) {
                    if (!item.isExpired(gameTimeMs)) {
                        int d = Math.abs(chaser.gridX - item.gridX) + Math.abs(chaser.gridY - item.gridY);
                        if (d <= chaser.detectionRadius + 4 && d < minTreatDist) {
                            minTreatDist = d;
                            closestTreat = item;
                        }
                    }
                }
            }

            if (closestTreat != null) {
                if (chaser.gridX == closestTreat.gridX && chaser.gridY == closestTreat.gridY) {
                    closestTreat.consume();
                    chaser.stun(3500, gameTimeMs);
                    reservedTiles.add(chaser.gridX + "," + chaser.gridY);
                    continue;
                }

                List<int[]> path = findPathBFS(maze, terrain, chaser.gridX, chaser.gridY, closestTreat.gridX, closestTreat.gridY, reservedTiles, chaser);
                if (path != null && !path.isEmpty()) {
                    chaser.speechBubble = "TREAT!";
                    chaser.speechColor = new Color(52, 152, 219);
                    applyStep(chaser, path.get(0)[0], path.get(0)[1], tileSize, gameTimeMs, reservedTiles);
                    continue;
                }
            }

            // 5. Coordinated Hunting, Prediction & Swarm Communication
            int playerDist = Math.abs(chaser.gridX - playerGridX) + Math.abs(chaser.gridY - playerGridY);
            int effectiveRadius = frenzyActive ? chaser.detectionRadius + 6 : chaser.detectionRadius;
            boolean directSight = !playerInStealthBush && playerDist <= effectiveRadius;

            if (chaser.type == ChaserType.BFS_TRACKER) {
                if (directSight || swarmAlert) {
                    chaser.state = ChaserState.HUNTING;
                    chaser.speechBubble = directSight ? (frenzyActive ? "!!" : "!") : "COMMS!";
                    chaser.speechColor = directSight ? Color.RED : new Color(230, 126, 34);

                    int goalX = directSight ? playerGridX : sharedAlertX;
                    int goalY = directSight ? playerGridY : sharedAlertY;
                    List<int[]> path = findPathBFS(maze, terrain, chaser.gridX, chaser.gridY, goalX, goalY, reservedTiles, chaser);
                    if (path != null && !path.isEmpty()) {
                        applyStep(chaser, path.get(0)[0], path.get(0)[1], tileSize, gameTimeMs, reservedTiles);
                        continue;
                    }
                }
            } else if (chaser.type == ChaserType.ASTAR_TACTICIAN) {
                if (directSight || swarmAlert) {
                    chaser.state = ChaserState.HUNTING;
                    chaser.speechBubble = "PREDICT!";
                    chaser.speechColor = new Color(155, 89, 182);

                    // Tactician intercepts the predicted future location to cut off the Cat!
                    int goalX = (playerDist <= 3) ? playerGridX : predX;
                    int goalY = (playerDist <= 3) ? playerGridY : predY;
                    List<int[]> path = findPathAStar(maze, terrain, chaser.gridX, chaser.gridY, goalX, goalY, reservedTiles, chaser);
                    if (path != null && !path.isEmpty()) {
                        applyStep(chaser, path.get(0)[0], path.get(0)[1], tileSize, gameTimeMs, reservedTiles);
                        continue;
                    }
                }
            } else if (chaser.type == ChaserType.SCENT_HOUND) {
                // Consume current tile scent so Hound never oscillates back and forth on 2-3 tiles!
                if (scentTrailObj != null) {
                    scentTrailObj.consume(chaser.gridX, chaser.gridY);
                    long[][] tsGrid = scentTrailObj.getTimestampGrid();
                    int[][] sGrid = scentTrailObj.getGrid();

                    long bestTimestamp = 0;
                    int bestScent = 0;
                    int[] bestMove = null;

                    for (int[] d : DIRECTIONS) {
                        int nx = chaser.gridX + d[0];
                        int ny = chaser.gridY + d[1];
                        if (!isDogPassable(maze, terrain, nx, ny) || reservedTiles.contains(nx + "," + ny)) continue;
                        // Anti-oscillation: avoid stepping back onto previous tile unless no other scent exists
                        if (nx == chaser.previousGridX && ny == chaser.previousGridY) continue;

                        long ts = tsGrid[ny][nx];
                        int s = sGrid[ny][nx];
                        if (ts > bestTimestamp || (ts == bestTimestamp && s > bestScent)) {
                            bestTimestamp = ts;
                            bestScent = s;
                            bestMove = new int[]{nx, ny};
                        }
                    }

                    if (bestScent > 0 && bestMove != null) {
                        chaser.state = ChaserState.HUNTING;
                        chaser.speechBubble = "~SNIFF~";
                        chaser.speechColor = new Color(52, 152, 219);
                        chaser.debugPath.clear();
                        chaser.debugPath.add(bestMove);
                        applyStep(chaser, bestMove[0], bestMove[1], tileSize, gameTimeMs, reservedTiles);
                        continue;
                    }
                }

                // If no fresh scent nearby, listen to Swarm Comms and flank to predicted location!
                if (swarmAlert) {
                    chaser.state = ChaserState.HUNTING;
                    chaser.speechBubble = "FLANK!";
                    chaser.speechColor = new Color(243, 156, 18);
                    int goalX = (playerDist <= 3) ? playerGridX : predX;
                    int goalY = (playerDist <= 3) ? playerGridY : predY;
                    List<int[]> path = findPathBFS(maze, terrain, chaser.gridX, chaser.gridY, goalX, goalY, reservedTiles, chaser);
                    if (path != null && !path.isEmpty()) {
                        applyStep(chaser, path.get(0)[0], path.get(0)[1], tileSize, gameTimeMs, reservedTiles);
                        continue;
                    }
                }
            }

            // 6. Smart Momentum Patrol (avoids reversing at junctions)
            chaser.state = ChaserState.PATROLLING;
            chaser.speechBubble = null;
            chaser.debugPath.clear();
            int[] patrolStep = pickPatrolStep(maze, terrain, chaser, reservedTiles);
            if (patrolStep != null) {
                applyStep(chaser, patrolStep[0], patrolStep[1], tileSize, gameTimeMs, reservedTiles);
            } else {
                reservedTiles.add(chaser.gridX + "," + chaser.gridY);
            }
        }
    }

    /**
     * Compatibility overload for existing callers/tests using raw int[][] scentGrid.
     */
    public void updateSwarm(
            int[][] maze,
            int[][] terrain,
            List<ChaserEntity> chasers,
            int playerGridX,
            int playerGridY,
            boolean playerInStealthBush,
            int[][] scentTrail,
            List<DistractionItem> distractions,
            List<int[]> strayKittens,
            int tileSize,
            long gameTimeMs,
            boolean frenzyActive) {

        ScentTrail temp = null;
        if (scentTrail != null) {
            temp = new ScentTrail(maze.length, maze[0].length, 48, 12000);
            for (int r = 0; r < maze.length; r++) {
                for (int c = 0; c < maze[0].length; c++) {
                    if (scentTrail[r][c] > 0) {
                        temp.deposit(c, r, gameTimeMs);
                    }
                }
            }
        }
        updateSwarm(maze, terrain, chasers, playerGridX, playerGridY, 0, 0, playerInStealthBush, temp, distractions, strayKittens, tileSize, gameTimeMs, frenzyActive);
    }

    public void updateSwarm(
            int[][] maze,
            List<ChaserEntity> chasers,
            int playerGridX,
            int playerGridY,
            int[][] scentTrail,
            List<DistractionItem> distractions,
            List<int[]> strayKittens,
            long now) {
        updateSwarm(maze, null, chasers, playerGridX, playerGridY, false, scentTrail, distractions, strayKittens, 40, now, false);
    }

    private void applyStep(ChaserEntity chaser, int nx, int ny, int tileSize, long now, Set<String> reservedTiles) {
        chaser.previousGridX = chaser.gridX;
        chaser.previousGridY = chaser.gridY;
        chaser.targetGridX = nx;
        chaser.targetGridY = ny;
        chaser.targetPixelX = nx * tileSize;
        chaser.targetPixelY = ny * tileSize;
        chaser.moving = (chaser.targetPixelX != chaser.pixelX || chaser.targetPixelY != chaser.pixelY);
        chaser.lastMoveTime = now;
        reservedTiles.add(nx + "," + ny);
    }

    private int[] pickRandomOpenTile(int[][] maze, int avoidX, int avoidY) {
        int rows = maze.length;
        int cols = maze[0].length;
        List<int[]> candidates = new ArrayList<>();
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (maze[r][c] == 0 && Math.abs(c - avoidX) + Math.abs(r - avoidY) >= 6) {
                    candidates.add(new int[]{c, r});
                }
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        return new int[]{avoidX, avoidY};
    }

    private int[] pickPatrolStep(int[][] maze, int[][] terrain, ChaserEntity chaser, Set<String> reservedTiles) {
        List<int[]> openNeighbors = new ArrayList<>();
        for (int[] d : DIRECTIONS) {
            int nx = chaser.gridX + d[0];
            int ny = chaser.gridY + d[1];
            if (isDogPassable(maze, terrain, nx, ny) && !reservedTiles.contains(nx + "," + ny)) {
                openNeighbors.add(new int[]{nx, ny});
            }
        }

        if (openNeighbors.size() > 1 && chaser.previousGridX != -1) {
            openNeighbors.removeIf(m -> m[0] == chaser.previousGridX && m[1] == chaser.previousGridY);
        }

        if (openNeighbors.isEmpty()) return null;
        return openNeighbors.get(random.nextInt(openNeighbors.size()));
    }

    public List<int[]> findPathBFS(
            int[][] maze,
            int[][] terrain,
            int startX,
            int startY,
            int targetX,
            int targetY,
            Set<String> reservedTiles,
            ChaserEntity telemetryTarget) {

        int rows = maze.length;
        int cols = maze[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int[][] parentX = new int[rows][cols];
        int[][] parentY = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            Arrays.fill(parentX[y], -1);
            Arrays.fill(parentY[y], -1);
        }

        Queue<int[]> queue = new ArrayDeque<>();
        queue.offer(new int[]{startX, startY});
        visited[startY][startX] = true;

        boolean found = false;
        int nodesVisited = 0;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];
            nodesVisited++;

            if (x == targetX && y == targetY) {
                found = true;
                break;
            }

            for (int[] d : DIRECTIONS) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (isDogPassable(maze, terrain, nx, ny) && !visited[ny][nx]) {
                    if (x == startX && y == startY && reservedTiles != null && reservedTiles.contains(nx + "," + ny)) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    parentX[ny][nx] = x;
                    parentY[ny][nx] = y;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        totalNodesExplored += nodesVisited;
        if (telemetryTarget != null) {
            telemetryTarget.lastNodesVisited = nodesVisited;
        }

        if (!found) return null;

        List<int[]> path = reconstructPath(parentX, parentY, startX, startY, targetX, targetY);
        if (telemetryTarget != null && path != null) {
            telemetryTarget.debugPath = path;
        }
        return path;
    }

    public List<int[]> findPathAStar(
            int[][] maze,
            int[][] terrain,
            int startX,
            int startY,
            int targetX,
            int targetY,
            Set<String> reservedTiles,
            ChaserEntity telemetryTarget) {

        int rows = maze.length;
        int cols = maze[0].length;
        int[][] bestG = new int[rows][cols];
        int[][] parentX = new int[rows][cols];
        int[][] parentY = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            Arrays.fill(bestG[y], Integer.MAX_VALUE);
            Arrays.fill(parentX[y], -1);
            Arrays.fill(parentY[y], -1);
        }

        PriorityQueue<AStarNode> minHeap = new PriorityQueue<>();
        bestG[startY][startX] = 0;
        minHeap.offer(new AStarNode(startX, startY, 0, Math.abs(targetX - startX) + Math.abs(targetY - startY)));

        boolean found = false;
        int nodesVisited = 0;

        while (!minHeap.isEmpty()) {
            AStarNode curr = minHeap.poll();
            if (curr.gCost > bestG[curr.y][curr.x]) continue;
            nodesVisited++;

            if (curr.x == targetX && curr.y == targetY) {
                found = true;
                break;
            }

            for (int[] d : DIRECTIONS) {
                int nx = curr.x + d[0];
                int ny = curr.y + d[1];
                if (!isDogPassable(maze, terrain, nx, ny)) continue;
                if (curr.x == startX && curr.y == startY && reservedTiles != null && reservedTiles.contains(nx + "," + ny)) {
                    continue;
                }

                int stepWeight = (terrain != null && terrain[ny][nx] == MazeGraph.TERRAIN_PUDDLE) ? 3 : 1;
                int nextG = curr.gCost + stepWeight;
                if (nextG < bestG[ny][nx]) {
                    bestG[ny][nx] = nextG;
                    parentX[ny][nx] = curr.x;
                    parentY[ny][nx] = curr.y;
                    int h = Math.abs(targetX - nx) + Math.abs(targetY - ny);
                    minHeap.offer(new AStarNode(nx, ny, nextG, nextG + h));
                }
            }
        }

        totalNodesExplored += nodesVisited;
        if (telemetryTarget != null) {
            telemetryTarget.lastNodesVisited = nodesVisited;
        }

        if (!found) return null;

        List<int[]> path = reconstructPath(parentX, parentY, startX, startY, targetX, targetY);
        if (telemetryTarget != null && path != null) {
            telemetryTarget.debugPath = path;
        }
        return path;
    }

    private List<int[]> reconstructPath(int[][] parentX, int[][] parentY, int startX, int startY, int targetX, int targetY) {
        List<int[]> path = new ArrayList<>();
        int cx = targetX;
        int cy = targetY;

        while (cx != startX || cy != startY) {
            path.add(new int[]{cx, cy});
            int px = parentX[cy][cx];
            int py = parentY[cy][cx];
            if (px == -1 || py == -1) return null;
            cx = px;
            cy = py;
        }
        Collections.reverse(path);
        return path;
    }

    public int[] findNextMoveBFS(int[][] maze, int startX, int startY, int targetX, int targetY, Set<String> reservedTiles) {
        List<int[]> path = findPathBFS(maze, null, startX, startY, targetX, targetY, reservedTiles, null);
        return (path != null && !path.isEmpty()) ? path.get(0) : null;
    }

    public int[] nextMove(int[][] maze, int chaserX, int chaserY, int playerX, int playerY, List<DistractionItem> distractions) {
        int[] move = findNextMoveBFS(maze, chaserX, chaserY, playerX, playerY, null);
        return move != null ? move : new int[]{chaserX, chaserY};
    }

    public boolean isOpen(int[][] maze, int x, int y) {
        return y >= 0 && y < maze.length && x >= 0 && x < maze[0].length && maze[y][x] == 0;
    }

    public boolean isDogPassable(int[][] maze, int[][] terrain, int x, int y) {
        if (!isOpen(maze, x, y)) return false;
        return terrain == null || terrain[y][x] != MazeGraph.TERRAIN_CAT_FLAP;
    }
}
