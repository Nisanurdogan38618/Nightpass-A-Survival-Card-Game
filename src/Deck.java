public class Deck {
    // ----- Inner AVL node: ordered by Health (then by order for ties) -----
    private static final class HNode {
        Card card;
        HNode left, right;
        int height;
        int minH, maxH; // subtree health range
        int size;       // subtree size (cards)

        HNode(Card c) {
            this.card = c;
            this.height = 1;
            this.minH = c.getHCur();
            this.maxH = c.getHCur();
            this.size  = 1;
        }
    }

    // ----- Outer AVL node: ordered by Attack (attack "band") -----
    private static final class ANode {
        int A;         // attack key of this band
        HNode rootH;   // root of inner health tree for this band

        // summaries
        int bandMinH, bandMaxH;       // min/max H inside this band
        int subtreeMinH, subtreeMaxH; // min/max H in whole subtree
        int subtreeMinA, subtreeMaxA; // min/max A in whole subtree

        ANode left, right;
        int height;
        int size; // total cards in subtree (sum of inner sizes)

        ANode(Card c) {
            this.A = c.getACur();
            this.rootH = new HNode(c);
            this.height = 1;
            this.bandMinH = c.getHCur();
            this.bandMaxH = c.getHCur();
            this.subtreeMinH = c.getHCur();
            this.subtreeMaxH = c.getHCur();
            this.subtreeMinA = this.subtreeMaxA = this.A;
            this.size = 1;
        }
    }

    private ANode root;
    private int cardCount;
    private int lastPickPriority;

    public Deck() {
        root = null;
        cardCount = 0;
        lastPickPriority = 0;
    }

    public int getCardCount() { return cardCount; }
    public int getLastPickPriority() { return lastPickPriority; }

    // Insert a card by its current attack band; updates counts/summaries
    public void insert(Card c) {
        root = aInsert(root, c);
        cardCount++;
    }

    // Delete a specific card (by A/H/order identity)
    public void delete(Card c) {
        boolean[] removed = new boolean[1]; // out param
        root = aDelete(root, c, removed);
        if (removed[0]) cardCount--;
    }

    // -------- HNode (Health AVL) helpers --------
    private static int hH(HNode n){ return n==null?0:n.height; }
    private static int szH(HNode n){ return n==null?0:n.size; }

    // Recompute height, min/max H, size
    private static void updH(HNode n){
        if (n==null) return;
        n.height = 1 + Math.max(hH(n.left), hH(n.right));
        n.minH = n.maxH = n.card.getHCur();
        if (n.left  != null){ n.minH = Math.min(n.minH, n.left.minH);  n.maxH = Math.max(n.maxH, n.left.maxH); }
        if (n.right != null){ n.minH = Math.min(n.minH, n.right.minH); n.maxH = Math.max(n.maxH, n.right.maxH); }
        n.size = 1 + szH(n.left) + szH(n.right);
    }

    // Order by (H_cur, order) so ties pick earlier inserted first
    private static int cmpH(Card a, Card b){
        if (a.getHCur()!=b.getHCur()) return Integer.compare(a.getHCur(), b.getHCur());
        return Integer.compare(a.getOrder(), b.getOrder());
    }

    private static HNode rotHRight(HNode y){
        HNode x = y.left, t2 = x.right;
        x.right = y; y.left = t2;
        updH(y); updH(x);
        return x;
    }

    private static HNode rotHLeft(HNode x){
        HNode y = x.right, t2 = y.left;
        y.left = x; x.right = t2;
        updH(x); updH(y);
        return y;
    }

    private static int bfH(HNode n){ return n==null?0:hH(n.left)-hH(n.right); }
    private static HNode balH(HNode n){
        if (n==null) return null;
        int bf = bfH(n);
        if (bf > 1){
            if (bfH(n.left) < 0) n.left = rotHLeft(n.left);
            return rotHRight(n);
        }
        if (bf < -1){
            if (bfH(n.right) > 0) n.right = rotHRight(n.right);
            return rotHLeft(n);
        }
        return n;
    }

    // Insert card into health-ordered AVL
    private static HNode hInsert(HNode n, Card c){
        if (n==null) return new HNode(c);
        if (cmpH(c, n.card) < 0) n.left = hInsert(n.left, c);
        else                     n.right = hInsert(n.right, c);
        updH(n);
        return balH(n);
    }

    private static HNode hMinNode(HNode n){
        while(n!=null && n.left!=null) n = n.left;
        return n;
    }

    // Delete card from health-ordered AVL
    private static HNode hDelete(HNode n, Card c, boolean[] removed){
        if (n==null) return null;
        int cmp = cmpH(c, n.card);
        if (cmp < 0) n.left = hDelete(n.left, c, removed);
        else if (cmp > 0) n.right = hDelete(n.right, c, removed);
        else {
            removed[0] = true;
            if (n.left==null) return n.right;
            if (n.right==null) return n.left;
            HNode succ = hMinNode(n.right);
            n.card = succ.card;
            n.right = hDelete(n.right, succ.card, new boolean[1]);
        }
        updH(n);
        return balH(n);
    }

    // First card with H >= keyH (within a band)
    private static Card hLowerBound(HNode n, int keyH){
        Card ans = null;
        while(n!=null){
            int h = n.card.getHCur();
            if (h >= keyH){
                if (ans==null || cmpH(n.card, ans) < 0) ans = n.card;
                n = n.left;
            } else {
                n = n.right;
            }
        }
        return ans;
    }

    // Minimum (by H then order) card in a band
    private static Card hMinCard(HNode n){
        HNode m = hMinNode(n);
        return (m==null?null:m.card);
    }

    // -------- ANode (Attack AVL) helpers --------
    private static int hA(ANode n){ return n==null?0:n.height; }
    private static int szA(ANode n){ return n==null?0:n.size; }

    private static int min3(int a, int b, int c){ return Math.min(a, Math.min(b,c)); }
    private static int max3(int a, int b, int c){ return Math.max(a, Math.max(b,c)); }

    private static void pullBandSummary(ANode n){
        if (n.rootH == null){ n.bandMinH = Integer.MAX_VALUE; n.bandMaxH = Integer.MIN_VALUE; }
        else { n.bandMinH = n.rootH.minH; n.bandMaxH = n.rootH.maxH; }
    }

    // Recompute height, subtree summaries and size
    private static void updA(ANode n){
        if (n==null) return;
        n.height = 1 + Math.max(hA(n.left), hA(n.right));
        pullBandSummary(n);

        int lMin = (n.left ==null)? Integer.MAX_VALUE : n.left.subtreeMinH;
        int rMin = (n.right==null)? Integer.MAX_VALUE : n.right.subtreeMinH;
        int lMax = (n.left ==null)? Integer.MIN_VALUE : n.left.subtreeMaxH;
        int rMax = (n.right==null)? Integer.MIN_VALUE : n.right.subtreeMaxH;
        n.subtreeMinH = min3(lMin, n.bandMinH, rMin);
        n.subtreeMaxH = max3(lMax, n.bandMaxH, rMax);

        int lMinA = (n.left ==null)? n.A : Math.min(n.left.subtreeMinA, n.A);
        int rMinA = (n.right==null)? n.A : Math.min(n.right.subtreeMinA, n.A);
        int lMaxA = (n.left ==null)? n.A : Math.max(n.left.subtreeMaxA, n.A);
        int rMaxA = (n.right==null)? n.A : Math.max(n.right.subtreeMaxA, n.A);
        n.subtreeMinA = Math.min(lMinA, rMinA);
        n.subtreeMaxA = Math.max(lMaxA, rMaxA);

        n.size = (n.rootH==null?0:n.rootH.size)
                + ((n.left==null)?0:n.left.size)
                + ((n.right==null)?0:n.right.size);
    }

    private static int bfA(ANode n){ return n==null?0:hA(n.left)-hA(n.right); }

    private static ANode rotARight(ANode y){
        ANode x = y.left, t2 = x.right;
        x.right = y; y.left = t2;
        updA(y); updA(x);
        return x;
    }
    private static ANode rotALeft(ANode x){
        ANode y = x.right, t2 = y.left;
        y.left = x; x.right = t2;
        updA(x); updA(y);
        return y;
    }
    private static ANode balA(ANode n){
        if (n==null) return null;
        int bf = bfA(n);
        if (bf > 1){
            if (bfA(n.left) < 0) n.left = rotALeft(n.left);
            return rotARight(n);
        }
        if (bf < -1){
            if (bfA(n.right) > 0) n.right = rotARight(n.right);
            return rotALeft(n);
        }
        return n;
    }

    // Insert card into proper attack band; create band or insert into its H tree
    private static ANode aInsert(ANode n, Card c){
        if (n==null) return new ANode(c);
        if (c.getACur() < n.A) n.left  = aInsert(n.left,  c);
        else if (c.getACur() > n.A) n.right = aInsert(n.right, c);
        else {
            n.rootH = hInsert(n.rootH, c);
        }
        updA(n);
        return balA(n);
    }

    private static ANode minANode(ANode n){
        while(n!=null && n.left!=null) n = n.left;
        return n;
    }

    // Delete a specific card; if band becomes empty, remove the band node
    private static ANode aDelete(ANode n, Card c, boolean[] removed){
        if (n==null) return null;
        if (c.getACur() < n.A) n.left  = aDelete(n.left, c, removed);
        else if (c.getACur() > n.A) n.right = aDelete(n.right, c, removed);
        else {
            n.rootH = hDelete(n.rootH, c, removed);
            if (n.rootH == null){
                // remove this band node
                if (n.left == null) return n.right;
                if (n.right== null) return n.left;
                // two children: replace by successor band
                ANode succ = minANode(n.right);
                n.A = succ.A;
                n.rootH = succ.rootH;  // move over the entire band tree
                // delete that band node (by A only)
                n.right = aDeleteBandByA(n.right, succ.A);
            }
        }
        updA(n);
        return balA(n);
    }

    // Delete a band node by A key (used when we promoted successor)
    private static ANode aDeleteBandByA(ANode n, int aKey){
        if (n==null) return null;
        if (aKey < n.A) n.left = aDeleteBandByA(n.left, aKey);
        else if (aKey > n.A) n.right = aDeleteBandByA(n.right, aKey);
        else {
            if (n.left == null) return n.right;
            if (n.right== null) return n.left;
            ANode succ = minANode(n.right);
            n.A = succ.A;
            n.rootH = succ.rootH;
            n.right = aDeleteBandByA(n.right, succ.A);
        }
        updA(n);
        return balA(n);
    }

    // ----- Priority band finders (P1..P4) -----
    // P1: A >= strangerHealth AND exists H >= strangerAttack+1
    private static ANode bandForP1(ANode n, int strangerHealth, int needHealth){
        if (n==null) return null;
        if (n.subtreeMaxH < needHealth) return null;

        if (n.A >= strangerHealth){
            ANode ans = null;
            if (n.left != null && n.left.subtreeMaxA >= strangerHealth && n.left.subtreeMaxH >= needHealth){
                ans = bandForP1(n.left, strangerHealth, needHealth);
            }
            if (ans != null) return ans;
            if (n.bandMaxH >= needHealth) return n;
            return bandForP1(n.right, strangerHealth, needHealth);
        } else {
            return bandForP1(n.right, strangerHealth, needHealth);
        }
    }

    // P2: A < strangerHealth BUT exists H >= strangerAttack+1
    private static ANode bandForP2(ANode n, int strangerHealth, int needHealth){
        if (n==null) return null;
        if (n.subtreeMaxH < needHealth) return null;

        if (n.A >= strangerHealth){
            return bandForP2(n.left, strangerHealth, needHealth);
        } else {
            ANode ans = null;
            if (n.right != null && n.right.subtreeMinA < strangerHealth && n.right.subtreeMaxH >= needHealth){
                ans = bandForP2(n.right, strangerHealth, needHealth);
                if (ans != null) return ans;
            }
            if (n.bandMaxH >= needHealth) return n;
            return bandForP2(n.left, strangerHealth, needHealth);
        }
    }

    // P3: can kill (A >= strangerHealth) but cannot survive (needs min H <= strangerAttack)
    private static ANode bandForP3(ANode n, int strangerHealth, int strangerAttack){
        if (n==null) return null;
        if (n.subtreeMinH > strangerAttack) return null;

        if (n.A >= strangerHealth){
            ANode ans = null;
            if (n.left != null && n.left.subtreeMaxA >= strangerHealth && n.left.subtreeMinH <= strangerAttack){
                ans = bandForP3(n.left, strangerHealth, strangerAttack);
            }
            if (ans != null) return ans;
            if (n.bandMinH <= strangerAttack) return n;
            return bandForP3(n.right, strangerHealth, strangerAttack);
        } else {
            return bandForP3(n.right, strangerHealth, strangerAttack);
        }
    }

    // P4: fallback = rightmost band (largest A)
    private static ANode bandForP4(ANode n){
        if (n==null) return null;
        while(n.right != null) n = n.right;
        return n;
    }

    // ----- Priority pickers: return the actual card from the chosen band -----
    private Card p1(int strangerAttack, int strangerHealth){
        ANode band = bandForP1(root, strangerHealth, strangerAttack+1);
        if (band == null) return null;
        return hLowerBound(band.rootH, strangerAttack+1);
    }

    private Card p2(int strangerAttack, int strangerHealth){
        ANode band = bandForP2(root, strangerHealth, strangerAttack+1);
        if (band == null) return null;
        return hLowerBound(band.rootH, strangerAttack+1);
    }

    private Card p3(int strangerAttack, int strangerHealth){
        ANode band = bandForP3(root, strangerHealth, strangerAttack);
        if (band == null) return null;
        Card minInBand = hMinCard(band.rootH);
        if (minInBand != null && minInBand.getHCur() <= strangerAttack) return minInBand;
        return null;
    }

    private Card p4(){
        ANode band = bandForP4(root);
        if (band == null) return null;
        return hMinCard(band.rootH);
    }

    // Public API: find the optimal battle card in priority order (1..4)
    public Card findOptimalBattleCard(int strangerAttack, int strangerHealth){
        Card r = p1(strangerAttack, strangerHealth); if (r!=null){ lastPickPriority=1; return r; }
        r = p2(strangerAttack, strangerHealth);      if (r!=null){ lastPickPriority=2; return r; }
        r = p3(strangerAttack, strangerHealth);      if (r!=null){ lastPickPriority=3; return r; }
        r = p4();                                    if (r!=null){ lastPickPriority=4; return r; }
        lastPickPriority = 0;
        return null;
    }

    // ----- Steal selection: strict limits (>, >), earliest by order on ties -----
    private static ANode bandForSteal(ANode n, int minAExclusive, int healthLimit){
        if (n==null) return null;
        if (n.subtreeMaxH <= healthLimit) return null; // strictly greater than limit

        if (n.A >= minAExclusive){
            ANode ans = null;
            if (n.left != null && n.left.subtreeMaxA >= minAExclusive && n.left.subtreeMaxH > healthLimit){
                ans = bandForSteal(n.left, minAExclusive, healthLimit);
            }
            if (ans != null) return ans;
            if (n.bandMaxH > healthLimit) return n;
            return bandForSteal(n.right, minAExclusive, healthLimit);
        } else {
            return bandForSteal(n.right, minAExclusive, healthLimit);
        }
    }

    public Card findBestStealCandidate(int attackLimit, int healthLimit){
        ANode band = bandForSteal(root, attackLimit+1, healthLimit);
        if (band == null) return null;
        return hLowerBound(band.rootH, healthLimit+1);
    }

    // ----- Sanity checker for counts (optional debug) -----
    public void verifyCount(){
        int real = countAll(root);
        if (real != cardCount){
            System.out.println("[VERIFY] Mismatch: real="+real+" stored="+cardCount);
        }
    }

    private static int countAll(ANode n){
        if (n==null) return 0;
        return countAll(n.left) + countH(n.rootH) + countAll(n.right);
    }
    private static int countH(HNode n){
        if (n==null) return 0;
        return 1 + countH(n.left) + countH(n.right);
    }
}