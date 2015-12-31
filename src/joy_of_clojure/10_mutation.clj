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

; Transactions
; fix board example
(defn make-move-v2 []
  (dosync
    (let [move (choose-move @to-move)]
      (move-piece move @to-move)
      (update-to-move move))))

(reset-board!)
(dothreads! make-move-v2 :threads 100 :times 100) 
(board-map #(dosync (deref %)) board)
(prn @to-move)
(prn @num-moves)

; Commutative change with commute
; sometimes the value of a ref in a given transaction isn't important to its completion semantics, i.e. a counter (num-moves)
; where the current value is irrelevant to how it should be incremented. can use `commute` in these instances

; using `commute` instead of `alter`:
; this will work for move-piece above, but not for update-to-move, because order is important for update-to-move
; commute also runs provided function `at least` twice during the course of a transaction (needs to be idempotent?)

; this stuff is crazy... probably need to review this later

; 10.3 Agents
; an agent is a reference type that provides independent asynchronous changes
; like all reference types, an agent represents an `identity`, or a thing whose
; value can change over time

; `send` queues an action on any agent
(def joy (agent []))
(send joy conj "First edition")
(prn @joy) ;; read as (deref joy)

(defn slow-conj [coll item]
  (Thread/sleep 1000)
  (conj coll item))

; using slow-conj, we slow down the `send`, allowing us to see that
; immediately, the REPL prints out the current (empty) value of the
; agent. if you wait and then print out the actual value, it will be
; updated.

; in a transaction, any actions sent are held until the transaction
; commits
(send joy slow-conj "Second Edition")
(prn @joy) ;; read as (deref joy)

; I/O w/ an agent
; serializing access to a resource (file or I/O stream)

(def log-agent (agent 0))

(defn do-log [msg-id message] ; msg-id is the state
  (println msg-id ":" message)
  (inc msg-id))

(defn do-step [channel message]
  (Thread/sleep 500)
  (send-off log-agent do-log (str channel message)))

(defn three-step [channel]
  (do-step channel " ready to begin (step 0)")
  (do-step channel " warming up (step 1)")
  (do-step channel " really getting going now (step 2)")
  (do-step channel " done! (step 3)"))

(defn all-together-now []
  (dothreads! #(three-step "alpha"))
  (dothreads! #(three-step "beta"))
  (dothreads! #(three-step "omega")))

(all-together-now)

; 10.3.4 (asynchronous) Error Handling with agents
; because agent actions run in other threads after the sending thread moves on,
; can't just use try/catch

; FAIL Mode (default)
; action function *must* take at least one argument
; wrong...
(def log-agent (agent 0))
(def test-agent (agent {:test 123}))
(send log-agent (fn [] 2000))
(prn @log-agent)
(prn @test-agent)
(agent-error log-agent) ; ArityException, wrong number of arguments, *Held* by the agent until error is cleared up

; also will throw if another action is sent to it
(send log-agent (fn [_] 2000)) ; Agent is failed, needs restart

; restart
(restart-agent log-agent 2500 :clear-actions true) ; sets log-agent value to 2500

(send-off log-agent do-log "The agent, it lives!")

; CONTINUE Mode
; any action that throws an error is skipped
; can also provide an error handler, which defaults mode to :continue

(def log-agent (agent 0))
(defn handle-log-error [the-agent the-err]
  (println "An action sent to the log-agent threw " the-err))
(set-error-handler! log-agent handle-log-error)
(set-error-mode! log-agent :continue)
(send log-agent (fn [x] (/ x 0)))
(send log-agent (fn [] (0)))
(send-off log-agent do-log "Stayin' alive, stayin' alive...") ; still alive, error handler handles and reports error

; 10.4 Atoms
; like refs (synchronous), but independent/uncoordinated (like agents)

; shareable across threads, example, globally accessible incrementing timer
(def ^:dynamic *time* (atom 0))
(defn tick [] (swap! *time* inc))
(dothreads! tick :threads 100 :times 100)
(prn @*time*) ;; => 1000, safe! across threads!

; atoms in transactions... be careful!
(defn manipulate-memoize [function]
  (let [cache (atom {})]
    (with-meta
      (fn [& args]
        (or (get @cache args)
            (let [ret (apply function args)]
              (swap! cache assoc args ret)
              ret)))
      {:cache cache})))

(def cache-add (manipulate-memoize +))
(cache-add 2 2)

(def slowly (fn [x] (Thread/sleep 1000) x))
(def sometimes-slowly (manipulate-memoize slowly))
(sometimes-slowly 123)
(sometimes-slowly 108)
(sometimes-slowly 456)
(meta sometimes-slowly)
(let [cache (:cache (meta sometimes-slowly))]
  (swap! cache dissoc '(108)))
; good practice to set ref values via the application of a function rather than
; the in-place value setting

; 10.5 locks
; sometimes necessary... good resource, but I don't have enough context to learn
; much from this. someday...

; 10.6 Vars and dynamic binding
; (perhaps) the most commonly used reference type
; - Vars can be named and interned in a namespace (other reference types aren't named... only stored in something with a name, meaning you get the referene object, not the value... to get value, need `@` deref)
; - dynamic Vars can provided thread-local state
(prn *read-eval*) ; *read-eval* is a named var, gives value
(var *read-eval*) ; gives named object itself, starts with `#`
(prn #'*read-eval*) ; `#'` reader does the same, expands to var

; the binding macro - root binding of a var can act as the base of a stack

; prints the current value of the var *read-eval*, either the root or thread-local
; value, whichever is currently in effect
(defn print-read-eval []
  (prn "*read-eval* is currently" *read-eval*))
(print-read-eval)

(defn binding-play []
  (print-read-eval)
  (binding [*read-eval* false] ; push binding, thread-bound
    (print-read-eval))         ; pop binding
  (print-read-eval))
(binding-play)

(def favorite-color :green) ; var itself is returned, #'joy-of-clojure.10-mutation/favorite-color
(var favorite-color)
(prn favorite-color)

