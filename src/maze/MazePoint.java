package maze;

import java.util.Objects;

/**
 * Immutable 2D integer coordinate point representing a tile position.
 */
public class MazePoint {
    public final int x;
    public final int y;

    public MazePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int manhattanDistance(MazePoint other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    public double euclideanDistance(MazePoint other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MazePoint mazePoint = (MazePoint) o;
        return x == mazePoint.x && y == mazePoint.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
