# 🕹️ Procedural Maze Evasion Game (Java)

A fully playable 2D arcade survival game built in pure Java. This project serves as our ADS (Advanced Data Structures) Mini-Project, demonstrating the practical application of Graphs, Trees, and Linear Data Structures through procedural environment generation and dynamic shortest-path AI routing.

## 🎮 How the Game Works

1. **The Environment:** A top-down, 2D maze constructed from textured sprites. The maze is **procedurally generated** from scratch every time the game starts, guaranteeing a unique layout for every playthrough.
2. **The Objective:** Control the character using WASD or Arrow keys to navigate from the starting point to the designated exit portal on the opposite side of the maze.
3. **The Threat:** An automated "Chaser" enemy spawns inside the maze. It constantly hunts the player using algorithmic pathfinding to calculate the absolute shortest route.
4. **The Outcome:** Outmaneuver the Chaser and reach the exit! If the Chaser intercepts the player's coordinates, the game ends.

---

## ⚙️ Technical Architecture & ADS Implementation

This game engine runs entirely on the data structures covered in our syllabus:

*   **Graph Storage Structure (Adjacency Matrix):** The visual game board is mathematically backed by a 2D integer array to store coordinates of walls and open paths. *(Fulfills Module 6 / Practical 9)*
*   **Minimum Spanning Tree (Maze Generation):** Utilizes Kruskal's or Prim's algorithm to compute an MST across the matrix, ensuring the maze is perfectly connected without infinite loops. *(Fulfills Module 6 / Practical 11)*
*   **Graph Traversal (AI Pathfinding):** The Chaser enemy utilizes Breadth-First Search (BFS) to dynamically recalculate the shortest path to the player's coordinates on every background tick. *(Fulfills Module 6 / Practical 10 & Self-Learning Topic)*
*   **Linear Data Structures (Queues):** A custom implementation of an Ordinary Queue is used to execute the BFS pathfinding level by level. *(Fulfills Module 3 / Practical 3)*
*   **Java UI Integration:** Built using Java Swing. A `javax.swing.Timer` thread runs the game loop, allowing the AI to move independently of the `KeyListener` handling the player. Sprites are drawn using `Graphics2D`.

---

## 👥 Task Delegation & Responsibilities

### Member 1: Maze Generation Architecture (Backend Data Structures)
*   **Target File:** `MazeGraph.java`
*   **Task:** You are responsible for generating the layout of the maze. You must create a 2D integer array (Adjacency Matrix) to represent a 20x15 grid.
*   **Algorithm Required:** Implement either Kruskal's algorithm or Prim's algorithm to carve the paths through the matrix, ensuring it is a perfect maze with no isolated rooms or closed loops.
*   **Deliverable:** Send a `.java` class containing a method that returns a completed 2D array (e.g., `1` for walls, `0` for open paths).

### Member 2: AI Pathfinding Engine (Backend Algorithms)
*   **Target File:** `ChaserAI.java`
*   **Task:** You are responsible for the enemy's brain. You must write an algorithm that calculates the absolute shortest path from the enemy's current coordinates to the player's current coordinates.
*   **Algorithm Required:** Implement Breadth-First Search (BFS). You must use a standard Ordinary Queue to process the graph nodes level by level.
*   **Deliverable:** Send a `.java` class with a method that accepts a 2D matrix, a start `(x, y)`, and a target `(x, y)`, and returns the coordinates of the single next tile the enemy should move to.

### Member 3: Visuals & Academic Documentation (Frontend)
*   **Target File:** `GameWindow.java` & `Project_Report.docx`
*   **Task:** You are responsible for drawing the game on the screen and writing the final submission document. Use Java Swing (`JPanel` and `Graphics2D`) to write a script that reads a 20x15 2D array and draws colored squares (or `.png` images) on the screen.
*   **Documentation Required:** Write the complete project report required for our continuous assessment. This includes documenting the time complexities of Kruskal's/Prim's and the BFS algorithms used by Members 1 and 2, drawing the flowcharts, and creating the final presentation slides.
*   **Deliverable:** Send the Java Swing rendering class and the finalized Word document/PPT.

### Member 4: Core Engine Integration & Game Loop
*   **Target File:** `Main.java` & `GameLoop.java`
*   **Task:** Lead Integrator. Once all the members send their individual `.java` files, I will wire the backend math (Member 1 & 2) to the frontend UI (Member 3).
*   **Additional Logic:** I will write the `javax.swing.Timer` thread for the background game loop, implement the Keyboard Listeners for player movement, handle the collision detection logic so the player cannot walk through walls, and debug the final compiled application.

---

## 🚀 Setup & Contribution Guide

### Prerequisites
*   [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) installed.
*   Java Development Kit (JDK) installed.
*   Git installed on your system.

### How to Start
1. **Clone the repository:** `git clone <repository_link_here>`
2. **Open in IntelliJ:** Open IntelliJ IDEA -> `File` -> `Open` -> Select the cloned project folder.
3. **Coding Sessions:** We will use Google Meet for live coding and debugging.
4. **Pushing Code:** If you are stuck, push what you have so far, and we will integrate and fix it together on the Google Meet call!
