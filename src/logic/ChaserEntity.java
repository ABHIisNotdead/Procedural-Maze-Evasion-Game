package logic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an individual Dog Chaser in the Multi-Chaser Swarm.
 * Uses pause-safe gameTimeMs for stun and movement cooldowns.
 */
public class ChaserEntity {

    public final int id;
    public final ChaserType type;
    public final String name;
    public ChaserState state;

    // Grid coordinates
    public int gridX;
    public int gridY;
    public int targetGridX;
    public int targetGridY;
    public int previousGridX = -1;
    public int previousGridY = -1;

    // World pixel coordinates for smooth interpolation
    public int pixelX;
    public int pixelY;
    public int targetPixelX;
    public int targetPixelY;

    // Movement & Animation
    public boolean moving = false;
    public String direction = "down";
    public int animFrame = 0;
    public int animTimer = 0;

    // Timing & Speed
    public int speed;
    public int delayMs;
    public long lastMoveTime = 0;
    public int detectionRadius;

    // Home Kennel Spawn for RETRIEVING state
    public int homeGridX;
    public int homeGridY;

    // FSM State Flags
    public boolean carryingKitten = false;
    public long stunnedUntil = 0;

    // Emotion / Speech Bubble
    public String speechBubble = null;
    public Color speechColor = Color.WHITE;

    // Live ADS X-Ray Visualizer telemetry
    public List<int[]> debugPath = new ArrayList<>();
    public int lastNodesVisited = 0;

    public ChaserEntity(int id, ChaserType type, int startGridX, int startGridY, int tileSize,
                        int speed, int delayMs, int detectionRadius) {
        this.id = id;
        this.type = type;
        this.name = type.displayName;
        this.state = ChaserState.PATROLLING;

        this.gridX = startGridX;
        this.gridY = startGridY;
        this.targetGridX = startGridX;
        this.targetGridY = startGridY;
        this.homeGridX = startGridX;
        this.homeGridY = startGridY;

        this.pixelX = startGridX * tileSize;
        this.pixelY = startGridY * tileSize;
        this.targetPixelX = this.pixelX;
        this.targetPixelY = this.pixelY;

        this.speed = speed;
        this.delayMs = delayMs;
        this.detectionRadius = detectionRadius;
    }

    public boolean isStunned(long gameTimeMs) {
        return gameTimeMs < stunnedUntil;
    }

    public boolean isStunned() {
        return System.currentTimeMillis() < stunnedUntil;
    }

    public boolean stun(long durationMs, long gameTimeMs) {
        this.stunnedUntil = gameTimeMs + durationMs;
        this.state = ChaserState.STUNNED;
        this.speechBubble = "*STUN*";
        this.speechColor = new Color(241, 196, 15);
        boolean droppedKitten = carryingKitten;
        this.carryingKitten = false;
        this.debugPath.clear();
        return droppedKitten;
    }

    public void stun(long durationMs) {
        stun(durationMs, System.currentTimeMillis());
    }

    public void reset(int startGridX, int startGridY, int tileSize) {
        this.gridX = startGridX;
        this.gridY = startGridY;
        this.targetGridX = startGridX;
        this.targetGridY = startGridY;
        this.homeGridX = startGridX;
        this.homeGridY = startGridY;
        this.pixelX = startGridX * tileSize;
        this.pixelY = startGridY * tileSize;
        this.targetPixelX = this.pixelX;
        this.targetPixelY = this.pixelY;
        this.moving = false;
        this.state = ChaserState.PATROLLING;
        this.direction = "down";
        this.animFrame = 0;
        this.animTimer = 0;
        this.carryingKitten = false;
        this.stunnedUntil = 0;
        this.lastMoveTime = 0;
        this.speechBubble = null;
        this.previousGridX = -1;
        this.previousGridY = -1;
        this.debugPath.clear();
        this.lastNodesVisited = 0;
    }

    /**
     * Steps pixel position toward targetPixel. Returns true if reached target.
     */
    public boolean updatePixelStep(int stepSpeed) {
        if (!moving) return true;

        if (pixelX < targetPixelX) {
            pixelX = Math.min(pixelX + stepSpeed, targetPixelX);
            direction = "right";
        } else if (pixelX > targetPixelX) {
            pixelX = Math.max(pixelX - stepSpeed, targetPixelX);
            direction = "left";
        }

        if (pixelY < targetPixelY) {
            pixelY = Math.min(pixelY + stepSpeed, targetPixelY);
            direction = "down";
        } else if (pixelY > targetPixelY) {
            pixelY = Math.max(pixelY - stepSpeed, targetPixelY);
            direction = "up";
        }

        animTimer++;
        if (animTimer > 8) {
            animFrame = (animFrame + 1) % 4;
            animTimer = 0;
        }

        if (pixelX == targetPixelX && pixelY == targetPixelY) {
            gridX = targetGridX;
            gridY = targetGridY;
            moving = false;
            return true;
        }
        return false;
    }

    public boolean updatePixelStep() {
        return updatePixelStep(this.speed);
    }
}
