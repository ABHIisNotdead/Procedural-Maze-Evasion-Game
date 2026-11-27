package logic;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RunHistory {

    private static final String FILE_NAME = "run_history.csv";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static class Record {
        public final String timestamp;
        public final long seed;
        public final int score;
        public final String result;   // "GAME OVER" or "ESCAPED"
        public final String mazeSize; // e.g. "15x21"

        public Record(String timestamp, long seed, int score, String result, String mazeSize) {
            this.timestamp = timestamp;
            this.seed = seed;
            this.score = score;
            this.result = result;
            this.mazeSize = mazeSize;
        }
    }

    public static void saveRun(long seed, int score, String result, int rows, int cols) {
        String time = LocalDateTime.now().format(FMT);
        String line = time + "," + seed + "," + score + "," + result + "," + rows + "x" + cols;
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            out.println(line);
        } catch (IOException e) {
            System.out.println("Failed to save run history: " + e.getMessage());
        }
    }

    public static List<Record> loadRuns() {
        List<Record> list = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    list.add(new Record(
                            parts[0],
                            Long.parseLong(parts[1]),
                            Integer.parseInt(parts[2]),
                            parts[3],
                            parts[4]
                    ));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Failed to load run history: " + e.getMessage());
        }

        Collections.reverse(list); // Most recent first
        return list;
    }

    /**
     * Returns Top-K highest-scoring runs using a Max-Heap (PriorityQueue).
     * Demonstrates Module 4/5 Heap Priority Queue sorting in O(N log N).
     */
    public static List<Record> loadTopRuns(int limit) {
        List<Record> all = loadRuns();
        java.util.PriorityQueue<Record> maxHeap = new java.util.PriorityQueue<>(
                (a, b) -> Integer.compare(b.score, a.score)
        );
        maxHeap.addAll(all);

        List<Record> top = new ArrayList<>();
        while (!maxHeap.isEmpty() && top.size() < limit) {
            top.add(maxHeap.poll());
        }
        return top;
    }

    public static void clearHistory() {
        File file = new File(FILE_NAME);
        if (file.exists()) file.delete();
    }
}