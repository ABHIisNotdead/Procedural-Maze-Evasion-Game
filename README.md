# Procedural-Maze-Evasion-Game
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

*(Developing this functional game satisfies the Module 7 mandate for a group project applying data structures).*

---

## 👥 Team Roles & Responsibilities

*   **Member 1 (Darshan):** Maze Generation Architecture (Backend Data Structures)
*   **Member 2 (Abhishek/TBD):** AI Pathfinding Engine (Backend Algorithms)
*   **Member 3 (Neha):** Visuals & Academic Documentation (Frontend UI)
*   **Member 4 (Abhishek):** Core Engine Integration & Game Loop

---

## 🚀 Setup & Contribution Guide

### Prerequisites
*   [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) installed.
*   Java Development Kit (JDK) installed.
*   Git installed on your system.

### How to Start
1. **Clone the repository:** 
   `git clone <repository_link_here>`
2. **Open in IntelliJ:** Open IntelliJ IDEA -> `File` -> `Open` -> Select the cloned project folder.
3. **Coding Sessions:** We will use Google Meet for live coding and debugging.
4. **Tasks:** Check the GitHub **Issues** tab for your specific To-Do list.

### Pushing Code
If you are stuck, push what you have so far, and we will integrate and fix it together on the Google Meet call!
