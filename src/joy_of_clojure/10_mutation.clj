(ns joy-of-clojure.10-mutation
  (:import java.util.concurrent.Executors))

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

(defn good-move?
  [to enemy-sq]
  (when (not= to enemy-sq)
    to))

(defn choose-move
  "Randomly choose a legal move"
  [[[mover mpos] [_ enemy-pos]]]
  [mover (some #(good-move? % enemy-pos)
               (shuffle (king-moves mpos)))])

(some #(good-move? % 1) [1 1 1 5]) ; returns first matching result, i.e. 5

(reset-board!)
(take 5 (repeatedly #(choose-move @to-move))) ; to-move is a ref, so we extract/de-ref with @

; create a function to make a random move for the piece at the front of to-move
(defn place [from to] to)

(defn move-piece [[piece dest] [[_ src] _]]
  (alter (get-in board dest) place piece)
  (alter (get-in board src) place :-)
  (alter num-moves inc))

(defn update-to-move [move]
  (alter to-move #(vector (second %) move))) ; fn passed to alter receives current value of the thing you are alter-ing

(choose-move @to-move)

(defn make-move []
  (let [move (choose-move @to-move)] ; choose a random valid move for a piece
    (dosync (move-piece move @to-move)) ; move it
    (dosync (update-to-move move)))) ; update to-move to the other value (White to Black, Black to White, etc.), take turns

(reset-board!)
(make-move)
(board-map deref board)

(def thread-pool
  (Executors/newFixedThreadPool
    (+ 2 (.availableProcessors (Runtime/getRuntime)))))

(defn dothreads!
  [f & {thread-count :threads
        exec-count   :times
        :or {thread-count 1 exec-count 1}}]
  (dotimes [t thread-count]
    (.submit thread-pool
             #(dotimes [_ exec-count] (f)))))

(dothreads! #(.print System/out "Hi ") :threads 2 :times 2)

(reset-board!)
(dothreads! make-move :threads 100 :times 100) 
(board-map deref board)
; fuuuuuck this breaks shit... why?
; Because! we use two separate dosync calls in make-move, giving us separate transactions which happen in different threads

; but what exactly is a transaction? more specifically, what is software transactional memory?

; 10.1.2

