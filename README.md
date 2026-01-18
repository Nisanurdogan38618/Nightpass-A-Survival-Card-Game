# Nightpass: A Survival Card Game (Basic Edition)

## 📖 Project Overview
Nightpass is a turn-based survival card game developed in **Java**. The game simulates a desperate struggle between "The Survivor" (the player) and a mysterious entity known as "The Stranger."

This project implements the **Type-1 (Basic)** mechanics of the game, focusing on strategic deck management, algorithmic battle priorities, and score tracking without the healing/revival systems.

## ⚔️ Game Mechanics
The core of the game is the nightly duel. The system processes commands to manage the deck and simulate battles based on strict algorithmic priorities:

### 1. Battle Logic (Priority System)
When The Stranger reveals a card, the system automatically selects the best counter-card from the deck based on these priorities:
1.  **Survive and Kill:** Defeat the opponent without dying.
2.  **Survive:** Defense is prioritized if killing isn't possible.
3.  **Kill:** Sacrifice the card to ensure the opponent is defeated.
4.  **Max Damage:** Inflict maximum damage if no other option exists.

### 2. The Stranger's Tactics
* **Steal Card:** The Stranger can remove specific cards from the player's deck based on attack/health limits.
* **Scoring:** Points are awarded based on damage dealt and kills.

## 🚀 Supported Commands
The system reads from an input file and executes the following Type-1 commands:
* `draw_card`: Adds a new card to the Survivor's deck with specific Attack and Health stats.
* `battle`: Initiates a duel against The Stranger (Healing pool is disabled in this version).
* `steal_card`: The Stranger removes a card fitting specific criteria from the deck.
* `deck_count`: Displays the number of active cards in the deck.
* `find_winning`: Calculates and displays the current winner based on total score.

## 🛠️ Technical Details
* **Language:** Java
* **Data Structures:** Implemented using **ArrayList** only (Strict constraint: No other Java Collections used).
* **Input/Output:** Processes commands via File I/O (`input.txt` -> `output.txt`).
* **Complexity:** Optimized to handle large datasets of commands efficiently.

## 💻 How to Run
1.  Clone the repository:
    ```bash
    git clone [https://github.com/Nisanurdogan38618/Freelancer-Customer-Matching.git](https://github.com/Nisanurdogan38618/Freelancer-Customer-Matching.git)
    ```
    *(Note: Repository name may vary, ensure you are in the project directory)*

2.  Navigate to the source folder:
    ```bash
    cd src
    ```

3.  Compile the project:
    ```bash
    javac *.java
    ```

4.  Run with input/output files:
    ```bash
    java Main input.txt output.txt
    ```

---
*This project was developed as part of the CMPE250 Data Structures and Algorithms course (Project 1 - Type 1 Implementation).*
