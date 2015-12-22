(ns joy-of-clojure.10-mutation)

; 10.1 When to use refs

(def initial-board
  [[:- :k :-]
   [:- :- :-]
   [:- :K :-]])

(defn board-map [f board]
  (vec (map #(vec (for [s %] (f s)))
            board)))
(board-map
  (fn [v] :-)
  initial-board)

(defn reset-board!
  "Resets the board state."
  []
  (def board (board-map ref initial-board))
  (def to-move (ref [[:K [2 1]] [:k [0 1]]]))
  (def num-moves (ref 0)))

(defn neighbors
  ([size yx] (neighbors [[-1 0] [1 0] [0 -1] [0 1]]
                        size
                        yx))
  ([deltas size yx]
   (filter (fn [new-yx]
             (every? #(< -1 % size) new-yx)) ; remove illegal neighbors
           (map #(vec (map + yx %)) ; get all (including illegal) neighbors for provided point
                deltas))))

(map + [1 1] [-1 0])
(map #(vec (map + [1 1] %))
     [[-1 0] [1 0]])

(def king-moves
  (partial neighbors
           [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] 3))
