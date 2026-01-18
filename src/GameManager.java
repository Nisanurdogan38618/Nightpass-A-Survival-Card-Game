public class GameManager {
    private Deck deck;
    private int survivorPoints;
    private int strangerPoints;
    private int entryCounter;

    // Constructor: initialize deck and score counters
    public GameManager() {
        this.deck = new Deck();
        this.survivorPoints = 0;
        this.strangerPoints = 0;
        this.entryCounter = 1;
    }

    /**
     * Handle drawing a new card into the deck.
     * Creates a Card, assigns it an order, and inserts it.
     */
    public String handleDrawCard(String name, int att, int hp) {
        Card newCard = new Card(name, att, hp, this.entryCounter);
        this.entryCounter++;
        deck.insert(newCard);
        return "Added " + name + " to the deck\n";
    }

    /**
     * Handle deck count query.
     * Ensures internal consistency (verifyCount) and returns current size.
     */
    public String handleDeckCount() {
        deck.verifyCount();
        int count = deck.getCardCount();
        return "Number of cards in the deck: " + count + "\n";
    }

    /**
     * Return the winning side at the end of the game
     * (Survivor wins ties).
     */
    public String handleFindWinning() {
        if (this.survivorPoints >= this.strangerPoints) {
            return "The Survivor, Score: " + this.survivorPoints + "\n";
        } else {
            return "The Stranger, Score: " + this.strangerPoints + "\n";
        }
    }

    /**
     * Handle stealing a card:
     * Find candidate with A_cur > attackLimit and H_cur > healthLimit.
     * If found, remove it from deck and report; else return "No card".
     */
    public String handleStealCard(int attackLimit, int healthLimit) {
        Card stolenCard = deck.findBestStealCandidate(attackLimit, healthLimit);

        if (stolenCard == null) {
            return "No card to steal\n";
        } else {
            deck.delete(stolenCard);
            return "The Stranger stole the card: " + stolenCard.getName() + "\n";
        }
    }

    /**
     * Handle a battle between Stranger and Survivor's optimal card.
     * Determines priority class (1–4), resolves damage, updates scores,
     * and reinserts or discards the played card depending on survival.
     */
    public String handleBattle(int strangerAttack, int strangerHealth, int healPoolAmount) {
        Card playedCard = deck.findOptimalBattleCard(strangerAttack, strangerHealth);

        if (playedCard == null) {
            updateScores("Stranger", 2);
            return "No cards to play, 0 cards revived\n";
        }

        String cardName = playedCard.getName();

        // Initial stats
        int H_cur_initial = playedCard.getHCur();
        int A_cur_initial = playedCard.getACur();
        int H_base = playedCard.getHBase();
        int A_base = playedCard.getABase();

        // Determine battle priority (survive/kill conditions)
        boolean survives_initial = H_cur_initial > strangerAttack;
        boolean kills_initial = A_cur_initial >= strangerHealth;
        int priority = 0;
        if (survives_initial && kills_initial) priority = 1;
        else if (survives_initial) priority = 2;
        else if (kills_initial) priority = 3;
        else priority = 4;

        // Apply damage
        int H_cur_final = H_cur_initial - strangerAttack;
        int H_stranger_final = strangerHealth - A_cur_initial;

        int survivorScore = 0;
        int strangerScore = 0;

        // Survivor card damage results
        if (H_cur_final <= 0) {
            strangerScore += 2; // card dies
        } else if (H_cur_final < H_base) {
            strangerScore += 1; // card damaged but survives
        }

        // Stranger damage results
        if (H_stranger_final <= 0) {
            survivorScore += 2; // stranger defeated
        } else if (H_stranger_final < strangerHealth) {
            survivorScore += 1; // stranger damaged but survives
        }

        // Update global scores
        updateScores("Survivor", survivorScore);
        updateScores("Stranger", strangerScore);

        String outputMessage;

        if (H_cur_final <= 0) {
            // Card is discarded (dies)
            deck.delete(playedCard);
            outputMessage = String.format(
                    "Found with priority %d, Survivor plays %s, the played card is discarded, 0 cards revived\n",
                    priority, cardName);
        } else {
            // Card survives: update stats and reinsert with new order
            deck.delete(playedCard);
            playedCard.setHCur(H_cur_final);

            long prod = (long) playedCard.getABase() * playedCard.getHCur();
            int newACur = (int) (prod / playedCard.getHBase());
            playedCard.setACur(Math.max(1, newACur));

            playedCard.setOrder(++this.entryCounter);
            deck.insert(playedCard);

            outputMessage = String.format(
                    "Found with priority %d, Survivor plays %s, the played card returned to deck, 0 cards revived\n",
                    priority, cardName);
        }

        return outputMessage;
    }

    // Internal helper: add points to Survivor or Stranger
    private void updateScores(String winner, int points) {
        if (winner.equals("Survivor")) {
            this.survivorPoints += points;
        } else if (winner.equals("Stranger")) {
            this.strangerPoints += points;
        }
    }
}